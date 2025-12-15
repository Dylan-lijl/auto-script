package pub.carzy.auto_script.activities;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;

import java.util.function.Consumer;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.controller.MacroListController;
import pub.carzy.auto_script.databinding.ActivityMacroListBinding;
import pub.carzy.auto_script.service.MyAccessibilityService;
import pub.carzy.auto_script.service.impl.RecordScriptAction;
import pub.carzy.auto_script.ui.ExtImageButton;

/**
 * @author admin
 */
public class MacroListActivity extends BaseActivity {
    private MacroListController controller;
    private Boolean ok = false;

    private ActivityMacroListBinding binding;

    @Override
    protected Integer getActionBarTitle() {
        return R.string.script_list_title;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_macro_list);
        controller = new MacroListController();
        ExtImageButton btnRecord = findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener((e) -> openService());
    }

    private void openService() {
        Runnable runnable = () -> {
            MyAccessibilityService service = BeanFactory.getInstance().get(MyAccessibilityService.class);
            if (service == null) {
                return;
            }
            if (!service.open(RecordScriptAction.ACTION_KEY, null)) {
                Toast.makeText(this, "打开失败!", Toast.LENGTH_SHORT);
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

    @Override
    protected void onStart() {
        super.onStart();
//        checkPermission();
    }

    private void checkPermission() {
        checkPermission(null);
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
