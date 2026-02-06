package pub.carzy.auto_script.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.qmuiteam.qmui.alpha.QMUIAlphaImageButton;
import com.qmuiteam.qmui.util.QMUIViewHelper;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.ControllerCallback;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.entity.Style;
import pub.carzy.auto_script.entity.SupportLocaleResult;
import pub.carzy.auto_script.ui.QMUIBottomSheetListItemModelExt;
import pub.carzy.auto_script.ui.entity.ActionInflater;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.ThreadUtil;
import pub.carzy.auto_script.utils.TriConsumer;
import pub.carzy.auto_script.utils.statics.StaticValues;


/**
 * 基础活动类
 *
 * @author admin
 */
public abstract class BaseActivity extends AppCompatActivity {
    /**
     * 地区
     */
    protected Locale locale;
    /**
     * 配置类
     */
    protected final Setting setting;
    /**
     * 样式版本
     */
    protected long styleVersion = StaticValues.EMPTY_DEFAULT_LONG_VALUE;

    public BaseActivity() {
        setting = BeanFactory.getInstance().get(Setting.class);
    }

    private void init(Context context) {
        //获取地方
        locale = ActivityUtils.getLocale(context, setting);
    }

    /**
     * 添加默认菜单
     *
     * @param builder b
     * @return 实例
     */
    protected QMUIBottomSheet.BottomListSheetBuilder addDefaultMenu(QMUIBottomSheet.BottomListSheetBuilder builder) {
        return addActionByXml(builder, this, R.xml.actions_common);
    }

    /**
     * 根据xml添加对应菜单
     *
     * @param builder b
     * @param context c
     * @param xmlId   资源id
     * @return 实例
     */
    public QMUIBottomSheet.BottomListSheetBuilder addActionByXml(QMUIBottomSheet.BottomListSheetBuilder builder, Context context, int xmlId) {
        //默认回调:添加
        return addActionByXml(builder, context, xmlId, (builder1, model, actionItem) -> {
            builder.addItem(model);
        });
    }

    /**
     * 根据xml添加对应菜单
     *
     * @param builder  b
     * @param context  c
     * @param xmlId    资源id
     * @param callback 回调
     * @return 实例
     */
    public QMUIBottomSheet.BottomListSheetBuilder addActionByXml(QMUIBottomSheet.BottomListSheetBuilder builder, Context context, int xmlId, TriConsumer<QMUIBottomSheet.BottomListSheetBuilder, QMUIBottomSheetListItemModelExt, ActionInflater.ActionItem> callback) {
        //使用资源类加载
        List<ActionInflater.ActionItem> list = ActionInflater.inflate(context, xmlId);
        for (ActionInflater.ActionItem item : list) {
            //忽略未启用的
            if (!item.isEnabled()) {
                continue;
            }
            //创建菜单项
            QMUIBottomSheetListItemModelExt model = new QMUIBottomSheetListItemModelExt(item.getTitle(), item.idToString());
            if (item.getIcon() != null) {
                model.image(item.getIcon());
            }
            //调用回调
            if (callback != null) {
                callback.accept(builder, model, item);
            }
        }
        return builder;
    }

    /**
     * 处理默认菜单项任务
     *
     * @param id id
     * @return 是否处理
     */
    public boolean defaultProcessMenu(int id) {
        if (id == R.id.menu_setting) {
            //跳转到设置界面
            Intent intent = new Intent(this, SettingActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_about && !(this instanceof AboutActivity)) {
            //跳转到关于界面
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_help) {
            //先不做处理
            return true;
        } else if (id == R.id.menu_language) {
            //显示语言选择对话框
            showLanguageDialog();
            return true;
        }
        return false;
    }

    /**
     * 初始化标题栏
     */
    protected void initTopBar() {
        QMUITopBarLayout actionBar = getTopBar();
        if (actionBar == null) {
            return;
        }
        //更新标题
        String title = getActionBarTitle();
        if (title != null) {
            actionBar.setTitle(title);
        }
        //创建右边多选功能按钮
        QMUIAlphaImageButton manyBtn = actionBar.addRightImageButton(R.drawable.many_horizontal, QMUIViewHelper.generateViewId());
        //点击打开菜单弹窗
        manyBtn.setOnClickListener(e -> openBottomSheet());
    }

    /**
     * 显示语言选择对话框
     */
    protected void showLanguageDialog() {
        ThreadUtil.runOnCpu(() -> {
            try {
                SupportLocaleResult result = new SupportLocaleResult();
                //获取支持的语言
                result.getLocales().putAll(ActivityUtils.getLocaleMap(getApplicationContext()));
                Setting setting = BeanFactory.getInstance().get(Setting.class, false);
                if (setting == null) {
                    return;
                }
                //获取已配置的语言
                String language = setting.getLanguage();
                if (language == null) {
                    //没有配置就尝试使用系统地区
                    Configuration config = new Configuration(getResources().getConfiguration());
                    config.setLocale(Locale.getDefault());
                    Context localizedContext = createConfigurationContext(config);
                    //查看有没有对应本地化资源
                    int resId = localizedContext.getResources().getIdentifier("language", "string", getPackageName());
                    if (resId != 0) {
                        //存在就使用该语言
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
                        //如果没有选中的语言,则先保存一个默认语言
                        changeLanguage((v) -> {
                        }, l);
                        result.setCurrentLocale(l);
                    }
                    //构建弹窗
                    QMUIDialog dialog = createLanguageDialog(result, keys);
                    dialog.show();
                });
            } catch (Exception e) {
                Log.d(this.getClass().getSimpleName(), "showLanguageDialog: " + e.getMessage());
            }
        });
    }

    /**
     * 回调
     *
     * @param newBase The new base context for this wrapper.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        init(newBase);
        //使用对应的语言
        super.attachBaseContext(ActivityUtils.updateLocale(newBase, getSyncLanguage()));
    }

    /**
     * 标题,子类可重写
     *
     * @return 标题
     */
    protected String getActionBarTitle() {
        return getString(R.string.app_name);
    }

    /**
     * 创建语言选择弹窗
     *
     * @param result r
     * @param keys   k
     * @return 实例
     */
    private QMUIDialog createLanguageDialog(SupportLocaleResult result, List<String> keys) {
        //当前选中的语言索引
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
                    //更新配置信息并改变语言,重新加载当前页面
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

    /**
     * 打开菜单弹窗
     */
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
        //使用默认菜单
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

    /**
     * 启动回调
     */
    @Override
    protected void onStart() {
        super.onStart();
        QMUITopBarLayout topBar = getTopBar();
        if (topBar != null) {
            //获取全局样式版本
            Long globalStyleVersion = BeanFactory.getInstance().get(StaticValues.STYLE_VERSION, false);
            //获取全局使用的样式
            Style style = BeanFactory.getInstance().get(StaticValues.STYLE_CURRENT, false);
            if (style == null) {
                //没有就使用默认样式
                style = Style.DEFAULT_STYLE;
            }
            if (styleVersion == StaticValues.EMPTY_DEFAULT_LONG_VALUE) {
                //如果当前版本是默认版本则直接更新
                updateStyle(style, topBar, globalStyleVersion);
            } else if (styleVersion < globalStyleVersion) {
                //当前版本小于全局版本也更新
                updateStyle(style, topBar, globalStyleVersion);
            }
        }
    }

    protected void updateStyle() {
        QMUITopBarLayout topBar = getTopBar();
        if (topBar == null) {
            return;
        }
        Long globalStyleVersion = BeanFactory.getInstance().get(StaticValues.STYLE_VERSION, false);
        Style style = BeanFactory.getInstance().get(StaticValues.STYLE_CURRENT, false);
        if (style == null) {
            style = Style.DEFAULT_STYLE;
        }
        updateStyle(style, topBar, globalStyleVersion);
    }

    /**
     * 更新样式
     * @param style 样式
     * @param topBarLayout 标题栏
     * @param updateVersion 样式id
     */
    protected void updateStyle(Style style, QMUITopBarLayout topBarLayout, Long updateVersion) {
        if (topBarLayout == null) {
            return;
        }
        QMUITopBar topBar = topBarLayout.getTopBar();
        if (style == null || topBar == null) {
            return;
        }
        //设置系统栏背景颜色
        ActivityUtils.setWindowsStatusBarColor(this, style.getStatusBarBackgroundColor());
        //设置系统栏文字是亮色还是暗色
        ActivityUtils.setWindowsStatusLight(this, style.isStatusBarMode());
        //设置标题栏背景颜色
        topBarLayout.setBackgroundColor(style.getTopBarBackgroundColor());
        if (topBar.getTitleView() != null) {
            //设置标题栏文字颜色
            topBar.getTitleView().setTextColor(style.getTopBarTextColor());
        }
        //遍历设置image颜色
        for (int i = 0; i < topBar.getChildCount(); i++) {
            View child = topBar.getChildAt(i);
            if (child == null) {
                continue;
            }
            //判断是否是图片组件
            if (child instanceof ImageView) {
                ImageView iv = (ImageView) child;
                Drawable d = iv.getDrawable();
                if (d != null) {
                    d.mutate().setTint(style.getTopBarImageColor());
                    iv.setImageDrawable(d);
                }
            }
        }
        //更新当前版本
        styleVersion = updateVersion;
    }

    /**
     * 子类重写
     * @return 标题栏
     */
    protected QMUITopBarLayout getTopBar() {
        return null;
    }
}
