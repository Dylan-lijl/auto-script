package pub.carzy.auto_script.components_rules.detector;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author admin
 */
public class FormFieldSlotDetector extends LayoutDetector {
    /**
     * Viewer 缺失 - 错误级别 (ERROR)
     */
    public static final Issue ISSUE_VIEWER = Issue.create(
            "MissingFormFieldViewerSlot",
            "必须存在至少一个app:slot=\"viewer\"子组件",
            "缺少app:slot=\"viewer\"子组件",
            Category.CORRECTNESS,
            8,
            Severity.ERROR,
            new Implementation(FormFieldSlotDetector.class, Scope.RESOURCE_FILE_SCOPE)
    );
    /**
     * Editor 缺失 - 警告级别 (WARNING)
     */
    public static final Issue ISSUE_EDITOR = Issue.create(
            "MissingFormFieldEditorSlot",
            "建议添加 app:slot=\"editor\" 子组件",
            "如果没有 editor 插槽，该表单项在编辑模式下将没有任何内容。",
            Category.CORRECTNESS, 5, Severity.WARNING,
            new Implementation(FormFieldSlotDetector.class, Scope.RESOURCE_FILE_SCOPE)
    );

    @Override
    public Collection<String> getApplicableElements() {
        // 只对我们的自定义控件感兴趣
        return Arrays.asList("pub.carzy.auto_script.ui_components.components.FormField", "FormField");
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        NodeList children = element.getChildNodes();
        boolean hasViewer = false;
        boolean hasEditor = false;

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                String slot = child.getAttributeNS("http://schemas.android.com/apk/res-auto", "slot");
                if ("viewer".equals(slot)) {
                    hasViewer = true;
                } else if ("editor".equals(slot)) {
                    hasEditor = true;
                }
            }
        }
        Location location = context.getLocation(element);
        if (!hasViewer) {
            //todo 修正问题出现互相覆盖
            appendViewer(context,element);
            LintFix fix = fix().name("添加 viewer 占位符")
                    .replace()
                    .range(location)
                    .reformat(true)
                    .build();
            context.report(ISSUE_VIEWER, element, context.getElementLocation(element),
                    "FormField 缺少必要的 app:slot=\"viewer\" 子组件", fix);
        }
        if (!hasEditor) {
            appendEditor(context,element);
            LintFix fix = fix().name("添加 editor 占位符")
                    .replace()
                    .range(location)
                    .reformat(true)
                    .build();
            context.report(ISSUE_EDITOR, element, context.getElementLocation(element),
                    "FormField 应该添加 app:slot=\"editor\" 子组件", fix);
        }
    }

    private void appendViewer(XmlContext context, Element element) {
        Element viewer = context.document.createElement("TextView");
        viewer.setAttribute("android:layout_width", "wrap_content");
        viewer.setAttribute("android:layout_height", "wrap_content");
        viewer.setAttributeNS("http://schemas.android.com/apk/res-auto", "app:slot", "viewer");

        element.appendChild(viewer);
    }

    private void appendEditor(XmlContext context, Element element) {
        Element editor = context.document.createElement("EditText");
        editor.setAttribute("android:layout_width", "wrap_content");
        editor.setAttribute("android:layout_height", "wrap_content");
        editor.setAttributeNS("http://schemas.android.com/apk/res-auto", "app:slot", "editor");

        element.appendChild(editor);
    }

}
