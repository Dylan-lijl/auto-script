package pub.carzy.auto_script.service.impl;


import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.MacroInfoActivity;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.IdGenerator;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.databinding.WindowRecordFloatingButtonBinding;
import pub.carzy.auto_script.databinding.WindowMaskViewBinding;
import pub.carzy.auto_script.db.entity.ScriptActionEntity;
import pub.carzy.auto_script.db.entity.ScriptEntity;
import pub.carzy.auto_script.db.entity.ScriptPointEntity;
import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.entity.KeyEntity;
import pub.carzy.auto_script.entity.MotionEntity;
import pub.carzy.auto_script.entity.PointEntity;
import pub.carzy.auto_script.model.RecordStateModel;
import pub.carzy.auto_script.service.BasicAction;
import pub.carzy.auto_script.service.data.ReplayModel;
import pub.carzy.auto_script.service.dto.CloseParam;
import pub.carzy.auto_script.service.dto.OpenParam;
import pub.carzy.auto_script.service.dto.UpdateParam;
import pub.carzy.auto_script.service.sub.SimpleReplay;
import pub.carzy.auto_script.utils.BeanHandler;
import pub.carzy.auto_script.utils.MixedUtil;
import pub.carzy.auto_script.utils.Stopwatch;
import pub.carzy.auto_script.utils.MyTypeToken;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class RecordScriptAction extends BasicAction {
    private WindowMaskViewBinding mask;
    private RecordStateModel recordStateModel;
    private WindowManager.LayoutParams maskParams;
    private final List<MotionEntity> finalMotions = new ArrayList<>();
    private final List<KeyEntity> finalKeyList = new ArrayList<>();
    private List<MotionEntity> motionList;
    private Map<Integer, MotionEntity> motionMap;
    private List<KeyEntity> keyList;
    private Map<Integer, KeyEntity> keyMap;
    private IdGenerator<Long> idWorker;
    private final ReentrantLock lock = new ReentrantLock();
    private boolean initialized = false;
    private WindowRecordFloatingButtonBinding binding;
    private WindowManager.LayoutParams bindingParams;
    public static final String ACTION_KEY = "record";
    private final Stopwatch watch = new Stopwatch();
    private final AtomicLong startTime = new AtomicLong(0);
    private Setting setting;
    private SimpleReplay replay;

    @Override
    public String key() {
        return ACTION_KEY;
    }

    @Override
    public boolean open(OpenParam param) {
        lock.lock();
        try {
            if (!initialized) {
                //注册上去
                BeanFactory.getInstance().register(this);
                idWorker = BeanFactory.getInstance().get(new MyTypeToken<IdGenerator<Long>>() {
                });
                setting = BeanFactory.getInstance().get(Setting.class);
                motionList = new ArrayList<>();
                motionMap = new HashMap<>();
                keyMap = new HashMap<>();
                keyList = new ArrayList<>();
                binding = DataBindingUtil.inflate(
                        LayoutInflater.from(service),
                        R.layout.window_record_floating_button,
                        null,
                        false
                );
                binding.setRecordState(recordStateModel = new RecordStateModel());
                recordStateModel.setAutoReplay(setting.getAutoPlay());
                bindingParams = createBindingParams(binding);
                mask = DataBindingUtil.inflate(
                        LayoutInflater.from(service),
                        R.layout.window_mask_view,
                        null,
                        false
                );
                maskParams = createMaskLayoutParams();
                processView();
                replay = new SimpleReplay(service);
                initialized = true;
            }
        } catch (Exception e) {
            return false;
        } finally {
            lock.unlock();
        }
        //后期再做这个录制问题,这种可能只有root模式下能使用
//        if (showWindow) {
        //打开窗口
        addView(binding, bindingParams);
//        }
//        if (autoRunDelay==0){
//
//        }
        return true;
    }

    private final boolean tint = false;

    private void processView() {
        //
        View root = mask.getRoot();
        root.setOnTouchListener(createMaskMotionEventListener());
        addViewTouch(createMoveListener(binding.getRoot(), bindingParams), binding.btnFloatingPause, binding.btnFloatingRecord, binding.btnFloatingRun, binding.btnFloatingStop);
        binding.autoPlayBtn.setOnClickListener(e -> {
            recordStateModel.setAutoReplay(!recordStateModel.isAutoReplay());
            Toast.makeText(service, service.getString(recordStateModel.isAutoReplay() ? R.string.auto_play_on : R.string.auto_play_off), Toast.LENGTH_SHORT).show();
        });
        binding.btnFloatingRecord.setOnClickListener(v -> {
            recordStateModel.setState(RecordStateModel.STATE_RECORDING);
            motionList.clear();
            motionMap.clear();
            keyList.clear();
            keyMap.clear();
            finalKeyList.clear();
            finalMotions.clear();
            addView(mask, maskParams);
            reAddView(binding, bindingParams);
            watch.reset();
            watch.start();
            startTime.set(watch.getElapsedMillis());
        });
        binding.btnFloatingPause.setOnClickListener(v -> {
            recordStateModel.setState(RecordStateModel.STATE_PAUSED);
            removeView(mask);
            watch.pause();
            //迁移数据
            finalMotions.addAll(motionList);
            finalKeyList.addAll(keyList);
            //存入临时变量
            List<MotionEntity> tmpMotions = new ArrayList<>(motionList);
            List<KeyEntity> tmpKeys = new ArrayList<>(keyList);
            motionList.clear();
            keyList.clear();
            motionMap.clear();
            keyMap.clear();
            if (recordStateModel.isAutoReplay() && (!tmpMotions.isEmpty() || !tmpKeys.isEmpty())) {
                ReplayModel model = createReplayActionModel(tmpMotions, tmpKeys);
                if (model != null) {
                    //进行回放
                    replay.setModel(model);
                    replay.setRepeatCount(1);
                    replay.clearCallback();
                    replay.addCallback(new SimpleReplay.ResultListener() {
                        @Override
                        public void stop(int code, String message, Exception e) {
                            if (code == SimpleReplay.ResultListener.SUCCESS) {
                                //停止了就唤醒 暂时不要,不然用户会停顿,还不如用户手动去点恢复
                                /*ThreadUtil.runOnUi(() -> {
                                    resumeTask();
                                });*/
                                //清空数据
                                replay.clear();
                            }
                        }
                    });
                    replay.start();
                }
            }
        });
        binding.btnFloatingRun.setOnClickListener(v -> {
            if (replay.getStatus() == SimpleReplay.RUNNING) {
                replay.stop();
            }
            resumeTask();
        });
        binding.btnFloatingStop.setOnClickListener(v -> {
            if (replay.getStatus() == SimpleReplay.RUNNING) {
                replay.stop();
            }
            long millis = watch.getElapsedMillis();
            watch.stop();
            recordStateModel.setState(RecordStateModel.STATE_IDLE);
            //这里需要打开MacroListActivity将motionList传递过去,然后清空数据
            Intent intent = new Intent(service, MacroInfoActivity.class);
            // **重要:** 从非 Activity 上下文 (Service) 启动 Activity 必须添加此 Flag
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //转换之前需要先将临时的添加到全局
            finalMotions.addAll(motionList);
            finalKeyList.addAll(keyList);
            // 3. 传递 motionList
            intent.putExtra("data", transformData(millis, 0, finalMotions, finalKeyList));
            intent.putExtra("add", true);
            // 4. 启动 Activity
            service.startActivity(intent);
            service.close(ACTION_KEY, null);
        });
        binding.btnFloatingClose.setOnClickListener(v -> {
            if (replay.getStatus() == SimpleReplay.RUNNING) {
                replay.stop();
            }
            watch.stop();
            service.close(ACTION_KEY, null);
        });
    }

    private void resumeTask() {
        recordStateModel.setState(RecordStateModel.STATE_RECORDING);
        addView(mask, maskParams);
        reAddView(binding, bindingParams);
        watch.resume();
        startTime.set(watch.getElapsedMillis());
    }

    private ReplayModel createReplayActionModel(List<MotionEntity> tmpMotions, List<KeyEntity> tmpKeys) {
        ScriptVoEntity entity = transformData(-1, startTime.get(), tmpMotions, tmpKeys);
        return ReplayModel.create(entity.getRoot(), entity.getActions(), entity.getPoints());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        super.onAccessibilityEvent(event);
        /*if (recordStateModel.getState() != RecordStateModel.STATE_RECORDING) {
            return;
        }
        Log.d(this.getClass().getCanonicalName(), "onAccessibilityEvent: " + event);*/
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (recordStateModel.getState() == RecordStateModel.STATE_RECORDING) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                KeyEntity keyEntity = new KeyEntity();
                keyEntity.setCode(event.getKeyCode());
                keyEntity.setDownTime(watch.getElapsedMillis());
                keyMap.put(event.getKeyCode(), keyEntity);
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                KeyEntity keyEntity = keyMap.remove(event.getKeyCode());
                if (keyEntity != null) {
                    keyEntity.setUpTime(watch.getElapsedMillis());
                    keyList.add(keyEntity);
                }
            }
        }
        return super.onKeyEvent(event);
    }

    private ScriptVoEntity transformData(long millis, long align, List<MotionEntity> motions, List<KeyEntity> keys) {
        ScriptVoEntity entity = new ScriptVoEntity();
        ScriptEntity root = new ScriptEntity();
        entity.setRoot(root);
        root.setId(idWorker.nextId());
        root.setName(service.getString(R.string.untitled));
        root.setActionCount(motions.size() + keys.size());
        root.setDelayStart(0L);
        long maxTime = 0L;
        //手势
        for (MotionEntity motionEntity : motions) {
            ScriptActionEntity actionEntity = BeanHandler.copy(motionEntity, ScriptActionEntity.class);
            actionEntity.setType(ScriptActionEntity.GESTURE);
            actionEntity.setId(idWorker.nextId());
            actionEntity.setScriptId(root.getId());
            actionEntity.setPointCount(motionEntity.getPoints().size());
            actionEntity.setStartTime(motionEntity.getDownTime() - align);
            actionEntity.setDuration(0L);
            int size = motionEntity.getPoints().size();
            for (int i = 0; i < size; i++) {
                PointEntity point = motionEntity.getPoints().get(i);
                ScriptPointEntity pointEntity = BeanHandler.copy(point, ScriptPointEntity.class);
                pointEntity.setId(idWorker.nextId());
                pointEntity.setActionId(actionEntity.getId());
                pointEntity.setScriptId(root.getId());
                //按照索引排序
                pointEntity.setOrder((float) i);
                //默认1ms
                pointEntity.setDeltaTime(1L);
                if (i + 1 < size) {
                    //不是最后一个点时-->计算间隔时长:当前点的时间减去后一个点的时间,最小值为1ms
                    pointEntity.setDeltaTime(Math.max(motionEntity.getPoints().get(i + 1).getTime() - point.getTime(), 1L));
                }
                //累加时长
                actionEntity.setDuration(actionEntity.getDuration() + pointEntity.getDeltaTime());
                entity.getPoints().add(pointEntity);
            }
            //等于0说明没有点信息,一般不可能出现这种情况,直接忽略掉
            if (actionEntity.getDuration() == 0L) {
                continue;
            }
            entity.getActions().add(actionEntity);
            maxTime = Math.max(maxTime, actionEntity.getStartTime() + actionEntity.getDuration());
        }
        //键
        for (KeyEntity keyEntity : keys) {
            if (keyEntity.getUpTime() == null) {
                keyEntity.setUpTime(keyEntity.getDownTime());
            }
            ScriptActionEntity actionEntity = BeanHandler.copy(keyEntity, ScriptActionEntity.class);
            actionEntity.setType(ScriptActionEntity.KEY_EVENT);
            actionEntity.setStartTime(keyEntity.getDownTime() - align);
            actionEntity.setDuration(Math.max(keyEntity.getUpTime() - keyEntity.getDownTime(), 1L));
            actionEntity.setId(idWorker.nextId());
            actionEntity.setScriptId(root.getId());
            actionEntity.setCode(keyEntity.getCode());
            entity.getActions().add(actionEntity);
            maxTime = Math.max(maxTime, actionEntity.getStartTime() + actionEntity.getDuration());
        }
        root.setDelayEnd(Math.max(millis - maxTime, 0));
        entity.getRoot().setTotalDuration(maxTime);
        return entity;
    }

    private View.OnTouchListener createMaskMotionEventListener() {
        return (v, event) -> {
            //消除idea警告
            if (tint) {
                v.performClick();
            }
            if (recordStateModel.getState() != RecordStateModel.STATE_RECORDING) {
                return true;
            }
            int action = event.getAction();
            Integer maskType = getAction(action);
            if (maskType == MotionEvent.ACTION_DOWN || maskType == MotionEvent.ACTION_POINTER_DOWN) {
                //按下事件就记录
                int index = getPointIndex(action);
                MotionEntity entity = new MotionEntity();
                motionList.add(entity);
                entity.setIndex(index);
                entity.setDownTime(watch.getElapsedMillis());
                motionMap.put(index, entity);
                entity.getPoints().add(new PointEntity(getEventRawX(event), getEventRawY(event), watch.getElapsedMillis()));
            } else if (maskType == MotionEvent.ACTION_UP || maskType == MotionEvent.ACTION_POINTER_UP) {
                //抬起事件就保存
                int index = getPointIndex(action);
                MotionEntity entity = motionMap.remove(index);
                if (entity == null) {
                    return true;
                }
                entity.getPoints().add(new PointEntity(getEventRawX(event), getEventRawY(event), watch.getElapsedMillis()));
            } else if (maskType == MotionEvent.ACTION_MOVE) {
                //记录触点的x,y坐标
                for (int i = 0; i < event.getPointerCount(); i++) {
                    MotionEntity entity = motionMap.get(i);
                    if (entity == null) {
                        continue;
                    }
                    event.getX(i);
                    entity.getPoints().add(new PointEntity(getEventRawX(event, i), getEventRawY(event, i), watch.getElapsedMillis()));
                }
            } else if (maskType == MotionEvent.ACTION_CANCEL) {
                int index = getPointIndex(action);
                MotionEntity entity = motionMap.remove(index);
                if (entity == null) {
                    return true;
                }
                entity.getPoints().add(new PointEntity(getEventRawX(event), getEventRawY(event), watch.getElapsedMillis()));
            }
            return true;
        };
    }

    private Float getEventRawY(MotionEvent event) {
        return getEventRawY(event, -1);
    }

    /**
     * 获取绝对y的坐标
     *
     * @param event 事件
     * @param index 索引
     * @return y的绝对坐标
     */
    private Float getEventRawY(MotionEvent event, int index) {
        if (index < 0) {
            return event.getRawY();
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? event.getRawY(index) : event.getY(index) + event.getRawY() - event.getY();
    }

    private Float getEventRawX(MotionEvent event) {
        return getEventRawX(event, -1);
    }

    /**
     * 获取绝对x的坐标
     *
     * @param event 事件
     * @param index 索引
     * @return x的绝对坐标
     */
    private Float getEventRawX(MotionEvent event, int index) {
        if (index < 0) {
            return event.getRawX();
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? event.getRawX(index) : event.getX(index) + event.getRawX() - event.getX();
    }

    private int getPointIndex(Integer action) {
        return action == MotionEvent.ACTION_DOWN ? 0 : ((action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);
    }

    public static Integer getAction(Integer action) {
        if (action == null) {
            return null;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return MotionEvent.ACTION_DOWN;
            case MotionEvent.ACTION_UP:
                return MotionEvent.ACTION_UP;
            case MotionEvent.ACTION_CANCEL:
                return MotionEvent.ACTION_CANCEL;
            case MotionEvent.ACTION_OUTSIDE:
                return MotionEvent.ACTION_OUTSIDE;
            case MotionEvent.ACTION_MOVE:
                return MotionEvent.ACTION_MOVE;
            case MotionEvent.ACTION_HOVER_MOVE:
                return MotionEvent.ACTION_HOVER_MOVE;
            case MotionEvent.ACTION_SCROLL:
                return MotionEvent.ACTION_SCROLL;
            case MotionEvent.ACTION_HOVER_ENTER:
                return MotionEvent.ACTION_HOVER_ENTER;
            case MotionEvent.ACTION_HOVER_EXIT:
                return MotionEvent.ACTION_HOVER_EXIT;
            case MotionEvent.ACTION_BUTTON_PRESS:
                return MotionEvent.ACTION_BUTTON_PRESS;
            case MotionEvent.ACTION_BUTTON_RELEASE:
                return MotionEvent.ACTION_BUTTON_RELEASE;
        }
        return action & MotionEvent.ACTION_MASK;
    }

    @Override
    public boolean close(CloseParam param) {
        try {
            removeView(mask);
            recordStateModel.setState(RecordStateModel.STATE_IDLE);
            removeView(binding);
            //清理数据
            motionList.clear();
            motionMap.clear();
            keyList.clear();
            keyMap.clear();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean update(UpdateParam param) {
        return false;
    }

    private static WindowManager.LayoutParams createMaskLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        // 默认居中
        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = 0;
        return params;
    }
}
