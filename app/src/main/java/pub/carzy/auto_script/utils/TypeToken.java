package pub.carzy.auto_script.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import lombok.Getter;

/**
 * @author admin
 */
@Getter
public abstract class TypeToken<T> {
    private final Type type;

    protected TypeToken() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof ParameterizedType) {
            this.type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        } else {
            throw new RuntimeException("TypeToken must specify actual type parameters");
        }
    }

}
