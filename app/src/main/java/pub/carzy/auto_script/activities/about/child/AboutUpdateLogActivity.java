package pub.carzy.auto_script.activities.about.child;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.BaseActivity;
import pub.carzy.auto_script.databinding.ComAboutUpdateLogBinding;

/**
 * @author admin
 */
public class AboutUpdateLogActivity extends BaseActivity {
    private ComAboutUpdateLogBinding binding;

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.update_log);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.com_about_update_log);
    }

}
