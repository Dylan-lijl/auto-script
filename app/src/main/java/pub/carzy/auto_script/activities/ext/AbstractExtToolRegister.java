package pub.carzy.auto_script.activities.ext;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import pub.carzy.auto_script.common_library.interfaces.ExtToolRegister;

public abstract class AbstractExtToolRegister implements ExtToolRegister {
    protected Drawable getIcon(Context context, @DrawableRes int id, String color) {
        return getIcon(context, id, Color.parseColor(color));
    }

    protected Drawable getIcon(Context context, @DrawableRes int id, @ColorInt int color) {
        Drawable drawable = ContextCompat.getDrawable(context, id);
        if (drawable != null) {
            drawable = drawable.mutate();
            DrawableCompat.setTint(drawable, color);
        }
        return drawable;
    }
}
