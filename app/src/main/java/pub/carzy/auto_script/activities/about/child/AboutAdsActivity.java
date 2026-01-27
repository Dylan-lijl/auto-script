package pub.carzy.auto_script.activities.about.child;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.qmuiteam.qmui.widget.QMUITopBarLayout;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.BaseActivity;
import pub.carzy.auto_script.databinding.ComAboutAdsBinding;

/**
 * @author admin
 */
public class AboutAdsActivity extends BaseActivity {
    private ComAboutAdsBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.com_about_ads);
        initTopBar();
    }
    @Override
    protected QMUITopBarLayout getTopBar() {
        return binding.topBarLayout.actionBar;
    }
}
