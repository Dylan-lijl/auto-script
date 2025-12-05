package pub.carzy.auto_script.service.impl;

import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.lang.mutable.MutablePair;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.databinding.MaskViewBinding;
import pub.carzy.auto_script.databinding.PreviewFloatingButtonBinding;
import pub.carzy.auto_script.db.ScriptActionEntity;
import pub.carzy.auto_script.db.ScriptPointEntity;
import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.model.PreviewFloatingStatus;
import pub.carzy.auto_script.service.BasicAction;
import pub.carzy.auto_script.service.dto.CloseParam;
import pub.carzy.auto_script.service.dto.OpenParam;
import pub.carzy.auto_script.utils.ThreadUtil;

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
            playScript();
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

    /**
     * 是否需要构建
     */
    private boolean needBuild = true;
    /**
     * 活动map
     */
    private Map<Long, Object> actionMap = new LinkedHashMap<>();
    /**
     * parentId,坐标列表
     */
    private Map<Long, Stack<ScriptPointEntity>> pointMap = new LinkedHashMap<>();
    /**
     * 运行中的map
     */
    private Set<ScriptActionEntity> runningAction = new LinkedHashSet<>();
    private Map<Long, Object> removeMap = new LinkedHashMap<>();
    private Map<Long, List<ScriptPointEntity>> removePointMap = new LinkedHashMap<>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean stop = false;

    private void playScript() {
        //构建完成或不需要构建回调,真正开始提交执行
        Runnable callback = () -> scheduler.schedule(createScriptRunnable(), 0, TimeUnit.MILLISECONDS);
        //先构建计划
        if (needBuild) {
            Toast.makeText(service, "正在构建计划...", Toast.LENGTH_SHORT).show();
            ThreadUtil.runOnCpu(() -> {
                //清空数据,后续可以优化从remove进行恢复而不需要重新构建
                runningAction.clear();
                actionMap.clear();
                pointMap.clear();
                removeMap.clear();
                removePointMap.clear();
                if (entity == null) {
                    throw new NullPointerException("entity is null");
                }
                //以按下时间分组和排序,而值可能是对象也有可能是装着对象的集合
                entity.getActions().stream()
                        .sorted(Comparator.comparingLong(ScriptActionEntity::getDownTime))
                        .forEach(action -> {
                            if (!actionMap.containsKey(action.getDownTime())) {
                                //单时刻
                                actionMap.put(action.getDownTime(), action);
                            } else {
                                //多时刻
                                Object object = actionMap.get(action.getDownTime());
                                if (object instanceof Set) {
                                    //已经是集合了就直接添加到集合
                                    ((Set<ScriptActionEntity>) object).add(action);
                                } else {
                                    //不是集合就创建集合
                                    Set<ScriptActionEntity> set = new LinkedHashSet<>();
                                    set.add((ScriptActionEntity) object);
                                    actionMap.put(action.getDownTime(), set);
                                }
                            }
                        });
                //坐标,以parentId分组,以时间倒序id倒叙排序,转成栈结构
                Map<Long, Stack<ScriptPointEntity>> collect = entity.getPoints().stream()
                        .sorted(Comparator.comparingLong(ScriptPointEntity::getTime).reversed()
                                .thenComparingLong(ScriptPointEntity::getId).reversed())
                        .collect(Collectors.groupingBy(
                                ScriptPointEntity::getParentId,
                                LinkedHashMap::new,
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> {
                                            Stack<ScriptPointEntity> stack = new Stack<>();
                                            stack.addAll(list);
                                            return stack;
                                        }
                                )));
                pointMap.putAll(collect);
                needBuild = false;
                ThreadUtil.runOnUi(() -> {
                    Toast.makeText(service, "计划已就绪", Toast.LENGTH_SHORT).show();
                    ThreadUtil.runOnCpu(callback);
                });
            });
        } else {
            ThreadUtil.runOnCpu(callback);
        }
    }

    private long time = -1;
    /**
     * 10ms
     */
    private long tick = 10;
    /**
     * key:id,value: 已经花费多长,坐标
     */
    private Map<Long, MutablePair<Long, ScriptPointEntity>> pointTimeMap = new HashMap<>();
    private Map<Long, GestureDescription.StrokeDescription> strokeMap = new HashMap<>();

    private Runnable createScriptRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    //第一次就获取时间
                    if (time < 0) {
                        time = System.currentTimeMillis();
                        //按下事件的时长 点击运行到实际记录时长肯定大于0
                        Long pointTime = actionMap.keySet().stream().findFirst().orElse(null);
                        if (pointTime == null) {
                            //空列表
                            return;
                        }
                        //延迟执行
                        scheduler.schedule(this, pointTime, TimeUnit.MILLISECONDS);
                    } else {
                        //如果运行列表为空则添加
                        if (runningAction.isEmpty()) {
                            Long pointTime = actionMap.keySet().stream().findFirst().orElse(null);
                            if (pointTime == null) {
                                return;
                            }
                            Object object = actionMap.remove(pointTime);
                            if (object instanceof Set) {
                                //列表
                                runningAction.addAll((Set<ScriptActionEntity>) object);
                            } else {
                                runningAction.add((ScriptActionEntity) object);
                            }
                        }
                        if (runningAction.isEmpty()) {
                            return;
                        }
                        Iterator<ScriptActionEntity> iterator = runningAction.iterator();
                        while (iterator.hasNext()) {
                            ScriptActionEntity action = iterator.next();
                            Long id = action.getId();
                            Stack<ScriptPointEntity> stack = pointMap.get(id);
                            if (stack == null || stack.isEmpty()) {
                                //没有数据就移除
                                if (removeMap.containsKey(action.getDownTime())) {
                                    //转成集合
                                    Set<ScriptActionEntity> set = new LinkedHashSet<>();
                                    set.add((ScriptActionEntity) removeMap.get(action.getDownTime()));
                                    removeMap.put(action.getDownTime(), set);
                                } else {
                                    removeMap.put(action.getDownTime(), action);
                                }
                                iterator.remove();
                                continue;
                            }
                            //如果pointTimeMap有数据说明上一个位移还没有处理完成
                            MutablePair<Long, ScriptPointEntity> pair = pointTimeMap.get(action.getId());
                            ScriptPointEntity item;
                            long remainingTick = tick;
                            if (pair != null) {
                                Long key = pair.getKey();
                                if (remainingTick < key) {
                                    //小于这个时间
                                    item = pair.getValue();
                                    pointTimeMap.remove(action.getId());
                                } else if (remainingTick > key) {
                                    //大于则减去时间
                                    pair.setKey(key - remainingTick);
                                    continue;
                                } else {
                                    //相等直接移除并返回
                                    pointTimeMap.remove(action.getId());
                                    continue;
                                }
                            } else {
                                item = stack.pop();
                                //剩余时间片
                                remainingTick = tick;
                            }
                            while (item != null) {
                                GestureDescription.StrokeDescription description = strokeMap.get(action.getId());
                                if (description == null) {
                                    Path path = new Path();
                                    path.moveTo(item.getX(), item.getY());
                                    strokeMap.put(action.getId(), new GestureDescription.StrokeDescription(
                                            path,
                                            0,
                                            0, true));
                                }
                                //这个是相对时长
                                long d = item.getTime() - action.getDownTime();
                                if (d - remainingTick > 0) {
                                    //说明大于时间片
                                    Path path = description.getPath();
                                    path.lineTo(item.getX(), item.getY());
                                    description.continueStroke(path, 0, remainingTick, !stack.isEmpty());
                                    //这个步骤还没有完成
                                    pointTimeMap.put(action.getId(), new MutablePair<>(d - remainingTick, item));
                                } else if (d - remainingTick == 0) {
                                    //等于时间片
                                    Path path = description.getPath();
                                    path.lineTo(item.getX(), item.getY());
                                    description.continueStroke(path, 0, remainingTick, !stack.isEmpty());
                                    remainingTick = 0;
                                    break;
                                } else {
                                    //小于时间片
                                    Path path = description.getPath();
                                    path.lineTo(item.getX(), item.getY());
                                    description.continueStroke(path, 0, remainingTick = remainingTick - d, !stack.isEmpty());
                                    item = null;
                                    item = stack.pop();
                                }
                            }
                        }
                        scheduler.schedule(this, tick, TimeUnit.MILLISECONDS);
                    }
                    //计算间隔
                } catch (Exception ignored) {

                }
            }
        };
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
