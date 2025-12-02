package pub.carzy.auto_script.service.impl;


import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.databinding.DataBindingUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.activities.MacroInfoActivity;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.ControllerCallback;
import pub.carzy.auto_script.config.IdGenerator;
import pub.carzy.auto_script.databinding.FloatingButtonBinding;
import pub.carzy.auto_script.databinding.MaskViewBinding;
import pub.carzy.auto_script.db.ScriptActionEntity;
import pub.carzy.auto_script.db.ScriptEntity;
import pub.carzy.auto_script.db.ScriptPointEntity;
import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.entity.MotionEntity;
import pub.carzy.auto_script.entity.PointEntity;
import pub.carzy.auto_script.model.RecordStateModel;
import pub.carzy.auto_script.service.BasicAction;
import pub.carzy.auto_script.service.dto.CloseParam;
import pub.carzy.auto_script.service.dto.OpenParam;
import pub.carzy.auto_script.utils.BeanHandler;
import pub.carzy.auto_script.utils.Stopwatch;
import pub.carzy.auto_script.utils.ThreadUtil;
import pub.carzy.auto_script.utils.TypeToken;

/**
 * @author admin
 */
public class RecordScriptAction extends BasicAction {
    private MaskViewBinding mask;
    private RecordStateModel recordStateModel;
    private WindowManager.LayoutParams maskParams;

    private List<MotionEntity> motionList;
    private Map<Integer, MotionEntity> motionMap;
    private IdGenerator<Long> idWorker;
    private ReentrantLock lock = new ReentrantLock();
    private boolean initialized = false;
    private FloatingButtonBinding binding;
    private WindowManager.LayoutParams bindingParams;
    public static final String ACTION_KEY = "record";
    private final Stopwatch watch = new Stopwatch();

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
                idWorker = BeanFactory.getInstance().get(new TypeToken<IdGenerator<Long>>() {
                });
                motionList = new ArrayList<>();
                motionMap = new HashMap<>();
                binding = DataBindingUtil.inflate(
                        LayoutInflater.from(service),
                        R.layout.floating_button,
                        null,
                        false
                );
                binding.setRecordState(recordStateModel = new RecordStateModel());
                bindingParams = createBindingParams(binding);
                mask = DataBindingUtil.inflate(
                        LayoutInflater.from(service),
                        R.layout.mask_view,
                        null,
                        false
                );
                maskParams = createMaskLayoutParams();
                processView();
                initialized = true;
            }
        } catch (Exception e) {
            return false;
        } finally {
            lock.unlock();
        }
        //打开窗口
        addView(binding, bindingParams);
        return true;
    }

    private final boolean tint = false;

    private void processView() {
        //
        View root = mask.getRoot();
        root.setOnTouchListener(createMaskMotionEventListener());
        addViewTouch(createMoveListener(binding.getRoot(), bindingParams), binding.btnFloatingPause, binding.btnFloatingRecord, binding.btnFloatingRun, binding.btnFloatingStop);
        binding.btnFloatingRecord.setOnClickListener(v -> {
            recordStateModel.setState(RecordStateModel.STATE_RECORDING);
            motionList.clear();
            addView(mask, maskParams);
            reAddView(binding, bindingParams);
            watch.reset();
            watch.start();
        });
        binding.btnFloatingPause.setOnClickListener(v -> {
            recordStateModel.setState(RecordStateModel.STATE_PAUSED);
            removeView(mask);
            watch.pause();
        });
        binding.btnFloatingRun.setOnClickListener(v -> {
            recordStateModel.setState(RecordStateModel.STATE_RECORDING);
            addView(mask, maskParams);
            reAddView(binding, bindingParams);
            watch.resume();
        });
        binding.btnFloatingStop.setOnClickListener(v -> {
            watch.stop();
            recordStateModel.setState(RecordStateModel.STATE_IDLE);
            //这里需要打开MacroListActivity将motionList传递过去,然后清空数据
            Intent intent = new Intent(service, MacroInfoActivity.class);
            // **重要:** 从非 Activity 上下文 (Service) 启动 Activity 必须添加此 Flag
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // 3. 传递 motionList
            intent.putExtra("data", transformData(motionList));
            // 4. 启动 Activity
            service.startActivity(intent);
            // 5. 清空 motionList 数据
            if (motionList != null) {
                motionList.clear();
                // 建议在这里加上日志或调试点，确认数据被清空
            }
            service.close(ACTION_KEY, null);
        });
        binding.btnFloatingClose.setOnClickListener(v -> {
            watch.stop();
            service.close(ACTION_KEY, null);
        });
    }

    private ScriptVoEntity transformData(List<MotionEntity> motionList) {
        ScriptVoEntity entity = new ScriptVoEntity();
        ScriptEntity root = new ScriptEntity();
        entity.setRoot(root);
        root.setId(idWorker.nextId());
        root.setName("未命名");
        root.setCount(motionList.size());
        long maxTime = 0L;
        for (MotionEntity motionEntity : motionList) {
            if (motionEntity.getUpTime() != null) {
                maxTime = Math.max(maxTime, motionEntity.getUpTime());
            }
            ScriptActionEntity script = BeanHandler.copy(motionEntity, ScriptActionEntity.class);
            script.setId(idWorker.nextId());
            script.setParentId(root.getId());
            script.setCount(motionEntity.getPoints().size());
            script.setMaxTime(script.getUpTime() != null ? script.getUpTime() : script.getDownTime());
            for (PointEntity point : motionEntity.getPoints()) {
                ScriptPointEntity pointEntity = BeanHandler.copy(point, ScriptPointEntity.class);
                pointEntity.setId(idWorker.nextId());
                pointEntity.setParentId(script.getId());
                if (point.getTime() > script.getMaxTime()) {
                    script.setMaxTime(point.getTime());
                }
                maxTime = Math.max(point.getTime(), maxTime);
                entity.getPoints().add(pointEntity);
            }
            if (script.getUpTime() == null) {
                script.setUpTime(script.getMaxTime());
            }
            entity.getActions().add(script);
        }
        entity.getRoot().setMaxTime(maxTime);
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
                entity.setEventTime(event.getEventTime());
                entity.setDownTime(watch.getElapsedMillis());
                motionMap.put(index, entity);
                entity.getPoints().add(new PointEntity(event.getX(), event.getY(), watch.getElapsedMillis(), event.getToolType(index)));
            } else if (maskType == MotionEvent.ACTION_UP || maskType == MotionEvent.ACTION_POINTER_UP) {
                //抬起事件就保存
                int index = getPointIndex(action);
                MotionEntity entity = motionMap.remove(index);
                if (entity == null) {
                    return true;
                }
                entity.setUpTime(watch.getElapsedMillis());
                entity.getPoints().add(new PointEntity(event.getX(), event.getY(), watch.getElapsedMillis(), event.getToolType(index)));
            } else if (maskType == MotionEvent.ACTION_MOVE) {
                //记录触点的x,y坐标
                for (int i = 0; i < event.getPointerCount(); i++) {
                    MotionEntity entity = motionMap.get(i);
                    if (entity == null) {
                        continue;
                    }
                    event.getX(i);
                    entity.getPoints().add(new PointEntity(event.getX(i), event.getY(i), watch.getElapsedMillis(), event.getToolType(i)));
                }
            } else if (maskType == MotionEvent.ACTION_CANCEL) {
                //这里如何处理
            }
            return true;
        };
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
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static WindowManager.LayoutParams createMaskLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        // 默认居中
        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = 0;
        return params;
    }
}
