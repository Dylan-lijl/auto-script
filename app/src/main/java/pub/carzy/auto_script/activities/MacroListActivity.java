package pub.carzy.auto_script.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.databinding.ObservableList;
import androidx.recyclerview.widget.RecyclerView;

import com.qmuiteam.qmui.alpha.QMUIAlphaImageButton;
import com.qmuiteam.qmui.recyclerView.QMUIRVItemSwipeAction;
import com.qmuiteam.qmui.recyclerView.QMUISwipeAction;
import com.qmuiteam.qmui.recyclerView.QMUISwipeViewHolder;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.util.QMUIKeyboardHelper;
import com.qmuiteam.qmui.util.QMUIViewHelper;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.pullLayout.QMUIPullLayout;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.databinding.ViewMacroListBinding;
import pub.carzy.auto_script.databinding.ComListItemMacroListBinding;
import pub.carzy.auto_script.db.AppDatabase;
import pub.carzy.auto_script.db.entity.ScriptActionEntity;
import pub.carzy.auto_script.db.entity.ScriptEntity;
import pub.carzy.auto_script.db.entity.ScriptPointEntity;
import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.model.MacroListModel;
import pub.carzy.auto_script.service.MyAccessibilityService;
import pub.carzy.auto_script.service.dto.OpenParam;
import pub.carzy.auto_script.service.impl.RecordScriptAction;
import pub.carzy.auto_script.service.impl.ReplayScriptAction;
import pub.carzy.auto_script.ui.entity.ActionInflater;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class MacroListActivity extends BaseActivity {
    private ViewMacroListBinding binding;
    private MacroListModel model;
    private AppDatabase db;

    private Adapter adapter;

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
                ScriptEntity tag = adapter.data.get(selected.getAdapterPosition());
                if (tag == null) {
                    return;
                }
                if (action == adapter.deleteAction) {
                    processDeleteItem(tag);
                } else if (action == adapter.runAction) {
                    runScript(tag);
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
        binding.topBarLayout.actionBar.setTitle(getActionBarTitle());
        QMUIAlphaImageButton manyBtn = binding.topBarLayout.actionBar.addRightImageButton(R.drawable.many_horizontal, QMUIViewHelper.generateViewId());
        manyBtn.setOnClickListener(e -> openBottomSheet());
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


    private void openBottomSheet() {
        QMUIBottomSheet.BottomListSheetBuilder builder = new QMUIBottomSheet.BottomListSheetBuilder(this)
                .setGravityCenter(false)
                .setAddCancelBtn(false)
                .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                    dialog.dismiss();
                    if (tag == null) {
                        return;
                    }
                    int id = ActionInflater.ActionItem.stringToId(tag);
                    if (defaultProcessMenu(id)) {
                        return;
                    }
                    if (id == R.id.record_script) {
                        openService();
                    }
                });
        addActionByXml(builder, this, R.xml.actions_macro_list);
        addDefaultMenu(builder);
        QMUIBottomSheet build = builder.build();
        build.show();
    }

    private void runScript(ScriptEntity script) {
        ThreadUtil.runOnCpu(() -> {
            //根据id查询action数据和point数据
            List<ScriptActionEntity> actions = db.scriptActionMapper().findByScriptId(script.getId());
            if (actions.isEmpty()) {
                return;
            }
            Set<Long> actionIds = actions.stream().map(ScriptActionEntity::getId).collect(Collectors.toSet());
            List<ScriptPointEntity> points = db.scriptPointMapper().findByActionIds(actionIds);
            ScriptVoEntity entity = new ScriptVoEntity();
            entity.setRoot(script);
            entity.getActions().addAll(actions);
            entity.getPoints().addAll(points);
            //打开服务
            ThreadUtil.runOnUi(() -> {
                MyAccessibilityService service = BeanFactory.getInstance().get(MyAccessibilityService.class, false);
                if (service != null) {
                    service.open(ReplayScriptAction.ACTION_KEY, new OpenParam(entity));
                }
            });
        });
    }

    private void processDeleteItem(ScriptEntity script) {
        if (!model.getDeleteIds().contains(script.getId())) {
            model.getDeleteIds().add(script.getId());
            ActivityUtils.createDeleteMessageDialog(this,
                    (dialog, which) -> ThreadUtil.runOnCpu(() -> {
                        try {
                            //删除数据
                            db.runInTransaction(() -> {
                                db.scriptMapper().delete(script);
                                List<Long> actionIds = db.scriptActionMapper().findIdByScriptId(script.getId());
                                if (!actionIds.isEmpty()) {
                                    db.scriptActionMapper().deleteByIds(actionIds);
                                    List<Long> pointIds = db.scriptPointMapper().findIdByActionIds(actionIds);
                                    if (!pointIds.isEmpty()) {
                                        db.scriptPointMapper().deleteByIds(pointIds);
                                    }
                                }
                            });
                            //在移除对应的数据
                            ThreadUtil.runOnUi(() -> {
                                model.getData().remove(script);
                                dialog.dismiss();
                            });
                        } finally {
                            ThreadUtil.runOnUi(() -> model.getDeleteIds().remove(script.getId()));
                        }
                    }), (dialog, i) -> {
                        model.getDeleteIds().remove(script.getId());
                        dialog.dismiss();
                    }).show();
        }
    }

    private void openService() {
        ActivityUtils.checkAccessibilityServicePermission(this,(ok) -> {
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
            deleteAction = builder.text("删除").backgroundColor(Color.RED).build();
            runAction = builder.text("详情").backgroundColor(Color.BLUE).build();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ComListItemMacroListBinding binding =
                    ComListItemMacroListBinding.inflate(inflater, parent, false);
            final VH vh = new VH(binding);
            vh.addSwipeAction(deleteAction);
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
    }

    static class VH extends QMUISwipeViewHolder {
        final ComListItemMacroListBinding binding;

        VH(ComListItemMacroListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
