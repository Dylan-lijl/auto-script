package pub.carzy.auto_script.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.databinding.ObservableBoolean;

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
import com.qmuiteam.qmui.skin.QMUISkinManager;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogBuilder;
import com.qmuiteam.qmui.widget.popup.QMUIPopup;
import com.qmuiteam.qmui.widget.popup.QMUIPopups;
import com.qmuiteam.qmui.widget.popup.QMUIQuickAction;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.IdGenerator;
import pub.carzy.auto_script.databinding.DialogActionInfoBinding;
import pub.carzy.auto_script.databinding.DialogScriptInfoBinding;
import pub.carzy.auto_script.databinding.ViewMacroInfoBinding;
import pub.carzy.auto_script.databinding.DialogPointInfoBinding;
import pub.carzy.auto_script.db.AppDatabase;
import pub.carzy.auto_script.db.entity.ScriptActionEntity;
import pub.carzy.auto_script.db.entity.ScriptEntity;
import pub.carzy.auto_script.db.entity.ScriptPointEntity;
import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.model.MacroInfoRefreshModel;
import pub.carzy.auto_script.model.ScriptVoEntityModel;
import pub.carzy.auto_script.service.MyAccessibilityService;
import pub.carzy.auto_script.service.data.ReplayModel;
import pub.carzy.auto_script.service.dto.OpenParam;
import pub.carzy.auto_script.service.impl.ReplayScriptAction;
import pub.carzy.auto_script.ui.BottomCustomSheetBuilder;
import pub.carzy.auto_script.ui.adapter.SingleStackRender;
import pub.carzy.auto_script.ui.entity.ActionInflater;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.MixedUtil;
import pub.carzy.auto_script.utils.ThreadUtil;
import pub.carzy.auto_script.utils.TypeToken;

/**
 * @author admin
 */
public class MacroInfoActivity extends BaseActivity {

    private ViewMacroInfoBinding binding;
    private ScriptVoEntityModel model;
    private MacroInfoRefreshModel refresh;
    private IdGenerator<Long> idWorker;
    private ActivityResultLauncher<Intent> addLauncher;
    private ActivityResultLauncher<Intent> addInfoLauncher;
    private AppDatabase db;
    private ObservableBoolean edit;
    private Runnable runnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        initChat();
        initIntent();
        initTopBar();
        initFlowChatLayout();
    }

    private void initFlowChatLayout() {
        // 设置没有数据时显示的文字,,,按照mvvm思想这个属性应该写在xml,但是这个库未提供xml属性
        binding.flowChatLayout.actionBarChart.setNoDataText(getString(R.string.message_no_data));
        binding.flowChatLayout.pointBarChart.setNoDataText(getString(R.string.message_no_data));
        binding.flowChatLayout.btnDelete.setOnClickListener(e -> deleteActionItem());
        binding.flowChatLayout.btnDeleteDetail.setOnClickListener(e -> deletePointItem());
        binding.flowChatLayout.btnInfo.setOnClickListener(e -> showInfo());
        binding.flowChatLayout.btnDetailInfo.setOnClickListener(e -> showDetailInfo());
        binding.flowChatLayout.btnAdd.setOnClickListener(e -> addAction());
        binding.flowChatLayout.btnInfoAdd.setOnClickListener(e -> addPoint());
        addLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), createProcessAddResult());
        addInfoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), createProcessAddInfoResult());
    }

    private void initTopBar() {
        initTopBar(binding.topBarLayout.actionBar);
        binding.flowChatToolbar.actionStartBtn.setOnClickListener(e -> changeRunService());
    }

    @Override
    protected void openBottomSheet() {
        QMUIBottomSheet.BottomListSheetBuilder builder = new QMUIBottomSheet.BottomListSheetBuilder(this)
                .setGravityCenter(false)
                .setAddCancelBtn(false)
                .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                    if (tag == null) {
                        dialog.dismiss();
                        return;
                    }
                    int id = ActionInflater.ActionItem.stringToId(tag);
                    if (id == R.id.action_remove) {
                        showDeleteDialog(dialog);
                        return;
                    }
                    if (id == R.id.action_run) {
                        changeRunService();
                    } else if (id == R.id.action_info) {
                        //这里需要弹窗来修改名字
                        if (model.getRoot() != null) {
                            showRoot();
                        }
                    } else if (id == R.id.action_save) {
                        saveData();
                    } else if (id == R.id.action_export) {
                        Toast.makeText(this, R.string.message_exporting, Toast.LENGTH_SHORT).show();
                        ThreadUtil.runOnCpu(() -> {
                            //将脚本保存为json文件
                            MixedUtil.exportScript(Collections.singleton(new ScriptVoEntity(model.getRoot(), model.getActionData(), model.getPointData())), this,
                                    (result) -> ThreadUtil.runOnUi(() -> {
                                        Toast.makeText(MacroInfoActivity.this, getString(R.string.message_saved_successfully_ph, result)
                                                , Toast.LENGTH_LONG).show();
                                        //打开对应文件夹位置
                                        //showFileInFolder(this, new File(result));
                                    }));
                        });
                    }
                    dialog.dismiss();
                });
        addActionByXml(builder, this, R.xml.actions_macro_info);
        QMUIBottomSheet build = builder.build();
        build.show();
    }

    private void changeRunService() {
        ActivityUtils.checkAccessibilityServicePermission(this, ok ->
                ThreadUtil.runOnCpu(() -> {
                    MyAccessibilityService service = BeanFactory.getInstance().get(MyAccessibilityService.class, false);
                    if (service != null) {
                        //重放这里需要剔除无用字段来节省内存
                        ReplayModel replayModel = ReplayModel.create(model.getRoot(), model.getActionData(), model.getPointData());
                        ThreadUtil.runOnUi(() -> service.open(ReplayScriptAction.ACTION_KEY, new OpenParam(replayModel)));
                    }
                }));
    }

    private void init() {
        idWorker = BeanFactory.getInstance().get(new TypeToken<IdGenerator<Long>>() {
        });
        db = BeanFactory.getInstance().get(AppDatabase.class);
        binding = DataBindingUtil.setContentView(this, R.layout.view_macro_info);
        model = new ScriptVoEntityModel();
        for (int c : getResources().getIntArray(R.array.script_info_chat_color)) {
            model.getColorsResource().add(c);
        }
        refresh = new MacroInfoRefreshModel();
        refresh.setInfo(true);
        binding.setModel(model);
        binding.setRefresh(refresh);
        edit = new ObservableBoolean(false);
        edit.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                model.setUnsaved(true);
            }
        });
        bindBackLogic();
    }

    private void bindBackLogic() {
        runnable = ActivityUtils.setOnBackPressed(this, r -> {
            if (model.getUnsaved()) {
                new QMUIDialog.MessageDialogBuilder(this)
                        .setTitle("提醒")
                        .setMessage("内容未保存，确定退出吗？")
                        .addAction("取消", (dialog, index) -> {
                            dialog.dismiss();
                            bindBackLogic();
                        })
                        .addAction("确定", (dialog, index) -> {
                            dialog.dismiss();
                            finish();
                        })
                        .create().show();
            } else {
                finish();
            }
        });
    }

    private ActivityResultCallback<ActivityResult> createProcessAddInfoResult() {
        return result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                if (data == null) {
                    return;
                }
                ScriptVoEntityModel.ScriptActionModel actionModel = model.getLastCheckAction();
                if (actionModel == null) {
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
                entity.setId(idWorker.nextId());
                entity.setActionId(actionModel.getKey());
                entity.setScriptId(model.getRoot().getId());
                model.addPoint(actionModel.getKey(), entity);
                ThreadUtil.runOnUi(() -> {
                    updateChartData(binding.flowChatLayout.actionBarChart, model.getActionBarEntries(), model.getActionColors());
                    updateChartData(binding.flowChatLayout.pointBarChart, model.getShowPointBarEntries(), model.getShowPointColors());
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
                entity.setScriptId(model.getRoot().getId());
                if (entity.getType() == ScriptActionEntity.GESTURE) {
                    entity.setDuration(0L);
                }
                model.addAction(entity);
                ThreadUtil.runOnUi(() -> {
                    //触发图表更新
                    updateChartData(binding.flowChatLayout.actionBarChart, model.getActionBarEntries(), model.getActionColors());
                });
            }
        };
    }


    private void showDeleteDialog(QMUIBottomSheet sheet) {
        if (refresh.getDelete()) {
            return;
        }
        //删除当前整个脚本
        ActivityUtils.createDeleteMessageDialog(this,
                        (d, i) -> {
                            refresh.setDelete(true);
                            ThreadUtil.runOnCpu(() -> {
                                try {
                                    db.runInTransaction(() -> {
                                        //删除对应数据
                                        Long id = model.getRoot().getId();
                                        db.scriptMapper().deleteById(id);
                                        db.scriptActionMapper().deleteByScriptId(id);
                                        db.scriptPointMapper().deleteByScriptId(id);
                                    });
                                    ThreadUtil.runOnUi(() -> {
                                        model.setUnsaved(false);
                                        d.dismiss();
                                        sheet.dismiss();
                                    });
                                } finally {
                                    ThreadUtil.runOnUi(() -> refresh.setDelete(false));
                                }
                            });
                        },
                        (d, i) -> d.dismiss())
                .show();
    }

    private void saveData() {
        if (refresh.getSave()) {
            return;
        }
        refresh.setSave(true);
        ThreadUtil.runOnCpu(() -> {
            try {
                //存数据
                db.runInTransaction(() -> {
                    //删除旧数据
                    db.scriptActionMapper().deleteByScriptId(model.getRoot().getId());
                    db.scriptPointMapper().deleteByScriptId(model.getRoot().getId());
                    ScriptEntity entity = model.getRoot();
                    if (entity == null) {
                        return;
                    }
                    Date now = new Date();
                    if (Objects.isNull(entity.getCreateTime())) {
                        entity.setCreateTime(now);
                    }
                    entity.setUpdateTime(now);
                    //保存
                    db.scriptMapper().save(model.getRoot());
                    db.scriptActionMapper().save(model.getActionData());
                    db.scriptPointMapper().save(model.getPointData());
                });
                //修改状态
                model.setUnsaved(false);
            } catch (Exception ex) {
                ThreadUtil.runOnUi(() -> Toast.makeText(MacroInfoActivity.this, "保存失败:" + ex.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                refresh.setSave(false);
            }
        });
    }

    private void addPoint() {
        ScriptVoEntityModel.ScriptActionModel action = model.getLastCheckAction();
        if (action == null || action.getData().getType() != ScriptActionEntity.GESTURE) {
            return;
        }
        ScriptPointEntity entity = new ScriptPointEntity();
        Intent intent = new Intent(this, PointAddActivity.class);
        ScriptVoEntityModel.ScriptPointModel point = model.getLastCheckShowPoint();
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(action.getPointIds()));
        if (!ids.isEmpty()) {
            int index = ids.indexOf(point.getKey());
            intent.putExtra("minOrder", model.getPoints().get(ids.get(0)).getOrder());
            intent.putExtra("maxOrder", model.getPoints().get(ids.get(ids.size() - 1)).getOrder());
            if (index != -1) {
                if (index > 0) {
                    intent.putExtra("beforeOrder", calculatedOrder(model.getPoints().get(ids.get(index - 1)).getOrder(), point.getData().getOrder()));
                }
                if (index < ids.size() - 1) {
                    intent.putExtra("afterOrder", calculatedOrder(point.getData().getOrder(), model.getPoints().get(ids.get(index + 1)).getOrder()));
                }
            }
            entity.setX(point.getData().getX());
            entity.setY(point.getData().getY());
        } else {
            intent.putExtra("minOrder", 50);
            intent.putExtra("maxOrder", 50);
        }
        if (point != null) {
            entity.setX(point.getData().getX());
            entity.setY(point.getData().getY());
            entity.setDeltaTime(point.getData().getDeltaTime());
            intent.putExtra("index", point.getBarEntry().getX());
        } else {
            entity.setX(200F);
            entity.setY(200F);
            entity.setDeltaTime(0L);
        }
        intent.putExtra("data", entity);
        //跳转到新增界面
        addInfoLauncher.launch(intent);
    }

    /**
     * 这个计算目的是防止小数位增长等于分化次数
     *
     * @param start 开始
     * @param end   结束
     * @return 中间值
     */
    private float calculatedOrder(float start, float end) {
        BigDecimal min = BigDecimal.valueOf(Math.min(start, end));
        BigDecimal max = BigDecimal.valueOf(Math.max(start, end));

        BigDecimal avg = min.add(max).divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);

        int minScale = min.stripTrailingZeros().scale();
        int maxScale = max.stripTrailingZeros().scale();
        int targetScale = Math.max(minScale, maxScale);

        // 规则 1：如果在当前精度下 avg 已经能区分
        BigDecimal avgAtScale = avg.setScale(targetScale, RoundingMode.HALF_UP);
        if (avgAtScale.compareTo(min) > 0 && avgAtScale.compareTo(max) < 0) {
            return avgAtScale.floatValue();
        }

        // 规则 2 / 3：在当前精度下尝试 ±1
        BigDecimal step = BigDecimal.ONE.movePointLeft(targetScale);
        BigDecimal down = avgAtScale.subtract(step);
        if (down.compareTo(min) > 0) {
            return down.floatValue();
        }

        BigDecimal up = avgAtScale.add(step);
        if (up.compareTo(max) < 0) {
            return up.floatValue();
        }

        // 都不行 → 增加一位小数
        BigDecimal finer = avg.setScale(targetScale + 1, RoundingMode.HALF_UP);
        return finer.floatValue();
    }


    private void addAction() {
        ScriptActionEntity entity = new ScriptActionEntity();
        entity.setStartTime(0L);
        entity.setCode(KeyEvent.KEYCODE_HOME);
        entity.setPointCount(0);
        entity.setIndex(0);
        entity.setType(ScriptActionEntity.GESTURE);
        Intent intent = new Intent(this, ActionAddActivity.class);
        intent.putExtra("data", entity);
        ScriptVoEntityModel.ScriptActionModel actionModel = model.getLastCheckAction();
        if (actionModel != null) {
            intent.putExtra("startTime", actionModel.getData().getStartTime());
            intent.putExtra("endTime", actionModel.getData().getStartTime() + actionModel.getData().getDuration());
            intent.putExtra("index", actionModel.getBarEntry().getX());
        }
        if (!model.getActions().isEmpty()) {
            ScriptVoEntityModel.ScriptActionModel last = model.getLastAction();
            if (last != null) {
                intent.putExtra("maxTime", last.getData().getStartTime() + last.getData().getDuration());
            }
        }
        //跳转到新增界面
        addLauncher.launch(intent);
    }

    private void showDetailInfo() {
        if (model.getCheckedPoint().isEmpty()) {
            return;
        }
        edit.set(false);
        DialogPointInfoBinding inflate = DialogPointInfoBinding.inflate(LayoutInflater.from(this));
        ScriptPointEntity data = model.getLastCheckShowPoint().getData();
        Long oldDeltaTime = data.getDeltaTime();
        inflate.setEntity(data);
        inflate.setEdit(edit);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        inflate.setWidth(metrics.widthPixels * 1F);
        inflate.setHeight(metrics.heightPixels * 1F);
        ImageButton imageButton = createModifyButton();
        imageButton.setOnClickListener(e -> {
            edit.set(!edit.get());
            imageButton.setImageResource(edit.get() ? R.drawable.exit : R.drawable.edit);
        });
        inflate.warnX.setOnClickListener(e -> {
            TextView textView = new TextView(this);
            textView.setText(getString(R.string.label_point_wranning, "x", metrics.widthPixels));
            textView.setPadding(50, 50, 50, 50);
            QMUIPopup popups = createPopups(this, textView);
            popups.show(e);
        });
        inflate.warnY.setOnClickListener(e -> {
            TextView textView = new TextView(this);
            textView.setText(getString(R.string.label_point_wranning, "y", metrics.widthPixels));
            textView.setPadding(50, 50, 50, 50);
            QMUIPopup popups = createPopups(this, textView);
            popups.show(e);
        });
        BottomCustomSheetBuilder builder = new BottomCustomSheetBuilder(this);
        builder.addView(inflate.getRoot())
                .setContentPaddingDp(20, 20)
                .setTitle(getString(R.string.dialog_action_info_title, (int) model.getLastCheckShowPoint().getBarEntry().getX()))
                .addRightButton(imageButton);
        QMUIBottomSheet build = builder.build();
        build.setOnDismissListener(e -> {
            //是否更改了持续时间
            boolean editDuration = oldDeltaTime.compareTo(data.getDeltaTime()) != 0;
            //是否更改了开始时间
            if (!editDuration) {
                return;
            }
            //添加更新root间隔
            model.getRoot().setTotalDuration(model.getRoot().getTotalDuration() + data.getDeltaTime() - oldDeltaTime);
            //这里还要更新action
            ScriptVoEntityModel.ScriptActionModel action = model.getLastCheckAction();
            action.getData().setDuration(action.getData().getDuration() + data.getDeltaTime() - oldDeltaTime);
            //更新布局
            updateChartData(binding.flowChatLayout.actionBarChart, model.getActionBarEntries(), model.getActionColors());
            updateChartData(binding.flowChatLayout.pointBarChart, model.getActionBarEntries(), model.getActionColors());
        });
        build.show();
    }

    private QMUIPopup createPopups(Context context, View view) {
        return QMUIPopups.popup(context, QMUIDisplayHelper.dp2px(context, 250))
                .preferredDirection(QMUIPopup.DIRECTION_BOTTOM)
                .view(view)
                .skinManager(QMUISkinManager.defaultInstance(context))
                .shadow(true)
                .arrow(true)
                .animStyle(QMUIPopup.ANIM_GROW_FROM_CENTER);
    }

    private void showInfo() {
        if (model.getCheckedAction().isEmpty()) {
            return;
        }
        edit.set(false);
        DialogActionInfoBinding inflate = DialogActionInfoBinding.inflate(LayoutInflater.from(this));
        ScriptActionEntity data = model.getLastCheckAction().getData();
        Long oldDuration = data.getDuration();
        Long oldStartTime = data.getStartTime();
        inflate.setEntity(data);
        inflate.setEdit(edit);
        ImageButton imageButton = createModifyButton();
        imageButton.setOnClickListener(e -> {
            edit.set(!edit.get());
            imageButton.setImageResource(edit.get() ? R.drawable.exit : R.drawable.edit);
        });
        List<CodeOption> options = CodeOption.allKeyEvent();
        inflate.dropdown.setAdapter(CodeOption.createAdapter(this, options));
        inflate.dropdown.setOnItemClickListener((parent, view, position, id) -> {
            CodeOption selected = options.get(position);
            model.getLastCheckAction().getData().setCode(selected.getCode());
        });
        BottomCustomSheetBuilder builder = new BottomCustomSheetBuilder(this);
        builder.addView(inflate.getRoot())
                .setContentPaddingDp(20, 20)
                .setTitle(getString(R.string.dialog_action_info_title, (int) model.getLastCheckAction().getBarEntry().getX()))
                .addRightButton(imageButton);
        QMUIBottomSheet build = builder.build();
        build.setOnDismissListener(e -> {
            //是否更改了持续时间
            boolean editDuration = oldDuration.compareTo(data.getDuration()) != 0;
            //是否更改了开始时间
            boolean editStartTime = oldStartTime.compareTo(data.getStartTime()) != 0;
            if (!editDuration && !editStartTime) {
                return;
            }
            //添加更新root间隔
            if (editDuration && data.getType() == ScriptActionEntity.KEY_EVENT) {
                model.updateActionDurationByActionId(data.getId(), oldDuration);
            }
            if (editStartTime) {
                model.updateActionStartTimeByActionId(data.getId(), oldStartTime);
            }
            //更新布局
            updateChartData(binding.flowChatLayout.actionBarChart, model.getActionBarEntries(), model.getActionColors());
        });
        build.show();
    }

    @NonNull
    private ImageButton createModifyButton() {
        ImageButton imageButton = new ImageButton(this);
        imageButton.setImageResource(R.drawable.edit);
        imageButton.setImageTintList(ContextCompat.getColorStateList(this, R.color.link));
        imageButton.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return imageButton;
    }

    private void deletePointItem() {
        QMUIDialog.CheckBoxMessageDialogBuilder builder = new QMUIDialog.CheckBoxMessageDialogBuilder(this)
                .setChecked(true)
                .setMessage(R.string.auto_adjust)
                .setTitle(R.string.delete_dialog_title);
        builder.addAction(R.string.cancel, (d, i) -> d.dismiss())
                .addAction(R.string.confirm, (d, i) -> removeCheckedPoint(builder.isChecked()))
                .create().show();
    }

    private void removeCheckedPoint(boolean checked) {
        if (refresh.getDeleteDetail() || refresh.getDetail()) {
            return;
        }
        refresh.setDeleteDetail(true);
        ThreadUtil.runOnCpu(() -> {
            try {
                model.deletePoint(model.getCheckedAction().keySet(), checked);
                ThreadUtil.runOnUi(() -> {
                    updateChartData(binding.flowChatLayout.actionBarChart, model.getActionBarEntries(), model.getActionColors());
                    updateChartData(binding.flowChatLayout.pointBarChart, model.getShowPointBarEntries(), model.getShowPointColors());
                    //标记未保存
                    model.setUnsaved(false);
                });
            } catch (Exception e) {
                Log.d(this.getClass().getCanonicalName(), "removeCheckedPoint", e);
            } finally {
                ThreadUtil.runOnUi(() -> refresh.setDeleteDetail(false));
            }
        });
    }

    private void deleteActionItem() {
        QMUIDialog.CheckBoxMessageDialogBuilder builder = new QMUIDialog.CheckBoxMessageDialogBuilder(this)
                .setChecked(true)
                .setMessage(R.string.auto_adjust)
                .setTitle(R.string.delete_dialog_title);
        builder.addAction(R.string.cancel, (d, i) -> d.dismiss())
                .addAction(R.string.confirm, (d, i) -> removeCheckedAction(builder.isChecked()))
                .create().show();
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
        setChartData(actionBarChart, model.getActionBarEntries(), model.getActionColors(), (Long id) -> {
            ScriptVoEntityModel.ScriptActionModel actionModel = model.getActions().get(id);
            if (actionModel == null) {
                return "--" + getString(R.string.unit_ms);
            }
            ScriptActionEntity data = actionModel.getData();
            return data.getDuration() + getString(R.string.unit_ms) + "(" + getString(ScriptActionEntity.getTypeName(data.getType())) + ")";
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
        setChartData(pointBarChart, model.getShowPointBarEntries(), model.getShowPointColors(), (Long id) -> {
            ScriptPointEntity point = model.getPoints().get(id);
            if (point == null) {
                return "--" + getString(R.string.unit_ms);
            }
            return point.getDeltaTime() + getString(R.string.unit_ms);
        });
    }

    private OnChartValueSelectedListener createPointSelectedListener() {
        return new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                model.addCheckPoint((Long) e.getData(), h);
                // Manually update the chart's highlights
                Highlight[] highlightsArray = model.getCheckedPoint().values().toArray(new Highlight[0]);
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
                model.addCheckAction((Long) e.getData(), h);
                // Manually update the chart's highlights
                Highlight[] highlightsArray = model.getCheckedAction().values().toArray(new Highlight[0]);
                binding.flowChatLayout.actionBarChart.highlightValues(highlightsArray);
                updateChartData(binding.flowChatLayout.pointBarChart, model.getShowPointBarEntries(), model.getShowPointColors());
            }

            @Override
            public void onNothingSelected() {
                Highlight[] highlightsArray = model.getCheckedAction().values().toArray(new Highlight[0]);
                binding.flowChatLayout.actionBarChart.highlightValues(highlightsArray);
                updateChartData(binding.flowChatLayout.pointBarChart, model.getShowPointBarEntries(), model.getShowPointColors());
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
                if (model.getCheckedAction().isEmpty()) {
                    return;
                }
                model.deleteAction(model.getCheckedAction().keySet(), checked);
                ThreadUtil.runOnUi(() -> {
                    updateChartData(binding.flowChatLayout.actionBarChart, model.getActionBarEntries(), model.getActionColors());
                    updateChartData(binding.flowChatLayout.pointBarChart, model.getShowPointBarEntries(), model.getShowPointColors());
                    //标记未保存
                    model.setUnsaved(false);
                });
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

    private void showRoot() {
        edit.set(false);
        DialogScriptInfoBinding binding = DialogScriptInfoBinding.inflate(LayoutInflater.from(this));
        binding.setEntity(model.getRoot());
        binding.setEdit(edit);
        ImageButton imageButton = createModifyButton();
        BottomCustomSheetBuilder builder = new BottomCustomSheetBuilder(this);
        builder.addView(binding.getRoot())
                .setContentPaddingDp(20, 20)
                .setTitle(getString(R.string.dialog_script_title))
                .addRightButton(imageButton);
        QMUIBottomSheet build = builder.build();
        build.show();
        imageButton.setOnClickListener(e -> {
            edit.set(!edit.get());
            imageButton.setImageResource(edit.get() ? R.drawable.exit : R.drawable.edit);
        });
    }

    private void initIntent() {
        if (getIntent() == null) {
            refresh.setInfo(false);
            return;
        }
        Intent intent = getIntent();
        ThreadUtil.runOnCpu(() -> {
            try {
                ScriptVoEntity entity = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                        intent.getParcelableExtra("data", ScriptVoEntity.class) : intent.getParcelableExtra("data");
                if (intent.getBooleanExtra("add", false)) {
                    ThreadUtil.runOnUi(() -> {
                        model.setUnsaved(true);
                        model.setAdd(true);
                    });
                }
                if (entity == null) {
                    return;
                }
                model.setRoot(entity.getRoot());
                model.setActions(entity.getActions());
                model.setPoints(entity.getPoints());
                ThreadUtil.runOnUi(() -> updateChartData(binding.flowChatLayout.actionBarChart, model.getActionBarEntries(), model.getActionColors()));
            } catch (Exception e) {
                Log.d(MacroInfoActivity.class.getCanonicalName(), "从intent获取数据失败! ", e);
            } finally {
                ThreadUtil.runOnUi(() -> refresh.setInfo(false));
            }
        });
    }

    @Getter
    public static class CodeOption {
        private final int code;
        private final String label;

        public CodeOption(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public static List<String> toLabels(List<CodeOption> options) {
            return options == null ? new ArrayList<>() : options.stream().map(CodeOption::getLabel).collect(Collectors.toList());
        }

        public static ArrayAdapter<String> createAdapter(Context context, List<CodeOption> options) {

            return new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, toLabels(options)) {
                @NonNull
                @Override
                public Filter getFilter() {
                    return new Filter() {
                        @Override
                        protected FilterResults performFiltering(CharSequence constraint) {
                            List<CodeOption> result = new ArrayList<>();
                            String value = constraint == null ? null : constraint.toString().trim().toUpperCase();
                            for (CodeOption opt : options) {
                                if (value == null || value.isEmpty() || opt.label.contains(value)) {
                                    result.add(opt);
                                }
                            }

                            FilterResults fr = new FilterResults();
                            fr.values = result;
                            fr.count = result.size();
                            return fr;
                        }

                        @SuppressWarnings("unchecked")
                        @Override
                        protected void publishResults(CharSequence constraint, FilterResults results) {
                            clear();
                            addAll(((List<CodeOption>) results.values).stream().map(CodeOption::getLabel).collect(Collectors.toList()));
                            notifyDataSetChanged();
                        }
                    };
                }
            };
        }

        public static Map<String, Integer> codeMap;

        public static List<CodeOption> allKeyEvent() {
            List<CodeOption> list = new ArrayList<>();
            if (codeMap == null) {
                synchronized (KeyEvent.class) {
                    if (codeMap == null) {
                        codeMap = new LinkedHashMap<>();
                        Field[] fields = KeyEvent.class.getDeclaredFields();
                        for (Field field : fields) {
                            if (Modifier.isStatic(field.getModifiers())
                                    && field.getName().startsWith("KEYCODE_")
                                    && field.getType() == int.class) {

                                try {
                                    int code = field.getInt(null);
                                    codeMap.put(KeyEvent.keyCodeToString(code), code);
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }
            }
            for (Map.Entry<String, Integer> entry : codeMap.entrySet()) {
                list.add(new CodeOption(entry.getValue(), entry.getKey()));
            }
            return list;
        }
    }
}
