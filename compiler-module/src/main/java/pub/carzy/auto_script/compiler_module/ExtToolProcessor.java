package pub.carzy.auto_script.compiler_module;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class ExtToolProcessor extends AbstractProcessor {

    private Filer filer;

    @Override
    public synchronized void init(javax.annotation.processing.ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(className);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
    public static final String className = "pub.carzy.auto_script.annotation_module.ExtTool";
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        List<ExtToolHolder> holders = new ArrayList<>();

        // ⭐ 核心修改 1：通过已识别的注解 TypeElement 集合去匹配，
        // 或者直接通过 getSupportedAnnotationTypes() 里的全路径字符串来扫描！
        for (TypeElement annotationElement : annotations) {
            // 确保扫描到的是我们想要的注解
            if (!className.equals(annotationElement.getQualifiedName().toString())) {
                continue;
            }

            // 找出所有贴了该注解的类
            for (Element element : roundEnv.getElementsAnnotatedWith(annotationElement)) {
                if (element instanceof TypeElement) {
                    TypeElement activityElement = (TypeElement) element;

                    ClassName activityClassName = ClassName.get(activityElement);
                    ClassName factoryClassName = null;

                    // ⭐ 核心修改 2：彻底放弃在处理器内部直接 import 对方注解！
                    // 我们通过底层无视编译顺序的镜像元素流（AnnotationMirror），精准剥离出 factory 的 ClassName
                    for (javax.lang.model.element.AnnotationMirror mirror : activityElement.getAnnotationMirrors()) {
                        // 匹配注解的全类名
                        if (className.equals(mirror.getAnnotationType().toString())) {
                            // 遍历注解内部的属性（比如 factory()）
                            for (java.util.Map.Entry<? extends javax.lang.model.element.ExecutableElement, ? extends javax.lang.model.element.AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                                if (entry.getKey().getSimpleName().toString().equals("factory")) {
                                    // 拿到的直接是编译期的 TypeMirror，完美规避 MirroredTypeException 异常！
                                    TypeMirror factoryTypeMirror = (TypeMirror) entry.getValue().getValue();
                                    factoryClassName = ClassName.get((TypeElement) processingEnv.getTypeUtils().asElement(factoryTypeMirror));
                                    break;
                                }
                            }
                        }
                    }

                    if (factoryClassName != null) {
                        holders.add(new ExtToolHolder(activityClassName, factoryClassName));
                    }
                }
            }
        }

        // 如果这一轮啥也没捞着，直接收工
        if (holders.isEmpty()) {
            return true;
        }
        // 3. 【生成阶段】：开始用 JavaPoet 熔炼你专属的 ExtManager
        // 3. 【生成阶段】：开始用 JavaPoet 熔炼你专属的 ExtManager (数组流版)
        try {
            ClassName contextClass = ClassName.get("android.content", "Context");
            ClassName listClass = ClassName.get("java.util", "List");
            ClassName arrayListClass = ClassName.get("java.util", "ArrayList");
            ClassName entityClass = ClassName.get("pub.carzy.auto_script.common_library.entity", "ExtToolEntity"); // 💡 请确保和项目中的实际包名对齐
            com.squareup.javapoet.TypeName toolsListType = ParameterizedTypeName.get(listClass, entityClass);
            // 1. 创建成员变量: private static List<ExtToolEntity> tools;
            com.squareup.javapoet.FieldSpec toolsField = com.squareup.javapoet.FieldSpec.builder(toolsListType, "tools")
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                    .build();
            // 2. 锻造核心方法: public static synchronized List<ExtToolEntity> getTools(Context context)
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getTools")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.SYNCHRONIZED)
                    .returns(toolsListType)
                    .addParameter(contextClass, "context");
            // 3. 写入方法体的头部逻辑
            methodBuilder.beginControlFlow("if (tools == null)");
            methodBuilder.addStatement("tools = new $T<>()", arrayListClass);
            methodBuilder.addComment("扫描配置了 @ExtTool 自动生成的装配线");
            // 4. 🔥【核心核心】：利用 CodeBlock 优雅构建一整条动态数组初始化语句
            com.squareup.javapoet.CodeBlock.Builder arrayInitBuilder = com.squareup.javapoet.CodeBlock.builder();
            arrayInitBuilder.add("$T[] entities = new $T[]{\n", entityClass, entityClass);
            // 挨个把所有 factory.create(context) 拼进去，中间用逗号和换行隔开
            for (int i = 0; i < holders.size(); i++) {
                ExtToolHolder holder = holders.get(i);
                // 💡 顺便把之前错误的 innerRegisterClass 改回你的 holder.factoryName
                arrayInitBuilder.add("    new $T().create(context)", holder.factoryName);
                if (i < holders.size() - 1) {
                    arrayInitBuilder.add(",\n"); // 如果不是最后一个，补上逗号和换行
                }
            }
            arrayInitBuilder.add("\n}"); // 闭合大括号
            // 把整条数组声明语句塞进方法体（自动追加分号）
            methodBuilder.addStatement(arrayInitBuilder.build());
            // 5. 写入统一的 for-each 循环校验逻辑
            methodBuilder.beginControlFlow("for ($T entity : entities)", entityClass);
            methodBuilder.beginControlFlow("if (entity != null && entity.isEnable())");
            methodBuilder.addStatement("tools.add(entity)");
            methodBuilder.endControlFlow(); // 结束 if
            methodBuilder.endControlFlow();   // 结束 for
            // 6. 写入方法体尾部逻辑
            methodBuilder.endControlFlow(); // 结束 if (tools == null)
            methodBuilder.addStatement("return new $T<>(tools)", arrayListClass);
            // 7. 组装最终的 ExtManager 类外壳
            TypeSpec extManagerClass = TypeSpec.classBuilder("ExtManager")
                    .addModifiers(Modifier.PUBLIC)
                    .addField(toolsField)
                    .addMethod(methodBuilder.build())
                    .build();
            // 8. 吐出最终物理文件
            JavaFile.builder("pub.carzy.auto_script.activities.ext", extManagerClass)
                    .build()
                    .writeTo(filer);

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "🎉 [数组版] ExtManager 完美熔炼成功！");
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "ExtManager 生成失败: " + e.getLocalizedMessage());
        }
        return true;
    }

    // 内部临时辅助实体
    private static class ExtToolHolder {
        final ClassName activityName;
        final ClassName factoryName;

        ExtToolHolder(ClassName activityName, ClassName factoryName) {
            this.activityName = activityName;
            this.factoryName = factoryName;
        }
    }
}