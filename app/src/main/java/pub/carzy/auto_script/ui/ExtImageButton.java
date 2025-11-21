package pub.carzy.auto_script.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.TooltipCompat;

import pub.carzy.auto_script.R;

/**
 * @author admin
 */
public class ExtImageButton extends androidx.appcompat.widget.AppCompatImageButton {
    public ExtImageButton(@NonNull Context context) {
        super(context);
    }

    public ExtImageButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtImageButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initTooltip(context, attrs);
    }

    private void initTooltip(Context context, AttributeSet attrs) {
        if (attrs == null) return;

        try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ExtImageButton)) {
            String tooltip = a.getString(R.styleable.ExtImageButton_tooltipText);
            if (tooltip != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setTooltipText(tooltip);
                } else {
                    TooltipCompat.setTooltipText(this, tooltip);
                }
            }
        }

    }

}
