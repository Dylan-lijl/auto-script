package pub.carzy.auto_script.config;

import java.util.*;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import pub.carzy.auto_script.ex.BeanNotFoundException;
import pub.carzy.auto_script.utils.MyTypeToken;

/**
 * @author admin
 */
public class BeanFactory {

    @Getter
    private static final BeanFactory instance = new BeanFactory();

    /**
     * 名字 -> bean 对象
     */
    private final Map<String, Object> nameMap = new ConcurrentHashMap<>();

    /**
     * 类型 -> bean 名称集合（可能有多个实现）
     */
    private final Map<Class<?>, Set<String>> typeMap = new ConcurrentHashMap<>();
    private final Map<Type, Object> genericTypeMap = new ConcurrentHashMap<>();

    private BeanFactory() {
    }

    /**
     * 注册对象（自动使用类名作为默认名字）
     */
    public <T> BeanFactory register(T instance) {
        return register(instance.getClass().getName(), instance);
    }

    /**
     * 注册对象（指定名字）
     */
    public <T> BeanFactory register(String name, T instance) {
        Objects.requireNonNull(name, "Bean name cannot be null");
        Objects.requireNonNull(instance, "Bean instance cannot be null");

        nameMap.put(name, instance);

        // 记录类型映射，包括所有接口和父类
        for (Class<?> type : getAllTypes(instance.getClass())) {
            typeMap.computeIfAbsent(type, k -> new HashSet<>()).add(name);
        }
        return this;
    }

    public <T> BeanFactory register(MyTypeToken<T> type, Object instance) {
        Objects.requireNonNull(type, "Generic type cannot be null");
        Objects.requireNonNull(instance, "Instance cannot be null");
        genericTypeMap.put(type.getType(), instance);
        register(instance);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(MyTypeToken<?> type, boolean ex) {
        Object instance = genericTypeMap.get(type.getType());
        if (instance == null) {
            if (ex) {
                throw new BeanNotFoundException("Bean not found for type: " + type);
            } else {
                return null;
            }

        }
        return (T) instance;
    }

    public <T> T get(MyTypeToken<?> type) {
        return get(type, true);
    }

    /**
     * 获取对象（按名字+类型）
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> type, boolean ex) {
        Object obj = nameMap.get(name);
        if (obj == null) {
            if (ex) {
                throw new BeanNotFoundException("Bean not found with name: " + name);
            } else {
                return null;
            }
        }
        if (type != null && !type.isInstance(obj)) {
            if (ex) {
                throw new BeanNotFoundException("Bean named '" + name + "' is not of type: " + type.getName());
            } else {
                return null;
            }
        }
        return (T) obj;
    }

    public <T> T get(String name, Class<T> type) {
        return get(name, type, true);
    }

    /**
     * 获取对象（按名字）
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name, boolean ex) {
        Object obj = nameMap.get(name);
        if (obj == null) {
            if (ex) {
                throw new BeanNotFoundException("Bean not found with name: " + name);
            } else {
                return null;
            }
        }
        return (T) obj;
    }

    public <T> T get(String name) {
        return get(name, true);
    }

    /**
     * 获取对象（按类型，唯一实现）
     */
    public <T> T get(Class<T> type) {
        return get(type, true);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type, boolean ex) {
        Set<String> names = typeMap.get(type);
        if (names == null || names.isEmpty()) {
            if (ex) {
                throw new BeanNotFoundException("Bean not found for type: " + type.getName());
            } else {
                return null;
            }
        }
        if (names.size() > 1) {
            if (ex) {
                throw new BeanNotFoundException("Multiple beans found for type: " + type.getName() + ", specify name");
            } else {
                return null;
            }
        }
        String name = names.iterator().next();
        return (T) nameMap.get(name);
    }

    /**
     * 获取对象（按类型+名字）
     */
    public <T> T get(Class<T> type, String name) {
        return get(name, type);
    }

    /**
     * 注销对象
     */
    public void unregister(String name) {
        Object obj = nameMap.remove(name);
        if (obj != null) {
            for (Class<?> type : getAllTypes(obj.getClass())) {
                Set<String> names = typeMap.get(type);
                if (names != null) {
                    names.remove(name);
                    if (names.isEmpty()) {
                        typeMap.remove(type);
                    }
                }
            }
        }
    }
    public void unregister(Class<?> clazz) {
        Set<String> names = typeMap.get(clazz);
        if (names == null || names.isEmpty()) return;

        // copy 防止 ConcurrentModification
        for (String name : new HashSet<>(names)) {
            unregister(name);
        }
    }

    public void unregister(MyTypeToken<?> token) {
        Type type = token.getType();
        Object bean = genericTypeMap.remove(type);
        if (bean == null) return;

        // 同时需要从 nameMap 中删除同一个 bean
        String removeKey = null;
        for (Map.Entry<String, Object> e : nameMap.entrySet()) {
            if (e.getValue() == bean) {
                removeKey = e.getKey();
                break;
            }
        }
        if (removeKey != null) {
            unregister(removeKey);
        }
    }

    /**
     * 清空所有对象
     */
    public void clear() {
        nameMap.clear();
        typeMap.clear();
    }

    /**
     * 获取类的所有接口和父类（不包括 Object）
     */
    private Set<Class<?>> getAllTypes(Class<?> clazz) {
        Set<Class<?>> types = new HashSet<>();
        while (clazz != null && clazz != Object.class) {
            types.add(clazz);
            for (Class<?> i : clazz.getInterfaces()) {
                types.add(i);
            }
            clazz = clazz.getSuperclass();
        }
        return types;
    }
}
