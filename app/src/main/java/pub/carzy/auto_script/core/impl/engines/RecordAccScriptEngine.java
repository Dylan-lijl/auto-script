package pub.carzy.auto_script.core.impl.engines;

import static com.google.android.material.internal.ViewUtils.getOverlay;

import android.accessibilityservice.AccessibilityService;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.IdGenerator;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.config.pojo.SettingKey;
import pub.carzy.auto_script.core.impl.ReplayScriptEngine;
import pub.carzy.auto_script.databinding.WindowMaskViewBinding;
import pub.carzy.auto_script.databinding.WindowRecordFloatingButtonBinding;
import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.entity.KeyEntity;
import pub.carzy.auto_script.entity.MaskConfig;
import pub.carzy.auto_script.entity.MotionEntity;
import pub.carzy.auto_script.entity.PointEntity;
import pub.carzy.auto_script.entity.SettingProxy;
import pub.carzy.auto_script.model.RecordStateModel;
import pub.carzy.auto_script.core.MyAccessibilityService;
import pub.carzy.auto_script.core.data.ReplayModel;
import pub.carzy.auto_script.core.impl.AccScriptEngine;
import pub.carzy.auto_script.core.impl.RecordScriptEngine;
import pub.carzy.auto_script.core.sub.AccessibilityReplay;
import pub.carzy.auto_script.core.sub.Replay;
import pub.carzy.auto_script.core.sub.AbstractReplay;
import pub.carzy.auto_script.ui.GridDrawable;
import pub.carzy.auto_script.utils.MyTypeToken;
import pub.carzy.auto_script.utils.Stopwatch;

/**
 * 无障碍录制引擎
 *
 * @author admin
 */
public class RecordAccScriptEngine extends AccScriptEngine implements RecordScriptEngine {
    private DataWrapper dataWrapper;
    private ViewWrapper viewWrapper;

    @Override
    public void start(Object... args) {
        super.start(args);
        //初始化数据
        if (!this.initialized) {
            synchronized (this) {
                if (!this.initialized) {
                    //数据包装器
                    dataWrapper = new DataWrapper(BeanFactory.getInstance().get(new MyTypeToken<IdGenerator<Long>>() {
                    }), service);
                    WindowRecordFloatingButtonBinding binding = DataBindingUtil.inflate(
                            LayoutInflater.from(service),
                            R.layout.window_record_floating_button,
                            null,
                            false
                    );
                    binding.setRecordState(new RecordStateModel());
                    //视图包装器
                    viewWrapper = new ViewWrapper(getWindowManager(), binding,
                            createBindingParams(binding),
                            DataBindingUtil.inflate(
                                    LayoutInflater.from(service),
                                    R.layout.window_mask_view,
                                    null,
                                    false
                            ));
                    addListenerByView();
                    setUpAccessibleCallback();
                    this.initialized = true;
                }
            }
        }
        for (Object arg : args) {
            if (arg instanceof RecordConfig) {
                RecordConfig config = (RecordConfig) arg;
                viewWrapper.dynamicUpdate = config.dynamicUpdate;
                viewWrapper.autoClose = config.autoClose;
                if (config.floatPoint != null) {
                    viewWrapper.moveView(config.floatPoint.getX(), config.floatPoint.getY());
                }
                if (config.maskConfig != null) {
                    MaskConfig maskConfig = config.maskConfig;
                    GridDrawable gridDrawable = new GridDrawable();
                    gridDrawable.setConfig(getContext(), maskConfig.getColor(), maskConfig.getSize(), maskConfig.getGrid(),
                            maskConfig.getLineWidth(), maskConfig.getGridColor(), maskConfig.getScale(), maskConfig.getFontSize(), maskConfig.getFontColor());
                    FrameLayout root = viewWrapper.maskViewBinding.backgroundRoot;
                    root.getOverlay().add(gridDrawable);
                    root.post(() -> {
                        gridDrawable.setBounds(0, 0, root.getWidth(), root.getHeight());
                    });
                    root.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> gridDrawable.setBounds(0, 0, v.getWidth(), v.getHeight()));
                }
                if (config.autoPlay != null) {
                    viewWrapper.binding.getRecordState().setAutoReplay(config.autoPlay);
                }
            }
        }
        //显示控制按钮
        viewWrapper.showControlView();
    }

    @Override
    protected boolean invokePointCallback() {
        return viewWrapper.dynamicUpdate;
    }

    private void setUpAccessibleCallback() {
        ((MyAccessibilityService) service).setCallback(new MyAccessibilityService.EventCallback() {
            @Override
            public void onActionEvent(String action) {
                //这里暂时不处理
            }

            @Override
            public void onKeyEvent(KeyEvent event) {
                if (viewWrapper.binding.getRecordState().getState() == RecordStateModel.STATE_RECORDING) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        KeyEntity keyEntity = new KeyEntity();
                        keyEntity.setCode(event.getKeyCode());
                        keyEntity.setDownTime(dataWrapper.watcher.getElapsedMillis());
                        dataWrapper.keyMap.put(event.getKeyCode(), keyEntity);
                    } else if (event.getAction() == KeyEvent.ACTION_UP) {
                        KeyEntity keyEntity = dataWrapper.keyMap.remove(event.getKeyCode());
                        if (keyEntity != null) {
                            keyEntity.setUpTime(dataWrapper.watcher.getElapsedMillis());
                            dataWrapper.keyList.add(keyEntity);
                        }
                    }
                }
            }
        });
    }

    private void addListenerByView() {
        WindowRecordFloatingButtonBinding binding = viewWrapper.binding;
        //给蒙层添加手势监听器
        addMaskViewListener();
        //添加长按拖动功能
        addViewTouch(createMoveListener(binding.getRoot(), viewWrapper.bindingParams),
                binding.btnFloatingPause, binding.btnFloatingRecord, binding.btnFloatingRun,
                binding.btnFloatingStop, binding.btnFloatingClose, binding.autoPlayBtn);
        //自动回放切换
        listenAutoReplay();
        //点击录制按钮
        listenRecordBtn();
        //暂停按钮
        listenPauseBtn();
        //恢复按钮
        listenResumeBtn();
        //停止按钮
        listenStopBtn();
        //退出按钮
        listenCloseBtn();
    }

    private void listenCloseBtn() {
        viewWrapper.binding.btnFloatingClose.setOnClickListener(v -> close());
    }

    private void listenStopBtn() {
        viewWrapper.binding.btnFloatingStop.setOnClickListener(v -> {
            if (dataWrapper.replay.getStatus() == AbstractReplay.RUNNING) {
                dataWrapper.replay.stop();
            }
            long millis = dataWrapper.watcher.getElapsedMillis();
            dataWrapper.watcher.stop();
            viewWrapper.binding.getRecordState().setState(RecordStateModel.STATE_IDLE);
            viewWrapper.removeMaskView();
            dataWrapper.saveData();
            jumpToInfo(transformData(dataWrapper.idWorker, millis, 0, dataWrapper.finalMotions, dataWrapper.finalKeyList));
            if (viewWrapper.autoClose) {
                close();
            }
        });
    }

    private void listenResumeBtn() {
        viewWrapper.binding.btnFloatingRun.setOnClickListener(v -> {
            if (dataWrapper.replay.getStatus() == AbstractReplay.RUNNING) {
                dataWrapper.replay.stop();
            }
            viewWrapper.binding.getRecordState().setState(RecordStateModel.STATE_RECORDING);
            viewWrapper.showAllView();
            dataWrapper.watcher.resume();
            dataWrapper.startTime.set(dataWrapper.watcher.getElapsedMillis());
        });
    }

    private void listenPauseBtn() {
        viewWrapper.binding.btnFloatingPause.setOnClickListener(v -> {
            RecordStateModel recordStateModel = viewWrapper.binding.getRecordState();
            recordStateModel.setState(RecordStateModel.STATE_PAUSED);
            viewWrapper.removeMaskView();
            dataWrapper.watcher.pause();
            //迁移数据
            dataWrapper.saveData();
            //存入临时变量
            List<MotionEntity> tmpMotions = new ArrayList<>(dataWrapper.motionList);
            List<KeyEntity> tmpKeys = new ArrayList<>(dataWrapper.keyList);
            //清空缓存数据
            dataWrapper.clearTempData();
            //自动回放
            if (recordStateModel.isAutoReplay() && (!tmpMotions.isEmpty() || !tmpKeys.isEmpty())) {
                ScriptVoEntity entity = transformData(dataWrapper.idWorker, -1, dataWrapper.startTime.get(), tmpMotions, tmpKeys);
                ReplayModel model = ReplayModel.create(entity.getRoot(), entity.getActions(), entity.getPoints());
                if (model != null) {
                    //进行回放
                    dataWrapper.replay.setModel(model);
                    dataWrapper.replay.setRepeatCount(1);
                    dataWrapper.replay.clearCallback();
                    dataWrapper.replay.addCallback(new Replay.ResultListener() {
                        @Override
                        public void stop(int code, String message, Exception e) {
                            if (code == Replay.ResultListener.SUCCESS) {
                                //清空数据
                                dataWrapper.replay.clear();
                            }
                        }
                    });
                    dataWrapper.replay.start();
                }
            }
        });
    }

    private void listenRecordBtn() {
        viewWrapper.binding.btnFloatingRecord.setOnClickListener(v -> {
            viewWrapper.binding.getRecordState().setState(RecordStateModel.STATE_RECORDING);
            dataWrapper.clear();
            //添加蒙层和控制组件
            viewWrapper.showAllView();
            dataWrapper.watcher.resetAndStart();
            //重置开始时间
            dataWrapper.startTime.set(dataWrapper.watcher.getElapsedMillis());
        });
    }

    private void listenAutoReplay() {
        viewWrapper.binding.autoPlayBtn.setOnClickListener(e -> {
            viewWrapper.binding.getRecordState().setAutoReplay(!viewWrapper.binding.getRecordState().isAutoReplay());
            Toast.makeText(service, service.getString(viewWrapper.binding.getRecordState().isAutoReplay() ? R.string.auto_play_on : R.string.auto_play_off), Toast.LENGTH_SHORT).show();
        });
    }

    private void addMaskViewListener() {
        viewWrapper.maskViewBinding.getRoot().setOnTouchListener((v, event) -> {
            if (viewWrapper.tint) {
                v.performClick();
            }
            if (viewWrapper.binding.getRecordState().getState() != RecordStateModel.STATE_RECORDING) {
                return true;
            }
            int action = event.getAction();
            Integer maskType = getAction(action);
            if (maskType == MotionEvent.ACTION_DOWN || maskType == MotionEvent.ACTION_POINTER_DOWN) {
                //按下事件就记录
                int index = getPointIndex(action);
                MotionEntity entity = new MotionEntity();
                dataWrapper.motionList.add(entity);
                entity.setIndex(index);
                entity.setDownTime(dataWrapper.watcher.getElapsedMillis());
                dataWrapper.motionMap.put(index, entity);
                entity.getPoints().add(new PointEntity(getEventRawX(event), getEventRawY(event), dataWrapper.watcher.getElapsedMillis()));
            } else if (maskType == MotionEvent.ACTION_UP || maskType == MotionEvent.ACTION_POINTER_UP) {
                //抬起事件就保存
                int index = getPointIndex(action);
                MotionEntity entity = dataWrapper.motionMap.remove(index);
                if (entity == null) {
                    return true;
                }
                entity.getPoints().add(new PointEntity(getEventRawX(event), getEventRawY(event), dataWrapper.watcher.getElapsedMillis()));
            } else if (maskType == MotionEvent.ACTION_MOVE) {
                //记录触点的x,y坐标
                for (int i = 0; i < event.getPointerCount(); i++) {
                    MotionEntity entity = dataWrapper.motionMap.get(i);
                    if (entity == null) {
                        continue;
                    }
                    event.getX(i);
                    entity.getPoints().add(new PointEntity(getEventRawX(event, i), getEventRawY(event, i), dataWrapper.watcher.getElapsedMillis()));
                }
            } else if (maskType == MotionEvent.ACTION_CANCEL) {
                int index = getPointIndex(action);
                MotionEntity entity = dataWrapper.motionMap.remove(index);
                if (entity == null) {
                    return true;
                }
                entity.getPoints().add(new PointEntity(getEventRawX(event), getEventRawY(event), dataWrapper.watcher.getElapsedMillis()));
            }
            return true;
        });
    }

    @Override
    public void close() {
        reset();
        if (viewWrapper != null) {
            viewWrapper.removeControlView();
            viewWrapper.removeMaskView();
        }
        super.close();
    }

    @Override
    public void reset() {
        if (dataWrapper != null) {
            dataWrapper.reset();
        }
        if (viewWrapper != null) {
            viewWrapper.reset();
        }
    }

    private static class DataWrapper {
        final List<MotionEntity> finalMotions;
        final List<KeyEntity> finalKeyList;
        final List<MotionEntity> motionList;
        final Map<Integer, MotionEntity> motionMap;
        final List<KeyEntity> keyList;
        final Map<Integer, KeyEntity> keyMap;
        final IdGenerator<Long> idWorker;
        final Stopwatch watcher;
        final Replay replay;
        final AtomicLong startTime;

        public DataWrapper(IdGenerator<Long> idWorker, AccessibilityService service) {
            this.idWorker = idWorker;
            finalMotions = new ArrayList<>();
            finalKeyList = new ArrayList<>();
            motionList = new ArrayList<>();
            motionMap = new HashMap<>();
            keyList = new ArrayList<>();
            keyMap = new HashMap<>();
            watcher = new Stopwatch();
            startTime = new AtomicLong(-1);
            replay = new AccessibilityReplay(service);
        }

        public void clear() {
            finalMotions.clear();
            finalKeyList.clear();
            motionList.clear();
            motionMap.clear();
            keyList.clear();
            keyMap.clear();
        }

        public void saveData() {
            finalKeyList.addAll(keyList);
            finalMotions.addAll(motionList);
        }

        public void clearTempData() {
            motionList.clear();
            keyList.clear();
            motionMap.clear();
            keyMap.clear();
        }

        public void reset() {
            clear();
            startTime.set(-1);
            replay.stop();
            watcher.reset();
        }
    }

    private class ViewWrapper {
        final WindowRecordFloatingButtonBinding binding;
        final WindowManager.LayoutParams bindingParams;
        final WindowMaskViewBinding maskViewBinding;
        final WindowManager.LayoutParams maskViewParams;
        /**
         * 消除警告
         */
        boolean tint = false;
        final WindowManager windowManager;
        boolean dynamicUpdate;
        boolean autoClose;

        public ViewWrapper(WindowManager windowManager, WindowRecordFloatingButtonBinding binding, WindowManager.LayoutParams bindingParams, WindowMaskViewBinding maskViewBinding) {
            this.windowManager = windowManager;
            this.binding = binding;
            this.bindingParams = bindingParams;
            this.maskViewBinding = maskViewBinding;
            maskViewParams = createMaskLayoutParams();
        }

        public void showAllView() {
            if (binding.getRoot().isAttachedToWindow()) {
                windowManager.removeView(binding.getRoot());
            }
            if (!maskViewBinding.getRoot().isAttachedToWindow()) {
                windowManager.addView(maskViewBinding.getRoot(), maskViewParams);
            }
            windowManager.addView(binding.getRoot(), bindingParams);
        }

        public void removeMaskView() {
            if (maskViewBinding.getRoot().isAttachedToWindow()) {
                windowManager.removeView(maskViewBinding.getRoot());
            }
        }

        public void showControlView() {
            if (!binding.getRoot().isAttachedToWindow()) {
                windowManager.addView(binding.getRoot(), bindingParams);
            }
        }

        public void removeControlView() {
            if (binding.getRoot().isAttachedToWindow()) {
                windowManager.removeView(binding.getRoot());
            }
        }

        public void addMaskView() {
            if (!maskViewBinding.getRoot().isAttachedToWindow()) {
                windowManager.addView(maskViewBinding.getRoot(), maskViewParams);
            }
        }

        public void reset() {
            removeMaskView();
            binding.getRecordState().setState(RecordStateModel.STATE_IDLE);
        }

        public void moveView(int x, int y) {
            bindingParams.x = x;
            bindingParams.y = y;
            if (binding.getRoot().isAttachedToWindow()) {
                removeControlView();
                showControlView();
            }
        }
    }

    private WindowManager.LayoutParams createMaskLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                getOverlayFlag(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        // 默认居中
        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = 0;
        return params;
    }

}
