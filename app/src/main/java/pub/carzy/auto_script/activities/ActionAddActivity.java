package pub.carzy.auto_script.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.qmuiteam.qmui.alpha.QMUIAlphaImageButton;
import com.qmuiteam.qmui.util.QMUIViewHelper;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.databinding.ViewActionAddBinding;
import pub.carzy.auto_script.db.entity.ScriptActionEntity;
import pub.carzy.auto_script.ui.entity.ActionInflater;
import pub.carzy.auto_script.utils.Option;

/**
 * @author admin
 */
public class ActionAddActivity extends BaseActivity {
    private Integer index;
    private Long endTime;
    private Long startTime;
    private Long maxTime;
    private ScriptActionEntity data;
    private ViewActionAddBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.view_action_add);
        initIntent();
        initTopBar();
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

    private void initTopBar() {
        binding.topBarLayout.actionBar.setTitle(getActionBarTitle());
        QMUIAlphaImageButton manyBtn = binding.topBarLayout.actionBar.addRightImageButton(R.drawable.many_horizontal, QMUIViewHelper.generateViewId());
        manyBtn.setOnClickListener(e -> openBottomSheet());
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
                });
        addDefaultMenu(builder);
        QMUIBottomSheet build = builder.build();
        build.show();
    }


    private void initIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            //抛异常 todo
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data = intent.getParcelableExtra("data", ScriptActionEntity.class);
        } else {
            data = intent.getParcelableExtra("data");
        }
        if (data == null) {
            //报错 todo
        }
        index = (index = intent.getIntExtra("index", -1)) == -1 ? null : index;
        endTime = (endTime = intent.getLongExtra("endTime", -1)) == -1 ? null : endTime;
        startTime = (startTime = intent.getLongExtra("downTime", -1)) == -1 ? null : startTime;
        maxTime = (maxTime = intent.getLongExtra("maxTime", -1)) == -1 ? null : maxTime;
        binding.setIndex(index);
        binding.setData(data);
    }

    private void initListener() {
        binding.addStart.setOnClickListener(e -> {
            data.setStartTime(0L);
        });
        binding.addEnd.setOnClickListener(e -> {
            data.setStartTime(maxTime);
        });
        binding.addBefore.setOnClickListener(e -> {
            data.setStartTime(startTime);
        });
        binding.addAfter.setOnClickListener(e -> {
            data.setStartTime(endTime);
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
                data.setDuration(Long.parseLong(binding.durationInput.getText().toString()));
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
