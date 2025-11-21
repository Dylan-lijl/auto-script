package pub.carzy.auto_script.config;

/**
 * @author admin
 */
@FunctionalInterface
public interface ControllerCallback<T> {
    void complete(T result);

    default void catchMethod(Exception e){

    }

    default void finallyMethod(){

    }
}
