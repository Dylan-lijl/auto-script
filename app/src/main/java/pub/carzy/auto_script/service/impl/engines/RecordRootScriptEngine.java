package pub.carzy.auto_script.service.impl.engines;

import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.databinding.DataBindingUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.IdGenerator;
import pub.carzy.auto_script.databinding.WindowRecordFloatingButtonBinding;
import pub.carzy.auto_script.entity.EventDevice;
import pub.carzy.auto_script.entity.KeyEntity;
import pub.carzy.auto_script.entity.MotionEntity;
import pub.carzy.auto_script.ex.DeviceNotRootedException;
import pub.carzy.auto_script.ex.ProcessReadOrWriteIOException;
import pub.carzy.auto_script.model.RecordStateModel;
import pub.carzy.auto_script.service.impl.RootScriptEngine;
import pub.carzy.auto_script.service.sub.SimpleReplay;
import pub.carzy.auto_script.utils.EventDeviceUtil;
import pub.carzy.auto_script.utils.MyTypeToken;
import pub.carzy.auto_script.utils.Shell;
import pub.carzy.auto_script.utils.Stopwatch;

/**
 * @author admin
 */
public class RecordRootScriptEngine extends RootScriptEngine {

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
                    dataWrapper = new DataWrapper(BeanFactory.getInstance().get(new MyTypeToken<IdGenerator<Long>>() {
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
        viewWrapper.showView();
    }

    private void addListenerByView() {
        WindowRecordFloatingButtonBinding binding = viewWrapper.binding;
        //添加长按拖动功能
        addViewTouch(createMoveListener(binding.getRoot(), viewWrapper.params),
                binding.btnFloatingPause, binding.btnFloatingRecord, binding.btnFloatingRun, binding.btnFloatingStop);
        binding.btnFloatingRecord.setOnClickListener((e) -> {
            binding.getRecordState().setState(RecordStateModel.STATE_RECORDING);
            //这里开始申请两个专用道 需要解决如何停止监听数据线程.
            dataWrapper.clear();
            dataWrapper.watcher.resetAndStart();
            //重置开始时间
            dataWrapper.startTime.set(dataWrapper.watcher.getElapsedMillis());
        });
        binding.btnFloatingPause.setOnClickListener(e -> {
            binding.getRecordState().setState(RecordStateModel.STATE_PAUSED);
            dataWrapper.watcher.pause();
        });
        binding.btnFloatingRun.setOnClickListener(v -> {
            viewWrapper.binding.getRecordState().setState(RecordStateModel.STATE_RECORDING);
            dataWrapper.watcher.resume();
            dataWrapper.startTime.set(dataWrapper.watcher.getElapsedMillis());
        });
        binding.btnFloatingClose.setOnClickListener(v -> {
            dataWrapper.watcher.stop();
            close();
        });
        binding.btnFloatingStop.setOnClickListener(v -> {
            long millis = dataWrapper.watcher.getElapsedMillis();
            dataWrapper.watcher.stop();
            viewWrapper.binding.getRecordState().setState(RecordStateModel.STATE_IDLE);
            jumpToInfo(transformData(dataWrapper.idWorker, millis, 0, dataWrapper.motions, dataWrapper.keys));
        });
    }

    @Override
    public void close() {

    }

    @Override
    public void reset() {

    }

    static class DataWrapper {
        final List<MotionEntity> motions;
        final List<KeyEntity> keys;
        final Stopwatch watcher;
        final AtomicLong startTime;
        final IdGenerator<Long> idWorker;
        public DataWrapper(IdGenerator<Long> idWorker) {
            motions = new ArrayList<>();
            keys = new ArrayList<>();
            watcher = new Stopwatch();
            startTime = new AtomicLong(-1);
            this.idWorker = idWorker;
        }

        public void clear() {
            motions.clear();
            keys.clear();
        }
    }

    static class ViewWrapper {
        WindowRecordFloatingButtonBinding binding;
        WindowManager windowManager;
        WindowManager.LayoutParams params;

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
    }
}
