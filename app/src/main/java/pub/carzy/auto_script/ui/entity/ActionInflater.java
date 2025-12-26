package pub.carzy.auto_script.ui.entity;

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
        public String idToString(){
            return String.valueOf(id);
        }
        public boolean equalsId(int id){
            return this.id == id;
        }
        public boolean equalsId(String id){
            if (id==null||id.isEmpty()){
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

    private static ActionItem parseAction(Context context, AttributeSet attrs) {
        try (TypedArray a = context.obtainStyledAttributes(
                attrs,
                new int[]{
                        android.R.attr.id,
                        android.R.attr.title,
                        android.R.attr.icon,
                        android.R.attr.enabled,
                        android.R.attr.tint
                }
        )) {
            int id = a.getResourceId(0, View.NO_ID);
            CharSequence title = a.getText(1);
            Drawable icon = a.getDrawable(2);
            boolean enabled = a.getBoolean(3, true);
            ColorStateList tint = a.getColorStateList(4);
            a.recycle();
            if (id == View.NO_ID) {
                throw new IllegalStateException("<action> must have android:id");
            }
            if (title == null) {
                throw new IllegalStateException(
                        "<action> must define android:title");
            }
            if (icon != null && tint != null) {
                icon = icon.mutate();
                icon.setTintList(tint);
            }
            return new ActionItem(id, title, icon, enabled);
        }
    }

    private static boolean isActionTag(String name) {
        return "action".equals(name) || "item".equals(name);
    }
}
