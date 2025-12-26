package pub.carzy.auto_script.activities;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheetBaseBuilder;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheetListItemModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.controller.BaseController;
import pub.carzy.auto_script.entity.SupportLocaleResult;
import pub.carzy.auto_script.ui.entity.ActionInflater;


/**
 * @author admin
 */
public abstract class BaseActivity extends AppCompatActivity {

    private final BaseController controller;

    public BaseActivity() {
        controller = new BaseController();
    }

    public void addDefaultMenu(QMUIBottomSheet.BottomListSheetBuilder builder) {
        List<ActionInflater.ActionItem> list = ActionInflater.inflate(this, R.xml.actions_common);
        for (ActionInflater.ActionItem item:list){
            if (!item.isEnabled()){
                continue;
            }
            QMUIBottomSheetListItemModel model = new QMUIBottomSheetListItemModel(item.getTitle(), item.idToString());
            if (item.getIcon()!=null){
                model.image(item.getIcon());
            }
            builder.addItem(model);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_setting) {
            //先不做处理
            return true;
        } else if (item.getItemId() == R.id.menu_about) {
            //先不做处理
            return true;
        } else if (item.getItemId() == R.id.menu_help) {
            //先不做处理
            return true;
        } else if (item.getItemId() == R.id.menu_language) {
            showLanguageDialog();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    protected void showLanguageDialog() {
        // 获取支持的语言
        controller.getSupportLocales((result) -> {
            if (result.getLocales().isEmpty()) {
                Toast.makeText(this, R.string.no_language_support, Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> keys = new ArrayList<>(result.getLocales().keySet());
            if (result.getCurrentLocale() == null) {
                String language = result.getLocales().keySet().iterator().next();
                controller.changeLanguage((v) -> {
                }, language);
                result.setCurrentLocale(language);
            }
            AlertDialog.Builder builder = createBuilder(result, keys);
            builder.show();
        });
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(updateLocale(newBase));
    }

    private Context updateLocale(Context context) {
        Locale locale = controller.getSyncLanguage();
        if (locale == null) {
            return context;
        }
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    protected String getActionBarTitle() {
        return getString(R.string.app_name);
    }

    private AlertDialog.Builder createBuilder(SupportLocaleResult result, List<String> keys) {
        AtomicInteger index = new AtomicInteger(-1);
        return new AlertDialog.Builder(this)
                .setTitle(R.string.menu_language)
                .setSingleChoiceItems(keys.toArray(new String[0]), keys.indexOf(result.getCurrentLocale()), (dialog, which) -> index.set(which))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (index.get() < 0) {
                        return;
                    }
                    int i = index.get();
                    controller.changeLanguage((v) -> {
                        // 切换语言（兼容新旧版本）
                        Resources res = getResources();
                        Configuration config = new Configuration(res.getConfiguration());
                        config.setLocale(result.getLocales().get(keys.get(i)));
                        recreate();
                    }, keys.get(i));
                }).setNegativeButton(android.R.string.cancel, null);
    }
}
