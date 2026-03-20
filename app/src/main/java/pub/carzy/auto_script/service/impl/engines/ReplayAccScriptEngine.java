package pub.carzy.auto_script.service.impl.engines;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableInt;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.databinding.WindowReplayFloatingButtonBinding;
import pub.carzy.auto_script.model.PreviewFloatingStatus;
import pub.carzy.auto_script.service.data.ReplayModel;
import pub.carzy.auto_script.service.impl.AccScriptEngine;
import pub.carzy.auto_script.service.sub.SimpleReplay;
import pub.carzy.auto_script.utils.OverlayInputDialog;

/**
 * 无障碍回放
 *
 * @author admin
 */
public class ReplayAccScriptEngine extends AccScriptEngine {
    private ViewWrapper viewWrapper;
    private DataWrapper dataWrapper;

    @Override
    public void start(Object... args) {
        super.start(args);
        //查找到参数
        if (!this.initialized) {
            synchronized (this) {
                if (!this.initialized) {
                    dataWrapper = new DataWrapper(service);
                    for (Object arg : args) {
                        if (arg instanceof ReplayModel) {
                            dataWrapper.replay.setModel((ReplayModel) arg);
                        }
                    }
                    WindowReplayFloatingButtonBinding binding = DataBindingUtil.inflate(
                            LayoutInflater.from(service),
                            R.layout.window_replay_floating_button,
                            null,
                            false
                    );
                    viewWrapper = new ViewWrapper(getWindowManager(), binding, createBindingParams(binding), new OverlayInputDialog(service));
                    binding.setCount(dataWrapper.count);
                    binding.setStatus(new PreviewFloatingStatus());
                    addListenerByView();
                    this.initialized = true;
                }
            }
        }
        viewWrapper.showView();
    }

    private void addListenerByView() {
        //各个按钮监听
        listenButtons();
        //回放回调
        replayCallback();
        WindowReplayFloatingButtonBinding binding = viewWrapper.binding;
        //添加长按移动事件
        addViewTouch(createMoveListener(binding.getRoot(), viewWrapper.bindingParams),
                binding.btnMore, binding.btnClose, binding.btnCount, binding.btnPause, binding.btnRestart,
                binding.btnRun, binding.btnStop);
    }

    private void replayCallback() {
        dataWrapper.replay.addCallback(new SimpleReplay.ResultListener() {
            @Override
            public void stop(int code, String message, Exception e) {
                if (code == SimpleReplay.ResultListener.SUCCESS) {
                    viewWrapper.binding.getStatus().setStatus(PreviewFloatingStatus.NONE);
                } else if (code == SimpleReplay.ResultListener.FAIL && message != null) {
                    Toast.makeText(service, message, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("player", "stop", e);
                }
            }

            @Override
            public void pause(int code, String message, Exception e) {
                if (code == SimpleReplay.ResultListener.SUCCESS) {
                    viewWrapper.binding.getStatus().setStatus(PreviewFloatingStatus.PAUSE);
                } else if (code == SimpleReplay.ResultListener.FAIL && message != null) {
                    Toast.makeText(service, message, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("player", "pause", e);
                }
            }

            @Override
            public void resume(int code, String message, Exception e) {
                if (code == SimpleReplay.ResultListener.SUCCESS) {
                    viewWrapper.binding.getStatus().setStatus(PreviewFloatingStatus.RUN);
                } else if (code == SimpleReplay.ResultListener.FAIL && message != null) {
                    Toast.makeText(service, message, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("player", "resume", e);
                }
            }

            @Override
            public void start(int code, String message, Exception e) {
                if (code == SimpleReplay.ResultListener.SUCCESS) {
                    viewWrapper.binding.getStatus().setStatus(PreviewFloatingStatus.RUN);
                } else if (code == SimpleReplay.ResultListener.FAIL && message != null) {
                    Toast.makeText(service, message, Toast.LENGTH_SHORT).show();
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

    private void listenButtons() {
        WindowReplayFloatingButtonBinding binding = viewWrapper.binding;
        binding.btnRun.setOnClickListener(v -> {
            if (dataWrapper.replay.getStatus() == SimpleReplay.PAUSE) {
                dataWrapper.replay.resume();
            } else {
                dataWrapper.replay.start();
            }
        });
        binding.btnStop.setOnClickListener(v -> dataWrapper.replay.stop());
        binding.btnPause.setOnClickListener(v -> dataWrapper.replay.pause());
        binding.btnRestart.setOnClickListener(v -> dataWrapper.replay.start());
        binding.btnClose.setOnClickListener(v -> {
            dataWrapper.replay.stop();
            close();
        });
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
    }

    @Override
    public void close() {
        reset();
        viewWrapper.removeView();
    }

    @Override
    public void reset() {
        dataWrapper.reset();
        viewWrapper.reset();
    }

    private static class DataWrapper {
        final ObservableInt count;
        final SimpleReplay replay;

        public DataWrapper(AccessibilityService service) {
            count = new ObservableInt(-1);
            replay = new SimpleReplay(service);
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

        public ViewWrapper(WindowManager windowManager, WindowReplayFloatingButtonBinding binding, WindowManager.LayoutParams bindingParams, OverlayInputDialog dialog) {
            this.binding = binding;
            this.windowManager = windowManager;
            this.bindingParams = bindingParams;
            this.dialog = dialog;
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

        }
    }
}
