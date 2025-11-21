package pub.carzy.auto_script.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.TooltipCompat;

import pub.carzy.auto_script.R;

/**
 * @author admin
 */
public class ExtImageButton extends androidx.appcompat.widget.AppCompatImageButton {

    private String tooltipText;

    public ExtImageButton(@NonNull Context context) {
        super(context);
    }

    public ExtImageButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initTooltip(context, attrs);
    }

    public ExtImageButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void initTooltip(Context context, AttributeSet attrs) {
        if (attrs == null) return;

        try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ExtImageButton)) {
            tooltipText = a.getString(R.styleable.ExtImageButton_tooltipText);
        }

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (tooltipText != null) {
            setClickable(true);
            setLongClickable(true);
            setOnLongClickListener((e) -> {
                Toast.makeText(getContext(), tooltipText, Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }
}
