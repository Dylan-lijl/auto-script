package pub.carzy.auto_script.config;

import android.util.Log;

/**
 * @author admin
 */
@FunctionalInterface
public interface ControllerCallback<T> {
    void complete(T result);

    default void catchMethod(Exception e) {
        Log.e("ControllerCallback", "catchMethod: ", e);
        throw new RuntimeException(e);
    }

    default void finallyMethod() {

    }
}
