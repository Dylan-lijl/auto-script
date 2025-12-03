package pub.carzy.auto_script.service.impl;

import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.databinding.DataBindingUtil;

import java.util.concurrent.locks.ReentrantLock;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.databinding.MaskViewBinding;
import pub.carzy.auto_script.databinding.PreviewFloatingButtonBinding;
import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.model.PreviewFloatingStatus;
import pub.carzy.auto_script.service.BasicAction;
import pub.carzy.auto_script.service.dto.CloseParam;
import pub.carzy.auto_script.service.dto.OpenParam;

/**
 * @author admin
 */
public class PreviewScriptAction extends BasicAction {
    private MaskViewBinding maskView;
    private WindowManager.LayoutParams maskParams;
    private ScriptVoEntity entity;
    private final ReentrantLock lock = new ReentrantLock();
    private boolean initialized;
    private PreviewFloatingButtonBinding binding;
    private WindowManager.LayoutParams bindingParams;
    private MaskViewBinding mask;

    public static final String ACTION_KEY = "preview_script";

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
                        R.layout.preview_floating_button,
                        null,
                        false
                );
                binding.setStatus(new PreviewFloatingStatus());
                bindingParams = createBindingParams(binding);
                mask = DataBindingUtil.inflate(
                        LayoutInflater.from(service),
                        R.layout.mask_view,
                        null,
                        false
                );
                maskParams = createMaskLayoutParams();
                addListeners();
                initialized = true;
            }
        } catch (Exception ignored) {
            return false;
        } finally {
            lock.unlock();
        }
        try {
            if (param != null) {
                Object data = param.getData();
                if (data instanceof ScriptVoEntity) {
                    this.entity = (ScriptVoEntity) data;
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
        binding.btnPreviewRun.setOnClickListener(v -> {
            binding.getStatus().setStatus(PreviewFloatingStatus.RUN);
            addView(mask, maskParams);
            reAddView(binding, bindingParams);
            //播放脚本

        });
        binding.btnPreviewStop.setOnClickListener(v -> {
            binding.getStatus().setStatus(PreviewFloatingStatus.NONE);
        });
        binding.btnPreviewPause.setOnClickListener(v -> {
            binding.getStatus().setStatus(PreviewFloatingStatus.PAUSE);
        });
        binding.btnPreviewRestart.setOnClickListener(v -> {
            //
        });
        binding.btnPreviewClose.setOnClickListener(v -> {
            service.close(ACTION_KEY, null);
        });
        addViewTouch(createMoveListener(binding.getRoot(), bindingParams), binding.btnPreviewRun, binding.btnPreviewStop, binding.btnPreviewPause, binding.btnPreviewRestart, binding.btnPreviewClose);
    }

    private void removeMaskView() {
        try {
            windowManager.removeView(maskView.getRoot());
        } catch (IllegalArgumentException ignored) {

        }
    }

    private WindowManager.LayoutParams createMaskLayoutParams() {
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

    @Override
    public boolean close(CloseParam param) {
        try {
            entity = null;
            binding.getStatus().setStatus(PreviewFloatingStatus.NONE);
            removeView(mask);
            removeView(binding);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
