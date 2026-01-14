package pub.carzy.auto_script.utils;

/**
 * 三个泛型接口
 * @author admin
 */
@FunctionalInterface
public interface TriConsumer<A, B, C> {
    void accept(A a, B b, C c);
}

