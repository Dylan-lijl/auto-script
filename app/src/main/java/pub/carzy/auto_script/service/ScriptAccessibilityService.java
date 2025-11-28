package pub.carzy.auto_script.service;


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
import android.view.accessibility.AccessibilityEvent;

import androidx.databinding.DataBindingUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.activities.MacroInfoActivity;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.ControllerCallback;
import pub.carzy.auto_script.config.IdGenerator;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.databinding.FloatingButtonBinding;
import pub.carzy.auto_script.databinding.MaskViewBinding;
import pub.carzy.auto_script.db.ScriptActionEntity;
import pub.carzy.auto_script.db.ScriptEntity;
import pub.carzy.auto_script.db.ScriptPointEntity;
import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.entity.MotionEntity;
import pub.carzy.auto_script.entity.PointEntity;
import pub.carzy.auto_script.model.RecordStateModel;
import pub.carzy.auto_script.utils.BeanHandler;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class ScriptAccessibilityService extends AccessibilityService {
    private WindowManager windowManager;
    private FloatingButtonBinding view;
    private MaskViewBinding mask;
    private RecordStateModel recordStateModel;

    private WindowManager.LayoutParams viewParams;
    private WindowManager.LayoutParams maskParams;

    private List<MotionEntity> motionList;
    private Map<Integer, MotionEntity> motionMap;
    private IdGenerator<Long> idWorker;

    @Override
    @SuppressWarnings("unchecked")
    public void onCreate() {
        super.onCreate();
        //注册上去
        BeanFactory.getInstance().register(this);
        Startup startup = BeanFactory.getInstance().get(Startup.class);
        idWorker = BeanFactory.getInstance().get(IdGenerator.class);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        motionList = new ArrayList<>();
        motionMap = new HashMap<>();
        // 悬浮窗根布局
        view = DataBindingUtil.inflate(
                LayoutInflater.from(this),
                R.layout.floating_button,
                null,
                false
        );
        view.setRecordState(recordStateModel = new RecordStateModel());
        mask = DataBindingUtil.inflate(
                LayoutInflater.from(this),
                R.layout.mask_view,
                null,
                false
        );
        int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        viewParams = createLayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER, flag);
        maskParams = createLayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, Gravity.CENTER
                , flag);
        processView();
    }

    private final boolean tint = false;

    private void processView() {
        //
        View root = mask.getRoot();
        root.setOnTouchListener(createMaskMotionEventListener());
        View.OnTouchListener listener = createTouchListener(viewParams);
        addViewTouch(listener, view.btnFloatingPause, view.btnFloatingRecord, view.btnFloatingRun, view.btnFloatingStop);
        view.btnFloatingRecord.setOnClickListener(v -> {
            recordStateModel.setState(RecordStateModel.STATE_RECORDING);
            motionList.clear();
            addMaskView(true);
        });
        view.btnFloatingPause.setOnClickListener(v -> {
            recordStateModel.setState(RecordStateModel.STATE_PAUSED);
            removeMaskView();
        });
        view.btnFloatingRun.setOnClickListener(v -> {
            recordStateModel.setState(RecordStateModel.STATE_RECORDING);
            addMaskView(true);
        });
        view.btnFloatingStop.setOnClickListener(v -> {
            recordStateModel.setState(RecordStateModel.STATE_IDLE);
            //这里需要打开MacroListActivity将motionList传递过去,然后清空数据
            Intent intent = new Intent(this, MacroInfoActivity.class);
            // **重要:** 从非 Activity 上下文 (Service) 启动 Activity 必须添加此 Flag
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // 3. 传递 motionList
            intent.putExtra("data", transformData(motionList));
            // 4. 启动 Activity
            startActivity(intent);
            // 5. 清空 motionList 数据
            if (motionList != null) {
                motionList.clear();
                // 建议在这里加上日志或调试点，确认数据被清空
            }
            removeMaskView();
        });
    }

    private ScriptVoEntity transformData(List<MotionEntity> motionList) {
        ScriptVoEntity entity = new ScriptVoEntity();
        ScriptEntity root = new ScriptEntity();
        entity.setRoot(root);
        root.setId(idWorker.nextId());
        root.setName("未命名");
        root.setCount(motionList.size());
        Long minTime = null;
        Long maxTime = null;
        for (MotionEntity motionEntity : motionList) {
            if (minTime == null) {
                minTime = motionEntity.getEventTime();
            } else {
                minTime = Math.min(minTime, motionEntity.getEventTime());
            }
            if (maxTime == null) {
                maxTime = motionEntity.getEventTime();
            } else {
                maxTime = Math.max(maxTime, motionEntity.getEventTime());
            }
            ScriptActionEntity script = BeanHandler.copy(motionEntity, ScriptActionEntity.class);
            script.setId(idWorker.nextId());
            script.setParentId(root.getId());
            script.setCount(motionEntity.getPoints().size());
            script.setMaxTime(script.getUpTime() != null ? script.getUpTime() : script.getEventTime());
            for (PointEntity point : motionEntity.getPoints()) {
                ScriptPointEntity pointEntity = BeanHandler.copy(point, ScriptPointEntity.class);
                pointEntity.setId(idWorker.nextId());
                pointEntity.setParentId(script.getId());
                if (point.getTime() > script.getMaxTime()) {
                    script.setMaxTime(point.getTime());
                }
                minTime = Math.min(point.getTime(), minTime);
                maxTime = Math.max(point.getTime(), maxTime);
                entity.getPoints().add(pointEntity);
            }
            entity.getActions().add(script);
        }
        entity.getRoot().setMinTime(minTime);
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
                entity.setDownTime(event.getDownTime());
                motionMap.put(index, entity);
                entity.getPoints().add(new PointEntity(event.getX(), event.getY(), event.getEventTime(), event.getToolType(index)));
            } else if (maskType == MotionEvent.ACTION_UP || maskType == MotionEvent.ACTION_POINTER_UP) {
                //抬起事件就保存
                int index = getPointIndex(action);
                MotionEntity entity = motionMap.remove(index);
                if (entity == null) {
                    return true;
                }
                entity.setUpTime(event.getEventTime());
                entity.getPoints().add(new PointEntity(event.getX(), event.getY(), event.getEventTime(), event.getToolType(index)));
            } else if (maskType == MotionEvent.ACTION_MOVE) {
                //记录触点的x,y坐标
                for (int i = 0; i < event.getPointerCount(); i++) {
                    MotionEntity entity = motionMap.get(i);
                    if (entity == null) {
                        continue;
                    }
                    event.getX(i);
                    entity.getPoints().add(new PointEntity(event.getX(i), event.getY(i), event.getEventTime(), event.getToolType(i)));
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

    private void addViewTouch(View.OnTouchListener listener, View... views) {
        for (View view : views) {
            view.setOnTouchListener(listener);
        }
    }

    public void open() {
        addFloatingView();
    }

    private void addFloatingView() {
        removeFloatingView();
        windowManager.addView(view.getRoot(), viewParams);
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

    private void removeMaskView() {
        try {
            windowManager.removeView(mask.getRoot());
        } catch (IllegalArgumentException ignored) {

        }
    }

    private void removeFloatingView() {
        try {
            windowManager.removeView(view.getRoot());
        } catch (IllegalArgumentException ignored) {

        }
    }

    private void addMaskView(boolean update) {
        windowManager.addView(mask.getRoot(), maskParams);
        if (update) {
            removeFloatingView();
            windowManager.addView(view.getRoot(), viewParams);
        }
    }

    public void close() {
        windowManager.removeView(view.getRoot());
        windowManager.removeView(mask.getRoot());
        //这里还需要释放资源等等
    }

    private static WindowManager.LayoutParams createLayoutParams(int w, int h, int gravity, Integer layoutFlag) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                w,
                h,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        // 默认居中
        params.gravity = gravity;
        params.x = 0;
        params.y = 0;
        return params;
    }

    private View.OnTouchListener createTouchListener(WindowManager.LayoutParams params) {
        final int dragThreshold = 10;
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
                            windowManager.updateViewLayout(view.getRoot(), params);
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

    public static void checkOpenAccessibility(ControllerCallback<Boolean> callback) {
        ThreadUtil.runOnCpu(() -> {
            boolean enabled = false;
            try {
                Startup context = BeanFactory.getInstance().get(Startup.class);
                int accessibilityEnabled = 0;
                final String service = context.getPackageName() + "/" + ScriptAccessibilityService.class.getCanonicalName();
                try {
                    accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(),
                            Settings.Secure.ACCESSIBILITY_ENABLED);
                } catch (Settings.SettingNotFoundException e) {
                    Log.e(ScriptAccessibilityService.class.getCanonicalName(), "Error finding setting, default accessibility to not found", e);
                }

                TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
                if (accessibilityEnabled == 1) {
                    String settingValue = Settings.Secure.getString(context.getContentResolver(),
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                    if (settingValue != null) {
                        colonSplitter.setString(settingValue);
                        while (colonSplitter.hasNext()) {
                            String componentName = colonSplitter.next();
                            if (componentName.equalsIgnoreCase(service)) {
                                enabled = true;
                                break;
                            }
                        }
                    }
                }
                final boolean tmp = enabled;
                ThreadUtil.runOnUi(() -> callback.complete(tmp));
            } catch (Exception e) {
                ThreadUtil.runOnUi(() -> callback.catchMethod(e));
            } finally {
                ThreadUtil.runOnUi(callback::finallyMethod);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // 应用被用户从最近任务划掉时调用
        disableSelf();
        stopSelf(); // 停止服务
    }

    public static void checkOpenFloatWindow(ControllerCallback<Boolean> callback) {
        ThreadUtil.runOnCpu(() -> {
            try {
                ThreadUtil.runOnUi(() -> callback.complete(Settings.canDrawOverlays(BeanFactory.getInstance().get(Startup.class))));
            } catch (Exception e) {
                ThreadUtil.runOnUi(() -> callback.catchMethod(e));
            } finally {
                ThreadUtil.runOnUi(callback::finallyMethod);
            }
        });
    }
}
