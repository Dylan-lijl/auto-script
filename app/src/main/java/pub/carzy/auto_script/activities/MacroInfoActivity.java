package pub.carzy.auto_script.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import java.util.ArrayList;
import java.util.List;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.databinding.ActivityMacroInfoBinding;
import pub.carzy.auto_script.db.ScriptActionEntity;
import pub.carzy.auto_script.db.ScriptPointEntity;
import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.model.MacroInfoRefreshModel;
import pub.carzy.auto_script.model.ScriptVoEntityModel;
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
