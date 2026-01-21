package pub.carzy.auto_script.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.qmuiteam.qmui.alpha.QMUIAlphaImageButton;
import com.qmuiteam.qmui.util.QMUIViewHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheetListItemModel;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import kotlin.Triple;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.ControllerCallback;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.entity.SupportLocaleResult;
import pub.carzy.auto_script.ui.QMUIBottomSheetListItemModelExt;
import pub.carzy.auto_script.ui.entity.ActionInflater;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.ThreadUtil;
import pub.carzy.auto_script.utils.TriConsumer;


/**
 * @author admin
 */
public abstract class BaseActivity extends AppCompatActivity {
    private Locale locale;
    private final Setting setting;

    public BaseActivity() {
        setting = BeanFactory.getInstance().get(Setting.class);
    }

    private void init(Context context) {
        locale = ActivityUtils.getLocale(context, setting);
    }

    protected QMUIBottomSheet.BottomListSheetBuilder addDefaultMenu(QMUIBottomSheet.BottomListSheetBuilder builder) {
        return addActionByXml(builder, this, R.xml.actions_common);
    }

    public QMUIBottomSheet.BottomListSheetBuilder addActionByXml(QMUIBottomSheet.BottomListSheetBuilder builder, Context context, int xmlId) {
        return addActionByXml(builder, context, xmlId, (builder1, model, actionItem) -> {
            builder.addItem(model);
        });
    }

    public QMUIBottomSheet.BottomListSheetBuilder addActionByXml(QMUIBottomSheet.BottomListSheetBuilder builder, Context context, int xmlId, TriConsumer<QMUIBottomSheet.BottomListSheetBuilder, QMUIBottomSheetListItemModelExt, ActionInflater.ActionItem> callback) {
        List<ActionInflater.ActionItem> list = ActionInflater.inflate(context, xmlId);
        for (ActionInflater.ActionItem item : list) {
            if (!item.isEnabled()) {
                continue;
            }
            QMUIBottomSheetListItemModelExt model = new QMUIBottomSheetListItemModelExt(item.getTitle(), item.idToString());
            if (item.getIcon() != null) {
                model.image(item.getIcon());
            }
            if (callback != null) {
                callback.accept(builder, model, item);
            }
        }
        return builder;
    }

    public boolean defaultProcessMenu(int id) {
        if (id == R.id.menu_setting) {
            //先不做处理
            return true;
        } else if (id == R.id.menu_about && !(this instanceof AboutActivity)) {
            //先不做处理
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_help) {
            //先不做处理
            return true;
        } else if (id == R.id.menu_language) {
            showLanguageDialog();
            return true;
        }
        return false;
    }

    protected void initTopBar(QMUITopBarLayout actionBar) {
        if (actionBar == null) {
            return;
        }
        String title = getActionBarTitle();
        if (title != null) {
            actionBar.setTitle(title);
        }
        QMUIAlphaImageButton manyBtn = actionBar.addRightImageButton(R.drawable.many_horizontal, QMUIViewHelper.generateViewId());
        manyBtn.setOnClickListener(e -> openBottomSheet());
    }

    protected void showLanguageDialog() {
        ThreadUtil.runOnCpu(() -> {
            try {
                SupportLocaleResult result = new SupportLocaleResult();
                result.getLocales().putAll(ActivityUtils.getLocaleMap(getApplicationContext()));
                Setting setting = BeanFactory.getInstance().get(Setting.class, false);
                if (setting == null) {
                    return;
                }
                //获取当前语言
                String language = setting.getLanguage();
                if (language == null) {
                    Configuration config = new Configuration(getResources().getConfiguration());
                    config.setLocale(Locale.getDefault());
                    Context localizedContext = createConfigurationContext(config);
                    int resId = localizedContext.getResources().getIdentifier("language", "string", getPackageName());
                    if (resId != 0) {
                        language = localizedContext.getString(resId);
                    }
                }
                if (language != null) {
                    result.setCurrentLocale(language);
                }
                ThreadUtil.runOnUi(() -> {
                    if (result.getLocales().isEmpty()) {
                        Toast.makeText(this, R.string.no_language_support, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<String> keys = new ArrayList<>(result.getLocales().keySet());
                    if (result.getCurrentLocale() == null) {
                        String l = result.getLocales().keySet().iterator().next();
                        changeLanguage((v) -> {
                        }, l);
                        result.setCurrentLocale(l);
                    }
                    QMUIDialog dialog = createLanguageDialog(result, keys);
                    dialog.show();
                });
            } catch (Exception e) {
                Log.d(this.getClass().getSimpleName(), "showLanguageDialog: " + e.getMessage());
            }
        });
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        init(newBase);
        super.attachBaseContext(ActivityUtils.updateLocale(newBase, getSyncLanguage()));
    }

    protected String getActionBarTitle() {
        return getString(R.string.app_name);
    }

    private QMUIDialog createLanguageDialog(SupportLocaleResult result, List<String> keys) {
        AtomicInteger index = new AtomicInteger(
                keys.indexOf(result.getCurrentLocale())
        );
        QMUIDialog.CheckableDialogBuilder builder =
                new QMUIDialog.CheckableDialogBuilder(this);
        builder.setTitle(R.string.menu_language);
        // 设置单选项
        builder.addItems(
                keys.toArray(new String[0]),
                (dialog, which) -> index.set(which)
        );
        // 设置当前选中项
        if (index.get() >= 0) {
            builder.setCheckedIndex(index.get());
        }
        // 取消
        builder.addAction(
                getString(android.R.string.cancel),
                (dialog, which) -> dialog.dismiss()
        );
        // 确认
        builder.addAction(
                getString(R.string.confirm),
                (dialog, which) -> {
                    int i = index.get();
                    if (i < 0) {
                        dialog.dismiss();
                        return;
                    }
                    changeLanguage(v -> {
                        Resources res = getResources();
                        Configuration config =
                                new Configuration(res.getConfiguration());
                        config.setLocale(
                                result.getLocales().get(keys.get(i))
                        );
                        recreate();
                    }, keys.get(i));

                    dialog.dismiss();
                }
        );
        return builder.create();
    }


    public Locale getSyncLanguage() {
        return locale;
    }

    protected void openBottomSheet() {
        QMUIBottomSheet.BottomListSheetBuilder builder = new QMUIBottomSheet.BottomListSheetBuilder(this)
                .setGravityCenter(false)
                .setAddCancelBtn(false)
                .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                    dialog.dismiss();
                    if (tag == null) {
                        return;
                    }
                    int id = ActionInflater.ActionItem.stringToId(tag);
                    if (defaultProcessMenu(id)) {
                        return;
                    }
                });
        addDefaultMenu(builder);
        QMUIBottomSheet build = builder.build();
        build.show();
    }

    public void changeLanguage(ControllerCallback<Void> callback, String language) {
        ThreadUtil.runOnCpu(() -> {
            setting.setLanguage(language);
            try {
                ThreadUtil.runOnUi(() -> callback.complete(null));
            } catch (Exception e) {
                ThreadUtil.runOnUi(() -> callback.catchMethod(e));
            } finally {
                ThreadUtil.runOnUi(callback::finallyMethod);
            }
        });
    }
}
