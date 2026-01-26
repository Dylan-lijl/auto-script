package pub.carzy.auto_script.activities;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.qmuiteam.qmui.skin.QMUISkinManager;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogView;
import com.qmuiteam.qmui.widget.popup.QMUIPopup;
import com.qmuiteam.qmui.widget.popup.QMUIPopups;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.adapter.SingleSimpleAdapter;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.IdGenerator;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.databinding.CommonColorSelectorBinding;
import pub.carzy.auto_script.databinding.ViewSettingBinding;
import pub.carzy.auto_script.entity.FloatPoint;
import pub.carzy.auto_script.entity.SettingProxy;
import pub.carzy.auto_script.entity.Style;
import pub.carzy.auto_script.model.CommonColorSelectorModel;
import pub.carzy.auto_script.model.SettingModel;
import pub.carzy.auto_script.ui.entity.ActionInflater;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.ThreadUtil;
import pub.carzy.auto_script.utils.TypeToken;
import pub.carzy.auto_script.utils.statics.StaticValues;

/**
 * 设置
 *
 * @author admin
 */
public class SettingActivity extends BaseActivity {
    private ViewSettingBinding binding;
    private SettingModel model;
    private Setting setting;
    private IdGenerator<Long> idGenerator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.view_setting);
        setting = BeanFactory.getInstance().get(Setting.class);
        idGenerator = BeanFactory.getInstance().get(new TypeToken<IdGenerator<Long>>() {
        });
        model = new SettingModel();
        binding.setModel(model);
        initTopBar(binding.topBarLayout.actionBar);
        readConfig();
        initListeners();
    }

    private void initListeners() {
        binding.styleAddBtn.setOnClickListener(e -> addNewStyle());
        binding.styleBtn.setOnClickListener(this::showAllStyle);
        binding.statusBarBgcBtn.setOnClickListener(e -> openColorSelector(model.getProxy().getCurrentStyle().getStatusBarBackgroundColor(),
                color -> {
                    model.getProxy().getCurrentStyle().setStatusBarBackgroundColor(color);
                    ActivityUtils.setWindowsStatusBarColor(this, color);
                    ThreadUtil.runOnCpu(() -> setting.updateStyle(model.getProxy().getCurrentStyle()));
                    updateGlobalStyle(model.getProxy().getCurrentStyle());
                }));
        binding.statusBarModeBtn.setOnClickListener(e -> {
            model.getProxy().getCurrentStyle().setStatusBarMode(!model.getProxy().getCurrentStyle().isStatusBarMode());
            ActivityUtils.setWindowsStatusLight(this, model.getProxy().getCurrentStyle().isStatusBarMode());
            updateGlobalStyle(model.getProxy().getCurrentStyle());
            ThreadUtil.runOnCpu(() -> setting.updateStyle(model.getProxy().getCurrentStyle()));
        });
        binding.topBarBgcBtn.setOnClickListener(e -> openColorSelector(model.getProxy().getCurrentStyle().getTopBarBackgroundColor(),
                color -> {
                    model.getProxy().getCurrentStyle().setTopBarBackgroundColor(color);
                    binding.topBarLayout.actionBar.setBackgroundColor(color);
                    updateGlobalStyle(model.getProxy().getCurrentStyle());
                    ThreadUtil.runOnCpu(() -> setting.updateStyle(model.getProxy().getCurrentStyle()));
                }));
        binding.topBarTxtBtn.setOnClickListener(e -> openColorSelector(model.getProxy().getCurrentStyle().getTopBarTextColor(),
                color -> {
                    model.getProxy().getCurrentStyle().setTopBarTextColor(color);
                    if (binding.topBarLayout.actionBar.getTitleView()!=null){
                        binding.topBarLayout.actionBar.getTitleView().setTextColor(color);
                    }
                    updateGlobalStyle(model.getProxy().getCurrentStyle());
                    ThreadUtil.runOnCpu(() -> setting.updateStyle(model.getProxy().getCurrentStyle()));
                }));
        binding.topBarImgBtn.setOnClickListener(e -> openColorSelector(model.getProxy().getCurrentStyle().getTopBarImageColor(),
                color -> {
                    model.getProxy().getCurrentStyle().setTopBarImageColor(color);
                    QMUITopBar topBar = binding.topBarLayout.actionBar.getTopBar();
                    if (topBar!=null){
                        for (int i=0;i<topBar.getChildCount();i++){
                            View child = topBar.getChildAt(i);
                            if (child==null){
                                continue;
                            }
                            //判断是否是图片组件
                            if (child instanceof ImageView){
                                ImageView iv = (ImageView) child;
                                Drawable d = iv.getDrawable();
                                if (d != null) {
                                    d.mutate().setTint(color);
                                    iv.setImageDrawable(d);
                                }
                            }
                        }
                    }
                    updateGlobalStyle(model.getProxy().getCurrentStyle());
                    ThreadUtil.runOnCpu(() -> setting.updateStyle(model.getProxy().getCurrentStyle()));
                }));
    }

    private void updateGlobalStyle(Style currentStyle) {
        BeanFactory.getInstance().register(StaticValues.STYLE_VERSION,System.currentTimeMillis());
        BeanFactory.getInstance().register(StaticValues.STYLE_CURRENT,currentStyle);
    }

    private void showAllStyle(View e) {
        List<SingleSimpleAdapter.Data> data = new ArrayList<>();
        Style currentStyle = model.getProxy().getCurrentStyle();
        for (Style style : model.getProxy().getStyles()) {
            data.add(new SingleSimpleAdapter.Data(style.getName(), style == currentStyle));
        }
        QMUIPopup[] popups = new QMUIPopup[]{null};

        AdapterView.OnItemClickListener onItemClickListener = (adapterView, view, i, l) -> {
            Style style = model.getProxy().getStyles().get(i);
            if (style != null && style != currentStyle) {
                style.setCurrentVersion(System.currentTimeMillis());
                model.getProxy().updateCurrentStyle();
                ThreadUtil.runOnCpu(() -> setting.updateStyle(style));
            }
            if (popups[0] != null) {
                popups[0].dismiss();
            }
        };
        popups[0] = QMUIPopups.listPopup(this,
                        QMUIDisplayHelper.dp2px(this, 250),
                        QMUIDisplayHelper.dp2px(this, 300),
                        new SingleSimpleAdapter(data),
                        onItemClickListener)
                .animStyle(QMUIPopup.ANIM_GROW_FROM_CENTER)
                .preferredDirection(QMUIPopup.DIRECTION_BOTTOM)
                .shadow(true)
                .offsetYIfTop(QMUIDisplayHelper.dp2px(this, 5))
                .skinManager(QMUISkinManager.defaultInstance(this))
                .show(e);
    }

    private void addNewStyle() {
        AtomicReference<String> name = new AtomicReference<>(getString(R.string.unknown));
        new QMUIDialog.CustomDialogBuilder(this) {
            @Nullable
            @Override
            protected View onCreateContent(QMUIDialog dialog, QMUIDialogView parent, Context context) {
                LinearLayout root = new LinearLayout(context);
                root.setOrientation(LinearLayout.HORIZONTAL);
                root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                TextView view = new TextView(context);
                view.setTextColor(Color.BLACK);
                view.setText(R.string.name);
                LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                view.setLayoutParams(lpText);
                EditText text = new EditText(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1.0f
                );
                text.setLayoutParams(params);
                text.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        name.set(s.toString());
                    }
                });
                text.setTextColor(Color.BLACK);
                root.addView(view);
                root.addView(text);
                return root;
            }
        }.setTitle("添加新样式")
                .addAction(R.string.cancel, (dialog, index) -> {
                    dialog.dismiss();
                })
                .addAction(R.string.confirm, (d, i) -> {
                    d.dismiss();
                    if (name.get().isEmpty()) {
                        name.set(getString(R.string.unknown));
                    }
                    Style style = new Style();
                    style.setId(idGenerator.nextId());
                    style.setName(name.get());
                    style.setTopBarImageColor(getColor(R.color.black));
                    style.setTopBarTextColor(getColor(R.color.white));
                    style.setTopBarBackgroundColor(getColor(R.color.teal_200));
                    style.setStatusBarBackgroundColor(getColor(R.color.teal_200));
                    style.setStatusBarMode(true);
                    style.setCurrentVersion(System.currentTimeMillis());
                    model.getProxy().getStyles().add(style);
                    //todo 添加和切换样式都要更新全局,同时国际化语言
                    ThreadUtil.runOnCpu(() -> {
                        setting.updateStyle(style);
                    });
                })
                .create().show();
    }

    private void openColorSelector(int defaultColor, Consumer<Integer> callback) {
        CommonColorSelectorBinding b = CommonColorSelectorBinding.inflate(LayoutInflater.from(this));
        CommonColorSelectorModel m = new CommonColorSelectorModel();
        m.setColor(defaultColor);
        b.setModel(m);
        m.setColorListener(callback);
        //打开一个弹窗
        QMUIDialog dialog = new QMUIDialog.CustomDialogBuilder(this) {
            @Nullable
            @Override
            protected View onCreateContent(QMUIDialog dialog, QMUIDialogView parent, Context context) {
                return b.getRoot();
            }
        }
                .setTitle("请选择颜色").create();
        dialog.show();
    }

    /**
     * 读取配置
     */
    private void readConfig() {
        ThreadUtil.runOnCpu(() -> {
            SettingProxy proxy = new SettingProxy();
            FloatPoint floatPoint = setting.getPoint();
            proxy.setFloatPoint(Objects.requireNonNullElseGet(floatPoint, () -> new FloatPoint(200, 200)));
            List<Style> list = setting.getAllStyle();
            proxy.setStyles(list);
            model.setProxy(proxy);
        });
    }

    /**
     * c重写过滤掉设置选项
     */
    @Override
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
        addActionByXml(builder, this, R.xml.actions_common,
                (b, m, item) -> {
                    if (item.getId() == R.id.menu_setting) {
                        return;
                    }
                    b.addItem(m);
                });
        QMUIBottomSheet build = builder.build();
        build.show();
    }
}
