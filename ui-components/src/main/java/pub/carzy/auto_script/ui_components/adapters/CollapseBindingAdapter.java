package pub.carzy.auto_script.ui_components.adapters;

import androidx.databinding.BindingAdapter;

import java.util.List;
import java.util.function.Consumer;

import pub.carzy.auto_script.ui_components.components.CollapseView;

/**
 * @author admin
 */
public class CollapseBindingAdapter {
    @BindingAdapter("collapsible")
    public static <T> void bindCollapsible(CollapseView<T> view, boolean collapsible) {
        view.setCollapsible(collapsible);
    }

    @BindingAdapter("accordion")
    public static <T> void bindAccordion(CollapseView<T> view, boolean accordion) {
        view.setAccordion(accordion);
    }

    @BindingAdapter("ghost")
    public static <T> void bindGhost(CollapseView<T> view, boolean ghost) {
        view.setGhost(ghost);
    }

    @BindingAdapter("onRenderListener")
    public static <T> void bindOnRenderListener(CollapseView<T> view, Consumer<CollapseView.CollapseItem<T>> listener) {
        view.setOnRenderListener(listener);
    }

    @BindingAdapter("items")
    public static <T> void bindItems(CollapseView<T> view, List<T> items) {
        if (items != null) {
            view.setItems(items);
        }
    }


}
