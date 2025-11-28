package pub.carzy.auto_script.config;

/**
 * @author admin
 */
public interface IdGenerator<T> {
    /**
     * 生成一个唯一 ID。
     *
     * @return 生成的唯一 ID。
     */
    T nextId();
}
