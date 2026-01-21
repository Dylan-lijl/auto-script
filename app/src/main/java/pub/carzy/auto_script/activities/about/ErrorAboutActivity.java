package pub.carzy.auto_script.activities.about;

import android.os.Bundle;

import androidx.annotation.Nullable;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.BaseActivity;

/**
 * @author admin
 */
public class ErrorAboutActivity extends BaseActivity {
    @Override
    protected String getActionBarTitle() {
        return getString(R.string.error_page);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
