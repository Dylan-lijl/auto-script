package pub.carzy.auto_script.activities.about.child;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.google.gson.Gson;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.BaseActivity;
import pub.carzy.auto_script.databinding.ComAboutLicensesBinding;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class AboutLicensesActivity extends BaseActivity {
    private ComAboutLicensesBinding binding;

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.open_source_license);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.com_about_licenses);
        initTopBar();
        loadData();
    }
    @Override
    protected QMUITopBarLayout getTopBar() {
        return binding.topBarLayout.actionBar;
    }
    private void loadData() {
        ThreadUtil.runOnCpu(() -> {
            InputStream is = getResources().openRawResource(R.raw.licenses);
            StringBuilder builder = new StringBuilder();
            try {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    String text;
                    while ((text = reader.readLine()) != null) {
                        builder.append(text).append("\n");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (builder.length() > 0) {
                updateText(builder.toString());
            }
        });
    }

    private void updateText(String text) {
        ThreadUtil.runOnUi(() -> {
            binding.licensesText.setText(text);
        });
    }
}
