package pub.carzy.auto_script.activities;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.databinding.ObservableList;

import java.util.ArrayList;
import java.util.function.Consumer;

import in.srain.cube.views.ptr.PtrClassicDefaultHeader;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.adapter.MacroTableAdapter;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.databinding.ActivityMacroListBinding;
import pub.carzy.auto_script.db.entity.ScriptEntity;
import pub.carzy.auto_script.model.MacroListModel;
import pub.carzy.auto_script.service.MyAccessibilityService;
import pub.carzy.auto_script.service.impl.RecordScriptAction;
import pub.carzy.auto_script.ui.ExtImageButton;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class MacroListActivity extends BaseActivity {
    private Boolean ok = false;

    private ActivityMacroListBinding binding;
    private MacroListModel model;

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.script_list_title);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = new MacroListModel();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_macro_list);
        MacroTableAdapter adapter = new MacroTableAdapter();
        binding.recycler.setAdapter(adapter);
        addListeners(adapter);
        binding.btnRecord.setOnClickListener((e) -> openService());
        model.reloadData();
//        binding.recycler.post(() -> model.reloadData());
    }

    private void addListeners(MacroTableAdapter adapter) {
        model.getData().addOnListChangedCallback(
                new ObservableList.OnListChangedCallback<>() {

                    @Override
                    public void onChanged(ObservableList<ScriptEntity> sender) {
                        adapter.submitList(new ArrayList<>(sender));
                    }

                    @Override
                    public void onItemRangeInserted(
                            ObservableList<ScriptEntity> sender,
                            int positionStart,
                            int itemCount
                    ) {
                        adapter.submitList(new ArrayList<>(sender));
                    }

                    @Override
                    public void onItemRangeRemoved(
                            ObservableList<ScriptEntity> sender,
                            int positionStart,
                            int itemCount
                    ) {
                        adapter.submitList(new ArrayList<>(sender));
                    }

                    @Override
                    public void onItemRangeChanged(
                            ObservableList<ScriptEntity> sender,
                            int positionStart,
                            int itemCount
                    ) {
                        adapter.submitList(new ArrayList<>(sender));
                    }

                    @Override
                    public void onItemRangeMoved(
                            ObservableList<ScriptEntity> sender,
                            int fromPosition,
                            int toPosition,
                            int itemCount
                    ) {
                        adapter.submitList(new ArrayList<>(sender));
                    }
                });
        binding.swipeRefresh.setPtrHandler(new PtrDefaultHandler() {
            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return super.checkCanDoRefresh(frame, content, header) && !binding.recycler.canScrollVertically(-1);
            }

            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                model.reloadData();
            }
        });
        PtrClassicDefaultHeader header =
                new PtrClassicDefaultHeader(this);

        binding.swipeRefresh.setHeaderView(header);
        binding.swipeRefresh.addPtrUIHandler(header);

        model.getLoading().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (model.getLoading().get()) {
                    binding.swipeRefresh.refreshComplete();
                }
            }
        });
    }

    private void openService() {
        Runnable runnable = () -> {
            MyAccessibilityService service = BeanFactory.getInstance().get(MyAccessibilityService.class);
            if (service == null) {
                return;
            }
            if (!service.open(RecordScriptAction.ACTION_KEY, null)) {
                Toast.makeText(this, "打开失败!", Toast.LENGTH_SHORT).show();
            }
        };
        if (ok) {
            runnable.run();
        } else {
            checkPermission(ok -> {
                if (ok) {
                    runnable.run();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RecordScriptAction service = BeanFactory.getInstance().get(RecordScriptAction.class);
        if (service == null) {
            return;
        }
        service.close(null);
    }

    private void checkPermission(Consumer<Boolean> callback) {
        MyAccessibilityService.checkOpenAccessibility((enabled) -> {
            if (!enabled) {
                //打开提示
                promptAccessibility();
                return;
            }
            //检查悬浮窗权限
            MyAccessibilityService.checkOpenFloatWindow((e) -> {
                if (!e) {
                    promptOverlay();
                    return;
                }
                ok = true;
                if (callback != null) {
                    callback.accept(ok);
                }
            });
        });
    }

    private void promptOverlay() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_prompt)
                .setMessage(R.string.float_button_permission)
                .setPositiveButton(R.string.go_to_open, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void promptAccessibility() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_prompt)
                .setMessage(R.string.permission_content)
                .setPositiveButton(R.string.go_to_open, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
