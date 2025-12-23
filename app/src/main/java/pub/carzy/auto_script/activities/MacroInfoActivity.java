package pub.carzy.auto_script.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableMap;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.IdGenerator;
import pub.carzy.auto_script.databinding.ActionInfoBinding;
import pub.carzy.auto_script.databinding.ActivityMacroInfoBinding;
import pub.carzy.auto_script.databinding.AutoAlignDialogBinding;
import pub.carzy.auto_script.databinding.ChatToolbarMoreMenuBinding;
import pub.carzy.auto_script.databinding.PointInfoBinding;
import pub.carzy.auto_script.db.ScriptActionEntity;
import pub.carzy.auto_script.db.ScriptPointEntity;
import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.model.MacroInfoRefreshModel;
import pub.carzy.auto_script.model.ScriptVoEntityModel;
import pub.carzy.auto_script.service.MyAccessibilityService;
import pub.carzy.auto_script.service.dto.OpenParam;
import pub.carzy.auto_script.service.impl.ReplayScriptAction;
import pub.carzy.auto_script.ui.adapter.SingleStackRender;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.StoreUtil;
import pub.carzy.auto_script.utils.ThreadUtil;
import pub.carzy.auto_script.utils.TypeToken;

/**
 * @author admin
 */
public class MacroInfoActivity extends BaseActivity {

    private ActivityMacroInfoBinding binding;
    private AutoAlignDialogBinding dataBinding;
    private ActionInfoBinding actionInfoBinding;
    private PointInfoBinding pointInfoBinding;
    private ChatToolbarMoreMenuBinding moreMenuBinding;
    private ScriptVoEntityModel model;
    private MacroInfoRefreshModel refresh;
    private IdGenerator<Long> idWorker;
    private ActivityResultLauncher<Intent> addLauncher;
    private ActivityResultLauncher<Intent> addInfoLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        idWorker = BeanFactory.getInstance().get(new TypeToken<IdGenerator<Long>>() {
        });
        binding = DataBindingUtil.setContentView(this, R.layout.activity_macro_info);
        dataBinding = AutoAlignDialogBinding.inflate(LayoutInflater.from(this));
        moreMenuBinding = ChatToolbarMoreMenuBinding.inflate(LayoutInflater.from(this));
        actionInfoBinding = ActionInfoBinding.inflate(LayoutInflater.from(this));
        pointInfoBinding = PointInfoBinding.inflate(LayoutInflater.from(this));
        model = new ScriptVoEntityModel();
        for (int c : getResources().getIntArray(R.array.script_info_chat_color)) {
            model.getColorsResource().add(c);
        }
        refresh = new MacroInfoRefreshModel();
        refresh.setInfo(true);
        binding.setModel(model);
        binding.setRefresh(refresh);
        moreMenuBinding.setModel(model);
        initChat();
        initIntent();
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        // 设置没有数据时显示的文字,,,按照mvvm思想这个属性应该写在xml,但是这个库未提供xml属性
        binding.flowChatLayout.actionBarChart.setNoDataText(getString(R.string.message_no_data));
        binding.flowChatLayout.pointBarChart.setNoDataText(getString(R.string.message_no_data));
        initialListeners();
        addLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), createProcessAddResult());
        addInfoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), createProcessAddInfoResult());
    }

    private ActivityResultCallback<ActivityResult> createProcessAddInfoResult() {
        return result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                if (data == null) {
                    return;
                }
                ScriptPointEntity entity;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    entity = data.getParcelableExtra("data", ScriptPointEntity.class);
                } else {
                    entity = data.getParcelableExtra("data");
                }
                if (entity == null) {
                    return;
                }
                ScriptActionEntity action = model.getLastCheckedAction().getValue();
                entity.setId(idWorker.nextId());
                entity.setParentId(action.getId());
                Set<Long> ids = model.getPointMapByParentId().get(entity.getParentId());
                if (ids != null && !ids.isEmpty()) {
                    long pre = -1;
                    for (Long id : ids) {
                        if (model.getPoints().get(id).getTime() >= entity.getTime()) {
                            break;
                        }
                        pre = id;
                    }
                    if (pre != -1) {
                        //计算差值
                        long d = entity.getTime() - model.getPoints().get(pre).getTime();
                        if (d > 0) {
                            model.adjustPointTime(entity.getId(), d);
                        }

                    }
                } else {
                    long d = entity.getTime() - model.getLastCheckedAction().getValue().getDownTime();
                    if (d > 0) {
                        model.adjustActionTime(entity.getParentId(), d);
                        action.setUpTime(entity.getTime());
                    }
                }
                Set<ScriptPointEntity> map = new LinkedHashSet<>(model.getPoints().values());
                map.add(entity);
                model.setPoints(map);
                Set<Long> set = model.getPointMapByParentId().get(action.getId());
                if (set != null) {
                    long maxTime = -1;
                    for (Long l : set) {
                        maxTime = Math.max(maxTime, model.getPoints().get(l).getTime());
                    }
                    //重新计算一下最大时间
                    if (maxTime != -1) {
                        action.setMaxTime(maxTime);
                        action.setUpTime(maxTime);
                        model.getRoot().setMaxTime(Math.max(model.getRoot().getMaxTime(), maxTime));
                    }
                }
                //重新触发一下选中
                Highlight highlight = model.getCheckedAction().remove(action.getId());
                model.getCheckedAction().put(action.getId(), highlight);
                ThreadUtil.runOnUi(() -> {
                    updateChartData(binding.flowChatLayout.actionBarChart,
                            new ArrayList<>(model.getActionBars().values()), new ArrayList<>(model.getActionColors().values()));
                    updateChartData(binding.flowChatLayout.pointBarChart,
                            new ArrayList<>(model.getPointBars().values()), new ArrayList<>(model.getPointColors().values()));
                });
            }
        };
    }

    private ActivityResultCallback<ActivityResult> createProcessAddResult() {
        return result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                if (data == null) {
                    return;
                }
                ScriptActionEntity entity;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    entity = data.getParcelableExtra("data", ScriptActionEntity.class);
                } else {
                    entity = data.getParcelableExtra("data");
                }
                if (entity == null) {
                    return;
                }
                // 处理返回数据
                entity.setId(idWorker.nextId());
                entity.setEventTime(System.nanoTime());
                entity.setParentId(model.getRoot().getId());
                if (entity.getType() == ScriptActionEntity.GESTURE) {
                    entity.setUpTime(entity.getDownTime());
                }
                long d = entity.getUpTime() - entity.getDownTime();
                if (entity.getType() == ScriptActionEntity.KEY_EVENT && data.getBooleanExtra("auto", false) && d > 0) {
                    //需要对齐
                    long id = -1;
                    for (ScriptActionEntity e : model.getActions().values()) {
                        if (e.getDownTime() >= entity.getDownTime()) {
                            break;
                        }
                        id = e.getId();
                    }
                    if (id != -1) {
                        model.adjustActionTime(id, d);
                    }
                }
                List<ScriptActionEntity> list = new ArrayList<>(model.getActions().values());
                list.add(entity);
                model.setActions(list);
                model.getRoot().setCount(model.getRoot().getCount() + 1);
                ThreadUtil.runOnUi(() -> {
                    //触发图表更新
                    updateChartData(binding.flowChatLayout.actionBarChart,
                            new ArrayList<>(model.getActionBars().values()), new ArrayList<>(model.getActionColors().values()));
                });
            }
        };
    }

    private void initialListeners() {
        binding.flowChatToolbar.btnMoreAction.setOnClickListener(e -> {
            PopupWindow popupWindow = new PopupWindow(
                    ActivityUtils.reinstatedView(moreMenuBinding.getRoot()),
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    true // 点击外部消失
            );
            popupWindow.setOutsideTouchable(true);
            popupWindow.setFocusable(true);
            moreMenuBinding.actionRun.setOnClickListener(event -> {
                //打开对应service悬浮窗口
                popupWindow.dismiss();
                MyAccessibilityService service = BeanFactory.getInstance().get(MyAccessibilityService.class);
                if (service != null) {
                    ScriptVoEntity entity = new ScriptVoEntity();
                    entity.setRoot(model.getRoot());
                    entity.getActions().addAll(model.getActions().values());
                    entity.getPoints().addAll(model.getPoints().values());
                    service.open(ReplayScriptAction.ACTION_KEY, new OpenParam(entity));
                }
            });
            moreMenuBinding.actionRename.setOnClickListener(event -> {
                popupWindow.dismiss();
                //这里需要弹窗来修改名字
                if (model.getRoot() != null) {
                    showRenameDialog();
                }
            });
            moreMenuBinding.actionRemark.setOnClickListener(event -> {
                popupWindow.dismiss();
                //这里需要弹窗来修改名字
                if (model.getRoot() != null) {
                    showRemarkDialog();
                }
            });
            moreMenuBinding.actionExport.setOnClickListener(event -> {
                popupWindow.dismiss();
                Toast.makeText(this, R.string.message_exporting, Toast.LENGTH_SHORT).show();
                ThreadUtil.runOnCpu(() -> {
                    //将脚本保存为json文件
                    Gson gson = new Gson();
                    ScriptVoEntity entity = new ScriptVoEntity();
                    entity.setRoot(model.getRoot());
                    entity.setActions(new ArrayList<>(model.getActions().values()));
                    entity.setPoints(new ArrayList<>(model.getPoints().values()));
                    //调用报错文件api
                    StoreUtil.promptAndSaveFile(gson.toJson(entity),
                            "file_" + new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss", Locale.getDefault()).format(new Date()) + ".json",
                            result -> ThreadUtil.runOnUi(() -> {
                                Toast.makeText(MacroInfoActivity.this, getString(R.string.message_saved_successfully_ph, result)
                                        , Toast.LENGTH_SHORT).show();
                                //打开对应文件夹位置
                                showFileInFolder(this, new File(result));
                            }));
                });
            });
            moreMenuBinding.actionRemove.setOnClickListener(event -> {
                popupWindow.dismiss();
                //删除当前整个脚本
                /*AlertDialog d = new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.delete_dialog_title)
                        .setMessage(R.string.delete_dialog_message)
                        .setPositiveButton(R.string.confirm, (dialog, which) -> {
                            model.getActions().remove(model.getDetailIndex());
                        }).create();
                d.show();*/
            });
            Button button = binding.flowChatToolbar.btnMoreAction;
            moreMenuBinding.getRoot().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            popupWindow.showAsDropDown(button, -moreMenuBinding.getRoot().getMeasuredWidth() * 3 / 4, 0);
        });
        binding.flowChatLayout.btnDelete.setOnClickListener(createDeleteActionItemListener());
        binding.flowChatLayout.btnDeleteDetail.setOnClickListener(createDeletePointItemListener());
        binding.flowChatLayout.btnInfo.setOnClickListener(createInfoListener());
        binding.flowChatLayout.btnDetailInfo.setOnClickListener(createDetailInfoListener());
        binding.flowChatLayout.btnAdd.setOnClickListener(createAddActionListener());
        binding.flowChatLayout.btnInfoAdd.setOnClickListener(createAddPointListener());
    }

    private View.OnClickListener createAddPointListener() {
        return e -> {
            Map.Entry<Long, ScriptActionEntity> action = model.getLastCheckedAction();
            if (action == null) {
                return;
            }
            ScriptPointEntity entity = new ScriptPointEntity();
            Intent intent = new Intent(this, PointAddActivity.class);
            Map.Entry<Long, ScriptPointEntity> point = model.getLastCheckedPoint();
            Set<Long> ids = model.getPointMapByParentId().get(action.getValue().getId());
            if (ids != null && !ids.isEmpty()) {
                ScriptPointEntity first = null;
                ScriptPointEntity last = null;
                int i = 0;
                for (Long id : ids) {
                    if (i == 0) {
                        first = model.getPoints().get(id);
                    }
                    if (i == ids.size() - 1) {
                        last = model.getPoints().get(id);
                    }
                    i++;
                }
                intent.putExtra("minTime", first.getTime());
                intent.putExtra("maxTime", last.getTime());
                entity.setX(last.getX());
                entity.setY(last.getY());
            } else {
                intent.putExtra("minTime", action.getValue().getDownTime());
                intent.putExtra("maxTime", action.getValue().getDownTime());
            }
            if (point != null) {
                entity.setX(point.getValue().getX());
                entity.setY(point.getValue().getY());
                entity.setTime(point.getValue().getTime());
            }else{
                entity.setX(200F);
                entity.setY(200F);
            }
            intent.putExtra("time", entity.getTime());
            intent.putExtra("data", entity);
            Integer index = model.getLastCheckedPointIndex();
            if (index != null) {
                intent.putExtra("index", index);
            }
            //跳转到新增界面
            addInfoLauncher.launch(intent);
        };
    }

    private View.OnClickListener createAddActionListener() {
        return e -> {
            ScriptActionEntity entity = new ScriptActionEntity();
            entity.setMaxTime(0L);
            entity.setUpTime(0L);
            entity.setCode(KeyEvent.KEYCODE_HOME);
            entity.setCount(0);
            entity.setIndex(0);
            entity.setType(ScriptActionEntity.GESTURE);
            Intent intent = new Intent(this, ActionAddActivity.class);
            intent.putExtra("data", entity);
            Map.Entry<Long, ScriptActionEntity> entry = model.getLastCheckedAction();
            if (entry != null) {
                intent.putExtra("upTime", entry.getValue().getUpTime());
                intent.putExtra("downTime", entry.getValue().getDownTime());
            }
            Integer index = model.getLastCheckedActionIndex();
            if (index != null) {
                intent.putExtra("index", index);
            }
            if (!model.getActions().isEmpty()) {
                ScriptActionEntity last = model.getActions().get(ScriptVoEntityModel.getLastKey(model.getActions()));
                if (last != null) {
                    intent.putExtra("maxTime", last.getUpTime());
                }
            }
            //跳转到新增界面
            addLauncher.launch(intent);
        };
    }

    private void showRemarkDialog() {
        // 创建一个 EditText 作为输入框
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(getString(R.string.message_input_prompt, getString(R.string.remark)));
        input.setText(model.getRoot().getRemark());
        input.setFocusable(true);
        input.setFocusableInTouchMode(true);
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.remark)
                .setView(input)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (!value.isEmpty()) {
                        if (model.getRoot() != null) {
                            model.getRoot().setRemark(value);
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.message_error_empty_ph, getString(R.string.remark)), Toast.LENGTH_SHORT).show();
                    }
                    model.setSaved(true);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss()).create();

        alertDialog.setOnShowListener(d -> {
            input.requestFocus();
            input.postDelayed(() -> {
                if (alertDialog.getWindow() == null) {
                    return;
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                alertDialog.getWindow().clearFlags(
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                );
                alertDialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                );
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }, 150);
        });
        alertDialog.show();
    }

    private View.OnClickListener createDetailInfoListener() {
        return v -> {
            if (model.getCheckedPoint().isEmpty()) {
                return;
            }
            pointInfoBinding.setEntity(model.getLastCheckedPoint().getValue());
            new MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.dialog_point_info_title, model.getLastCheckedPointIndex()))
                    .setView(ActivityUtils.reinstatedView(pointInfoBinding.getRoot()))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        };
    }

    private View.OnClickListener createInfoListener() {
        return v -> {
            if (model.getCheckedAction().isEmpty()) {
                return;
            }
            actionInfoBinding.setEntity(model.getLastCheckedAction().getValue());
            new MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.dialog_action_info_title, model.getLastCheckedActionIndex()))
                    .setView(ActivityUtils.reinstatedView(actionInfoBinding.getRoot()))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        };
    }

    private View.OnClickListener createDeletePointItemListener() {
        return e -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(R.string.delete_dialog_message)
                .setView(ActivityUtils.reinstatedView(dataBinding.getRoot()))
                .setPositiveButton(R.string.confirm, (dialog, which) -> removeCheckedPoint(dataBinding.checkBox.isChecked()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void removeCheckedPoint(boolean checked) {
        if (refresh.getDeleteDetail() || refresh.getDetail()) {
            return;
        }
        refresh.setDeleteDetail(true);
        ThreadUtil.runOnCpu(() -> {
            try {
                for (Long id : new ArrayList<>(model.getCheckedPoint().keySet())) {
                    ScriptPointEntity point = model.getPoints().get(id);
                    if (point == null) {
                        continue;
                    }
                    //删除对应索引数据
                    if (checked) {
                        Set<Long> ids = model.getPointMapByParentId().get(point.getParentId());
                        if (ids == null) {
                            continue;
                        }
                        Long pre = null;
                        for (Long id1 : ids) {
                            if (id1.compareTo(id) == 0) {
                                break;
                            }
                            pre = id1;
                        }
                        Long time;
                        if (pre == null) {
                            ScriptActionEntity action = model.getActions().get(point.getParentId());
                            if (action == null) {
                                continue;
                            }
                            time = action.getDownTime();
                        } else {
                            ScriptPointEntity scriptPoint = model.getPoints().get(pre);
                            if (scriptPoint == null) {
                                continue;
                            }
                            time = scriptPoint.getTime();
                        }
                        long duration = point.getTime() - time;
                        if (duration > 0) {
                            //更新后续节点
                            model.adjustPointTime(id, -duration);
                        }
                    }
                    ScriptActionEntity action = model.getActions().get(point.getParentId());
                    if (action != null) {
                        action.setCount(action.getCount() - 1);
                    }
                    model.getPoints().remove(id);
                }
                updateChartData(binding.flowChatLayout.actionBarChart, new ArrayList<>(model.getActionBars().values()), new ArrayList<>(model.getActionColors().values()));
                updateChartData(binding.flowChatLayout.pointBarChart, new ArrayList<>(model.getPointBars().values()), new ArrayList<>(model.getPointColors().values()));
                //标记未保存
                model.setSaved(false);
            } catch (Exception e) {
                Log.d(this.getClass().getCanonicalName(), "removeCheckedPoint", e);
            } finally {
                ThreadUtil.runOnUi(() -> refresh.setDeleteDetail(false));
            }
        });
    }

    private View.OnClickListener createDeleteActionItemListener() {
        return e -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(R.string.delete_dialog_message)
                .setView(ActivityUtils.reinstatedView(dataBinding.getRoot()))
                .setPositiveButton(R.string.confirm, (dialog, which) -> removeCheckedAction(dataBinding.checkBox.isChecked()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void initChat() {
        //actionBar
        HorizontalBarChart actionBarChart = binding.flowChatLayout.actionBarChart;
        HorizontalBarChart pointBarChart = binding.flowChatLayout.pointBarChart;
        actionBarChart.getDescription().setEnabled(false);
        actionBarChart.setDrawValueAboveBar(true);
        actionBarChart.setPinchZoom(false);
        actionBarChart.setDrawGridBackground(false);
        XAxis xAxis = actionBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setAxisMinimum(-1f);
        YAxis leftAxis = actionBarChart.getAxisLeft();
        leftAxis.setEnabled(false);
        YAxis rightAxis = actionBarChart.getAxisRight();
        rightAxis.setDrawGridLines(true);
        rightAxis.setAxisMinimum(-1f);
        actionBarChart.setRenderer(new SingleStackRender(actionBarChart, actionBarChart.getAnimator(), actionBarChart.getViewPortHandler()));
        actionBarChart.setOnChartValueSelectedListener(createActionSelectedListener());
        setChartData(actionBarChart, new ArrayList<>(model.getActionBars().values()), new ArrayList<>(model.getActionColors().values()), (Long id) -> {
            ScriptActionEntity action = model.getActions().get(id);
            if (action == null) {
                return "--" + getString(R.string.unit_ms);
            }
            return (action.getUpTime() - action.getDownTime()) + getString(R.string.unit_ms) + "(" + getString(ScriptActionEntity.getTypeName(action.getType())) + ")";
        });
        actionBarChart.getLegend().setEnabled(false);
        //pointBar
        pointBarChart.getDescription().setEnabled(false);
        pointBarChart.setDrawValueAboveBar(true);
        pointBarChart.setPinchZoom(false);
        pointBarChart.setDrawGridBackground(false);
        XAxis xAxis2 = pointBarChart.getXAxis();
        xAxis2.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis2.setDrawGridLines(false);
        xAxis2.setGranularity(1f);
        xAxis2.setAxisMinimum(-1f);
        YAxis leftAxis2 = pointBarChart.getAxisLeft();
        leftAxis2.setEnabled(false);
        YAxis rightAxis2 = pointBarChart.getAxisRight();
        rightAxis2.setDrawGridLines(true);
        rightAxis2.setAxisMinimum(-1f);
        pointBarChart.setRenderer(new SingleStackRender(pointBarChart, pointBarChart.getAnimator(), pointBarChart.getViewPortHandler()));
        pointBarChart.setOnChartValueSelectedListener(createPointSelectedListener());
        pointBarChart.getLegend().setEnabled(false);
        setChartData(pointBarChart, new ArrayList<>(model.getPointBars().values()), new ArrayList<>(model.getPointColors().values()), (Long id) -> {
            ScriptPointEntity point = model.getPoints().get(id);
            if (point == null) {
                return "?";
            }
            Set<Long> ids = model.getPointMapByParentId().get(point.getParentId());
            if (ids == null || ids.isEmpty()) {
                return "?";
            }
            Long pre = null;
            for (Long item : ids) {
                if (item.compareTo(id) == 0) {
                    if (pre == null) {
                        ScriptActionEntity action = model.getActions().get(point.getParentId());
                        return action == null ? "?" : point.getTime() - action.getDownTime() + "ms";
                    } else {
                        ScriptPointEntity preEntity = model.getPoints().get(pre);
                        return preEntity == null ? "?" : point.getTime() - preEntity.getTime() + "ms";
                    }
                }
                pre = item;
            }
            return point.getX() + "ms";
        });
        //给checkAction添加监听
        model.getCheckedAction().addOnMapChangedCallback(new ObservableMap.OnMapChangedCallback<>() {

            @Override
            public void onMapChanged(ObservableMap<Long, Highlight> sender, Long key) {
                updateChartData(pointBarChart, new ArrayList<>(model.getPointBars().values()), new ArrayList<>(model.getPointColors().values()));
            }
        });
    }

    private OnChartValueSelectedListener createPointSelectedListener() {
        return new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                ObservableMap<Long, Highlight> map = model.getCheckedPoint();
                Long id = (Long) e.getData();
                if (map.containsKey(id)) {
                    map.remove(id);
                } else {
                    map.put(id, h);
                }
                // Manually update the chart's highlights
                Highlight[] highlightsArray = map.values().toArray(new Highlight[0]);
                binding.flowChatLayout.pointBarChart.highlightValues(highlightsArray);
            }

            @Override
            public void onNothingSelected() {
                Highlight[] highlightsArray = model.getCheckedPoint().values().toArray(new Highlight[0]);
                binding.flowChatLayout.pointBarChart.highlightValues(highlightsArray);
            }
        };
    }

    private OnChartValueSelectedListener createActionSelectedListener() {
        return new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                ObservableMap<Long, Highlight> map = model.getCheckedAction();
                Long id = (Long) e.getData();
                if (map.containsKey(id)) {
                    map.remove(id);
                } else {
                    map.put(id, h);
                }
                // Manually update the chart's highlights
                Highlight[] highlightsArray = map.values().toArray(new Highlight[0]);
                binding.flowChatLayout.actionBarChart.highlightValues(highlightsArray);
            }

            @Override
            public void onNothingSelected() {
                Highlight[] highlightsArray = model.getCheckedAction().values().toArray(new Highlight[0]);
                binding.flowChatLayout.actionBarChart.highlightValues(highlightsArray);
            }
        };
    }

    private void updateChartData(HorizontalBarChart chart, List<BarEntry> entries, List<Integer> colors) {
        BarData data = chart.getData();
        if (data == null) {
            return;
        }
        if (entries != null && data.getDataSetCount() > 0) {
            BarDataSet dataSet = (BarDataSet) data.getDataSetByIndex(0);
            dataSet.setValues(entries);
            if (colors != null) {
                dataSet.setColors(colors);
            }
        }
        //触发高亮
        chart.highlightValue(null, true);
        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        ThreadUtil.runOnUi(chart::invalidate);
    }

    @SuppressWarnings("unchecked")
    private <T> void setChartData(HorizontalBarChart chart, List<BarEntry> entries, List<Integer> colors, Function<T, String> formatter) {
        BarDataSet set = new BarDataSet(entries, "0");
        set.setStackLabels(new String[]{"", ""});
        set.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry barEntry) {
                return formatter.apply((T) barEntry.getData());
            }
        });
        set.setColors(colors);
        chart.setData(new BarData(set));
        chart.invalidate();
    }

    private void removeCheckedAction(boolean checked) {
        if (refresh.getDelete() || refresh.getDeleteDetail()) {
            return;
        }
        refresh.setDelete(true);
        ThreadUtil.runOnCpu(() -> {
            try {
                for (Long id : new ArrayList<>(model.getCheckedAction().keySet())) {
                    ScriptActionEntity action = model.getActions().get(id);
                    if (action == null) {
                        continue;
                    }
                    long duration = action.getMaxTime() - action.getDownTime();
                    //更新后续步骤
                    if (checked && duration > 0) {
                        model.adjustActionTime(id, -duration);
                    }
                    if (model.getRoot() != null) {
                        model.getRoot().setCount(model.getRoot().getCount() - 1);
                    }
                    model.getActions().remove(id);
                }
                updateChartData(binding.flowChatLayout.actionBarChart, new ArrayList<>(model.getActionBars().values()), new ArrayList<>(model.getActionColors().values()));
                updateChartData(binding.flowChatLayout.pointBarChart, new ArrayList<>(model.getPointBars().values()), new ArrayList<>(model.getPointColors().values()));
                //标记未保存
                model.setSaved(false);
            } catch (Exception e) {
                Log.d(this.getClass().getCanonicalName(), "removeCheckedAction", e);
            } finally {
                ThreadUtil.runOnUi(() -> refresh.setDelete(false));
            }
        });
    }

    public static void showFileInFolder(Context context, File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(context, R.string.message_file_not_exist, Toast.LENGTH_SHORT).show();
            return;
        }

        // 使用 FileProvider 生成 uri
        Uri uri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".provider",
                file
        );

        // 弹窗选择
        openFile(context, uri);
    }

    private static void openFile(Context context, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, R.string.message_no_available_app_open_file, Toast.LENGTH_SHORT).show();
        }
    }

    private void showRenameDialog() {
        // 创建一个 EditText 作为输入框
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(getString(R.string.message_input_prompt, getString(R.string.name)));
        input.setText(model.getRoot().getName());
        input.setFocusable(true);
        input.setFocusableInTouchMode(true);
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.rename)
                .setView(input)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        if (model.getRoot() != null) {
                            model.getRoot().setName(newName);
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.message_error_empty_ph, getString(R.string.name)), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss()).create();

        alertDialog.setOnShowListener(d -> {
            input.requestFocus();
            input.postDelayed(() -> {
                if (alertDialog.getWindow() == null) {
                    return;
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                alertDialog.getWindow().clearFlags(
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                );
                alertDialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                );
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }, 150);
        });
        alertDialog.show();
    }

    private void initIntent() {
        if (getIntent() == null) {
            refresh.setInfo(false);
            return;
        }
        Intent intent = getIntent();
        ThreadUtil.runOnCpu(() -> {
            ScriptVoEntity entity = null;
            try {
                entity = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                        intent.getParcelableExtra("data", ScriptVoEntity.class) : intent.getParcelableExtra("data");
                if (intent.getBooleanExtra("add", false)) {
                    model.setSaved(true);
                    model.setAdd(true);
                }
            } catch (Exception e) {
                Log.d(MacroInfoActivity.class.getCanonicalName(), "从intent获取数据失败! ", e);
                return;
            } finally {
                refresh.setInfo(false);
            }
            if (entity != null) {
                model.setRoot(entity.getRoot());
                model.setActions(entity.getActions());
                model.setPoints(entity.getPoints());
                ThreadUtil.runOnUi(() -> {
                    updateChartData(binding.flowChatLayout.actionBarChart, new ArrayList<>(model.getActionBars().values()), new ArrayList<>(model.getActionColors().values()));
                });
            }
        });
    }
}
