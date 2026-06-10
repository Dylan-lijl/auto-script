package pub.carzy.auto_script.activities.ext;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import java.util.ArrayList;
import java.util.List;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.ext.children.CommandTerminalActivity;
import pub.carzy.auto_script.entity.ExtToolEntity;

public class ExtManager {
    private static List<ExtToolEntity> tools;

    public synchronized static List<ExtToolEntity> getTools(Context context) {
        if (tools == null) {
            tools = new ArrayList<>();
            //设置对应扩展插件
            tools.add(new ExtToolEntity(CommandTerminalActivity.class, true,
                    getIcon(context,R.drawable.terminal, ContextCompat.getColor(context,R.color.link)),
                    "命令行工具", ""));
        }
        return new ArrayList<>(tools);
    }

    public static Drawable getIcon(Context context, @DrawableRes int id, String color) {
        return getIcon(context, id, Color.parseColor(color));
    }

    public static Drawable getIcon(Context context, @DrawableRes int id, @ColorInt int color) {
        Drawable drawable = ContextCompat.getDrawable(context, id);
        if (drawable != null) {
            drawable = drawable.mutate();
            DrawableCompat.setTint(drawable, color);
        }
        return drawable;
    }
}
