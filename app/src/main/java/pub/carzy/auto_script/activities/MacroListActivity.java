package pub.carzy.auto_script.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.qmuiteam.qmui.alpha.QMUIAlphaImageButton;
import com.qmuiteam.qmui.recyclerView.QMUIRVItemSwipeAction;
import com.qmuiteam.qmui.recyclerView.QMUISwipeAction;
import com.qmuiteam.qmui.recyclerView.QMUISwipeViewHolder;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.util.QMUIKeyboardHelper;
import com.qmuiteam.qmui.util.QMUIViewHelper;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.pullLayout.QMUIPullLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.IdGenerator;
import pub.carzy.auto_script.databinding.DialogScriptImportBinding;
import pub.carzy.auto_script.databinding.ViewMacroListBinding;
import pub.carzy.auto_script.databinding.ComListItemMacroListBinding;
import pub.carzy.auto_script.db.AppDatabase;
import pub.carzy.auto_script.db.entity.ScriptActionEntity;
import pub.carzy.auto_script.db.entity.ScriptEntity;
import pub.carzy.auto_script.db.entity.ScriptPointEntity;
import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.entity.ExportScriptEntity;
import pub.carzy.auto_script.model.DialogScriptImportModel;
import pub.carzy.auto_script.model.MacroListModel;
import pub.carzy.auto_script.service.MyAccessibilityService;
import pub.carzy.auto_script.service.data.ReplayModel;
import pub.carzy.auto_script.service.dto.OpenParam;
import pub.carzy.auto_script.service.impl.RecordScriptAction;
import pub.carzy.auto_script.service.impl.ReplayScriptAction;
import pub.carzy.auto_script.ui.BottomCustomSheetBuilder;
import pub.carzy.auto_script.ui.entity.ActionInflater;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.BeanHandler;
import pub.carzy.auto_script.utils.MixedUtil;
import pub.carzy.auto_script.utils.ThreadUtil;
import pub.carzy.auto_script.utils.TypeToken;

/**
 * @author admin
 */
public class MacroListActivity extends BaseActivity {
    private ViewMacroListBinding binding;
    private MacroListModel model;
    private AppDatabase db;

    private Adapter adapter;
    private IdGenerator<Long> idWorker;
    private final ActivityResultLauncher<Intent> pickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    List<Uri> uris = new ArrayList<>();
                    if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            uris.add(clipData.getItemAt(i).getUri());
                        }
                    } else if (data.getData() != null) {
                        uris.add(data.getData());
                    }
                    ThreadUtil.runOnCpu(() -> {
                        handleImportFiles(uris);
                    });
                }
            });

    private void handleImportFiles(List<Uri> uris) {
        if (uris.isEmpty()) {
            return;
        }
        Map<String, ExportScriptEntity> data = new HashMap<>();
        Map<Integer, Set<Object>> warning = new HashMap<>();
        List<String> failed = new ArrayList<>();
        Set<ScriptVoEntity> success = new HashSet<>();
        Gson gson = new Gson();
        Set<Long> ids = new HashSet<>();
        //获取当前宽高
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        for (Uri uri : uris) {
            String path = uri.getLastPathSegment() == null ? "??" : uri.getLastPathSegment().substring(uri.getLastPathSegment().lastIndexOf("/") + 1);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)))) {
                String content = reader.lines().collect(Collectors.joining("\n")).trim();
                if (content.isEmpty()) {
                    continue;
                }
                //这里需要先读取version字段 后期遇到变更时再说
                //根据version字段在决定使用
                ExportScriptEntity exportScript = gson.fromJson(content, ExportScriptEntity.class);
                if (exportScript.getData().isEmpty()) {
                    continue;
                }
                data.put(path, exportScript);
                //宽高不一致则警告
                if (exportScript.getScreenWidth().compareTo(metrics.widthPixels) != 0 || exportScript.getScreenHeight().compareTo(metrics.heightPixels) != 0) {
                    warning.computeIfAbsent(ExportScriptEntity.SIZE_DISCREPANCY, k -> new HashSet<>()).add(path);
                }
                exportScript.getData().forEach(item -> {
                    ids.add(item.getRoot().getId());
                    success.add(item);
                });
            } catch (IOException e) {
                failed.add(path);
            }
        }
        //查看数据是否已经存在
        db.scriptMapper().findIdByIds(ids).forEach(id -> {
            ScriptVoEntity find = null;
            for (ScriptVoEntity entity : success) {
                if (entity.getRoot().getId().compareTo(id) == 0) {
                    find = entity;
                    break;
                }
            }
            if (find != null) {
                success.remove(find);
            }
            data.forEach((k, v) -> v.getData().forEach(item -> {
                if (item.getRoot().getId().compareTo(id) == 0) {
                    warning.computeIfAbsent(ExportScriptEntity.DATA_ALREADY_EXISTS, key -> new HashSet<>()).add(item);
                }
            }));
        });
        //没解析到数据直接提示
        if (data.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_data_to_import), Toast.LENGTH_SHORT).show();
        } else if (failed.isEmpty() && warning.isEmpty()) {
            saveExportScript(data.values().stream().map(ExportScriptEntity::getData).flatMap(Collection::stream).collect(Collectors.toList()), null);
        } else {
            ThreadUtil.runOnUi(() -> {
                DialogScriptImportBinding inflate = DialogScriptImportBinding.inflate(LayoutInflater.from(this));
                DialogScriptImportModel m = new DialogScriptImportModel();
                m.setFailed(failed);
                Set<Object> exist = warning.get(ExportScriptEntity.DATA_ALREADY_EXISTS);
                if (exist != null) {
                    //获取所有data
                    m.setExistWarning(exist.stream().map(item -> ((ScriptVoEntity) item))
                            //全部扁平化成一个集合
                            //遍历root返回名字+id的组合
                            .map(item -> item.getRoot().getName() + "(" + item.getRoot().getId() + ")")
                            .collect(Collectors.toList()));
                }
                Set<Object> size = warning.get(ExportScriptEntity.SIZE_DISCREPANCY);
                if (size != null) {
                    m.setSizeWarning(size.stream().map(Object::toString).collect(Collectors.toList()));
                }
                m.setSuccess(success.stream().map(item -> item.getRoot().getName() + "(" + item.getRoot().getId() + ")").collect(Collectors.toList()));
                inflate.setModel(m);
                ImageButton imageButton = new ImageButton(this);
                imageButton.setImageResource(R.drawable.save);
                imageButton.setImageTintList(ContextCompat.getColorStateList(this, R.color.link));
                imageButton.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                if (m.isHasSuccess()) {
                    inflate.successList.setAdapter(new StringAdapter(m.getSuccess()));
                    inflate.successList.setLayoutManager(new LinearLayoutManager(this));
                }
                if (m.isHasExistWarning()) {
                    inflate.existWarning.setAdapter(new StringAdapter(m.getExistWarning()));
                    inflate.existWarning.setLayoutManager(new LinearLayoutManager(this));
                }
                if (m.isHasSizeWarning()) {
                    inflate.sizeWarning.setAdapter(new StringAdapter(m.getSizeWarning()));
                    inflate.sizeWarning.setLayoutManager(new LinearLayoutManager(this));
                }
                if (m.isHasFailed()) {
                    inflate.failureList.setAdapter(new StringAdapter(failed));
                    inflate.failureList.setLayoutManager(new LinearLayoutManager(this));
                }
                //创建弹窗
                BottomCustomSheetBuilder builder = new BottomCustomSheetBuilder(this);
                builder.addView(inflate.getRoot())
                        .setContentPaddingDp(20, 20)
                        .setTitle(getString(R.string.dialog_import_title))
                        .addRightButton(imageButton);
                QMUIBottomSheet build = builder.build();
                imageButton.setOnClickListener((e) -> {
                    List<ScriptVoEntity> dbData = new ArrayList<>(data.size());
                    dbData.addAll(success);
                    //判断是否需要屏幕适配
                    if (m.getSizeAdapted() != R.id.size_skip) {
                        if (size != null) {
                            //等比例缩放
                            size.stream().map(item -> (String) item).forEach((v) -> {
                                ExportScriptEntity exportScript = data.get(v);
                                if (exportScript == null) {
                                    return;
                                }
                                if (m.getSizeAdapted() == R.id.size_auto_adapt) {
                                    if (exportScript.getScreenWidth() != metrics.widthPixels) {
                                        BigDecimal proportion = BigDecimal.valueOf(metrics.widthPixels).divide(BigDecimal.valueOf(exportScript.getScreenWidth()), 4, RoundingMode.HALF_UP);
                                        exportScript.getData().forEach(line -> {
                                            line.getPoints().forEach(point -> {
                                                point.setX(Math.max(0, proportion.multiply(BigDecimal.valueOf(point.getX())).setScale(2, RoundingMode.HALF_UP).floatValue()));
                                            });
                                        });
                                    }
                                    if (exportScript.getScreenHeight() != metrics.heightPixels) {
                                        BigDecimal proportion = BigDecimal.valueOf(metrics.heightPixels).divide(BigDecimal.valueOf(exportScript.getScreenHeight()), 4, RoundingMode.HALF_UP);
                                        exportScript.getData().forEach(line -> {
                                            line.getPoints().forEach(point -> {
                                                point.setY(Math.max(0, proportion.multiply(BigDecimal.valueOf(point.getY())).setScale(2, RoundingMode.HALF_UP).floatValue()));
                                            });
                                        });
                                    }
                                }
                                dbData.addAll(exportScript.getData());
                            });
                        }
                    }
                    //重复数据处理
                    if (exist != null) {
                        exist.stream().map(item -> (ScriptVoEntity) item).forEach(item -> {
                            if (m.getExistAdapted() == R.id.exist_skip) {
                                dbData.remove(item);
                                return;
                            }
                            Date now = new Date();
                            if (m.getExistAdapted() == R.id.exist_new) {
                                ScriptVoEntity entity = new ScriptVoEntity();
                                ScriptEntity root = BeanHandler.copy(item.getRoot(), ScriptEntity.class);
                                root.setCreateTime(now);
                                root.setUpdateTime(now);
                                entity.setRoot(root);
                                //重置id
                                root.setId(idWorker.nextId());
                                //映射id关系
                                Map<Long, Long> idsMap = new HashMap<>();
                                item.getActions().forEach(action -> {
                                    ScriptActionEntity copy = BeanHandler.copy(action, ScriptActionEntity.class);
                                    copy.setId(idWorker.nextId());
                                    copy.setScriptId(root.getId());
                                    idsMap.put(action.getId(), copy.getId());
                                    entity.getActions().add(copy);
                                });
                                item.getPoints().forEach(point -> {
                                    ScriptPointEntity copy = BeanHandler.copy(point, ScriptPointEntity.class);
                                    copy.setId(idWorker.nextId());
                                    copy.setScriptId(root.getId());
                                    copy.setActionId(idsMap.get(point.getActionId()));
                                    entity.getPoints().add(copy);
                                });
                                //添加新建的
                                dbData.add(entity);
                            }
                            //添加进去
                            exist.stream().map(o -> (ScriptVoEntity) o).forEach(o -> {
                                if (!dbData.contains(o)) {
                                    dbData.add(o);
                                }
                            });
                        });
                    }
                    //入库
                    saveExportScript(dbData, () -> ThreadUtil.runOnUi(build::dismiss));
                });
                build.show();
            });
        }

    }

    private void saveExportScript(List<ScriptVoEntity> voEntities, Runnable runnable) {
        ThreadUtil.runOnCpu(() -> {
            Set<Long> ids = voEntities.stream().map(item -> item.getRoot().getId()).collect(Collectors.toSet());
            //暂时不做id冲突检查
            db.runInTransaction(() -> {
                //删除数据
                db.scriptMapper().deleteByIds(ids);
                db.scriptActionMapper().deleteByScriptIds(ids);
                db.scriptPointMapper().deleteByScriptIds(ids);
                //添加数据
                db.scriptMapper().save(voEntities.stream().map(ScriptVoEntity::getRoot).collect(Collectors.toList()));
                db.scriptActionMapper().save(voEntities.stream().map(ScriptVoEntity::getActions).flatMap(Collection::stream).collect(Collectors.toList()));
                db.scriptPointMapper().save(voEntities.stream().map(ScriptVoEntity::getPoints).flatMap(Collection::stream).collect(Collectors.toList()));
            });
            //刷新数据
            model.reloadData();
            if (runnable != null) {
                runnable.run();
            }
        });
        ThreadUtil.runOnUi(() -> {
            Toast.makeText(this, getString(R.string.import_successful) + ":" + voEntities.size(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.script_list_title);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBase();
        initToolbar();
        initPullLayout();
        initDataList();
        model.reloadData();
//        binding.recycler.post(() -> model.reloadData());
    }

    private void initBase() {
        db = BeanFactory.getInstance().get(AppDatabase.class);
        idWorker = BeanFactory.getInstance().get(new TypeToken<IdGenerator<Long>>() {
        });
        model = new MacroListModel();
        binding = DataBindingUtil.setContentView(this, R.layout.view_macro_list);
        binding.setModel(model);
    }

    private void initDataList() {
        binding.dataList.setAdapter(adapter = new Adapter());
        QMUIRVItemSwipeAction action = new QMUIRVItemSwipeAction(true, new QMUIRVItemSwipeAction.Callback() {

            @Override
            public int getSwipeDirection(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return QMUIRVItemSwipeAction.SWIPE_LEFT;
            }

            @Override
            public void onClickAction(QMUIRVItemSwipeAction swipeAction, RecyclerView.ViewHolder selected, QMUISwipeAction action) {
                ScriptEntity script = adapter.data.get(selected.getAdapterPosition());
                if (script == null) {
                    return;
                }
                if (action == adapter.deleteAction) {
                    processDeleteItem(Collections.singleton(script.getId()), null);
                } else if (action == adapter.runAction) {
                    runScript(script);
                } else if (action == adapter.exportAction) {
                    Toast.makeText(MacroListActivity.this, R.string.message_exporting, Toast.LENGTH_SHORT).show();
                    ThreadUtil.runOnCpu(() -> {
                        List<ScriptActionEntity> actions = db.scriptActionMapper().findByScriptId(script.getId());
                        List<ScriptPointEntity> points = db.scriptPointMapper().findByScriptId(script.getId());
                        //将脚本保存为json文件
                        MixedUtil.exportScript(Collections.singletonList(new ScriptVoEntity(script, actions, points)), MacroListActivity.this,
                                (result) -> ThreadUtil.runOnUi(() ->
                                        Toast.makeText(MacroListActivity.this, getString(R.string.message_saved_successfully_ph, result)
                                                , Toast.LENGTH_LONG).show()));
                    });
                }
            }
        });
        action.attachToRecyclerView(binding.dataList);
    }

    private void initPullLayout() {
        AtomicReference<QMUIPullLayout.PullAction> currentPullAction = new AtomicReference<>();
        binding.pullLayout.setActionListener(pullAction -> {
            if (pullAction.getPullEdge() == QMUIPullLayout.PULL_EDGE_TOP) {
                model.reloadData();
                currentPullAction.set(pullAction);
            } else if (pullAction.getPullEdge() == QMUIPullLayout.PULL_EDGE_BOTTOM) {
                model.loadData();
                currentPullAction.set(pullAction);
            }
        });
        model.getLoading().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (!model.getLoading().get() && currentPullAction.get() != null) {
                    binding.pullLayout.finishActionRun(currentPullAction.get());
                }
            }
        });
    }

    private void initToolbar() {
        initTopBar(binding.topBarLayout.actionBar);
        QMUIAlphaImageButton searchBtn = binding.topBarLayout.actionBar.addRightImageButton(R.drawable.search, QMUIViewHelper.generateViewId());
        EditText searchEdit = createSearchEditText();
        searchBtn.setOnClickListener(v -> {
            binding.topBarLayout.actionBar.removeAllRightViews();
            binding.topBarLayout.actionBar.setTitle(null);
            binding.topBarLayout.actionBar.setCenterView(searchEdit);
            binding.topBarLayout.actionBar.addLeftBackImageButton().setOnClickListener(e -> {
                binding.topBarLayout.actionBar.removeCenterViewAndTitleView();
                binding.topBarLayout.actionBar.removeAllLeftViews();
                initToolbar();
            });
            searchEdit.post(() -> {
                searchEdit.requestFocus();
                QMUIKeyboardHelper.showKeyboard(searchEdit, true);
            });
        });
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                model.reloadData();
            }
        });
//        binding.btnRecord.setOnClickListener((e) -> openService());
    }

    private EditText createSearchEditText() {
        EditText et = new EditText(this);
        et.setHint(R.string.search_keyword);
        et.setSingleLine(true);
        et.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        et.setBackground(null);
        et.setTextSize(16);
        et.setPadding(0, 0, 0, 0);
        return et;
    }


    @Override
    protected void openBottomSheet() {
        QMUIBottomSheet.BottomListSheetBuilder builder = new QMUIBottomSheet.BottomListSheetBuilder(this)
                .setGravityCenter(false)
                .setAddCancelBtn(false)
                .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                    int id = ActionInflater.ActionItem.stringToId(tag);
                    if (defaultProcessMenu(id)) {
                        dialog.dismiss();
                        return;
                    }
                    if (id == R.id.delete_script) {
                        processDeleteItem(adapter.checkedIds, () -> {
                            adapter.checkedIds.clear();
                            List<Integer> indexes = new ArrayList<>();
                            for (int i = 0; i < model.getData().size(); i++) {
                                ScriptEntity e = model.getData().get(i);
                                if (!adapter.checkedIds.contains(e.getId())) {
                                    continue;
                                }
                                indexes.add(i);
                            }
                            indexes.forEach(i -> model.getData().remove(i.intValue()));
                            dialog.dismiss();
                        });
                        return;
                    }
                    if (id == R.id.export_script) {
                        exportSelectScript();
                    }
                    if (id == R.id.record_script) {
                        openService();
                    }
                    if (id == R.id.import_script) {
                        openImportDialog();
                    }
                    dialog.dismiss();
                });
        addActionByXml(builder, this, R.xml.actions_macro_list,
                (b, m, item) -> {
                    if (item.getId() == R.id.delete_script || item.getId() == R.id.export_script) {
                        if (!adapter.hasMultipleData()) {
                            return;
                        }
                        m.setText(item.getTitle() + "(" + adapter.checkedSize() + ")");
                    }
                    builder.addItem(m);
                });
        addDefaultMenu(builder);
        QMUIBottomSheet build = builder.build();
        build.show();
    }

    private void openImportDialog() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        //打开
        pickerLauncher.launch(intent);
    }

    private void exportSelectScript() {
        if (!adapter.hasMultipleData()) {
            return;
        }
        Toast.makeText(this, R.string.message_exporting, Toast.LENGTH_SHORT).show();
        ThreadUtil.runOnCpu(() -> {
            List<ScriptVoEntity> entities = new ArrayList<>(adapter.checkedSize());
            //查询对应数据
            List<ScriptActionEntity> actions = db.scriptActionMapper().findByScriptIds(adapter.checkedIds);
            List<ScriptPointEntity> points = db.scriptPointMapper().findByScriptIds(adapter.checkedIds);
            //组装起来
            for (ScriptEntity root : model.getData()) {
                if (!adapter.checkedIds.contains(root.getId())) {
                    continue;
                }
                List<ScriptActionEntity> actionEntities = new ArrayList<>();
                for (ScriptActionEntity action : actions) {
                    if (action.getScriptId().compareTo(root.getId()) == 0) {
                        actionEntities.add(action);
                    }
                }
                List<ScriptPointEntity> pointEntities = new ArrayList<>();
                for (ScriptPointEntity point : points) {
                    if (point.getScriptId().compareTo(root.getId()) == 0) {
                        pointEntities.add(point);
                    }
                }
                entities.add(new ScriptVoEntity(root, actionEntities, pointEntities));
            }
            MixedUtil.exportScript(entities, MacroListActivity.this,
                    (result) -> ThreadUtil.runOnUi(() -> {
                        adapter.checkedIds.clear();
                        Toast.makeText(MacroListActivity.this, getString(R.string.message_saved_successfully_ph, result)
                                , Toast.LENGTH_LONG).show();
                    }));
        });
    }

    private void runScript(ScriptEntity script) {
        ThreadUtil.runOnCpu(() -> {
            //根据id查询action数据和point数据
            List<ScriptActionEntity> actions = db.scriptActionMapper().findByScriptId(script.getId());
            if (actions.isEmpty()) {
                return;
            }
            List<ScriptPointEntity> points = db.scriptPointMapper().findByScriptId(script.getId());
            ReplayModel replayModel = ReplayModel.create(script, actions, points);
            //打开服务
            ThreadUtil.runOnUi(() -> ActivityUtils.checkAccessibilityServicePermission(this, ok -> {
                MyAccessibilityService service = BeanFactory.getInstance().get(MyAccessibilityService.class, false);
                if (service != null) {
                    service.open(ReplayScriptAction.ACTION_KEY, new OpenParam(replayModel));
                }
            }));
        });
    }

    private void processDeleteItem(Collection<Long> ids, Runnable success) {
        if (ids.isEmpty()) {
            return;
        }
        ActivityUtils.createDeleteMessageDialog(this,
                (dialog, which) -> ThreadUtil.runOnCpu(() -> {
                    model.getDeleteIds().addAll(ids);
                    try {
                        //删除数据
                        db.runInTransaction(() -> {
                            db.scriptMapper().deleteByIds(ids);
                            db.scriptActionMapper().deleteByScriptIds(ids);
                            db.scriptPointMapper().deleteByScriptIds(ids);
                        });
                        //在移除对应的数据
                        ThreadUtil.runOnUi(() -> {
                            //移除id对应的数据
                            for (ScriptEntity script : model.getData().stream()
                                    .filter(item -> ids.contains(item.getId())).collect(Collectors.toList())) {
                                model.getData().remove(script);
                            }
                            dialog.dismiss();
                        });
                        if (success != null) {
                            success.run();
                        }
                    } finally {
                        ThreadUtil.runOnUi(() -> ids.forEach(model.getDeleteIds()::remove));
                    }
                }), (dialog, i) -> {
                    dialog.dismiss();
                }).show();
    }

    private void openService() {
        ActivityUtils.checkAccessibilityServicePermission(this, (ok) -> {
            MyAccessibilityService service = BeanFactory.getInstance().get(MyAccessibilityService.class);
            if (service == null) {
                return;
            }
            if (!service.open(RecordScriptAction.ACTION_KEY, null)) {
                Toast.makeText(this, "打开失败!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RecordScriptAction service = BeanFactory.getInstance().get(RecordScriptAction.class, false);
        if (service == null) {
            return;
        }
        service.close(null);
    }

    private void jumpInfo(ScriptEntity entity) {
        ThreadUtil.runOnCpu(() -> {
            ScriptVoEntity data = new ScriptVoEntity();
            data.setRoot(entity);
            List<ScriptActionEntity> actions = db.scriptActionMapper().findByScriptId(entity.getId());
            data.getActions().addAll(actions);
            if (!actions.isEmpty()) {
                List<ScriptPointEntity> points = db.scriptPointMapper().findByActionIds(actions.stream().map(ScriptActionEntity::getId).collect(Collectors.toSet()));
                data.getPoints().addAll(points);
            }
            ThreadUtil.runOnUi(() -> {
                Intent intent = new Intent(this, MacroInfoActivity.class);
                intent.putExtra("data", data);
                intent.putExtra("add", false);
                startActivity(intent);
            });
        });
    }

    class Adapter extends RecyclerView.Adapter<VH> {
        private final ObservableList<ScriptEntity> data;
        private final QMUISwipeAction deleteAction;
        private final QMUISwipeAction runAction;
        private final QMUISwipeAction exportAction;
        private final ObservableList<Long> checkedIds = new ObservableArrayList<>();
        private final ObservableBoolean multiple = new ObservableBoolean(false);

        public Adapter() {
            this.data = model.getData();
            setHasStableIds(true);
            ObservableList.OnListChangedCallback<ObservableList<ScriptEntity>> callback = new ObservableList.OnListChangedCallback<>() {
                @SuppressLint("NotifyDataSetChanged")
                @Override
                public void onChanged(ObservableList<ScriptEntity> sender) {
                    notifyDataSetChanged();
                }

                @Override
                public void onItemRangeInserted(
                        ObservableList<ScriptEntity> sender,
                        int positionStart,
                        int itemCount
                ) {
                    notifyItemRangeInserted(positionStart, itemCount);
                }

                @Override
                public void onItemRangeRemoved(
                        ObservableList<ScriptEntity> sender,
                        int positionStart,
                        int itemCount
                ) {
                    notifyItemRangeRemoved(positionStart, itemCount);
                }

                @Override
                public void onItemRangeChanged(
                        ObservableList<ScriptEntity> sender,
                        int positionStart,
                        int itemCount
                ) {
                    notifyItemRangeChanged(positionStart, itemCount);
                }

                @Override
                public void onItemRangeMoved(
                        ObservableList<ScriptEntity> sender,
                        int fromPosition,
                        int toPosition,
                        int itemCount
                ) {
                    for (int i = 0; i < itemCount; i++) {
                        notifyItemMoved(fromPosition + i, toPosition + i);
                    }
                }
            };
            data.addOnListChangedCallback(callback);
            QMUISwipeAction.ActionBuilder builder = new QMUISwipeAction.ActionBuilder()
                    .textSize(QMUIDisplayHelper.sp2px(getApplicationContext(), 14))
                    .textColor(Color.WHITE)
                    .paddingStartEnd(QMUIDisplayHelper.dp2px(getApplicationContext(), 14));
            runAction = builder.text(getString(R.string.start)).backgroundColor(Color.BLUE).build();
            deleteAction = builder.text(getString(R.string.delete)).backgroundColor(Color.RED).build();
            exportAction = builder.text(getString(R.string.export)).backgroundColor(Color.GRAY).build();
        }

        public void reset() {
            data.clear();
            checkedIds.clear();
            multiple.set(false);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ComListItemMacroListBinding binding =
                    ComListItemMacroListBinding.inflate(inflater, parent, false);
            binding.setMultiple(multiple);
            binding.setIds(checkedIds);
            binding.getRoot().setOnLongClickListener(e -> {
                multiple.set(!multiple.get());
                return true;
            });
            final VH vh = new VH(binding);
            vh.addSwipeAction(deleteAction);
            vh.addSwipeAction(exportAction);
            vh.addSwipeAction(runAction);
            //点击跳转到详情
            binding.getRoot().setOnClickListener(e -> jumpInfo(data.get(vh.getAdapterPosition())));
            return vh;
        }


        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.binding.setItem(data.get(position));
        }


        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }

        public boolean hasMultipleData() {
            return multiple.get() && !checkedIds.isEmpty();
        }

        public int checkedSize() {
            return checkedIds.size();
        }
    }

    static class VH extends QMUISwipeViewHolder {
        final ComListItemMacroListBinding binding;

        VH(ComListItemMacroListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    class StringAdapter extends RecyclerView.Adapter<StringAdapter.VH> {

        private List<String> data;

        public StringAdapter(List<String> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView view = (TextView) LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.textView.setText(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public class VH extends RecyclerView.ViewHolder {
            TextView textView;

            VH(@NonNull View itemView) {
                super(itemView);
                textView = (TextView) itemView;
            }
        }
    }

}
