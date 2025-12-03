package pub.carzy.auto_script.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import cn.hutool.core.lang.Pair;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.databinding.ActivityMacroInfoBinding;
import pub.carzy.auto_script.db.ScriptActionEntity;
import pub.carzy.auto_script.db.ScriptPointEntity;
import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.model.MacroInfoRefreshModel;
import pub.carzy.auto_script.model.ScriptVoEntityModel;
import pub.carzy.auto_script.service.MyAccessibilityService;
import pub.carzy.auto_script.service.dto.OpenParam;
import pub.carzy.auto_script.service.impl.PreviewScriptAction;
import pub.carzy.auto_script.ui.adapter.SingleStackRender;
import pub.carzy.auto_script.utils.ThreadUtil;
import pub.carzy.auto_script.utils.statics.StaticValues;

/**
 * @author admin
 */
public class MacroInfoActivity extends BaseActivity {

    private ActivityMacroInfoBinding binding;

    private ScriptVoEntityModel model;
    private MacroInfoRefreshModel refresh;
    private List<Integer> colorArray;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_macro_info);
        model = new ScriptVoEntityModel();
        refresh = new MacroInfoRefreshModel();
        refresh.setInfo(true);
        binding.setModel(model);
        binding.setRefresh(refresh);
        colorArray = new ArrayList<>();
        Arrays.stream(getResources().getIntArray(R.array.script_info_chat_color)).forEach(color -> colorArray.add(color));
        initChat();
        initIntent();
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        binding.flowChatToolbar.btnMoreAction.setOnClickListener(e -> {
            PopupMenu popup = new PopupMenu(this, e);
            popup.getMenuInflater().inflate(R.menu.macro_info_more_action_menus, popup.getMenu());
            popup.setOnMenuItemClickListener(createActionClickListener());
            popup.show();
        });

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
        actionBarChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {

            @Override
            public void onValueSelected(Entry e, Highlight h) {
                Pair<Integer, Long> pair = (Pair<Integer, Long>) e.getData();
                if (pair == null || refresh.getDetail()) {
                    return;
                }
                refresh.setDetail(true);
                try {
                    int i = pair.getKey();
                    if (i >= model.getActions().size()) {
                        return;
                    }
                    model.setDetailIndex(i);
                    ScriptActionEntity action = model.getActions().get(i);
                    if (action != null && !model.getPoints().isEmpty()) {
                        List<BarEntry> entries = new ArrayList<>();
                        long time = action.getDownTime();
                        int k = 0;
                        for (int j = 0; j < model.getPoints().size(); j++) {
                            ScriptPointEntity point = model.getPoints().get(j);
                            if (action.getId().compareTo(point.getParentId()) != 0) {
                                continue;
                            }
                            Pair<Integer, Long> pointPair = new Pair<>(j, point.getTime() - time);
                            entries.add(new BarEntry(k++, new float[]{time, pointPair.getValue()}, pointPair));
                            time = point.getTime();
                        }
                        if (!entries.isEmpty()) {
                            ThreadUtil.runOnUi(() -> {
                                try {
                                    setChartData(pointBarChart, entries, new ArrayList<>(colorArray), "", (Pair<Integer, Long> pointPair) -> pointPair.getValue() + "ms");
                                    pointBarChart.invalidate();
                                } finally {
                                    refresh.setDetail(false);
                                }
                            });
                        }
                    }
                } catch (Exception exception) {
                    refresh.setDetail(false);
                    Log.e("MacroInfoActivity", "onValueSelected: ", exception);
                }
            }

            @Override
            public void onNothingSelected() {
//                model.setDetailIndex(StaticValues.DEFAULT_INDEX);
            }
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
        pointBarChart.getLegend().setEnabled(false);
    }

    @SuppressWarnings("unchecked")
    private <T> void setChartData(HorizontalBarChart chart, List<BarEntry> entries, List<Integer> colors, String label, Function<T, String> formatter) {
        BarDataSet set = new BarDataSet(entries, label);
        set.setStackLabels(new String[]{"", ""});
        set.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry barEntry) {
                return formatter.apply((T) barEntry.getData());
            }
        });
        set.setColors(colors);
        chart.setData(new BarData(set));
    }

    private PopupMenu.OnMenuItemClickListener createActionClickListener() {
        return (item) -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_run) {

            } else if (itemId == R.id.action_preview) {
                //打开对应service悬浮窗口
                MyAccessibilityService service = BeanFactory.getInstance().get(MyAccessibilityService.class);
                if (service != null) {
                    ScriptVoEntity entity = new ScriptVoEntity();
                    entity.setRoot(model.getRoot());
                    entity.getActions().addAll(model.getActions());
                    entity.getPoints().addAll(model.getPoints());
                    service.open(PreviewScriptAction.ACTION_KEY, new OpenParam(entity));
                }
            } else if (itemId == R.id.action_remove) {

            } else if (itemId == R.id.action_rename) {
                //这里需要弹窗来修改名字
                if (model.getRoot() != null) {
                    showRenameDialog();
                }
            } else if (itemId == R.id.action_save) {

            }
            return false;
        };
    }

    private void showRenameDialog() {
        // 创建一个 EditText 作为输入框
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("请输入新名称");

        new MaterialAlertDialogBuilder(this)
                .setTitle("重命名")
                .setView(input)
                .setPositiveButton("确认", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        if (model.getRoot() != null) {
                            model.getRoot().setName(newName);
                        }
                    } else {
                        Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .show();
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    entity = intent.getParcelableExtra("data", ScriptVoEntity.class);
                } else {
                    entity = intent.getParcelableExtra("data");
                }
            } catch (Exception e) {
                Log.d(MacroInfoActivity.class.getCanonicalName(), "从intent获取数据失败! ", e);
                return;
            } finally {
                refresh.setInfo(false);
            }
            if (entity != null) {
                model.setRoot(entity.getRoot());
                for (ScriptPointEntity pointEntity : entity.getPoints()) {
                    model.getPoints().add(pointEntity);
                }
                int[] colorArray = getResources().getIntArray(R.array.script_info_chat_color);
                int colorLength = colorArray.length;
                List<BarEntry> entries = new ArrayList<>();
                List<Integer> colors = new ArrayList<>();
                for (int i = 0; i < entity.getActions().size(); i++) {
                    ScriptActionEntity actionEntity = entity.getActions().get(i);
                    colors.add(colorArray[actionEntity.getIndex() % colorLength]);
                    Pair<Integer, Long> pair = new Pair<>(i, actionEntity.getUpTime() - actionEntity.getDownTime());
                    entries.add(new BarEntry(i, new float[]{actionEntity.getDownTime(), pair.getValue()}, pair));
                    model.getActions().add(actionEntity);
                }
                ThreadUtil.runOnUi(() -> {
                    setChartData(binding.flowChatLayout.actionBarChart, entries, colors, "", (Pair<Integer, Long> pair) -> pair.getValue() + "ms");
                    binding.flowChatLayout.actionBarChart.invalidate();
                });
            }
        });
    }
}
