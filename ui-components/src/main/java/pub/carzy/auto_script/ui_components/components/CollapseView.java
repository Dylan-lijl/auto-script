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
 * @author admin
 */
public class CollapseView<T> extends LinearLayout {
    private RecyclerView recyclerView;
    private CollapseAdapter<T> adapter;

    private boolean accordion;
    private boolean collapsible;
    private boolean ghost;
    private Drawable titleBackground;

    public void setGhost(boolean ghost) {
        this.ghost = ghost;
        adapter.notifyDataSetChanged();
    }

    private Function<DataWrapper<T>, View> titleFactory;
    private Function<DataWrapper<T>, View> rightFactory;
    private Function<DataWrapper<T>, View> contentFactory;

    private Consumer<CollapseItem<T>> onRenderListener;

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

    public void setTitleFactory(Function<DataWrapper<T>, View> factory) {
        this.titleFactory = factory;
    }

    public void setRightFactory(Function<DataWrapper<T>, View> factory) {
        this.rightFactory = factory;
    }

    public void setContentFactory(Function<DataWrapper<T>, View> factory) {
        this.contentFactory = factory;
    }

    public void setAccordion(boolean accordion) {
        this.accordion = accordion;
    }

    public void setCollapsible(boolean collapsible) {
        this.collapsible = collapsible;
    }

    public void setOnRenderListener(Consumer<CollapseItem<T>> listener) {
        this.onRenderListener = listener;
    }

    public Drawable getTitleBackground() {
        return titleBackground;
    }

    public void setTitleBackground(Drawable titleBackground) {
        this.titleBackground = titleBackground;
    }

    public void setItems(List<T> items) {
        List<DataWrapper<T>> wrappers = new ArrayList<>(items.size());
        for (T item : items) {
            wrappers.add(new DataWrapper<>(item));
        }
        adapter.setItems(wrappers);
    }

    // ---------- Adapter ----------
    public static class CollapseAdapter<T> extends RecyclerView.Adapter<VH> {
        private final CollapseView<T> view;
        private final List<DataWrapper<T>> items = new ArrayList<>();

        CollapseAdapter(CollapseView<T> view) {
            this.view = view;
        }

        void setItems(List<DataWrapper<T>> list) {
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
            DataWrapper<T> data = items.get(position);

            CollapseItem<T> item = new CollapseItem<>();
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
            holder.contentContainer.setVisibility(data.isExpanded() ? VISIBLE : GONE);

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
    public static class CollapseItem<T> {
        private View rootView;
        private View titleView;
        private View rightView;
        private View contentView;
        private DataWrapper<T> data;


        public View getRootView() {
            return rootView;
        }

        public void setRootView(View rootView) {
            this.rootView = rootView;
        }

        public View getTitleView() {
            return titleView;
        }

        public void setTitleView(View titleView) {
            this.titleView = titleView;
        }

        public View getRightView() {
            return rightView;
        }

        public void setRightView(View rightView) {
            this.rightView = rightView;
        }

        public View getContentView() {
            return contentView;
        }

        public void setContentView(View contentView) {
            this.contentView = contentView;
        }

        public DataWrapper<T> getData() {
            return data;
        }

        public void setData(DataWrapper<T> data) {
            this.data = data;
        }
    }

    public static class DataWrapper<T> {
        private T data;
        public boolean expanded;

        public DataWrapper(T data, boolean expanded) {
            this.data = data;
            this.expanded = expanded;
        }

        public DataWrapper(T data) {
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

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}
