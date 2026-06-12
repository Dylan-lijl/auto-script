package pub.carzy.auto_script.annotation_module;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import pub.carzy.auto_script.common_library.interfaces.ExtToolRegister;
/**
 * 扩展工具注解
 */
@Retention(CLASS)
@Target(TYPE)
public @interface ExtTool {
    /**
     * 使用注册器的原因是实例对应对象开销比较小,如果使用接口方式开销大很多
     * @return 注册器
     */
    Class<? extends ExtToolRegister> factory();
}
