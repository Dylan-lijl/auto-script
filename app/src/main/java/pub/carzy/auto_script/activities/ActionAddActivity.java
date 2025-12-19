package pub.carzy.auto_script.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.adapter.CodeOptionAdapter;
import pub.carzy.auto_script.databinding.ActivityActionAddBinding;
import pub.carzy.auto_script.db.ScriptActionEntity;
import pub.carzy.auto_script.utils.Option;

/**
 * @author admin
 */
public class ActionAddActivity extends BaseActivity {
    private Integer index;
    private Long upTime;
    private Long downTime;
    private Long maxTime;
    private ScriptActionEntity data;
    private ActivityActionAddBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_action_add);
        Intent intent = getIntent();
        if (intent == null) {
            //抛异常
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data = intent.getParcelableExtra("data", ScriptActionEntity.class);
        } else {
            data = intent.getParcelableExtra("data");
        }
        if (data == null) {
            //报错
        }
        index = (index = intent.getIntExtra("index", -1)) == -1 ? null : index;
        //更新标题
        updateActionBarTitle(getActionBarTitle());
        upTime = (upTime = intent.getLongExtra("upTime", -1)) == -1 ? null : upTime;
        downTime = (downTime = intent.getLongExtra("downTime", -1)) == -1 ? null : downTime;
        maxTime = (maxTime = intent.getLongExtra("maxTime", -1)) == -1 ? null : maxTime;
        binding.setIndex(index);
        binding.setData(data);
        List<Option<Integer>> options = Arrays.asList(
                new Option<>(KeyEvent.keyCodeToString(KeyEvent.KEYCODE_HOME), KeyEvent.KEYCODE_HOME),
                new Option<>(KeyEvent.keyCodeToString(KeyEvent.KEYCODE_BACK), KeyEvent.KEYCODE_BACK),
                new Option<>(KeyEvent.keyCodeToString(KeyEvent.KEYCODE_MENU), KeyEvent.KEYCODE_MENU),
                new Option<>(KeyEvent.keyCodeToString(KeyEvent.KEYCODE_VOLUME_UP), KeyEvent.KEYCODE_VOLUME_UP),
                new Option<>(KeyEvent.keyCodeToString(KeyEvent.KEYCODE_VOLUME_DOWN), KeyEvent.KEYCODE_VOLUME_DOWN),
                new Option<>(KeyEvent.keyCodeToString(KeyEvent.KEYCODE_POWER), KeyEvent.KEYCODE_POWER)
        );
        ArrayAdapter<Option<Integer>> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        options
                );
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        binding.codeInput.setAdapter(adapter);
        initListener();
    }

    private void initListener() {
        binding.addStart.setOnClickListener(e -> {
            data.setDownTime(0L);
        });
        binding.addEnd.setOnClickListener(e -> {
            data.setDownTime(maxTime);
        });
        binding.addBefore.setOnClickListener(e -> {
            data.setDownTime(downTime);
        });
        binding.addAfter.setOnClickListener(e -> {
            data.setDownTime(upTime);
        });
        binding.btnSubmit.setOnClickListener(createSubmitListener());
        binding.btnCancel.setOnClickListener(createCancelListener());
    }

    private View.OnClickListener createCancelListener() {
        return e -> {
            setResult(RESULT_OK, null);
            finish();
        };
    }

    private View.OnClickListener createSubmitListener() {
        return v -> {
            List<String> errors = checkForm();
            if (!errors.isEmpty()) {
                Toast.makeText(this, String.join("\n", errors), Toast.LENGTH_LONG).show();
                return;
            }
            //如果是按键事件就要添加持续时长
            if (data.getType() == ScriptActionEntity.KEY_EVENT) {
                data.setUpTime(data.getDownTime() + Integer.parseInt(binding.durationInput.getText().toString()));
            }
            Intent intent = new Intent();
            intent.putExtra("data", data);
            intent.putExtra("auto", binding.autoAdjustInput.isChecked());
            intent.putExtra("index", index);
            setResult(RESULT_OK, intent);
            finish();
        };
    }

    private List<String> checkForm() {
        return new ArrayList<>();
    }

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.add) + getString(R.string.steps) + (index == null ? "" : "-" + index);
    }
}
