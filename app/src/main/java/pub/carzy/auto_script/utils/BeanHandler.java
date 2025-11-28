package pub.carzy.auto_script.utils;

/**
 * @author admin
 */

import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;
import pub.carzy.auto_script.ex.BeanInstantiationException;
import pub.carzy.auto_script.ex.BeansException;
import pub.carzy.auto_script.ex.FatalBeanException;

public class BeanHandler {
    public static final Map<Class<?>, PropertyDescriptor[]> CACHED_PROPERTY_DESCRIPTORS = new ConcurrentHashMap<>();

    /**
     * 创建目标类型的新实例，并将源对象的属性复制到新实例中。
     *
     * @param source 源对象
     * @param target 目标类
     * @return 复制了属性的新目标对象实例
     */
    public static <T> T copy(Object source, Class<T> target) {
        if (source == null || target == null) {
            return null;
        }
        if (target.isInterface()) {
            throw new BeanInstantiationException(target, "Specified class is an interface");
        }
        T instance = null;
        try {
            // 实例化新对象
            instance = target.getDeclaredConstructor().newInstance();
        } catch (InstantiationException var2) {
            throw new BeanInstantiationException(target, "Is it an abstract class?", var2);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException var3) {
            throw new BeanInstantiationException(target, "Is the constructor accessible?", var3);
        }
        copyProperties(source, instance);
        return instance;
    }

    /**
     * 增强型属性复制：首选 Getter/Setter，如果 Setter 不存在则尝试直接设置字段。
     */
    public static void copyProperties(Object source, Object target) throws BeansException {
        copyProperties(source, target, null, (String[])null);
    }

    public static void copyProperties(Object source, Object target, Class<?> editable) throws BeansException {
        copyProperties(source, target, editable, (String[])null);
    }

    public static void copyProperties(Object source, Object target, String... ignoreProperties) throws BeansException {
        copyProperties(source, target, null, ignoreProperties);
    }

    private static void copyProperties(Object source, Object target, @Nullable Class<?> editable, @Nullable String... ignoreProperties) throws BeansException {
        Assert.notNull(source, "Source must not be null");
        Assert.notNull(target, "Target must not be null");
        Class<?> actualEditable = target.getClass();
        if (editable != null) {
            if (!editable.isInstance(target)) {
                throw new IllegalArgumentException("Target class [" + target.getClass().getName() + "] not assignable to Editable class [" + editable.getName() + "]");
            }

            actualEditable = editable;
        }

        PropertyDescriptor[] targetPds = getPropertyDescriptors(actualEditable);
        List<String> ignoreList = ignoreProperties != null ? Arrays.asList(ignoreProperties) : null;
        int var8 = targetPds.length;

        for (PropertyDescriptor targetPd : targetPds) {
            Method writeMethod = targetPd.getWriteMethod();
            if (writeMethod != null && (ignoreList == null || !ignoreList.contains(targetPd.getName()))) {
                PropertyDescriptor sourcePd = getPropertyDescriptor(source.getClass(), targetPd.getName());
                if (sourcePd != null) {
                    Method readMethod = sourcePd.getReadMethod();
                    if (readMethod != null && isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType())) {
                        try {
                            if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
                                readMethod.setAccessible(true);
                            }

                            Object value = readMethod.invoke(source);
                            if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
                                writeMethod.setAccessible(true);
                            }

                            writeMethod.invoke(target, value);
                        } catch (Throwable var15) {
                            throw new FatalBeanException("Could not copy property '" + targetPd.getName() + "' from source to target", var15);
                        }
                    }
                }
            }
        }

    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = new HashMap<>();
    static {
        PRIMITIVE_WRAPPER_MAP.put(Boolean.TYPE, Boolean.class);
        PRIMITIVE_WRAPPER_MAP.put(Byte.TYPE, Byte.class);
        PRIMITIVE_WRAPPER_MAP.put(Character.TYPE, Character.class);
        PRIMITIVE_WRAPPER_MAP.put(Short.TYPE, Short.class);
        PRIMITIVE_WRAPPER_MAP.put(Integer.TYPE, Integer.class);
        PRIMITIVE_WRAPPER_MAP.put(Long.TYPE, Long.class);
        PRIMITIVE_WRAPPER_MAP.put(Float.TYPE, Float.class);
        PRIMITIVE_WRAPPER_MAP.put(Double.TYPE, Double.class);
    }

    /**
     * 检查 source 值 (returnType) 是否可以赋值给 target 变量 (parameterType)。
     */
    private static boolean isAssignable(Class<?> parameterType, Class<?> returnType) {
        // 1. 标准继承或接口实现检查
        if (parameterType.isAssignableFrom(returnType)) {
            return true;
        }

        // 2. 基本类型和包装类的转换检查
        if (parameterType.isPrimitive()) {
            Class<?> resolvedParameterType = PRIMITIVE_WRAPPER_MAP.get(parameterType);
            if (resolvedParameterType != null) {
                return resolvedParameterType.equals(returnType);
            }
        } else if (returnType.isPrimitive()) {
            Class<?> resolvedReturnType = PRIMITIVE_WRAPPER_MAP.get(returnType);
            if (resolvedReturnType != null) {
                return parameterType.isAssignableFrom(resolvedReturnType);
            }
        }

        // 3. 检查基本类型的隐式转换（仅限相同类型，因为不处理 int -> long 这种自动转换）
        if (parameterType.isPrimitive() && returnType.isPrimitive()) {
            return parameterType.equals(returnType);
        }

        return false;
    }

    private static PropertyDescriptor getPropertyDescriptor(Class<?> aClass, String name) {
        return Arrays.stream(getPropertyDescriptors(aClass))
                .filter(pd -> pd.getName().equals(name))
                .findFirst().orElse(null);
    }

    public static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) throws BeansException {
        if (clazz == null) {
            throw new IllegalArgumentException("Class must not be null");
        }

        // 1. 检查缓存 (Cache Check)
        PropertyDescriptor[] cached = CACHED_PROPERTY_DESCRIPTORS.get(clazz);
        if (cached != null) {
            // 命中缓存，直接返回
            return cached;
        }

        // 2. 执行内省 (Introspection)
        try {
            // ⚠️ 占位符：此处调用的是手动实现的属性查找逻辑
            PropertyDescriptor[] descriptors = introspectClassAndBuildDescriptors(clazz);

            // 3. 缓存结果 (Cache Storage)
            // 使用 putIfAbsent 确保如果多个线程同时计算出结果，只有一个能写入，其他线程使用已写入的值
            PropertyDescriptor[] existing = CACHED_PROPERTY_DESCRIPTORS.putIfAbsent(clazz, descriptors);

            // 如果 putIfAbsent 返回 non-null，说明在计算过程中有其他线程已经写入了，返回已写入的值。
            return existing != null ? existing : descriptors;

        } catch (Exception ex) {
            // 捕获反射过程中可能抛出的任何异常
            throw new FatalBeanException("Failed to introspect class [" + clazz.getName() + "]", ex);
        }
    }
    private static PropertyDescriptor[] introspectClassAndBuildDescriptors(Class<?> clazz) {

        // 临时存储已发现的 PropertyDescriptor，Key 为属性名
        Map<String, PropertyDescriptor> descriptorMap = new HashMap<>();

        // 遍历所有公共方法 (getMethods())
        for (Method method : clazz.getMethods()) {
            // 忽略静态方法和桥接方法
            if (Modifier.isStatic(method.getModifiers()) || method.isBridge()) {
                continue;
            }

            String methodName = method.getName();
            Class<?>[] paramTypes = method.getParameterTypes();

            if (methodName.startsWith("get") && paramTypes.length == 0) {
                // 发现 Getter (getXXX, 无参数)
                if ("getClass".equals(methodName)) {
                    continue;
                }

                String propertyName = methodNameToPropertyName(methodName, "get");
                Class<?> propertyType = method.getReturnType();

                // 确保 Getter 有返回值
                if (propertyType.equals(Void.TYPE)) {
                    continue;
                }

                // 存入或更新 Map
                PropertyDescriptor pd = descriptorMap.get(propertyName);
                if (pd == null) {
                    pd = new PropertyDescriptor(propertyName, propertyType, method, null);
                    descriptorMap.put(propertyName, pd);
                } else {
                    // 如果已存在 Setter，检查类型是否匹配
                    if (pd.getPropertyType().equals(propertyType)) {
                        pd.setReadMethod(method);
                    }
                }

            } else if (methodName.startsWith("is") && paramTypes.length == 0) {
                // 发现 Boolean Getter (isXXX, 无参数)
                if (!method.getReturnType().equals(Boolean.TYPE) && !method.getReturnType().equals(Boolean.class)) {
                    continue;
                }

                String propertyName = methodNameToPropertyName(methodName, "is");
                Class<?> propertyType = method.getReturnType();

                PropertyDescriptor pd = descriptorMap.get(propertyName);
                if (pd == null) {
                    pd = new PropertyDescriptor(propertyName, propertyType, method, null);
                    descriptorMap.put(propertyName, pd);
                } else {
                    if (pd.getPropertyType().equals(propertyType)) {
                        pd.setReadMethod(method);
                    }
                }

            } else if (methodName.startsWith("set") && paramTypes.length == 1) {
                // 发现 Setter (setXXX, 仅一个参数)
                String propertyName = methodNameToPropertyName(methodName, "set");
                Class<?> propertyType = paramTypes[0];

                PropertyDescriptor pd = descriptorMap.get(propertyName);
                if (pd == null) {
                    pd = new PropertyDescriptor(propertyName, propertyType, null, method);
                    descriptorMap.put(propertyName, pd);
                } else {
                    // 如果已存在 Getter，确保类型匹配
                    if (pd.getPropertyType().equals(propertyType)) {
                        pd.setWriteMethod(method);
                    }
                }
            }
        }

        // 将 Map 中的所有 PropertyDescriptor 转换为数组
        return descriptorMap.values().toArray(new PropertyDescriptor[0]);
    }

    /**
     * 辅助方法：将 Getter/Setter/isXXX 方法名转换为属性名。
     * 例如：getUserId -> userId
     */
    private static String methodNameToPropertyName(String methodName, String prefix) {
        if (methodName.length() <= prefix.length()) {
            return methodName;
        }
        String propertyName = methodName.substring(prefix.length());
        // 首字母小写
        return propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
    }

    /**
     * [Android-Friendly] PropertyDescriptor 的无依赖替代品。
     * 封装了属性名称、类型、Getter 和 Setter 方法。
     */
    @Getter
    @Setter
    public static class PropertyDescriptor {

        private final String name;
        private final Class<?> propertyType;

        @Nullable private Method readMethod;
        @Getter
        @Nullable private Method writeMethod;

        /**
         * 构造一个新的 PropertyDescriptor。
         * * @param name 属性名称（例如 "userId"）
         * @param propertyType 属性的数据类型
         * @param readMethod 属性的读取方法 (Getter)
         * @param writeMethod 属性的写入方法 (Setter)
         */
        public PropertyDescriptor(String name, Class<?> propertyType,
                                  @Nullable Method readMethod, @Nullable Method writeMethod) {

            Assert.notNull(name, "Property name must not be null");
            Assert.notNull(propertyType, "Property type must not be null");

            this.name = name;
            this.propertyType = propertyType;
            this.readMethod = readMethod;
            this.writeMethod = writeMethod;
        }
    }
}
