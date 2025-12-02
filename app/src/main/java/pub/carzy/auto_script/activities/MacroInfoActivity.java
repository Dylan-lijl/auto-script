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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.databinding.ActivityMacroInfoBinding;
import pub.carzy.auto_script.db.ScriptActionEntity;
import pub.carzy.auto_script.db.ScriptPointEntity;
import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.model.MacroInfoRefreshModel;
import pub.carzy.auto_script.model.ScriptVoEntityModel;
import pub.carzy.auto_script.service.dto.OpenParam;
import pub.carzy.auto_script.service.impl.PreviewScriptAction;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class MacroInfoActivity extends BaseActivity {

    private ActivityMacroInfoBinding binding;

    private ScriptVoEntityModel model;
    private MacroInfoRefreshModel refresh;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_macro_info);
        model = new ScriptVoEntityModel();
        refresh = new MacroInfoRefreshModel();
        refresh.setInfo(true);
        binding.setModel(model);
        binding.setRefresh(refresh);
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

    private PopupMenu.OnMenuItemClickListener createActionClickListener() {
        return (item) -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_run) {

            } else if (itemId == R.id.action_preview) {
                //打开对应service悬浮窗口
                PreviewScriptAction service = BeanFactory.getInstance().get(PreviewScriptAction.class);
                if (service != null) {
                    ScriptVoEntity entity = new ScriptVoEntity();
                    entity.setRoot(model.getRoot());
                    entity.getActions().addAll(model.getActions());
                    entity.getPoints().addAll(model.getPoints());
                    OpenParam openParam = new OpenParam();
                    openParam.setData(entity);
                    service.open(openParam);
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
                for (ScriptActionEntity actionEntity : entity.getActions()) {
                    model.getActions().add(actionEntity);
                }
            }
        });
    }
}
