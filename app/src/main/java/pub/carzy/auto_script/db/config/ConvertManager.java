package pub.carzy.auto_script.db.config;

import java.util.HashMap;
import java.util.Map;

/**
 * @author admin
 */
public class ConvertManager {
    public interface Converter<J, D> {
        Class<J> javaType();

        Class<D> dbType();

        D toDb(J javaValue);

        J toJava(D dbValue);
    }

    private static final Map<Class<?>, Converter<?, ?>> JAVA_MAP = new HashMap<>();
    private static final Map<Class<?>, Converter<?, ?>> DB_MAP = new HashMap<>();

    static {
        register(new DateConverter());
    }

    public static void register(Converter<?, ?> c) {
        JAVA_MAP.put(c.javaType(), c);
        DB_MAP.put(c.dbType(), c);
    }

    /* Java → DB */
    @SuppressWarnings("unchecked")
    public static <J> Object toDb(J value) {
        if (value == null) return null;
        Converter<J, ?> c = (Converter<J, ?>) JAVA_MAP.get(value.getClass());
        return c == null ? value : c.toDb(value);
    }

    /* DB → Java */
    @SuppressWarnings("unchecked")
    public static <D, J> J toJava(D value, Class<J> targetType) {
        if (value == null) return null;
        Converter<J, D> c = (Converter<J, D>) DB_MAP.get(value.getClass());
        return c == null ? (J) value : c.toJava(value);
    }
}


