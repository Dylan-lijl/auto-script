package pub.carzy.auto_script.core.impl.engines;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableInt;

import java.util.List;

import cn.hutool.core.lang.Pair;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.databinding.WindowReplayFloatingButtonBinding;
import pub.carzy.auto_script.entity.EventDevice;
import pub.carzy.auto_script.model.PreviewFloatingStatus;
import pub.carzy.auto_script.core.data.ReplayModel;
import pub.carzy.auto_script.core.impl.ReplayScriptEngine;
import pub.carzy.auto_script.core.impl.RootScriptEngine;
import pub.carzy.auto_script.core.sub.Replay;
import pub.carzy.auto_script.core.sub.RootReplay;
import pub.carzy.auto_script.core.sub.AbstractReplay;
import pub.carzy.auto_script.utils.EventDeviceUtil;
import pub.carzy.auto_script.utils.OverlayInputDialog;
import pub.carzy.auto_script.utils.Shell;

/**
 * @author admin
 */
public class ReplayRootScriptEngine extends RootScriptEngine implements ReplayScriptEngine {
    private DataWrapper dataWrapper;
    private ViewWrapper viewWrapper;

    @Override
    public void start(Object... args) {
        super.start(args);
        //查找到参数
        if (!this.initialized) {
            synchronized (this) {
                if (!this.initialized) {
                    //获取设备
                    List<EventDevice> list = EventDeviceUtil.parse(Shell.getEventList(cmdProcess));
                    EventDevice gestureActuator = EventDeviceUtil.findGestureActuator(list);
                    EventDevice keyActuator = EventDeviceUtil.findKeyActuator(list);
                    dataWrapper = new DataWrapper(Pair.of(gestureActuator == null ? null : Shell.getRootProcess(), gestureActuator),
                            Pair.of(keyActuator == null ? null : Shell.getRootProcess(), keyActuator));
                    WindowReplayFloatingButtonBinding binding = DataBindingUtil.inflate(
                            LayoutInflater.from(getContext()),
                            R.layout.window_replay_floating_button,
                            null,
                            false
                    );
                    binding.setCount(dataWrapper.count);
                    binding.setStatus(new PreviewFloatingStatus());
                    viewWrapper = new ViewWrapper(getWindowManager(), binding, createBindingParams(binding), new OverlayInputDialog(getContext(), getOverlayFlag()));
                    addListenerByView();
                    //回放回调
                    replayCallback();
                    this.initialized = true;
                }
            }
        }
        for (Object arg : args) {
            if (arg instanceof ReplayModel) {
                dataWrapper.replay.setModel((ReplayModel) arg);
            }
        }
        viewWrapper.showView();
    }

    private void replayCallback() {
        dataWrapper.replay.addCallback(new Replay.ResultListener() {
            @Override
            public void stop(int code, String message, Exception e) {
                if (code == Replay.ResultListener.SUCCESS) {
                    viewWrapper.binding.getStatus().setStatus(PreviewFloatingStatus.NONE);
                } else if (code == Replay.ResultListener.FAIL && message != null) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("player", "stop", e);
                }
            }

            @Override
            public void pause(int code, String message, Exception e) {
                if (code == Replay.ResultListener.SUCCESS) {
                    viewWrapper.binding.getStatus().setStatus(PreviewFloatingStatus.PAUSE);
                } else if (code == Replay.ResultListener.FAIL && message != null) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("player", "pause", e);
                }
            }

            @Override
            public void resume(int code, String message, Exception e) {
                if (code == Replay.ResultListener.SUCCESS) {
                    viewWrapper.binding.getStatus().setStatus(PreviewFloatingStatus.RUN);
                } else if (code == Replay.ResultListener.FAIL && message != null) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("player", "resume", e);
                }
            }

            @Override
            public void start(int code, String message, Exception e) {
                if (code == Replay.ResultListener.SUCCESS) {
                    viewWrapper.binding.getStatus().setStatus(PreviewFloatingStatus.RUN);
                } else if (code == Replay.ResultListener.FAIL && message != null) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("player", "start", e);
                }
            }

            @Override
            public void after(int status, int count) {
                dataWrapper.count.set(count);
            }
        });
    }

    private void addListenerByView() {
        WindowReplayFloatingButtonBinding binding = viewWrapper.binding;
        addViewTouch(createMoveListener(binding.getRoot(), viewWrapper.bindingParams),
                binding.btnStop, binding.btnRun, binding.btnCount, binding.btnRestart,
                binding.btnPause, binding.btnClose, binding.btnMore);
        binding.btnRun.setOnClickListener(v -> {
            if (dataWrapper.replay.getStatus() == AbstractReplay.PAUSE) {
                dataWrapper.replay.resume();
            } else {
                dataWrapper.replay.start();
            }
        });
        binding.btnStop.setOnClickListener(v -> dataWrapper.replay.stop());
        binding.btnPause.setOnClickListener(v -> dataWrapper.replay.pause());
        binding.btnRestart.setOnClickListener(v -> {
            dataWrapper.replay.stop();
            dataWrapper.replay.start();
        });
        binding.btnClose.setOnClickListener(v -> close());
        binding.btnCount.setOnClickListener(v -> {
            if (viewWrapper.dialog.isShowing()) {
                viewWrapper.dialog.dismiss();
            } else {
                viewWrapper.dialog.show(dataWrapper.count.get(), (result) -> {
                    dataWrapper.count.set(result);
                    dataWrapper.replay.setRepeatCount(result);
                });
            }
        });
        binding.btnMore.setOnClickListener(e -> binding.getStatus().setSelected(!binding.getStatus().getSelected()));
    }

    @Override
    public void close() {
        reset();
        if (viewWrapper != null) {
            viewWrapper.removeView();
        }
        super.close();
        if (dataWrapper != null) {
            dataWrapper.replay.close();
        }
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
        final ObservableInt count;
        final Replay replay;

        public DataWrapper(Pair<Process, EventDevice> gestureProcess, Pair<Process, EventDevice> keyEventProcess) {
            count = new ObservableInt(-1);
            replay = new RootReplay(gestureProcess, keyEventProcess);
        }

        public void reset() {
            count.set(-1);
            replay.clear();
        }
    }

    private static class ViewWrapper {
        final WindowReplayFloatingButtonBinding binding;
        final WindowManager windowManager;
        final WindowManager.LayoutParams bindingParams;
        final OverlayInputDialog dialog;
        final int count;

        public ViewWrapper(WindowManager windowManager, WindowReplayFloatingButtonBinding binding, WindowManager.LayoutParams bindingParams, OverlayInputDialog dialog) {
            this.binding = binding;
            this.windowManager = windowManager;
            this.bindingParams = bindingParams;
            this.dialog = dialog;
            count = binding.getCount().get();
        }

        public void removeView() {
            if (binding.getRoot().isAttachedToWindow()) {
                windowManager.removeView(binding.getRoot());
            }
        }

        public void showView() {
            if (!binding.getRoot().isAttachedToWindow()) {
                windowManager.addView(binding.getRoot(), bindingParams);
            }
        }

        public void reset() {
            PreviewFloatingStatus status = binding.getStatus();
            status.setStatus(PreviewFloatingStatus.NONE);
            binding.getCount().set(count);
        }
    }
}
