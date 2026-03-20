package pub.carzy.auto_script.service;


import android.accessibilityservice.AccessibilityService;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.appcompat.app.AlertDialog;
import androidx.databinding.ViewDataBinding;

import com.qmuiteam.qmui.util.QMUIDisplayHelper;

import java.util.List;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.activities.MacroInfoActivity;
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
    protected volatile boolean initialized;
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
            callback.onFail(ResultCallback.UNKNOWN | ResultCallback.FLOATING);
            return;
        }
        ThreadUtil.runOnUi(() -> new AlertDialog.Builder(Startup.CURRENT)
                .setTitle(R.string.permission_prompt)
                .setMessage(R.string.permission_content)
                .setPositiveButton(R.string.go_to_open, (dialog, which) -> {
                    dialog.dismiss();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    Startup.CURRENT.startActivity(intent);
                    callback.onFail(ResultCallback.JUMP | ResultCallback.FLOATING);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                    callback.onFail(ResultCallback.CANCEL | ResultCallback.FLOATING);
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
    public void jumpToInfo(ScriptVoEntity entity){
        Context context = getContext();
        //这里需要打开MacroListActivity将motionList传递过去,然后清空数据
        Intent intent = new Intent(context, MacroInfoActivity.class);
        // **重要:** 从非 Activity 上下文 (Service) 启动 Activity 必须添加此 Flag
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //转换之前需要先将临时的添加到全局
        // 3. 传递 motionList
        intent.putExtra("data", entity);
        intent.putExtra("add", true);
        // 4. 启动 Activity
        context.startActivity(intent);
    }
    protected void addViewTouch(View.OnTouchListener listener, View... views) {
        for (View view : views) {
            view.setOnTouchListener(listener);
        }
    }

    protected View.OnTouchListener createMoveListener(View view, WindowManager.LayoutParams params) {
        final int dragThreshold = 10;
        WindowManager windowManager = (WindowManager) getContext().getSystemService(AccessibilityService.WINDOW_SERVICE);
        return new View.OnTouchListener() {
            private int lastX, lastY;
            private float startX, startY;
            private boolean isDragging;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        lastX = params.x;
                        lastY = params.y;
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - startX;
                        float dy = event.getRawY() - startY;

                        if (!isDragging) {
                            if (Math.sqrt(dx * dx + dy * dy) > dragThreshold) {
                                isDragging = true; // 超过阈值才认为是拖动
                            }
                        }

                        if (isDragging) {
                            params.x = lastX + (int) dx;
                            params.y = lastY + (int) dy;
                            windowManager.updateViewLayout(view, params);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            // 手指未移动超过阈值，触发点击事件
                            v.performClick();
                        }
                        return true;
                }
                return false;
            }
        };
    }
}
