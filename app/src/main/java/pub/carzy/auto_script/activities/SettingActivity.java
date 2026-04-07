package pub.carzy.auto_script.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qmuiteam.qmui.recyclerView.QMUIRVItemSwipeAction;
import com.qmuiteam.qmui.recyclerView.QMUISwipeAction;
import com.qmuiteam.qmui.recyclerView.QMUISwipeViewHolder;
import com.qmuiteam.qmui.skin.QMUISkinManager;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogView;
import com.qmuiteam.qmui.widget.popup.QMUIPopup;
import com.qmuiteam.qmui.widget.popup.QMUIPopups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.Inflater;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.adapter.SingleSimpleAdapter;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.IdGenerator;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.config.pojo.SettingKey;
import pub.carzy.auto_script.databinding.CommonColorSelectorBinding;
import pub.carzy.auto_script.databinding.ViewSettingBinding;
import pub.carzy.auto_script.entity.FloatPoint;
import pub.carzy.auto_script.entity.MaskConfig;
import pub.carzy.auto_script.entity.SettingProxy;
import pub.carzy.auto_script.entity.Style;
import pub.carzy.auto_script.model.CommonColorSelectorModel;
import pub.carzy.auto_script.model.SettingModel;
import pub.carzy.auto_script.ui.CoordinateViewWrapper;
import pub.carzy.auto_script.ui.NormalDividerItemDecoration;
import pub.carzy.auto_script.ui.QMUIBottomSheetConfirmBuilder;
import pub.carzy.auto_script.ui.QMUIBottomSheetCustomBuilder;
import pub.carzy.auto_script.ui.QMUIBottomSheetInputConfirmBuilder;
import pub.carzy.auto_script.ui.entity.ActionInflater;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.StringUtils;
import pub.carzy.auto_script.utils.ThreadUtil;
import pub.carzy.auto_script.utils.MyTypeToken;
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
        idGenerator = BeanFactory.getInstance().get(new MyTypeToken<IdGenerator<Long>>() {
        });
        model = new SettingModel();
        model.setProxy(SettingProxy.DEFAULT.clone());
        binding.setModel(model);
        initTopBar();
        readConfig();
        initListeners();
    }

    private void initListeners() {
        //模式
        binding.typeRootLayout.setOnClickListener(e -> {
            //展开下拉选
            List<SingleSimpleAdapter.Data> data = new ArrayList<>();
            Integer type = model.getProxy().getType();
            data.add(new SingleSimpleAdapter.Data(getString(R.string.auto), type == SettingProxy.AUTO, SettingProxy.AUTO));
            data.add(new SingleSimpleAdapter.Data(getString(R.string.accessibility_mode), type == SettingProxy.ACCESSIBILITY, SettingProxy.ACCESSIBILITY));
            data.add(new SingleSimpleAdapter.Data(getString(R.string.root_mode), type == SettingProxy.ROOT, SettingProxy.ROOT));
            SingleSimpleAdapter adapter = new SingleSimpleAdapter(new ArrayList<>(data));
            QMUIPopup[] popups = new QMUIPopup[]{null};
            popups[0] = ActivityUtils.listPopup(SettingActivity.this, 500, 400, adapter,
                    (parent, view, position, id) -> {
                        SingleSimpleAdapter.Data item = (SingleSimpleAdapter.Data) adapter.getItem(position);
                        model.getProxy().setType((Integer) item.args[0]);
                        setting.write(SettingKey.TYPE, model.getProxy().getType());
                        popups[0].dismiss();
                    }, null).show(binding.typeInput);
        });
        //事件片
        binding.tickRootLayout.setOnClickListener(e -> {
            //这里我想弹出一个输入框,然后一个取消和确认按钮
            builderNumberInputDialog("时间片", model.getProxy().getTick(), v -> {
                model.getProxy().setTick(v);
                setting.write(SettingKey.TICK, v);
            });
        });
        Map<View, SettingAction> settingConfigs = new LinkedHashMap<>();
        //自动关闭
        settingConfigs.put(binding.autoCloseRootLayout, new SettingAction(SettingKey.AUTO_CLOSE, model.getProxy()::getAutoClose, model.getProxy()::setAutoClose));
        //显示点按操作
        binding.showOperationRootLayout.setOnClickListener(e -> {
            jumpDevPage();
        });
//        settingConfigs.put(binding.showOperationRootLayout, new SettingAction(SettingKey.SHOW_OPERATION, model.getProxy()::getShowOperation, model.getProxy()::setShowOperation));
        /*binding.operationSizeRootLayout.setOnClickListener(e -> {
            //打开弹窗
            builderNumberInputDialog("圆点尺寸", model.getProxy().getOperationConfig().getSize(), v -> {
                model.getProxy().getOperationConfig().setSize(v);
                setting.write(SettingKey.OPERATION_CONFIG, model.getProxy().getOperationConfig());
            });
        });
        //边框线条
        binding.operationLineWidthRootLayout.setOnClickListener(e -> {
            //打开弹窗
            builderNumberInputDialog("边框线条", model.getProxy().getOperationConfig().getLineWidth(), v -> {
                model.getProxy().getOperationConfig().setLineWidth(v);
                setting.write(SettingKey.OPERATION_CONFIG, model.getProxy().getOperationConfig());
            });
        });
        //激活颜色
        binding.operationTouchColorRootLayout.setOnClickListener(e -> {
            //打开弹窗
            builderColorInputDialog(model.getProxy().getOperationConfig().getTouchColor(), v -> {
                model.getProxy().getOperationConfig().setTouchColor(v);
                setting.write(SettingKey.OPERATION_CONFIG, model.getProxy().getOperationConfig());
            });
        });
        //默认颜色
        binding.operationIdleColorRootLayout.setOnClickListener(e -> {
            //打开弹窗
            builderColorInputDialog(model.getProxy().getOperationConfig().getIdleColor(), v -> {
                model.getProxy().getOperationConfig().setIdleColor(v);
                setting.write(SettingKey.OPERATION_CONFIG, model.getProxy().getOperationConfig());
            });
        });*/
        //自动回放
        settingConfigs.put(binding.autoPlayRootLayout, new SettingAction(SettingKey.AUTO_PLAY, model.getProxy()::getAutoPlay, model.getProxy()::setAutoPlay));
        //忽略悬浮窗操作手势
        settingConfigs.put(binding.rootIgnoreRootLayout, new SettingAction(SettingKey.IGNORE_FLOATING_SCRIPT, model.getProxy()::getIgnoreFloatingScript, model.getProxy()::setIgnoreFloatingScript));
        //位置
        binding.floatPointRootLayout.setOnClickListener(createFloatPointListener());
        //动态更新
        settingConfigs.put(binding.floatPointUpdateRootLayout, new SettingAction(SettingKey.DYNAMIC_UPDATE, model.getProxy()::getDynamicUpdate, model.getProxy()::setDynamicUpdate));
        //蒙层颜色
        binding.maskColorRootLayout.setOnClickListener(e -> {
            builderColorInputDialog(model.getProxy().getMaskConfig().getColor(), v -> {
                model.getProxy().getMaskConfig().setColor(v);
                setting.write(SettingKey.MASK_CONFIG, model.getProxy().getMaskConfig());
            });
        });
        //标尺
        binding.maskSizeRootLayout.setOnClickListener(e -> {
            builderNumberInputDialog("标尺", model.getProxy().getMaskConfig().getSize(), v -> {
                model.getProxy().getMaskConfig().setSize(v);
                setting.write(SettingKey.MASK_CONFIG, model.getProxy().getMaskConfig());
            });
        });
        //网格
        binding.maskGridRootLayout.setOnClickListener(e -> {
            MaskConfig config = model.getProxy().getMaskConfig();
            if (config == null) {
                return;
            }
            config.setGrid(!config.getGrid());
            setting.write(SettingKey.MASK_CONFIG, config);
        });
        //网格线颜色
        binding.maskGridColorRootLayout.setOnClickListener(e -> {
            //打开弹窗
            builderColorInputDialog(model.getProxy().getMaskConfig().getGridColor(), v -> {
                model.getProxy().getMaskConfig().setGridColor(v);
                setting.write(SettingKey.MASK_CONFIG, model.getProxy().getMaskConfig());
            });
        });
        //线宽
        binding.maskGridSizeRootLayout.setOnClickListener(e -> {
            builderNumberInputDialog("网格线宽度", model.getProxy().getMaskConfig().getLineWidth(), v -> {
                model.getProxy().getMaskConfig().setLineWidth(v);
                setting.write(SettingKey.MASK_CONFIG, model.getProxy().getMaskConfig());
            });
        });
        Runnable updateCurrentStyle = () -> {
            Style currentStyle = model.getProxy().getCurrentStyle();
            if (currentStyle == null) {
                return;
            }
            model.getProxy().updateCurrentStyle();
            updateGlobalStyle(currentStyle);
            setting.write(new SettingKey<>(SettingKey.STYLE.getKey() + currentStyle.getId(), Style.class), model.getProxy().getCurrentStyle());
        };
        //样式配置
        binding.styleRootLayout.setOnClickListener(e -> {
            builderStyleDialog();
        });
        binding.statusBarBackgroundRootLayout.setOnClickListener(e -> {
            Style currentStyle = model.getProxy().getCurrentStyle();
            builderColorInputDialog(currentStyle.getStatusBarBackgroundColor(), v -> {
                currentStyle.setStatusBarBackgroundColor(v);
                updateCurrentStyle.run();
            });
        });
        binding.statusBarDarkRootLayout.setOnClickListener(e -> {
            Style currentStyle = model.getProxy().getCurrentStyle();
            currentStyle.setStatusBarMode(!currentStyle.isStatusBarMode());
            updateCurrentStyle.run();
        });
        binding.topBarBackgroundRootLayout.setOnClickListener(e -> {
            Style currentStyle = model.getProxy().getCurrentStyle();
            builderColorInputDialog(currentStyle.getTopBarBackgroundColor(), v -> {
                currentStyle.setTopBarBackgroundColor(v);
                updateCurrentStyle.run();
            });
        });
        binding.topBarTextColorRootLayout.setOnClickListener(e -> {
            Style currentStyle = model.getProxy().getCurrentStyle();
            builderColorInputDialog(currentStyle.getTopBarTextColor(), v -> {
                currentStyle.setTopBarTextColor(v);
                updateCurrentStyle.run();
            });
        });
        binding.topBarImageColorRootLayout.setOnClickListener(e -> {
            Style currentStyle = model.getProxy().getCurrentStyle();
            builderColorInputDialog(currentStyle.getTopBarImageColor(), v -> {
                currentStyle.setTopBarImageColor(v);
                updateCurrentStyle.run();
            });
        });
        binding.resetRootLayout.setOnClickListener(e -> {
            QMUIBottomSheetConfirmBuilder<?> builder = new QMUIBottomSheetConfirmBuilder<>(this);
            builder.setTitle("重置确认")
                    .setConfirm((sheet, views) -> {
                        reset();
                        updateCurrentStyle.run();
                        sheet.dismiss();
                    }).setCancel(((sheet, views) -> sheet.dismiss()))
                    .build().show();
        });
        // 3. 定义通用监听器
        View.OnClickListener commonClickListener = v -> {
            SettingAction action = settingConfigs.get(v);
            if (action == null) {
                return;
            }
            // 执行逻辑：获取当前值 -> 取反 -> 执行 Proxy 方法 -> 持久化
            boolean nextValue = !action.getMethod.get();
            action.setMethod.accept(nextValue);
            setting.write(action.key, nextValue);
        };
        // 4. 遍历配置表：统一绑定监听器 & 初始化 UI 状态
        settingConfigs.forEach((rootView, action) -> {
            // 绑定点击事件
            rootView.setOnClickListener(commonClickListener);
        });
    }

    private void jumpDevPage() {
        boolean isDevMode = Settings.Global.getInt(
                getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
        ) != 0;
        if (isDevMode) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
            // 某些系统支持通过这几个 key 来实现高亮定位
            intent.putExtra(":settings:fragment_args_key", "show_touches");
            intent.putExtra(":settings:show_fragment_args_key", "show_touches");
            // 甚至有些厂商支持搜索模式的高亮（粉红色闪烁效果）
            Bundle bundle = new Bundle();
            bundle.putString(":settings:fragment_args_key", "show_touches");
            intent.putExtra("SHOW_FRAGMENT_BUNDLE", bundle);

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (Exception e) {
                // 退而求其次，只跳转页面
                Log.d("error", "ssss", e);
            }
        } else {
            //跳转到版本界面
            Intent intent = new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS);
            // 尝试高亮“版本号”条目 (Build Number)
            // 注意：不同系统的 Key 可能不同，常见的有 "build_number" 或 "model_info"
            intent.putExtra(":settings:fragment_args_key", "model_info");
            intent.putExtra(":settings:show_fragment_args_key", "model_info");
            // 某些系统支持传递一个 Bundle 来实现高亮闪烁效果
            Bundle bundle = new Bundle();
            bundle.putString(":settings:fragment_args_key", "model_info");
            intent.putExtra("SHOW_FRAGMENT_BUNDLE", bundle);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            String message = "点击操作系统版本4次开启开发者模式";
            try {
                startActivity(intent);
            } catch (Exception e) {
                message = "请到关于手机的版本页面," + message;
                //回退
                intent = new Intent(Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                } catch (Exception i) {
                    message = "打开设置界面失败,请手动打开!";
                }
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void builderStyleDialog() {
        //实例化选择样式页面
        View inflate = LayoutInflater.from(this).inflate(R.layout.dialog_style_change, null, false);
        QMUISwipeAction.ActionBuilder actionBuilder = new QMUISwipeAction.ActionBuilder()
                .textSize(QMUIDisplayHelper.sp2px(this, 14))
                .textColor(Color.WHITE)
                .paddingStartEnd(QMUIDisplayHelper.dp2px(this, 14));
        //创建删除按钮和重命名按钮
        Drawable d = AppCompatResources.getDrawable(this, R.drawable.delete);
        d.setTint(getColor(R.color.danger));
        QMUISwipeAction removeAction = actionBuilder.icon(d).build();
        Drawable e = AppCompatResources.getDrawable(this, R.drawable.edit);
        e.setTint(getColor(R.color.link));
        QMUISwipeAction renameAction = actionBuilder.icon(e).build();
        //保存样式回调
        Consumer<Style> saveStyle = (s) -> setting.write(new SettingKey<>(SettingKey.STYLE.getKey() + s.getId(), Style.class), s);
        //删除样式回调
        Consumer<Style> removeStyle = (s) -> setting.remove(new SettingKey<>(SettingKey.STYLE.getKey() + s.getId(), Style.class));
        View view = inflate.findViewById(R.id.style_add_btn);
        //列表适配器
        RecyclerView.Adapter<QMUISwipeViewHolder> adapter = new RecyclerView.Adapter<>() {

            @NonNull
            @Override
            public QMUISwipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_checked_list_item, parent, false);
                final QMUISwipeViewHolder vh = new QMUISwipeViewHolder(view);
                vh.addSwipeAction(removeAction);
                vh.addSwipeAction(renameAction);
                view.setOnClickListener(e -> {
                    int position = vh.getAdapterPosition();
                    Style style = model.getProxy().getStyles().get(position);
                    if (style == null) {
                        return;
                    }
                    style.setCurrentVersion(System.currentTimeMillis());
                    model.getProxy().updateCurrentStyle();
                    updateGlobalStyle(style);
                    notifyDataSetChanged();
                });
                return vh;
            }

            @Override
            public void onBindViewHolder(@NonNull QMUISwipeViewHolder holder, int position) {
                TextView textView = holder.itemView.findViewById(R.id.text);
                Style style = model.getProxy().getStyles().get(position);
                textView.setText(style.getName());
                View view = holder.itemView.findViewById(R.id.checked);
                view.setVisibility(style == model.getProxy().getCurrentStyle() ? View.VISIBLE : View.INVISIBLE);
            }

            @Override
            public int getItemCount() {
                return model.getProxy().getStyles().size();
            }
        };
        //点击切换选中的样式
        view.setOnClickListener(e1 -> {
            String string = getString(R.string.add);
            Style style = Style.DEFAULT_STYLE.clone();
            style.setId(idGenerator.nextId());
            style.setName(string + (model.getProxy().getStyles().size() + 1));
            style.setCurrentVersion(System.currentTimeMillis());
            model.getProxy().getStyles().add(style);
            model.getProxy().updateCurrentStyle();
            updateGlobalStyle(style);
            saveStyle.accept(style);
            adapter.notifyDataSetChanged();
        });
        //左划逻辑和点击逻辑
        QMUIRVItemSwipeAction swipeAction = new QMUIRVItemSwipeAction(true, new QMUIRVItemSwipeAction.Callback() {
            @Override
            public int getSwipeDirection(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return QMUIRVItemSwipeAction.SWIPE_LEFT;
            }

            @Override
            public void onClickAction(QMUIRVItemSwipeAction swipeAction, RecyclerView.ViewHolder selected, QMUISwipeAction action) {
                super.onClickAction(swipeAction, selected, action);
                int position = selected.getAdapterPosition();
                Style style = model.getProxy().getStyles().get(position);
                if (action == removeAction) {
                    //移除
                    ActivityUtils.createDeleteMessageDialog(SettingActivity.this, (dialog, which) -> {
                        Style currentStyle = model.getProxy().getCurrentStyle();
                        model.getProxy().getStyles().remove(style);
                        updateGlobalStyle(model.getProxy().getCurrentStyle());
                        if (currentStyle == style) {
                            model.getProxy().updateCurrentStyle();
                        }
                        adapter.notifyDataSetChanged();
                        removeStyle.accept(style);
                        dialog.dismiss();
                    }, (dialog, which) -> dialog.dismiss()).show();
                } else if (action == renameAction) {
                    //弹窗修改
                    QMUIBottomSheetInputConfirmBuilder b = new QMUIBottomSheetInputConfirmBuilder(SettingActivity.this);
                    b.setTitle("修改样式名称")
                            .setCancel((sheet, views) -> sheet.dismiss()).setConfirm((sheet, views) -> {
                                if (views.length == 2) {
                                    String s = ((EditText) views[1]).getText().toString();
                                    if (!StringUtils.isEmpty(s)) {
                                        model.getProxy().getCurrentStyle().setName(s);
                                        adapter.notifyItemChanged(position);
                                        saveStyle.accept(model.getProxy().getCurrentStyle());
                                    }
                                }
                                sheet.dismiss();
                            })
                            .setConfigEditView(view -> {
                                Style currentStyle = model.getProxy().getCurrentStyle();
                                view.setInputType(InputType.TYPE_CLASS_TEXT);
                                if (currentStyle != null) {
                                    view.setText(currentStyle.getName());
                                }
                            }).build().show();
                }
            }
        });
        RecyclerView listView = inflate.findViewById(R.id.style_list);
        // 创建一个带边框的分割线
        DividerItemDecoration divider = new NormalDividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setSize(1, QMUIDisplayHelper.dp2px(this, 1));
        drawable.setColor(getColor(R.color.secondary2));
        divider.setDrawable(drawable);
        listView.addItemDecoration(divider);
        listView.setLayoutManager(new LinearLayoutManager(this));
        //设置适配器
        listView.setAdapter(adapter);
        //挂载到RecyclerView
        swipeAction.attachToRecyclerView(listView);
        //构建显示选择样式弹窗
        QMUIBottomSheetCustomBuilder<?> builder = new QMUIBottomSheetCustomBuilder<>(this);
        builder.setTitle("样式切换")
                .setContentView(inflate)
                .build().show();
    }


    private View.OnClickListener createFloatPointListener() {
        return e -> {
            CoordinateViewWrapper wrapper = new CoordinateViewWrapper(this, null);
            FloatPoint floatPoint = model.getProxy().getFloatPoint();
            wrapper.setPoint(floatPoint.getX(), floatPoint.getY());
            QMUIBottomSheetConfirmBuilder<?> builder = new QMUIBottomSheetConfirmBuilder<>(this);
            builder.setTitle("位置")
                    .setRadius(QMUIDisplayHelper.dp2px(this, 15))
                    .setContentView(wrapper.getRootView())
                    .setCancel((s, views) -> s.dismiss())
                    .setConfirm((s, views) -> {
                        Point point = wrapper.getPoint();
                        model.getProxy().setFloatPoint(new FloatPoint(point.x, point.y));
                        setting.write(SettingKey.FLOAT_POINT, model.getProxy().getFloatPoint());
                        s.dismiss();
                    }).build().show();
        };
    }

    private void builderNumberInputDialog(String title, Number number, Consumer<Integer> consumer) {
        QMUIBottomSheetInputConfirmBuilder builder = new QMUIBottomSheetInputConfirmBuilder(this);
        builder.setTitle(title)
                .setConfigEditView(v -> {
                    v.setInputType(InputType.TYPE_CLASS_NUMBER);
                    if (number != null) {
                        v.setText(number.toString());
                    }
                })
                .setRadius(QMUIDisplayHelper.dp2px(this, 15))
                .setCancel((s, views) -> s.dismiss())
                .setConfirm((dialog, views) -> {
                    if (views.length < 2) {
                        return;
                    }
                    EditText editText = (EditText) views[1];
                    if (editText == null) {
                        return;
                    }
                    CharSequence text = editText.getText();
                    if (text == null) {
                        return;
                    }
                    try {
                        int tick = Integer.parseInt(text.toString());
                        if (tick > 0) {
                            // 1. 先保存数据
                            consumer.accept(tick);
                            // 2. 只有成功了才关闭对话框
                            dialog.dismiss();
                        } else {
                            Toast.makeText(this, "请输入大于0的数字", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException exception) {
                        Toast.makeText(this, "输入内容无效", Toast.LENGTH_SHORT).show();
                    }
                }).build().show();
    }

    /**
     * 更新全局样式
     *
     * @param currentStyle 样式
     */
    private void updateGlobalStyle(Style currentStyle) {
        long version = System.currentTimeMillis();
        BeanFactory.getInstance().register(StaticValues.STYLE_VERSION, version);
        if (currentStyle == null) {
            BeanFactory.getInstance().unregister(StaticValues.STYLE_CURRENT);
        } else {
            BeanFactory.getInstance().register(StaticValues.STYLE_CURRENT, currentStyle);
        }
        updateStyle(currentStyle, getTopBar(), version);
    }

    /**
     * 展示所有样式
     *
     * @param e
     */
    private void showAllStyle(View e) {
        List<SingleSimpleAdapter.Data> data = new ArrayList<>();
        //设置选择的样式
        Style currentStyle = model.getProxy().getCurrentStyle();
        for (Style style : model.getProxy().getStyles()) {
            data.add(new SingleSimpleAdapter.Data(style.getName(), style == currentStyle));
        }
        QMUIPopup[] popups = new QMUIPopup[]{null};
        //切换回调
        AdapterView.OnItemClickListener onItemClickListener = (adapterView, view, i, l) -> {
            Style style = model.getProxy().getStyles().get(i);
            if (style != null && style != currentStyle) {
                style.setCurrentVersion(System.currentTimeMillis());
                model.getProxy().updateCurrentStyle();
                updateGlobalStyle(style);
                ThreadUtil.runOnCpu(() -> setting.update(new SettingKey<>(SettingKey.STYLE.getKey() + style.getId(), Style.class), style));
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

    /**
     * 编辑样式
     *
     * @param add 是否新增
     */
    private void editStyle(boolean add) {
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
                if (!add) {
                    Style style = model.getProxy().getCurrentStyle();
                    if (style != null) {
                        text.setText(style.getName());
                    }
                }
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
        }.setTitle(add ? R.string.add_new_style : R.string.rename)
                .addAction(R.string.cancel, (dialog, index) -> {
                    dialog.dismiss();
                })
                .addAction(R.string.confirm, (d, i) -> {
                    d.dismiss();
                    if (name.get().isEmpty()) {
                        name.set(getString(R.string.unknown));
                    }
                    Style style;
                    if (add) {
                        style = new Style();
                        style.setId(idGenerator.nextId());
                        style.setName(name.get());
                        style.setTopBarImageColor(getColor(R.color.black));
                        style.setTopBarTextColor(getColor(R.color.white));
                        style.setTopBarBackgroundColor(getColor(R.color.teal_200));
                        style.setStatusBarBackgroundColor(getColor(R.color.teal_200));
                        style.setStatusBarMode(true);
                        style.setCurrentVersion(System.currentTimeMillis());
                        model.getProxy().getStyles().add(style);
                        model.getProxy().updateCurrentStyle();
                        updateGlobalStyle(style);
                    } else {
                        style = model.getProxy().getCurrentStyle();
                        style.setName(name.get());
                        if (style == null) {
                            return;
                        }
                    }
                    ThreadUtil.runOnCpu(() -> setting.update(new SettingKey<>(SettingKey.STYLE.getKey() + style.getId(), Style.class), style));
                })
                .create().show();
    }

    @Override
    protected QMUITopBarLayout getTopBar() {
        return binding.topBarLayout.actionBar;
    }

    private void builderColorInputDialog(int defaultColor, Consumer<Integer> callback) {
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
                .setTitle(R.string.select_color_message).create();
        dialog.show();
    }

    /**
     * 读取配置
     */
    private void readConfig() {
        //如果读取的不是null就覆盖值
        Optional.ofNullable(setting.read(SettingKey.TYPE)).ifPresent(model.getProxy()::setType);
        Optional.ofNullable(setting.read(SettingKey.TICK)).ifPresent(model.getProxy()::setTick);
        Optional.ofNullable(setting.read(SettingKey.AUTO_CLOSE)).ifPresent(model.getProxy()::setAutoClose);
        Optional.ofNullable(setting.read(SettingKey.SHOW_OPERATION)).ifPresent(model.getProxy()::setShowOperation);
        Optional.ofNullable(setting.read(SettingKey.OPERATION_CONFIG)).ifPresent(model.getProxy()::setOperationConfig);
        Optional.ofNullable(setting.read(SettingKey.AUTO_PLAY)).ifPresent(model.getProxy()::setAutoPlay);
        Optional.ofNullable(setting.read(SettingKey.IGNORE_FLOATING_SCRIPT)).ifPresent(model.getProxy()::setIgnoreFloatingScript);
        Optional.ofNullable(setting.read(SettingKey.FLOAT_POINT)).ifPresent(model.getProxy()::setFloatPoint);
        Optional.ofNullable(setting.read(SettingKey.DYNAMIC_UPDATE)).ifPresent(model.getProxy()::setDynamicUpdate);
        Optional.ofNullable(setting.read(SettingKey.MASK_CONFIG)).ifPresent(model.getProxy()::setMaskConfig);
        Optional.ofNullable(setting.getAll(SettingKey.STYLE)).ifPresent(a -> model.getProxy().setStyles(new ArrayList<>(a.values())));
    }

    private void reset() {
        model.getProxy().getStyles().forEach(item -> setting.remove(new SettingKey<>(SettingKey.STYLE.getKey() + item.getId(), Style.class)));
        model.setProxy(SettingProxy.DEFAULT.clone());
        setting.write(SettingKey.TYPE, model.getProxy().getType());
        setting.write(SettingKey.TICK, model.getProxy().getTick());
        setting.write(SettingKey.AUTO_CLOSE, model.getProxy().getAutoClose());
        setting.write(SettingKey.SHOW_OPERATION, model.getProxy().getShowOperation());
        setting.write(SettingKey.OPERATION_CONFIG, model.getProxy().getOperationConfig());
        setting.write(SettingKey.AUTO_PLAY, model.getProxy().getAutoPlay());
        setting.write(SettingKey.IGNORE_FLOATING_SCRIPT, model.getProxy().getIgnoreFloatingScript());
        setting.write(SettingKey.FLOAT_POINT, model.getProxy().getFloatPoint());
        setting.write(SettingKey.DYNAMIC_UPDATE, model.getProxy().getDynamicUpdate());
        setting.write(SettingKey.MASK_CONFIG, model.getProxy().getMaskConfig());
        model.getProxy().getStyles().forEach(item -> setting.write(new SettingKey<>(SettingKey.STYLE.getKey() + item.getId(), Style.class), item));
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

    static class SettingAction {
        final SettingKey<Boolean> key;
        final Consumer<Boolean> setMethod;
        final Supplier<Boolean> getMethod;

        public SettingAction(SettingKey<Boolean> key, Supplier<Boolean> getMethod, Consumer<Boolean> setMethod) {
            this.key = key;
            this.setMethod = setMethod;
            this.getMethod = getMethod;
        }
    }
}
