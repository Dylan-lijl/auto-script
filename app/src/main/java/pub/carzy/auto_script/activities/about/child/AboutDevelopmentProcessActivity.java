package pub.carzy.auto_script.activities.about.child;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.viewpager.widget.PagerAdapter;

import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUIProgressBar;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.qmuiteam.qmui.widget.popup.QMUIPopups;
import com.qmuiteam.qmui.widget.tab.QMUITabBuilder;
import com.qmuiteam.qmui.widget.tab.QMUITabSegment;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import io.noties.markwon.Markwon;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.BaseActivity;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.databinding.ComAboutDevOtherBinding;
import pub.carzy.auto_script.databinding.ComAboutDevTestBinding;
import pub.carzy.auto_script.databinding.ComAboutDevelopmentProcessBinding;
import pub.carzy.auto_script.databinding.ComAboutDialogueProcessBinding;
import pub.carzy.auto_script.databinding.ComAboutFuturePlansBinding;
import pub.carzy.auto_script.entity.DevelopmentProcessItem;
import pub.carzy.auto_script.entity.FuturePlanEntity;
import pub.carzy.auto_script.entity.WrapperEntity;
import pub.carzy.auto_script.model.AboutDevTestModel;
import pub.carzy.auto_script.model.AboutDevelopmentProcessModel;
import pub.carzy.auto_script.ui.GridBackgroundView;
import pub.carzy.auto_script.ui_components.components.CollapseView;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.MixedUtil;
import pub.carzy.auto_script.utils.StringUtils;
import pub.carzy.auto_script.utils.ThreadUtil;

import com.google.gson.reflect.TypeToken;

/**
 * @author admin
 */
public class AboutDevelopmentProcessActivity extends BaseActivity {
    private ComAboutDevelopmentProcessBinding binding;
    private AboutDevelopmentProcessModel model;
    private List<TabCallback<?>> tabList;
    private static final List<Class<? extends TabCallback<?>>> CLASSES;

    static {
        CLASSES = new ArrayList<>();
        CLASSES.add(DialogueProcessCallback.class);
        CLASSES.add(FuturePlansCallback.class);
        CLASSES.add(TestCallback.class);
        CLASSES.add(OtherCallback.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.com_about_development_process);
        model = new AboutDevelopmentProcessModel();
        initTopBar();
        initTab();
    }

    private void initTab() {
        QMUITabBuilder builder = binding.tabSegment.tabBuilder();
        Markwon markwon = BeanFactory.getInstance().get(Markwon.class, false);
        tabList = new ArrayList<>(CLASSES.size());
        //初始化
        for (Class<? extends TabCallback<?>> tabClass : CLASSES) {
            TabCallback<?> callback;
            try {
                callback = tabClass.getDeclaredConstructor().newInstance();
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException |
                     NoSuchMethodException e) {
                Log.d("AboutDevelopmentProcessActivity.initTab", "newInstance is error ", e);
                continue;
            }
            tabList.add(callback);
            callback.setContext(this);
            callback.setModel(model);
            callback.setMarkwon(markwon);
            String tabName = callback.getTabName();
            binding.tabSegment.addTab(builder.setText(tabName).build(this));
        }
        binding.tabSegment.setupWithViewPager(binding.contentViewPager, false);
        binding.tabSegment.setMode(QMUITabSegment.MODE_FIXED);
        PagerAdapter adapter = new PagerAdapter() {
            @Override
            public int getCount() {
                return tabList.size();
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                return view == object;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull final ViewGroup container, int position) {
                View view = tabList.get(position).getView();
                if (view != null) {
                    ViewGroup parent = (ViewGroup) view.getParent();
                    if (parent != null) {
                        parent.removeView(view);
                    }
                    container.addView(view);
                    return view;
                }
                return new TextView(AboutDevelopmentProcessActivity.this);
            }

            @Override
            public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View) object);
            }
        };
        binding.contentViewPager.setAdapter(adapter);
        binding.contentViewPager.setCurrentItem(0);
        tabList.get(0).selected(true);
    }

    @Override
    protected QMUITopBarLayout getTopBar() {
        return binding.topBarLayout.actionBar;
    }

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.development_process);
    }


    abstract static class TabCallback<T extends ViewDataBinding> {
        protected Context context;
        protected T binding;
        protected AboutDevelopmentProcessModel model;
        protected Markwon markwon;

        public void setMarkwon(Markwon markwon) {
            this.markwon = markwon;
        }

        public void setModel(AboutDevelopmentProcessModel model) {
            this.model = model;
        }

        public void setContext(Context context) {
            this.context = context;
        }

        public abstract String getTabName();

        @LayoutRes
        public Integer getLayoutId() {
            return null;
        }

        protected void selected(boolean first) {

        }

        public void unselected() {

        }

        public View getView() {
            if (binding != null) {
                selected(false);
                return binding.getRoot();
            }
            if (getLayoutId() != null) {
                binding = DataBindingUtil.inflate(LayoutInflater.from(context), getLayoutId(), null, false);
                selected(true);
                return binding.getRoot();
            }
            return null;
        }
    }

    static class DialogueProcessCallback extends TabCallback<ComAboutDialogueProcessBinding> {
        @Override
        public String getTabName() {
            return context.getString(R.string.dialogue_process);
        }

        @Override
        public Integer getLayoutId() {
            return R.layout.com_about_dialogue_process;
        }

        @Override
        protected void selected(boolean first) {
            if (first) {
                loadData();
            }
        }

        private void loadData() {
            ThreadUtil.runOnCpu(() -> {
                WrapperEntity<DevelopmentProcessItem> data = MixedUtil.loadFileData(
                        context,
                        R.raw.development_process,
                        new TypeToken<>() {
                        }
                );
                if (data != null) {
                    updateList(data.getData());
                }
            });
        }

        private void updateList(List<DevelopmentProcessItem> data) {
            ThreadUtil.runOnUi(() -> {
                //移除旧数据
                binding.listView.removeAllViews();
                QMUIGroupListView.Section section = QMUIGroupListView.newSection(context);
                //从小新建
                for (DevelopmentProcessItem item : data) {
                    QMUICommonListItemView view = binding.listView.createItemView(item.getTitle());
                    section.addItemView(view, e -> clickView(item, e));
                }
                section.addTo(binding.listView);
            });
        }

        private void clickView(DevelopmentProcessItem item, View e) {
            if (item.getHref() != null) {
                ActivityUtils.openToBrowser(context, item.getHref());
            }
        }
    }

    static class FuturePlansCallback extends TabCallback<ComAboutFuturePlansBinding> {

        @Override
        public String getTabName() {
            return context.getString(R.string.future_plans);
        }

        @Override
        public Integer getLayoutId() {
            return R.layout.com_about_future_plans;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void selected(boolean first) {
            super.selected(first);
            int color = context.getResources().getColor(R.color.safe, null);
            if (first) {
                binding.setModel(model);
                CollapseView<FuturePlanEntity, TextView, LinearLayout, LinearLayout> collapseView = binding.collapseView;
                collapseView.setTitleFactory(wrapper -> {
                    TextView textView = new TextView(context);
                    textView.setText(wrapper.getData().getTitle());
                    if (wrapper.getData().getProgress() >= 100) {
                        textView.setTextColor(color);
                    }
                    return textView;
                });
                collapseView.setRightFactory(wrapper -> {
                    LinearLayout layout = new LinearLayout(context);
                    layout.setOrientation(LinearLayout.HORIZONTAL);
                    layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    if (wrapper.getData().getProgress() != null) {
                        QMUIProgressBar bar = new QMUIProgressBar(context);
                        bar.setMaxValue(100);
                        bar.setQMUIProgressBarTextGenerator((b, v, m) -> 100 * v / m + "%");
                        bar.setLayoutParams(new LinearLayout.LayoutParams(QMUIDisplayHelper.dp2px(context, 22), QMUIDisplayHelper.dp2px(context, 22)));
                        bar.setTextSize(22);
                        bar.setProgress(wrapper.getData().getProgress());
                        bar.setStrokeWidth(QMUIDisplayHelper.dp2px(context, 3));
                        bar.setType(QMUIProgressBar.TYPE_CIRCLE);
                        if (wrapper.getData().getProgress() >= 100) {
                            bar.setTextColor(color);
                            bar.setProgressColor(color);
                        }
                        bar.setOnClickListener(e -> {
                            if (wrapper.getData().getUpdateTime() == null) {
                                return;
                            }
                            TextView view = new TextView(context);
                            view.setText(StringUtils.formatTime("yyyy-MM-dd", wrapper.getData().getUpdateTime()));
                            int padding = QMUIDisplayHelper.dp2px(context, 10);
                            view.setPadding(padding, padding, padding, padding);
                            //打开
                            QMUIPopups.popup(context, QMUIDisplayHelper.dp2px(context, 100))
                                    .view(view)
                                    .show(e);

                        });
                        layout.addView(bar);
                    }
                    return layout;
                });
                collapseView.setContentFactory(wrapper -> {
                    TextView textView = new TextView(context);
                    if (markwon != null) {
                        markwon.setMarkdown(textView, wrapper.getData().getDetail());
                    } else {
                        textView.setText(wrapper.getData().getDetail());
                    }
                    LinearLayout layout = new LinearLayout(context);
                    layout.addView(textView);
                    return layout;
                });
                //初始化数据
                ThreadUtil.runOnCpu(() -> {
                    WrapperEntity<FuturePlanEntity> data = MixedUtil.loadFileData(
                            context,
                            R.raw.plans, new TypeToken<>() {
                            });
                    if (data != null) {
                        model.setFuturePlanData(data.getData());
                    }
                });
            }
        }
    }

    static class OtherCallback extends TabCallback<ComAboutDevOtherBinding> {

        @Override
        public String getTabName() {
            return context.getString(R.string.other);
        }

        @Override
        public Integer getLayoutId() {
            return R.layout.com_about_dev_other;
        }
    }

    static class TestCallback extends TabCallback<ComAboutDevTestBinding> {

        @Override
        public String getTabName() {
            return context.getString(R.string.test);
        }

        @Override
        public Integer getLayoutId() {
            return R.layout.com_about_dev_test;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        protected void selected(boolean first) {
            super.selected(first);
            if (first) {
                AboutDevTestModel m = new AboutDevTestModel();
                float[] pos = new float[]{-1, -1};
                //添加格子背景
                binding.root.post(() -> {
                    View grid = new GridBackgroundView(binding.root.getContext());
                    binding.root.addView(grid, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    //获取x,y
                    pos[0] = binding.draggingBtn.getX();
                    pos[1] = binding.draggingBtn.getY();
                });
                m.setListener(c -> {
                    int px = QMUIDisplayHelper.dp2px(context, c);
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) binding.draggingBtn.getLayoutParams();
                    params.width = px;
                    params.height = px;
                    binding.draggingBtn.setLayoutParams(params);
                    binding.draggingBtn.setX(pos[0]);
                    binding.draggingBtn.setY(pos[1]);
                });
                binding.setModel(m);
                binding.confirmBtn.setOnClickListener(e -> {
                    QMUIDialog.MessageDialogBuilder builder = new QMUIDialog.MessageDialogBuilder(context);
                    builder.addAction(R.string.confirm, (dialog, index) -> {
                                dialog.dismiss();
                            }).setMessage(context.getString(R.string.test) + m.getCount())
                            .create()
                            .show();
                });
                binding.draggingBtn.setOnClickListener(e -> Toast.makeText(context, R.string.move_message, Toast.LENGTH_SHORT).show());
                binding.draggingBtn.setOnTouchListener(new View.OnTouchListener() {
                    float dX, dY;
                    long startTime;
                    static final int CLICK_THRESHOLD = 200;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                startTime = System.currentTimeMillis();
                                m.setDragging(true);
                                dX = v.getX() - event.getRawX();
                                dY = v.getY() - event.getRawY();
                                return true;

                            case MotionEvent.ACTION_MOVE:
                                v.setX(event.getRawX() + dX);
                                v.setY(event.getRawY() + dY);
                                return true;
                            case MotionEvent.ACTION_UP:
                                long clickDuration = System.currentTimeMillis() - startTime;
                                if (clickDuration < CLICK_THRESHOLD) {
                                    v.performClick();
                                }
                                m.setDragging(false);
                                break;
                            case MotionEvent.ACTION_CANCEL:
                                m.setDragging(false);
                                return true;
                        }
                        return false;
                    }

                });
                binding.resetBtn.setOnClickListener(e -> {
                    m.reset();
                });
            }
        }
    }
}
