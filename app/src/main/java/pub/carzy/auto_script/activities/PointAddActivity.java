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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.databinding.ActivityActionAddBinding;
import pub.carzy.auto_script.databinding.ActivityPointAddBinding;
import pub.carzy.auto_script.db.ScriptActionEntity;
import pub.carzy.auto_script.db.ScriptPointEntity;
import pub.carzy.auto_script.utils.Option;

/**
 * @author admin
 */
public class PointAddActivity extends BaseActivity {
    private Integer index;
    private Long time;
    private Long maxTime;
    private ScriptPointEntity data;
    private ActivityPointAddBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_point_add);
        Intent intent = getIntent();
        if (intent == null) {
            //抛异常
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data = intent.getParcelableExtra("data", ScriptPointEntity.class);
        } else {
            data = intent.getParcelableExtra("data");
        }
        if (data == null) {
            //报错
        }
        index = (index = intent.getIntExtra("index", -1)) == -1 ? null : index;
        //更新标题
        updateActionBarTitle(getActionBarTitle());
        time = (time = intent.getLongExtra("time", -1)) == -1 ? null : time;
        maxTime = (maxTime = intent.getLongExtra("maxTime", -1)) == -1 ? null : maxTime;
        if (index != null && time != null) {
            data.setTime(time);
        } else if (maxTime != null) {
            data.setTime(maxTime);
        }
        binding.setIndex(index);
        binding.setData(data);
        initListener();
    }

    private void initListener() {
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
            Intent intent = new Intent();
            intent.putExtra("data", data);
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
        return getString(R.string.add) + getString(R.string.details) + (index == null ? "" : "-" + index);
    }
}
