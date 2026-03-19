package pub.carzy.auto_script.service;


import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.appcompat.app.AlertDialog;
import androidx.databinding.ViewDataBinding;

import com.qmuiteam.qmui.util.QMUIDisplayHelper;

import java.util.List;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.IdGenerator;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.db.entity.ScriptActionEntity;
import pub.carzy.auto_script.db.entity.ScriptEntity;
import pub.carzy.auto_script.db.entity.ScriptPointEntity;
import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.entity.FloatPoint;
import pub.carzy.auto_script.entity.KeyEntity;
import pub.carzy.auto_script.entity.MotionEntity;
import pub.carzy.auto_script.entity.PointEntity;
import pub.carzy.auto_script.utils.BeanHandler;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public abstract class AbstractScriptEngine implements ScriptEngine {
    @Override
    public void start(Object... args) {

    }

    protected boolean hasFloating() {
        return Settings.canDrawOverlays(BeanFactory.getInstance().get(Startup.class));
    }

    @Override
    public void init(ResultCallback callback) {
        if (hasFloating()) {
            callback.onSuccess();
        } else {
            authorizeFloating(callback);
        }
    }

    protected void authorizeFloating(ResultCallback callback) {
        //全局不可用
        if (Startup.CURRENT == null || Startup.CURRENT.isDestroyed() || Startup.CURRENT.isFinishing()) {
            callback.onFail(ResultCallback.UNKNOWN);
            return;
        }
        ThreadUtil.runOnUi(() -> new AlertDialog.Builder(Startup.CURRENT)
                .setTitle(R.string.permission_prompt)
                .setMessage(R.string.permission_content)
                .setPositiveButton(R.string.go_to_open, (dialog, which) -> {
                    dialog.dismiss();
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    Startup.CURRENT.startActivity(intent);
                    callback.onFail(ResultCallback.JUMP);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                    callback.onFail(ResultCallback.CANCEL);
                })
                .show());
    }

    public int[] getScreenSize() {
        WindowManager manager = getWindowManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics metrics = manager.getCurrentWindowMetrics();
            return new int[]{metrics.getBounds().width(), metrics.getBounds().height()};
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(dm);
            return new int[]{dm.widthPixels, dm.heightPixels};
        }
    }

    public Context getContext() {
        return BeanFactory.getInstance().get(Startup.class);
    }

    public WindowManager getWindowManager() {
        return (WindowManager) getContext().getSystemService(Application.WINDOW_SERVICE);
    }

    protected WindowManager.LayoutParams createBindingParams(ViewDataBinding binding) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.START | Gravity.TOP;
        int[] screenSize = getScreenSize();
        params.x = screenSize[0] - binding.getRoot().getWidth() - QMUIDisplayHelper.dp2px(getContext(), 56);
        params.y = screenSize[1] - binding.getRoot().getHeight() - QMUIDisplayHelper.dp2px(getContext(), 56);
        //如果设置里面有位置则设置
        Setting setting = BeanFactory.getInstance().get(Setting.class);
        if (setting != null) {
            ThreadUtil.runOnCpu(() -> {
                try {
                    FloatPoint point = setting.getPoint();
                    if (point != null) {
                        params.x = point.getX();
                        params.y = point.getY();
                    }
                } catch (Exception e) {
                    Log.w("Setting", "获取point失败:" + e.getMessage());
                }
            });
        }
        return params;
    }

    protected ScriptVoEntity transformData(IdGenerator<Long> idWorker, long millis, long align, List<MotionEntity> motions, List<KeyEntity> keys) {
        ScriptVoEntity entity = new ScriptVoEntity();
        ScriptEntity root = new ScriptEntity();
        entity.setRoot(root);
        root.setId(idWorker.nextId());
        root.setName(getContext().getString(R.string.untitled));
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
}
