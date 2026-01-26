package pub.carzy.auto_script.ui_components.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import pub.carzy.auto_script.ui_components.R;

/**
 * 第一个参数是数据泛型,剩下依次是标题View,右侧View,内容View,这样写回调时就不需要强转,不会弄错类型
 * @author admin
 */
public class CollapseView<D, T extends View, E extends View, C extends View> extends LinearLayout {
    private RecyclerView recyclerView;
    private CollapseAdapter<D,T,E,C> adapter;

    private boolean accordion;
    private boolean collapsible;
    private boolean ghost;
    private Drawable titleBackground;

    public void setGhost(boolean ghost) {
        this.ghost = ghost;
        adapter.notifyDataSetChanged();
    }

    private Function<DataWrapper<D>, T> titleFactory;
    private Function<DataWrapper<D>, E> rightFactory;
    private Function<DataWrapper<D>, C> contentFactory;

    private Consumer<CollapseItem<D,T,E,C>> onRenderListener;
    private Consumer<CollapseItem<D,T,E,C>> onHeaderClickListener;

    public CollapseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CollapseView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(VERTICAL);
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new CollapseAdapter<>(this);
        recyclerView.setAdapter(adapter);
        addView(recyclerView, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        try (TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CollapseView)) {
            setCollapsible(ta.getBoolean(R.styleable.CollapseView_collapsible, true));
            setAccordion(ta.getBoolean(R.styleable.CollapseView_accordion, false));
            setGhost(ta.getBoolean(R.styleable.CollapseView_ghost, false));
            setTitleBackground(ContextCompat.getDrawable(getContext(),
                    ta.getResourceId(R.styleable.CollapseView_titleBackground, R.drawable.bg_collapse_bordered)));
        }
    }

    public void setTitleFactory(Function<DataWrapper<D>, T> factory) {
        this.titleFactory = factory;
    }

    public void setRightFactory(Function<DataWrapper<D>, E> factory) {
        this.rightFactory = factory;
    }

    public void setContentFactory(Function<DataWrapper<D>, C> factory) {
        this.contentFactory = factory;
    }

    public void setAccordion(boolean accordion) {
        this.accordion = accordion;
    }

    public void setCollapsible(boolean collapsible) {
        this.collapsible = collapsible;
    }

    public void setOnRenderListener(Consumer<CollapseItem<D,T,E,C>> listener) {
        this.onRenderListener = listener;
    }

    public Drawable getTitleBackground() {
        return titleBackground;
    }

    public void setTitleBackground(Drawable titleBackground) {
        this.titleBackground = titleBackground;
    }

    public Consumer<CollapseItem<D,T,E,C>> getOnHeaderClickListener() {
        return onHeaderClickListener;
    }

    public void setOnHeaderClickListener(Consumer<CollapseItem<D,T,E,C>> onHeaderClickListener) {
        this.onHeaderClickListener = onHeaderClickListener;
    }

    public void setItems(List<D> items) {
        List<DataWrapper<D>> wrappers = new ArrayList<>(items.size());
        for (D item : items) {
            wrappers.add(new DataWrapper<>(item));
        }
        adapter.setItems(wrappers);
    }

    // ---------- Adapter ----------
    public static class CollapseAdapter<D, T extends View, E extends View, C extends View> extends RecyclerView.Adapter<VH> {
        private final CollapseView<D, T, E, C> view;
        private final List<DataWrapper<D>> items = new ArrayList<>();

        CollapseAdapter(CollapseView<D, T, E, C> view) {
            this.view = view;
        }

        void setItems(List<DataWrapper<D>> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_collapse, parent, false);
            return new VH(root);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DataWrapper<D> data = items.get(position);

            CollapseItem<D, T, E, C> item = new CollapseItem<>();
            item.data = data;
            item.rootView = holder.itemView;
            if (view.getTitleBackground() != null) {
                holder.header.setBackground(view.getTitleBackground());
            }
            if (view.ghost) {
                holder.header.setBackground(null);
                holder.header.setPadding(0, 0, 0, 0);
            }
            if (view.titleFactory != null) {
                holder.titleContainer.removeAllViews();
                item.titleView = view.titleFactory.apply(data);
                holder.titleContainer.addView(item.titleView);
            }

            if (view.rightFactory != null) {
                holder.rightContainer.removeAllViews();
                item.rightView = view.rightFactory.apply(data);
                holder.rightContainer.addView(item.rightView);
            }

            if (view.contentFactory != null) {
                holder.contentContainer.removeAllViews();
                item.contentView = view.contentFactory.apply(data);
                holder.contentContainer.addView(item.contentView);
            }
            holder.contentContainer.setVisibility(data.isExpanded() && item.getContentView() != null ? VISIBLE : GONE);

            holder.header.setOnClickListener(v -> {
                if (!view.collapsible) return;

                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                if (view.accordion) {
                    if (!item.getData().expanded) {
                        // 先关闭其他
                        for (int i = 0; i < items.size(); i++) {
                            if (i != position && items.get(i).isExpanded()) {
                                items.get(i).setExpanded(false);
                                notifyItemChanged(i);
                            }
                        }
                    }
                    // 打开或关闭当前
                    item.getData().setExpanded(!item.getData().isExpanded());
                    notifyItemChanged(position);
                } else {
                    // 独立模式
                    item.getData().setExpanded(!item.getData().isExpanded());
                    notifyItemChanged(position);
                }
                if (view.getOnHeaderClickListener() != null) {
                    view.getOnHeaderClickListener().accept(item);
                }
            });
            if (view.onRenderListener != null) {
                view.onRenderListener.accept(item);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout titleContainer, rightContainer, contentContainer, header;

        VH(@NonNull View itemView) {
            super(itemView);
            titleContainer = itemView.findViewById(R.id.title_container);
            rightContainer = itemView.findViewById(R.id.right_container);
            contentContainer = itemView.findViewById(R.id.content_container);
            header = itemView.findViewById(R.id.header);
        }
    }

    // ---------- Item ----------
    public static class CollapseItem<D, T extends View, E extends View, C extends View> {
        private View rootView;
        private T titleView;
        private E rightView;
        private C contentView;
        private DataWrapper<D> data;


        public View getRootView() {
            return rootView;
        }

        public void setRootView(View rootView) {
            this.rootView = rootView;
        }

        public T getTitleView() {
            return titleView;
        }

        public void setTitleView(T titleView) {
            this.titleView = titleView;
        }

        public E getRightView() {
            return rightView;
        }

        public void setRightView(E rightView) {
            this.rightView = rightView;
        }

        public C getContentView() {
            return contentView;
        }

        public void setContentView(C contentView) {
            this.contentView = contentView;
        }

        public DataWrapper<D> getData() {
            return data;
        }

        public void setData(DataWrapper<D> data) {
            this.data = data;
        }
    }

    public static class DataWrapper<D> {
        private D data;
        public boolean expanded;

        public DataWrapper(D data, boolean expanded) {
            this.data = data;
            this.expanded = expanded;
        }

        public DataWrapper(D data) {
            this(data, false);
        }

        public DataWrapper() {
            this(null);
        }

        public boolean isExpanded() {
            return expanded;
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
        }

        public D getData() {
            return data;
        }

        public void setData(D data) {
            this.data = data;
        }
    }
}
