package pub.carzy.auto_script.core.impl.engines;

import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.databinding.DataBindingUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanContainer;
import pub.carzy.auto_script.config.IdGenerator;
import pub.carzy.auto_script.databinding.WindowRecordFloatingButtonBinding;
import pub.carzy.auto_script.entity.EventDevice;
import pub.carzy.auto_script.entity.KeyEntity;
import pub.carzy.auto_script.entity.MotionEntity;
import pub.carzy.auto_script.ex.DeviceNotRootedException;
import pub.carzy.auto_script.ex.ProcessReadOrWriteIOException;
import pub.carzy.auto_script.model.RecordStateModel;
import pub.carzy.auto_script.core.impl.RecordScriptEngine;
import pub.carzy.auto_script.core.impl.RootScriptEngine;
import pub.carzy.auto_script.core.sub.RecorderLifeCycle;
import pub.carzy.auto_script.utils.EventDeviceUtil;
import pub.carzy.auto_script.core.sub.GestureRecorder;
import pub.carzy.auto_script.core.sub.KeyRecorder;
import pub.carzy.auto_script.utils.InputConstants;
import pub.carzy.auto_script.utils.MyTypeToken;
import pub.carzy.auto_script.utils.Shell;
import pub.carzy.auto_script.utils.Stopwatch;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * root录制引擎
 * @author admin
 */
public class RecordRootScriptEngine extends RootScriptEngine implements RecordScriptEngine {

    private EventDevice gestureDevice;
    private EventDevice keyDevice;

    private DataWrapper dataWrapper;
    private ViewWrapper viewWrapper;

    @Override
    protected void doSubInit(ResultCallback callback) {
        try {
            //先获取设备列表
            List<EventDevice> list = EventDeviceUtil.parse(Shell.getEventList(cmdProcess));
            //手势设备
            gestureDevice = EventDeviceUtil.findGestureActuator(list);
            //按键设备
            keyDevice = EventDeviceUtil.findKeyActuator(list);
            if (gestureDevice == null && keyDevice == null) {
                callback.onFail(ResultCallback.ROOT | ResultCallback.MISSING, 0);
            }
        } catch (DeviceNotRootedException | ProcessReadOrWriteIOException e) {
            callback.onFail(ResultCallback.EXCEPTION | ResultCallback.ROOT, e);
        }
    }

    @Override
    public void start(Object... args) {
        super.start(args);
        if (!this.initialized) {
            synchronized (this) {
                if (!this.initialized) {
                    dataWrapper = new DataWrapper(BeanContainer.getInstance().get(new MyTypeToken<IdGenerator<Long>>() {
                    }));
                    WindowRecordFloatingButtonBinding binding = DataBindingUtil.inflate(
                            LayoutInflater.from(getContext()),
                            R.layout.window_record_floating_button,
                            null,
                            false
                    );
                    binding.setRecordState(new RecordStateModel());
                    binding.getRecordState().setRoot(true);
                    viewWrapper = new ViewWrapper(getWindowManager(), binding, createBindingParams(binding));
                    addListenerByView();
                    this.initialized = true;
                }
            }
        }
        for (Object arg : args) {
            if (arg instanceof RecordConfig) {
                RecordConfig config = (RecordConfig) arg;
                viewWrapper.ignoreFloatingScript = config.ignoreFloatingScript;
                viewWrapper.autoClose = config.autoClose;
            }
        }
        viewWrapper.showView();
    }

    private void addListenerByView() {
        WindowRecordFloatingButtonBinding binding = viewWrapper.binding;
        //添加长按拖动功能
        addViewTouch(createMoveListener(binding.getRoot(), viewWrapper.params, (x, y) -> {
                    if (binding.getRecordState().getState() != RecordStateModel.STATE_RECORDING) {
                        return;
                    }
                    ThreadUtil.runOnUi(() -> {
                        if (viewWrapper.ignoreFloatingScript) {
                            dataWrapper.removeLastMotion();
                        }
                    }, 50);
                }),
                binding.btnFloatingPause, binding.btnFloatingRecord, binding.btnFloatingRun, binding.btnFloatingStop, binding.btnFloatingClose);
        AtomicInteger state = new AtomicInteger(0);
        binding.btnFloatingRecord.setOnClickListener((e) -> {
            binding.getRecordState().setState(RecordStateModel.STATE_RECORDING);
            dataWrapper.clear();
            dataWrapper.watcher.resetAndStart();
            state.set(0);
            dataWrapper.cycle = createLifeCycle(new RecorderLifeCycle.OnRecordReading() {
                final Set<Integer> set = new HashSet<>();

                @Override
                public void pause(Object... args) {

                }

                @Override
                public void stop(Object... args) {
                    set.add((Integer) args[1]);
                    //具体收到停止
                    if (set.size() == ((Collection<?>) args[0]).size() && state.get() == 0) {
                        state.set(1);
                        jumpToInfo(transformData(dataWrapper.idWorker, dataWrapper.watcher.getElapsedMillis(),
                                0, dataWrapper.getMotionsData(), dataWrapper.getKeysData()));
                        if (viewWrapper.autoClose) {
                            close();
                        }
                    }
                }
            });
            dataWrapper.cycle.start(null, null);
            //重置开始时间
            dataWrapper.startTime.set(dataWrapper.watcher.getElapsedMillis());
        });
        binding.btnFloatingPause.setOnClickListener(e -> {
            binding.getRecordState().setState(RecordStateModel.STATE_PAUSED);
            dataWrapper.watcher.pause();
            dataWrapper.cycle.pause();
        });
        binding.btnFloatingRun.setOnClickListener(v -> {
            viewWrapper.binding.getRecordState().setState(RecordStateModel.STATE_RECORDING);
            dataWrapper.watcher.resume();
            dataWrapper.startTime.set(dataWrapper.watcher.getElapsedMillis());
            dataWrapper.cycle.resume();
        });
        binding.btnFloatingClose.setOnClickListener(v -> close());
        binding.btnFloatingStop.setOnClickListener(v -> {
            //只是通知停止
            dataWrapper.watcher.stop();
            dataWrapper.cycle.stop();
            viewWrapper.binding.getRecordState().setState(RecordStateModel.STATE_IDLE);
            //保险策略
            ThreadUtil.runOnUi(() -> {
                if (state.get() != 0) {
                    return;
                }
                state.set(1);
                jumpToInfo(transformData(dataWrapper.idWorker, dataWrapper.watcher.getElapsedMillis(),
                        0, dataWrapper.getMotionsData(), dataWrapper.getKeysData()));
                if (viewWrapper.autoClose) {
                    close();
                }
            }, 1000);
        });
    }

    private RecorderLifeCycle<Void> createLifeCycle(final RecorderLifeCycle.OnRecordReading r) {
        RecorderLifeCycle<Void> cycle = new RecorderLifeCycle<>() {
            GestureRecorder gestureRecorder;
            KeyRecorder keyRecorder;
            RecorderLifeCycle.OnRecordReading reading;
            final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

            @Override
            public void start(String devicePath, OnRecordListener<Void> listener) {
                List<Integer> types = new ArrayList<>();
                //手势
                if (gestureDevice != null) {
                    types.add(InputConstants.EV_ABS);
                    gestureRecorder = new GestureRecorder(dataWrapper.watcher);
                    gestureRecorder.setReadingBack(new OnRecordReading() {
                        @Override
                        public void pause(Object... args) {
                            if (viewWrapper.ignoreFloatingScript) {
                                dataWrapper.removeLastMotion();
                            }
                            if (reading != null) {
                                reading.pause(types, InputConstants.EV_ABS);
                            }
                        }

                        @Override
                        public void stop(Object... args) {
                            if (viewWrapper.ignoreFloatingScript) {
                                dataWrapper.removeLastMotion();
                            }
                            if (reading != null) {
                                reading.stop(types, InputConstants.EV_ABS);
                            }
                        }
                    });
                    executor.submit(() -> gestureRecorder.start(gestureDevice.getPath(), e -> dataWrapper.addMotion(e)));
                }
                //按键
                if (keyDevice != null) {
                    types.add(InputConstants.EV_KEY);
                    keyRecorder = new KeyRecorder(dataWrapper.watcher);
                    keyRecorder.setReadingBack(new OnRecordReading() {
                        @Override
                        public void pause(Object... args) {
                            if (reading != null) {
                                reading.pause(types, InputConstants.EV_KEY);
                            }
                        }

                        @Override
                        public void stop(Object... args) {
                            if (reading != null) {
                                reading.stop(types, InputConstants.EV_KEY);
                            }
                        }
                    });
                    executor.submit(() -> keyRecorder.start(keyDevice.getPath(), e -> dataWrapper.addKey(e)));
                }
            }

            @Override
            public void stop() {
                //如果还有区分按键方式和控制按钮暂停和停止时就需要调用不同的移除
                if (gestureRecorder != null) {
                    gestureRecorder.stop();
                }
                if (keyRecorder != null) {
                    keyRecorder.stop();
                }
            }

            @Override
            public void clear() {
                if (gestureRecorder != null) {
                    gestureRecorder.clear();
                }
                if (keyRecorder != null) {
                    keyRecorder.clear();
                }
            }

            @Override
            public void pause() {
                if (gestureRecorder != null) {
                    gestureRecorder.pause();
                }
                if (keyRecorder != null) {
                    keyRecorder.pause();
                }
            }

            @Override
            public void resume() {
                if (gestureRecorder != null) {
                    gestureRecorder.resume();
                }
                if (keyRecorder != null) {
                    keyRecorder.resume();
                }
            }

            @Override
            public void destroy() {
                if (gestureRecorder != null) {
                    gestureRecorder.destroy();
                }
                if (keyRecorder != null) {
                    keyRecorder.destroy();
                }
                executor.shutdown();
            }

            @Override
            public void setReadingBack(OnRecordReading readingBack) {
                reading = readingBack;
            }
        };
        cycle.setReadingBack(r);
        return cycle;
    }

    @Override
    public void close() {
        reset();
        if (viewWrapper != null) {
            viewWrapper.removeView();
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

    static class DataWrapper {
        final List<MotionEntity> motions;
        final List<KeyEntity> keys;
        final Stopwatch watcher;
        final AtomicLong startTime;
        final IdGenerator<Long> idWorker;
        RecorderLifeCycle<Void> cycle;

        public DataWrapper(IdGenerator<Long> idWorker) {
            motions = new ArrayList<>();
            keys = new ArrayList<>();
            watcher = new Stopwatch();
            startTime = new AtomicLong(-1);
            this.idWorker = idWorker;
        }

        public synchronized List<MotionEntity> getMotionsData() {
            return new ArrayList<>(motions);
        }

        public synchronized List<KeyEntity> getKeysData() {
            return new ArrayList<>(keys);
        }

        public synchronized void clear() {
            motions.clear();
            keys.clear();
        }

        public synchronized void reset() {
            clear();
            watcher.stop();
            if (cycle != null) {
                cycle.stop();
            }
        }

        public synchronized void removeLastMotion() {
            motions.remove(motions.size() - 1);
        }

        public synchronized void addKey(KeyEntity e) {
            keys.add(e);
        }

        public synchronized void addMotion(MotionEntity e) {
            motions.add(e);
        }
    }

    static class ViewWrapper {
        final WindowRecordFloatingButtonBinding binding;
        final WindowManager windowManager;
        final WindowManager.LayoutParams params;
        boolean ignoreFloatingScript;
        boolean autoClose;

        public ViewWrapper(WindowManager windowManager, WindowRecordFloatingButtonBinding binding, WindowManager.LayoutParams params) {
            this.windowManager = windowManager;
            this.binding = binding;
            this.params = params;
        }

        public void showView() {
            if (!binding.getRoot().isAttachedToWindow()) {
                windowManager.addView(binding.getRoot(), params);
            }
        }

        public void removeView() {
            if (binding.getRoot().isAttachedToWindow()) {
                windowManager.removeView(binding.getRoot());
            }
        }

        public void reset() {
            removeView();
        }
    }
}
