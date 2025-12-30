package pub.carzy.auto_script.ui.entity;

import static com.google.android.material.internal.ViewUtils.parseTintMode;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
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
import java.util.List;
import java.util.Objects;

import lombok.Getter;

/**
 * @author admin
 */
public class ActionInflater {
    @Getter
    public static final class ActionItem {

        private final int id;
        private final CharSequence title;
        private final Drawable icon;
        private final boolean enabled;

        public ActionItem(int id,
                          CharSequence title,
                          Drawable icon,
                          boolean enabled) {
            this.id = id;
            this.title = title;
            this.icon = icon;
            this.enabled = enabled;
        }

        public String idToString() {
            return String.valueOf(id);
        }

        public static int stringToId(String id) {
            return Integer.parseInt(id);
        }

        public boolean equalsId(int id) {
            return this.id == id;
        }

        public boolean equalsId(String id) {
            if (id == null || id.isEmpty()) {
                return false;
            }
            return this.id == Integer.parseInt(id);
        }
    }

    private ActionInflater() {
    }

    public static List<ActionItem> inflate(Context context, @XmlRes int resId) {
        try {
            return parse(context, resId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inflate actions", e);
        }
    }

    private static List<ActionItem> parse(Context context, int resId)
            throws IOException, XmlPullParserException {

        List<ActionItem> items = new ArrayList<>();

        Resources res = context.getResources();
        XmlResourceParser parser = res.getXml(resId);
        AttributeSet attrs = Xml.asAttributeSet(parser);

        int event;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {

            if (event == XmlPullParser.START_TAG
                    && isActionTag(parser.getName())) {

                items.add(parseAction(context, attrs));
            }
        }
        parser.close();
        return items;
    }

    public static final String NAMESPACE = "http://schemas.android.com/apk/res/android";
    private static ActionItem parseAction(Context context, AttributeSet attrs) {

        int id = attrs.getAttributeResourceValue(
                NAMESPACE,
                "id",
                View.NO_ID
        );

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

        if (id == View.NO_ID) {
            throw new IllegalStateException("<action> must have android:id");
        }
        if (title == null) {
            throw new IllegalStateException("<action> must have android:title");
        }

        return new ActionItem(id, title, icon, enabled);
    }


    private static boolean isActionTag(String name) {
        return "action".equals(name) || "item".equals(name);
    }
}
