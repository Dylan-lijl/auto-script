package pub.carzy.auto_script.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;

import pub.carzy.auto_script.R;

/**
 * @author admin
 */
public class MacroListActivity extends BaseActivity {
    @Override
    protected Integer getActionBarTitle() {
        return R.string.script_list_title;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_macro_list);
    }
}
