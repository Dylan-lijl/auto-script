package pub.carzy.auto_script.activities.about.child;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.BaseActivity;
import pub.carzy.auto_script.databinding.ComAboutTroubleshootingBinding;

/**
 * @author admin
 */
public class AboutTroubleshootingActivity extends BaseActivity {
    private ComAboutTroubleshootingBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.com_about_troubleshooting);
        initTopBar(binding.topBarLayout.actionBar);
    }
}
