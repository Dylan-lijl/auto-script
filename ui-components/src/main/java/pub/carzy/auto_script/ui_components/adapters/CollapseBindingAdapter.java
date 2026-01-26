package pub.carzy.auto_script.ui_components.adapters;

import android.view.View;

import androidx.databinding.BindingAdapter;

import java.util.List;
import java.util.function.Consumer;

import pub.carzy.auto_script.ui_components.components.CollapseView;

/**
 * @author admin
 */
public class CollapseBindingAdapter {
    @BindingAdapter("collapsible")
    public static <D, T extends View, E extends View, C extends View> void bindCollapsible(CollapseView<D,T,E,C> view, boolean collapsible) {
        view.setCollapsible(collapsible);
    }

    @BindingAdapter("accordion")
    public static <D, T extends View, E extends View, C extends View> void bindAccordion(CollapseView<D,T,E,C> view, boolean accordion) {
        view.setAccordion(accordion);
    }

    @BindingAdapter("ghost")
    public static <D, T extends View, E extends View, C extends View> void bindGhost(CollapseView<D,T,E,C> view, boolean ghost) {
        view.setGhost(ghost);
    }

    @BindingAdapter("onRenderListener")
    public static <D, T extends View, E extends View, C extends View> void bindOnRenderListener(CollapseView<D,T,E,C> view, Consumer<CollapseView.CollapseItem<D,T,E,C>> listener) {
        view.setOnRenderListener(listener);
    }

    @BindingAdapter("onHeaderClickListener")
    public static <D, T extends View, E extends View, C extends View> void bindOnHeaderClickListener(CollapseView<D,T,E,C> view, Consumer<CollapseView.CollapseItem<D,T,E,C>> listener) {
        view.setOnHeaderClickListener(listener);
    }

    @BindingAdapter("items")
    public static <D, T extends View, E extends View, C extends View> void bindItems(CollapseView<D,T,E,C> view, List<D> items) {
        if (items != null) {
            view.setItems(items);
        }
    }


}
