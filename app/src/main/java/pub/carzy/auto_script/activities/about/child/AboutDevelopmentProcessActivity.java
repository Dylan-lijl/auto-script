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

import com.google.gson.Gson;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUIProgressBar;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.qmuiteam.qmui.widget.popup.QMUIPopups;
import com.qmuiteam.qmui.widget.tab.QMUITabBuilder;
import com.qmuiteam.qmui.widget.tab.QMUITabSegment;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

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
import pub.carzy.auto_script.entity.EventDevice;
import pub.carzy.auto_script.entity.FuturePlanEntity;
import pub.carzy.auto_script.entity.KeyEntity;
import pub.carzy.auto_script.entity.WrapperEntity;
import pub.carzy.auto_script.model.AboutDevTestModel;
import pub.carzy.auto_script.model.AboutDevelopmentProcessModel;
import pub.carzy.auto_script.core.sub.KeyRecorder;
import pub.carzy.auto_script.ui.GridBackgroundView;
import pub.carzy.auto_script.ui_components.components.CollapseView;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.EventDeviceUtil;
import pub.carzy.auto_script.utils.MixedUtil;
import pub.carzy.auto_script.utils.Shell;
import pub.carzy.auto_script.utils.Stopwatch;
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

        @Override
        protected void selected(boolean first) {
            super.selected(first);
            if (first) {
                binding.linkBtn.setOnClickListener(e -> ActivityUtils.openToBrowser(context, binding.linkBtn.getText().toString()));
                binding.linkBtn.setOnLongClickListener(e -> {
                    ActivityUtils.copyToClipboard(context, "link", binding.linkBtn.getText().toString());
                    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();
                    return true;
                });
                binding.issuesBtn.setOnClickListener(e -> ActivityUtils.openToBrowser(context, binding.issuesBtn.getText().toString()));
                binding.issuesBtn.setOnLongClickListener(e -> {
                    ActivityUtils.copyToClipboard(context, "link", binding.issuesBtn.getText().toString());
                    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();
                    return true;
                });
            }
        }
    }

    static class TestCallback extends TabCallback<ComAboutDevTestBinding> {
        private ExecutorService executor = Executors.newSingleThreadExecutor();

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
                binding.testBtn.setOnClickListener(e -> createTestListener());
                m.setMax(1000);
                AtomicReference<Thread> ref = new AtomicReference<>(null);
                binding.beginProcess.setOnClickListener((e) -> {
                    if (ref.get() != null) {
                        ref.get().interrupt();
                        m.setProcess(0);
                        m.setMax(1000);
                        ref.set(null);
                    } else {
                        ref.set(new Thread(() -> {
                            while (m.getProcess() < m.getMax()) {
                                ThreadUtil.runOnUi(() -> m.setProcess(m.getProcess() + 10));
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException exception) {
                                }
                            }
                        }));
                        ref.get().start();
                    }
                });
                Stopwatch stopwatch = new Stopwatch();
                KeyRecorder recorder = new KeyRecorder(stopwatch);
                List<KeyEntity> data = new ArrayList<>();
                binding.rootBtn.setOnClickListener(v -> ThreadUtil.runOnCpu(() -> {
                    try {
                        Process process = Shell.getRootProcess();
                        String listStr = Shell.getEventList(process);
                        List<EventDevice> devices = EventDeviceUtil.parse(listStr);
                        EventDevice target = EventDeviceUtil.findKeyActuator(devices);

                        if (target != null) {
                            ThreadUtil.runOnUi(() -> {
                                Toast.makeText(context, "开始录制: " + target.getName(), Toast.LENGTH_SHORT).show();
                                binding.stopBtn.setVisibility(View.VISIBLE);
                            });
                            stopwatch.start();
                            recorder.start(target.getPath(), data::add);
                        }
                    } catch (Exception e) {
                        Log.e(this.getClass().getCanonicalName(), "error", e);
                    }
                }));
                binding.stopBtn.setOnClickListener(v -> {
                    recorder.stop();
                    binding.stopBtn.setVisibility(View.GONE);
                    String json = new Gson().toJson(data);
                    Log.d("FINAL_DATA", json);
                });
            }
        }

        private void createTestListener() {
        }
    }

    private static final int[][] TEST_DATA = new int[][]{{3, 57, 7820},
            {3, 48, 2},
            {3, 53, 593},
            {3, 54, 945},
            {1, 330, 1},
            {0, 0, 0},
            {3, 48, 3},
            {3, 54, 946},
            {0, 0, 0},
            {3, 54, 947},
            {0, 0, 0},
            {3, 53, 592},
            {0, 0, 0},
            {3, 54, 948},
            {0, 0, 0},
            {3, 54, 949},
            {0, 0, 0},
            {3, 54, 950},
            {0, 0, 0},
            {3, 53, 591},
            {0, 0, 0},
            {3, 54, 951},
            {0, 0, 0},
            {3, 53, 590},
            {0, 0, 0},
            {3, 54, 952},
            {0, 0, 0},
            {3, 53, 589},
            {0, 0, 0},
            {3, 53, 588},
            {3, 54, 953},
            {0, 0, 0},
            {3, 53, 587},
            {0, 0, 0},
            {3, 53, 586},
            {3, 54, 954},
            {0, 0, 0},
            {3, 53, 585},
            {0, 0, 0},
            {3, 54, 955},
            {0, 0, 0},
            {3, 53, 584},
            {0, 0, 0},
            {3, 54, 956},
            {0, 0, 0},
            {3, 53, 583},
            {0, 0, 0},
            {3, 53, 582},
            {0, 0, 0},
            {3, 53, 581},
            {0, 0, 0},
            {3, 53, 580},
            {0, 0, 0},
            {3, 53, 579},
            {3, 47, 1},
            {3, 57, 7821},
            {3, 48, 3},
            {3, 53, 579},
            {3, 54, 1474},
            {0, 0, 0},
            {3, 47, 0},
            {3, 53, 578},
            {0, 0, 0},
            {3, 47, 1},
            {3, 53, 578},
            {0, 0, 0},
            {3, 47, 0},
            {3, 53, 577},
            {3, 47, 1},
            {3, 53, 577},
            {3, 54, 1475},
            {0, 0, 0},
            {3, 53, 576},
            {3, 54, 1478},
            {0, 0, 0},
            {3, 53, 575},
            {3, 54, 1481},
            {0, 0, 0},
            {3, 53, 574},
            {3, 54, 1485},
            {0, 0, 0},
            {3, 47, 0},
            {3, 53, 576},
            {3, 47, 1},
            {3, 53, 572},
            {3, 54, 1489},
            {0, 0, 0},
            {3, 53, 571},
            {3, 54, 1493},
            {0, 0, 0},
            {3, 53, 570},
            {3, 54, 1498},
            {0, 0, 0},
            {3, 53, 569},
            {3, 54, 1503},
            {0, 0, 0},
            {3, 47, 0},
            {3, 54, 957},
            {3, 47, 1},
            {3, 53, 568},
            {3, 54, 1507},
            {0, 0, 0},
            {3, 54, 1512},
            {0, 0, 0},
            {3, 47, 0},
            {3, 53, 575},
            {3, 47, 1},
            {3, 53, 567},
            {3, 54, 1516},
            {0, 0, 0},
            {3, 53, 566},
            {3, 54, 1520},
            {0, 0, 0},
            {3, 53, 565},
            {3, 54, 1523},
            {0, 0, 0},
            {3, 54, 1527},
            {0, 0, 0},
            {3, 47, 0},
            {3, 53, 574},
            {3, 47, 1},
            {3, 53, 564},
            {3, 54, 1529},
            {0, 0, 0},
            {3, 54, 1532},
            {0, 0, 0},
            {3, 54, 1534},
            {0, 0, 0},
            {3, 54, 1535},
            {0, 0, 0},
            {3, 54, 1536},
            {0, 0, 0},
            {3, 47, 0},
            {3, 53, 573},
            {3, 47, 1},
            {3, 53, 565},
            {0, 0, 0},
            {3, 54, 1535},
            {0, 0, 0},
            {3, 53, 566},
            {3, 54, 1534},
            {0, 0, 0},
            {3, 47, 0},
            {3, 53, 572},
            {3, 47, 1},
            {3, 53, 568},
            {3, 54, 1533},
            {0, 0, 0},
            {3, 53, 570},
            {3, 54, 1530},
            {0, 0, 0},
            {3, 47, 0},
            {3, 54, 958},
            {3, 47, 1},
            {3, 53, 572},
            {3, 54, 1528},
            {0, 0, 0},
            {3, 47, 0},
            {3, 53, 571},
            {3, 47, 1},
            {3, 53, 574},
            {3, 54, 1525},
            {0, 0, 0},
            {3, 53, 577},
            {3, 54, 1522},
            {0, 0, 0},
            {3, 47, 0},
            {3, 53, 570},
            {3, 47, 1},
            {3, 53, 579},
            {3, 54, 1519},
            {0, 0, 0},
            {3, 53, 582},
            {3, 54, 1516},
            {0, 0, 0},
            {3, 53, 585},
            {3, 54, 1513},
            {0, 0, 0},
            {3, 53, 588},
            {3, 54, 1510},
            {0, 0, 0},
            {3, 47, 0},
            {3, 53, 569},
            {3, 47, 1},
            {3, 53, 591},
            {3, 54, 1507},
            {0, 0, 0},
            {3, 53, 593},
            {3, 54, 1503},
            {0, 0, 0},
            {3, 53, 596},
            {3, 54, 1501},
            {0, 0, 0},
            {3, 53, 598},
            {3, 54, 1498},
            {0, 0, 0},
            {3, 47, 0},
            {3, 53, 568},
            {3, 54, 959},
            {3, 47, 1},
            {3, 53, 601},
            {3, 54, 1496},
            {0, 0, 0},
            {3, 53, 603},
            {3, 54, 1494},
            {0, 0, 0},
            {3, 53, 605},
            {3, 54, 1492},
            {0, 0, 0},
            {3, 53, 607},
            {3, 54, 1490},
            {0, 0, 0},
            {3, 47, 0},
            {3, 53, 567},
            {3, 47, 1},
            {3, 53, 609},
            {3, 54, 1489},
            {0, 0, 0},
            {3, 53, 610},
            {3, 54, 1487},
            {0, 0, 0},
            {3, 53, 612},
            {3, 54, 1486},
            {0, 0, 0},
            {3, 53, 613},
            {3, 54, 1485},
            {0, 0, 0},
            {3, 47, 0},
            {3, 53, 566},
            {3, 47, 1},
            {3, 53, 614},
            {3, 54, 1484},
            {0, 0, 0},
            {3, 53, 615},
            {3, 54, 1483},
            {0, 0, 0},
            {3, 53, 616},
            {3, 54, 1482},
            {0, 0, 0},
            {3, 54, 1481},
            {0, 0, 0},
            {3, 53, 617},
            {3, 54, 1480},
            {0, 0, 0},
            {3, 53, 618},
            {3, 54, 1479},
            {0, 0, 0},
            {3, 53, 619},
            {3, 54, 1478},
            {0, 0, 0},
            {3, 54, 1477},
            {0, 0, 0},
            {3, 53, 620},
            {0, 0, 0},
            {3, 54, 1476},
            {0, 0, 0},
            {3, 47, 0},
            {3, 54, 960},
            {3, 47, 1},
            {3, 53, 621},
            {0, 0, 0},
            {3, 54, 1475},
            {0, 0, 0},
            {3, 53, 622},
            {0, 0, 0},
            {3, 47, 0},
            {3, 53, 565},
            {3, 47, 1},
            {3, 54, 1474},
            {0, 0, 0},
            {3, 53, 623},
            {0, 0, 0},
            {3, 54, 1473},
            {0, 0, 0},
            {3, 53, 624},
            {0, 0, 0},
            {3, 54, 1472},
            {0, 0, 0},
            {3, 53, 625},
            {0, 0, 0},
            {3, 54, 1471},
            {0, 0, 0},
            {3, 53, 626},
            {0, 0, 0},
            {3, 54, 1470},
            {0, 0, 0},
            {3, 53, 627},
            {0, 0, 0},
            {3, 54, 1469},
            {0, 0, 0},
            {3, 53, 628},
            {0, 0, 0},
            {3, 54, 1468},
            {0, 0, 0},
            {3, 53, 629},
            {0, 0, 0},
            {3, 53, 630},
            {3, 54, 1467},
            {0, 0, 0},
            {3, 53, 631},
            {0, 0, 0},
            {3, 53, 632},
            {3, 54, 1466},
            {0, 0, 0},
            {3, 53, 633},
            {0, 0, 0},
            {3, 53, 634},
            {3, 54, 1465},
            {0, 0, 0},
            {3, 53, 635},
            {0, 0, 0},
            {3, 53, 636},
            {3, 54, 1464},
            {0, 0, 0},
            {3, 54, 1463},
            {0, 0, 0},
            {3, 53, 637},
            {0, 0, 0},
            {3, 54, 1462},
            {0, 0, 0},
            {3, 53, 638},
            {0, 0, 0},
            {3, 53, 639},
            {0, 0, 0},
            {3, 53, 640},
            {0, 0, 0},
            {3, 54, 1461},
            {0, 0, 0},
            {3, 57, -1},
            {0, 0, 0},
            {3, 57, 7822},
            {3, 48, 2},
            {3, 53, 796},
            {3, 54, 1410},
            {0, 0, 0},
            {3, 53, 795},
            {0, 0, 0},
            {3, 53, 794},
            {3, 54, 1411},
            {0, 0, 0},
            {3, 53, 792},
            {3, 54, 1413},
            {0, 0, 0},
            {3, 53, 790},
            {3, 54, 1417},
            {0, 0, 0},
            {3, 53, 787},
            {3, 54, 1420},
            {0, 0, 0},
            {3, 53, 784},
            {3, 54, 1425},
            {0, 0, 0},
            {3, 53, 781},
            {3, 54, 1429},
            {0, 0, 0},
            {3, 53, 778},
            {3, 54, 1433},
            {0, 0, 0},
            {3, 53, 775},
            {3, 54, 1438},
            {0, 0, 0},
            {3, 53, 772},
            {3, 54, 1444},
            {0, 0, 0},
            {3, 53, 769},
            {3, 54, 1449},
            {0, 0, 0},
            {3, 53, 766},
            {3, 54, 1455},
            {0, 0, 0},
            {3, 47, 0},
            {3, 48, 4},
            {3, 47, 1},
            {3, 53, 762},
            {3, 54, 1462},
            {0, 0, 0},
            {3, 53, 758},
            {3, 54, 1469},
            {0, 0, 0},
            {3, 53, 753},
            {3, 54, 1477},
            {0, 0, 0},
            {3, 53, 749},
            {3, 54, 1484},
            {0, 0, 0},
            {3, 53, 744},
            {3, 54, 1491},
            {0, 0, 0},
            {3, 53, 740},
            {3, 54, 1498},
            {0, 0, 0},
            {3, 53, 736},
            {3, 54, 1504},
            {0, 0, 0},
            {3, 53, 732},
            {3, 54, 1510},
            {0, 0, 0},
            {3, 53, 728},
            {3, 54, 1517},
            {0, 0, 0},
            {3, 53, 724},
            {3, 54, 1522},
            {0, 0, 0},
            {3, 53, 720},
            {3, 54, 1527},
            {0, 0, 0},
            {3, 53, 717},
            {3, 54, 1531},
            {0, 0, 0},
            {3, 53, 714},
            {3, 54, 1535},
            {0, 0, 0},
            {3, 53, 711},
            {3, 54, 1539},
            {0, 0, 0},
            {3, 53, 708},
            {3, 54, 1542},
            {0, 0, 0},
            {3, 53, 705},
            {3, 54, 1546},
            {0, 0, 0},
            {3, 53, 702},
            {3, 54, 1549},
            {0, 0, 0},
            {3, 48, 3},
            {3, 53, 699},
            {3, 54, 1552},
            {0, 0, 0},
            {3, 53, 697},
            {3, 54, 1555},
            {0, 0, 0},
            {3, 53, 694},
            {3, 54, 1557},
            {0, 0, 0},
            {3, 53, 692},
            {3, 54, 1559},
            {0, 0, 0},
            {3, 53, 690},
            {3, 54, 1560},
            {0, 0, 0},
            {3, 53, 688},
            {3, 54, 1562},
            {0, 0, 0},
            {3, 53, 686},
            {3, 54, 1563},
            {0, 0, 0},
            {3, 53, 685},
            {3, 54, 1564},
            {0, 0, 0},
            {3, 53, 684},
            {3, 54, 1565},
            {0, 0, 0},
            {3, 53, 683},
            {3, 54, 1566},
            {0, 0, 0},
            {3, 53, 682},
            {0, 0, 0},
            {3, 53, 681},
            {3, 54, 1567},
            {0, 0, 0},
            {3, 57, -1},
            {0, 0, 0},
            {3, 47, 0},
            {3, 54, 961},
            {0, 0, 0},
            {3, 47, 1},
            {3, 57, 7823},
            {3, 48, 2},
            {3, 53, 534},
            {3, 54, 1737},
            {0, 0, 0},
            {3, 54, 1736},
            {0, 0, 0},
            {3, 53, 535},
            {0, 0, 0},
            {3, 53, 537},
            {3, 54, 1735},
            {0, 0, 0},
            {3, 53, 540},
            {3, 54, 1733},
            {0, 0, 0},
            {3, 53, 543},
            {3, 54, 1730},
            {0, 0, 0},
            {3, 53, 546},
            {3, 54, 1727},
            {0, 0, 0},
            {3, 53, 550},
            {3, 54, 1723},
            {0, 0, 0},
            {3, 53, 554},
            {3, 54, 1719},
            {0, 0, 0},
            {3, 53, 559},
            {3, 54, 1715},
            {0, 0, 0},
            {3, 53, 563},
            {3, 54, 1710},
            {0, 0, 0},
            {3, 53, 567},
            {3, 54, 1706},
            {0, 0, 0},
            {3, 53, 571},
            {3, 54, 1701},
            {0, 0, 0},
            {3, 53, 574},
            {3, 54, 1697},
            {0, 0, 0},
            {3, 53, 577},
            {3, 54, 1693},
            {0, 0, 0},
            {3, 53, 580},
            {3, 54, 1689},
            {0, 0, 0},
            {3, 53, 582},
            {3, 54, 1685},
            {0, 0, 0},
            {3, 53, 584},
            {3, 54, 1682},
            {0, 0, 0},
            {3, 53, 585},
            {3, 54, 1679},
            {0, 0, 0},
            {3, 53, 587},
            {3, 54, 1676},
            {0, 0, 0},
            {3, 53, 588},
            {3, 54, 1673},
            {0, 0, 0},
            {3, 53, 589},
            {3, 54, 1670},
            {0, 0, 0},
            {3, 53, 590},
            {3, 54, 1667},
            {0, 0, 0},
            {3, 53, 591},
            {3, 54, 1665},
            {0, 0, 0},
            {3, 53, 592},
            {3, 54, 1662},
            {0, 0, 0},
            {3, 54, 1659},
            {0, 0, 0},
            {3, 53, 593},
            {3, 54, 1657},
            {0, 0, 0},
            {3, 54, 1655},
            {0, 0, 0},
            {3, 53, 594},
            {3, 54, 1653},
            {0, 0, 0},
            {3, 53, 595},
            {3, 54, 1650},
            {0, 0, 0},
            {3, 54, 1647},
            {0, 0, 0},
            {3, 53, 596},
            {3, 54, 1643},
            {0, 0, 0},
            {3, 54, 1640},
            {0, 0, 0},
            {3, 53, 597},
            {3, 54, 1636},
            {0, 0, 0},
            {3, 54, 1633},
            {0, 0, 0},
            {3, 53, 598},
            {3, 54, 1630},
            {0, 0, 0},
            {3, 54, 1627},
            {0, 0, 0},
            {3, 53, 599},
            {3, 54, 1624},
            {0, 0, 0},
            {3, 54, 1622},
            {0, 0, 0},
            {3, 53, 600},
            {3, 54, 1619},
            {0, 0, 0},
            {3, 54, 1617},
            {0, 0, 0},
            {3, 53, 601},
            {3, 54, 1615},
            {0, 0, 0},
            {3, 54, 1613},
            {0, 0, 0},
            {3, 53, 602},
            {3, 54, 1611},
            {0, 0, 0},
            {3, 53, 603},
            {3, 54, 1609},
            {0, 0, 0},
            {3, 53, 604},
            {3, 54, 1607},
            {0, 0, 0},
            {3, 54, 1605},
            {0, 0, 0},
            {3, 53, 605},
            {3, 54, 1603},
            {0, 0, 0},
            {3, 53, 606},
            {3, 54, 1602},
            {0, 0, 0},
            {3, 54, 1600},
            {0, 0, 0},
            {3, 53, 607},
            {3, 54, 1599},
            {0, 0, 0},
            {3, 54, 1597},
            {0, 0, 0},
            {3, 53, 608},
            {3, 54, 1596},
            {0, 0, 0},
            {3, 54, 1595},
            {0, 0, 0},
            {3, 53, 609},
            {3, 54, 1594},
            {0, 0, 0},
            {3, 54, 1593},
            {0, 0, 0},
            {3, 54, 1592},
            {0, 0, 0},
            {3, 54, 1591},
            {0, 0, 0},
            {3, 53, 610},
            {3, 54, 1590},
            {0, 0, 0},
            {3, 54, 1589},
            {0, 0, 0},
            {3, 54, 1588},
            {0, 0, 0},
            {3, 54, 1587},
            {0, 0, 0},
            {3, 53, 611},
            {0, 0, 0},
            {3, 54, 1586},
            {0, 0, 0},
            {3, 47, 0},
            {3, 54, 962},
            {3, 47, 1},
            {3, 54, 1585},
            {0, 0, 0},
            {3, 57, -1},
            {0, 0, 0},
            {3, 47, 0},
            {3, 53, 564},
            {0, 0, 0},
            {3, 54, 963},
            {0, 0, 0},
            {3, 53, 565},
            {0, 0, 0},
            {3, 54, 962},
            {0, 0, 0},
            {3, 48, 3},
            {0, 0, 0},
            {3, 57, -1},
            {1, 330, 0},
            {0, 0, 0},
            {3, 57, 7824},
            {3, 48, 2},
            {3, 53, 1015},
            {3, 54, 2100},
            {1, 330, 1},
            {0, 0, 0},
            {3, 57, -1},
            {1, 330, 0},
            {0, 0, 0}};
}
