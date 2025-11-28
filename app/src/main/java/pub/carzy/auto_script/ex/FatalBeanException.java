package pub.carzy.auto_script.ex;

import androidx.annotation.Nullable;

/**
 * @author admin
 */
public class FatalBeanException extends BeansException{
    public FatalBeanException(String msg) {
        super(msg);
    }

    public FatalBeanException(@Nullable String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }
}
