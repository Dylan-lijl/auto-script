package pub.carzy.auto_script.utils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 封装了底层反射操作的工具类，用于处理访问权限和异常。
 */
public abstract class ReflectionUtils {

    /**
     * 确保 Member (Method, Field, Constructor) 可以访问，即使它是私有的。
     * @param member 待设置的反射成员
     */
    public static void makeAccessible(Member member) {
        AccessibleObject ao = (AccessibleObject) member;
        // 如果成员是 private 或 protected，则设置可访问
        if (!ao.isAccessible() && !Modifier.isPublic(member.getModifiers())) {
            ao.setAccessible(true);
        }
    }

    /**
     * 调用指定对象的方法。封装了方法权限设置和异常处理。
     * @param method 待调用的方法
     * @param target 调用方法的对象实例
     * @param args 方法参数
     * @return 方法返回值
     */
    public static Object invokeMethod(Method method, Object target, Object... args) throws RuntimeException {
        makeAccessible(method);
        try {
            return method.invoke(target, args);
        } catch (Exception e) {
            // 将所有反射相关的受检异常包装成 RuntimeException
            throw new RuntimeException("Reflection failed during method invocation: " + method.getName(), e);
        }
    }

    /**
     * 设置指定对象的字段值。封装了权限设置和异常处理。
     * @param field 待设置的字段
     * @param target 拥有字段的对象实例
     * @param value 待设置的值
     */
    public static void setField(Field field, Object target, Object value) throws RuntimeException {
        makeAccessible(field);
        try {
            field.set(target, value);
        } catch (Exception e) {
            // 将所有反射相关的受检异常包装成 RuntimeException
            throw new RuntimeException("Reflection failed during field setting: " + field.getName(), e);
        }
    }

    /**
     * 获取指定对象的字段值。
     */
    public static Object getField(Field field, Object target) throws RuntimeException {
        makeAccessible(field);
        try {
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException("Reflection failed during field getting: " + field.getName(), e);
        }
    }
}
