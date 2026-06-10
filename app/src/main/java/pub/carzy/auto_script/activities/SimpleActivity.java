package pub.carzy.auto_script.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.qmuiteam.qmui.widget.QMUITopBarLayout;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.databinding.ViewSimpleBinding;

public class SimpleActivity extends BaseActivity {
    private ViewSimpleBinding binding;
    @Override
    protected QMUITopBarLayout getTopBar() {
        return binding.topBarLayout.actionBar;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.view_simple);
        initTopBar();
    }

}
