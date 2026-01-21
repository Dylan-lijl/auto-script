package pub.carzy.auto_script.activities.about.child;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.BaseActivity;
import pub.carzy.auto_script.databinding.ComAboutFAQBinding;

/**
 * @author admin
 */
public class AboutFAQActivity extends BaseActivity {
    private ComAboutFAQBinding binding;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.com_about_f_a_q);
        initTopBar(binding.topBarLayout.actionBar);
    }

}
