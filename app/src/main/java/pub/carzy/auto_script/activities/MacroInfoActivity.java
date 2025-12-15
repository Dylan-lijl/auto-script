package pub.carzy.auto_script.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

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
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.databinding.ActionInfoBinding;
import pub.carzy.auto_script.databinding.ActivityMacroInfoBinding;
import pub.carzy.auto_script.databinding.AutoAlignDialogBinding;
import pub.carzy.auto_script.databinding.ChatToolbarMoreMenuBinding;
import pub.carzy.auto_script.db.ScriptActionEntity;
import pub.carzy.auto_script.db.ScriptPointEntity;
import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.model.MacroInfoRefreshModel;
import pub.carzy.auto_script.model.ScriptVoEntityModel;
import pub.carzy.auto_script.service.MyAccessibilityService;
import pub.carzy.auto_script.service.dto.OpenParam;
import pub.carzy.auto_script.service.impl.ReplayScriptAction;
import pub.carzy.auto_script.ui.adapter.SingleStackRender;
import pub.carzy.auto_script.utils.StoreUtil;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class MacroInfoActivity extends BaseActivity {

    private ActivityMacroInfoBinding binding;
    private AutoAlignDialogBinding dataBinding;
    private ActionInfoBinding actionInfoBinding;
    private ChatToolbarMoreMenuBinding moreMenuBinding;
    private ScriptVoEntityModel model;
    private MacroInfoRefreshModel refresh;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_macro_info);
        dataBinding = AutoAlignDialogBinding.inflate(LayoutInflater.from(this));
        moreMenuBinding = ChatToolbarMoreMenuBinding.inflate(LayoutInflater.from(this));
        actionInfoBinding = ActionInfoBinding.inflate(LayoutInflater.from(this));
        model = new ScriptVoEntityModel();
        for (int c : getResources().getIntArray(R.array.script_info_chat_color)) {
            model.getColorsResource().add(c);
        }
        refresh = new MacroInfoRefreshModel();
        refresh.setInfo(true);
        binding.setModel(model);
        binding.setRefresh(refresh);
        initChat();
        initIntent();
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        // 设置没有数据时显示的文字,,,按照mvvm思想这个属性应该写在xml,但是这个库未提供xml属性
        binding.flowChatLayout.actionBarChart.setNoDataText(getString(R.string.message_no_data));
        binding.flowChatLayout.pointBarChart.setNoDataText(getString(R.string.message_no_data));
        initialListeners();
    }

    private void initialListeners() {
        binding.flowChatToolbar.btnMoreAction.setOnClickListener(e -> {
            PopupWindow popupWindow = new PopupWindow(
                    reinstatedView(moreMenuBinding.getRoot()),
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
    }

    private View.OnClickListener createInfoListener() {
        return v -> {
            if (model.getCheckedAction().isEmpty()) {
                return;
            }
            actionInfoBinding.setEntity(model.getLastCheckedAction().getValue());
            new MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.dialog_action_info_title, model.getLastCheckedActionIndex()))
                    .setView(reinstatedView(actionInfoBinding.getRoot()))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        };
    }

    private View.OnClickListener createDeletePointItemListener() {
        return e -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(R.string.delete_dialog_message)
                .setView(reinstatedView(dataBinding.getRoot()))
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
                .setView(reinstatedView(dataBinding.getRoot()))
                .setPositiveButton(R.string.confirm, (dialog, which) -> removeCheckedAction(dataBinding.checkBox.isChecked()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private View reinstatedView(View root) {
        //复用
        // 如果已有父容器，先从父容器移除
        ViewParent parent = root.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(root);
        }
        return root;
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
            return action.getUpTime() - action.getDownTime() + getString(R.string.unit_ms);
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
        input.setHint(R.string.message_rename_prompt);
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
