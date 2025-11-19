package pub.carzy.auto_script.config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import pub.carzy.auto_script.ex.BeanNotFoundException;

public class BeanFactory {

    private static final BeanFactory instance = new BeanFactory();

    /** 名字 -> bean 对象 */
    private final Map<String, Object> nameMap = new ConcurrentHashMap<>();

    /** 类型 -> bean 名称集合（可能有多个实现） */
    private final Map<Class<?>, Set<String>> typeMap = new ConcurrentHashMap<>();

    private BeanFactory() {}

    public static BeanFactory getInstance() {
        return instance;
    }

    /** 注册对象（自动使用类名作为默认名字） */
    public <T> void register(T instance) {
        register(instance.getClass().getName(), instance);
    }

    /** 注册对象（指定名字） */
    public <T> void register(String name, T instance) {
        Objects.requireNonNull(name, "Bean name cannot be null");
        Objects.requireNonNull(instance, "Bean instance cannot be null");

        nameMap.put(name, instance);

        // 记录类型映射，包括所有接口和父类
        for (Class<?> type : getAllTypes(instance.getClass())) {
            typeMap.computeIfAbsent(type, k -> new HashSet<>()).add(name);
        }
    }

    /** 获取对象（按名字+类型） */
    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> type) {
        Object obj = nameMap.get(name);
        if (obj == null) {
            throw new BeanNotFoundException("Bean not found with name: " + name);
        }
        if (type != null && !type.isInstance(obj)) {
            throw new BeanNotFoundException("Bean named '" + name + "' is not of type: " + type.getName());
        }
        return (T) obj;
    }

    /** 获取对象（按名字） */
    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        Object obj = nameMap.get(name);
        if (obj == null) {
            throw new BeanNotFoundException("Bean not found with name: " + name);
        }
        return (T) obj;
    }

    /** 获取对象（按类型，唯一实现） */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        Set<String> names = typeMap.get(type);
        if (names == null || names.isEmpty()) {
            throw new BeanNotFoundException("Bean not found for type: " + type.getName());
        }
        if (names.size() > 1) {
            throw new BeanNotFoundException("Multiple beans found for type: " + type.getName() + ", specify name");
        }
        String name = names.iterator().next();
        return (T) nameMap.get(name);
    }

    /** 获取对象（按类型+名字） */
    public <T> T get(Class<T> type, String name) {
        return get(name, type);
    }

    /** 注销对象 */
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

    /** 清空所有对象 */
    public void clear() {
        nameMap.clear();
        typeMap.clear();
    }

    /** 获取类的所有接口和父类（不包括 Object） */
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
