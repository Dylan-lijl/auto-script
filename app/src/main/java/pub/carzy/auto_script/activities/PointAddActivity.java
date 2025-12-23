package pub.carzy.auto_script.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOverlay;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableBoolean;

import java.util.ArrayList;
import java.util.List;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.databinding.ActivityPointAddBinding;
import pub.carzy.auto_script.db.ScriptPointEntity;
import pub.carzy.auto_script.utils.DraggableDotView;

/**
 * @author admin
 */
public class PointAddActivity extends BaseActivity {
    private Integer index;
    private Long time;
    private Long minTime;
    private Long maxTime;
    private ScriptPointEntity data;
    private ActivityPointAddBinding binding;
    private DraggableDotView dotView;
    private ViewGroup overlay;
    private int r = 20;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_point_add);
        binding.setShow(new ObservableBoolean(false));
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
        minTime = (minTime = intent.getLongExtra("minTime", -1)) == -1 ? null : minTime;
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
        binding.btnShow.setOnClickListener(createShowListener());
        binding.addInfoCurrent.setOnClickListener(e -> {
            if (time != null) {
                data.setTime(time);
            }
        });
        binding.addInfoStart.setOnClickListener(e -> {
            if (minTime != null) {
                data.setTime(minTime);
            }
        });
        binding.addInfoEnd.setOnClickListener(e -> {
            if (maxTime != null) {
                data.setTime(maxTime);
            }
        });
    }

    private void showDot() {
        if (dotView != null) return;

        overlay = (ViewGroup) getWindow().getDecorView();

        dotView = new DraggableDotView(this, r);
        dotView.setOnDotMoveListener(
                new DraggableDotView.OnDotMoveListener() {
                    @Override
                    public void onMove(float rawX, float rawY) {
                        data.setX(rawX);
                        data.setY(rawY);
                    }

                    @Override
                    public void onUp(float rawX, float rawY) {
                        data.setX(rawX);
                        data.setY(rawY);
                    }

                }
        );

        overlay.addView(dotView,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
        overlay.post(() -> updateDotByRaw(data.getX(), data.getY()));
    }

    private void updateDotByRaw(float rawX, float rawY) {
        int[] loc = new int[2];
        overlay.getLocationOnScreen(loc);

        dotView.setX(rawX - loc[0] - r);
        dotView.setY(rawY - loc[1] - r);
    }


    private View.OnClickListener createShowListener() {
        return ignite -> {
            if (dotView == null) {
                showDot();
                binding.getShow().set(true);
            } else {
                overlay.removeView(dotView);
                dotView = null;
                binding.getShow().set(false);
            }
        };
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
