package pub.carzy.auto_script.ui.entity;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;

import androidx.annotation.XmlRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import lombok.Data;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.about.DefaultEmptyAboutActivity;
import pub.carzy.auto_script.activities.about.ErrorAboutActivity;

/**
 * @author admin
 */
public class PageMappingInflater {
    public static final String NAMESPACE = "http://schemas.android.com/apk/res/android";
    public static final String APP_NAMESPACE = "http://schemas.android.com/apk/res-auto";

    private PageMappingInflater() {
    }

    public static List<PageItem> inflate(Context context, @XmlRes int resId) {
        try {
            return parse(context, resId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inflate actions", e);
        }
    }

    private static List<PageItem> parse(Context context, int resId)
            throws IOException, XmlPullParserException {

        List<PageItem> items = new ArrayList<>();

        Resources res = context.getResources();
        XmlResourceParser parser = res.getXml(resId);
        AttributeSet attrs = Xml.asAttributeSet(parser);

        int event;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {

            if (event == XmlPullParser.START_TAG
                    && isSupportTag(parser.getName())) {

                items.add(parseItem(context, attrs));
            }
        }
        parser.close();
        items.sort(Comparator.comparing(PageItem::getOrder));
        return items;
    }

    @SuppressWarnings("unchecked")
    private static PageItem parseItem(Context context, AttributeSet attrs) {
        PageItem pageItem = new PageItem();
        int id = attrs.getAttributeResourceValue(
                NAMESPACE,
                "id",
                View.NO_ID
        );
        pageItem.setId(id);
        int titleRes = attrs.getAttributeResourceValue(
                NAMESPACE,
                "title",
                0
        );
        CharSequence title = titleRes != 0
                ? context.getText(titleRes)
                : attrs.getAttributeValue(
                NAMESPACE,
                "title"
        );
        pageItem.setTitle(title);
        int iconRes = attrs.getAttributeResourceValue(
                NAMESPACE,
                "icon",
                0
        );
        boolean enabled = attrs.getAttributeBooleanValue(
                NAMESPACE,
                "enabled",
                true
        );
        pageItem.setEnabled(enabled);
        int tint = attrs.getAttributeResourceValue(
                NAMESPACE,
                "tint",
                0
        );
        Drawable icon = null;
        if (iconRes != 0) {
            icon = AppCompatResources.getDrawable(context, iconRes);
            if (icon != null) {
                icon = DrawableCompat.wrap(icon).mutate();
                if (tint != 0) {
                    int color = ContextCompat.getColor(context, tint);
                    DrawableCompat.setTint(icon, color);
                }
            }
        }
        pageItem.setIcon(icon);
        int order = attrs.getAttributeIntValue(APP_NAMESPACE, "order", Integer.MAX_VALUE);
        pageItem.setOrder(order);
        String activity = attrs.getAttributeValue("http://schemas.android.com/apk/res-auto", "activity");
        Class<? extends Activity> a = null;
        if (activity == null || activity.isEmpty()) {
            a = DefaultEmptyAboutActivity.class;
        } else {
            try {
                Class<?> aClass = Class.forName(activity);
                if (Activity.class.isAssignableFrom(aClass)) {
                    a = (Class<? extends Activity>) aClass;
                } else {
                    throw new IllegalArgumentException("Not an Activity type!");
                }
            } catch (ClassNotFoundException e) {
                a = ErrorAboutActivity.class;
            }
        }
        pageItem.setActivity(a);
        if (id == View.NO_ID) {
            throw new IllegalStateException("<action> must have android:id");
        }
        return pageItem;
    }

    private static boolean isSupportTag(String name) {
        return "item".equals(name);
    }

    @Data
    public static class PageItem {
        private int id;
        private CharSequence title;
        private Drawable icon;
        private boolean enabled;
        private Class<? extends Activity> activity;
        private Integer order;

        public PageItem() {

        }

        public PageItem(int id, CharSequence title, Drawable icon, boolean enabled, Integer order) {

        }
    }

}
