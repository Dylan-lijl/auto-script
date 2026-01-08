package pub.carzy.auto_script.service.impl;

import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableInt;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.databinding.WindowMaskViewBinding;
import pub.carzy.auto_script.databinding.WindowReplayFloatingButtonBinding;
import pub.carzy.auto_script.model.PreviewFloatingStatus;
import pub.carzy.auto_script.service.BasicAction;
import pub.carzy.auto_script.service.data.ReplayModel;
import pub.carzy.auto_script.service.sub.SimpleReplay;
import pub.carzy.auto_script.service.dto.BasicParam;
import pub.carzy.auto_script.service.dto.CloseParam;
import pub.carzy.auto_script.service.dto.OpenParam;
import pub.carzy.auto_script.service.dto.UpdateParam;
import pub.carzy.auto_script.utils.OverlayInputDialog;

/**
 * @author admin
 */
public class ReplayScriptAction extends BasicAction {
    private WindowManager.LayoutParams maskParams;
    private ReplayModel model;
    private final ReentrantLock lock = new ReentrantLock();
    private boolean initialized;
    private WindowReplayFloatingButtonBinding binding;
    private WindowManager.LayoutParams bindingParams;
    private WindowMaskViewBinding mask;
    private OverlayInputDialog dialog;
    public static final String ACTION_KEY = "replay_script";
    private SimpleReplay player;
    private ObservableInt count;

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
                binding = DataBindingUtil.inflate(
                        LayoutInflater.from(service),
                        R.layout.window_replay_floating_button,
                        null,
                        false
                );
                count = new ObservableInt(-1);
                binding.setStatus(new PreviewFloatingStatus());
                binding.setCount(count);
                bindingParams = createBindingParams(binding);
                dialog = new OverlayInputDialog(service);
                mask = DataBindingUtil.inflate(
                        LayoutInflater.from(service),
                        R.layout.window_mask_view,
                        null,
                        false
                );
                maskParams = createMaskLayoutParams();
                player = new SimpleReplay(service);
                addListeners();
                initialized = true;
            }
        } catch (Exception e) {
            Log.e(this.getClass().getCanonicalName(), "open", e);
            return false;
        } finally {
            lock.unlock();
        }
        return updateData(param);
    }

    private boolean updateData(BasicParam param) {
        try {
            if (param != null) {
                Object data = param.getData();
                if (data instanceof ReplayModel) {
                    this.model = (ReplayModel) data;
                    player.setModel(model);
                } else {
                    Log.e("open", "data is not ScriptVoEntity");
                    return false;
                }
            }
            addView(binding, bindingParams);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void addListeners() {
        AtomicBoolean closed = new AtomicBoolean(false);
        binding.btnRun.setOnClickListener(v -> {
            if (player.getStatus() == SimpleReplay.PAUSE) {
                player.resume();
            } else {
                player.start();
            }
        });
        binding.btnStop.setOnClickListener(v -> player.stop());
        binding.btnPause.setOnClickListener(v -> player.pause());
        binding.btnRestart.setOnClickListener(v -> player.start());
        binding.btnClose.setOnClickListener(v -> {
            closed.set(true);
            player.stop();
        });
        binding.btnCount.setOnClickListener(v -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            } else {
                dialog.show(count.get(), (result) -> {
                    count.set(result);
                    player.setRepeatCount(result);
                });
            }
        });
        player.addCallback(new SimpleReplay.ResultListener() {
            @Override
            public void stop(int code, String message, Exception e) {
                if (code == SimpleReplay.ResultListener.SUCCESS) {
                    binding.getStatus().setStatus(PreviewFloatingStatus.NONE);
                    removeView(mask);
                } else if (code == SimpleReplay.ResultListener.FAIL && message != null) {
                    Toast.makeText(service, message, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("player", "stop", e);
                }
                if (closed.get()) {
                    service.close(ACTION_KEY, null);
                    closed.set(true);
                }
            }

            @Override
            public void pause(int code, String message, Exception e) {
                if (code == SimpleReplay.ResultListener.SUCCESS) {
                    binding.getStatus().setStatus(PreviewFloatingStatus.PAUSE);
                    removeView(mask);
                } else if (code == SimpleReplay.ResultListener.FAIL && message != null) {
                    Toast.makeText(service, message, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("player", "pause", e);
                }
            }

            @Override
            public void resume(int code, String message, Exception e) {
                if (code == SimpleReplay.ResultListener.SUCCESS) {
                    binding.getStatus().setStatus(PreviewFloatingStatus.RUN);
                    if (binding.getStatus().getSimulate()) {
                        addView(mask, maskParams);
                    }
                    reAddView(binding, bindingParams);
                } else if (code == SimpleReplay.ResultListener.FAIL && message != null) {
                    Toast.makeText(service, message, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("player", "resume", e);
                }
            }

            @Override
            public void start(int code, String message, Exception e) {
                if (code == SimpleReplay.ResultListener.SUCCESS) {
                    binding.getStatus().setStatus(PreviewFloatingStatus.RUN);
                    if (binding.getStatus().getSimulate()) {
                        addView(mask, maskParams);
                    }
                    reAddView(binding, bindingParams);
                } else if (code == SimpleReplay.ResultListener.FAIL && message != null) {
                    Toast.makeText(service, message, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("player", "start", e);
                }
            }

            @Override
            public void after(int status, int count) {
                ReplayScriptAction.this.count.set(count);
            }
        });
        addViewTouch(createMoveListener(binding.getRoot(), bindingParams),
                binding.btnRun, binding.btnStop,
                binding.btnPause, binding.btnRestart,
                binding.btnClose, binding.btnMore, binding.btnSimulate, binding.btnCount);
    }

    private WindowManager.LayoutParams createMaskLayoutParams() {
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

    @Override
    public boolean close(CloseParam param) {
        try {
            model = null;
            binding.getStatus().setStatus(PreviewFloatingStatus.NONE);
            removeView(mask);
            removeView(binding);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public boolean update(UpdateParam param) {
        return updateData(param);
    }
}
