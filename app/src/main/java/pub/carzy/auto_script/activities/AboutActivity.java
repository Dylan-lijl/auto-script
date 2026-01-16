package pub.carzy.auto_script.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.databinding.ViewAboutBinding;

/**
 * @author admin
 */
public class AboutActivity extends BaseActivity {
    private ViewAboutBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.view_about);

    }
}
