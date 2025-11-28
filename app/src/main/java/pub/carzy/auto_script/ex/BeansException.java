package pub.carzy.auto_script.ex;

import androidx.annotation.Nullable;

public abstract class BeansException extends RuntimeException {
    public BeansException(String msg) {
        super(msg);
    }

    public BeansException(@Nullable String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }
}
