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
import com.android.utils.XmlUtils;
import com.intellij.util.DocumentUtil;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author admin
 */
public class FormFieldSlotDetector {
    public static Class<?>[] getDetectors() {
        return new Class[]{FormFieldViewerDetector.class, FormFieldEditorDetector.class};
    }

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
            new Implementation(FormFieldViewerDetector.class, Scope.RESOURCE_FILE_SCOPE)
    );
    /**
     * Editor 缺失 - 警告级别 (WARNING)
     */
    public static final Issue ISSUE_EDITOR = Issue.create(
            "MissingFormFieldEditorSlot",
            "建议添加 app:slot=\"editor\" 子组件",
            "如果没有 editor 插槽，该表单项在编辑模式下将没有任何内容。",
            Category.CORRECTNESS, 5, Severity.WARNING,
            new Implementation(FormFieldEditorDetector.class, Scope.RESOURCE_FILE_SCOPE)
    );

    private static String insertContent(Element element, String content) {
        String original = elementToString(element);
        int insertPos = original.indexOf('>') + 1;
        return original.substring(0, insertPos) + "\n    " + content + "\n" + original.substring(insertPos);
    }
    private static String elementToString(Element e) {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(e.getTagName());
        NamedNodeMap map = e.getAttributes();
        for (int i = 0; i < map.getLength(); i++) {
            Node attr = map.item(i);
            sb.append(" ").append(attr.getNodeName())
                    .append("=\"").append(attr.getNodeValue()).append("\"");
        }
        sb.append(">");
        Node child = e.getFirstChild();
        while (child != null) {
            sb.append(nodeRaw(child));
            child = child.getNextSibling();
        }
        sb.append("</").append(e.getTagName()).append(">");
        return sb.toString();
    }

    private static String nodeRaw(Node node) {
        return node == null ? "" : node.getTextContent();
    }
    public static class FormFieldViewerDetector extends LayoutDetector {
        @Override
        public Collection<String> getApplicableElements() {
            return Arrays.asList("pub.carzy.auto_script.ui_components.components.FormField", "FormField");
        }

        @Override
        public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
            NodeList children = element.getChildNodes();
            boolean hasViewer = false;
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element child = (Element) node;
                    String slot = child.getAttributeNS("http://schemas.android.com/apk/res-auto", "slot");
                    if ("viewer".equals(slot)) {
                        hasViewer = true;
                        break;
                    }
                }
            }

            if (!hasViewer) {
                Location location = context.getLocation(element);
                // 构建修复方案
                LintFix fix = fix().name("添加 viewer 子组件")
                        .replace()
                        .range(location)
                        .with(insertContent(element,"<TextView android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" app:slot=\"viewer\" />"))
                        .reformat(true)
                        .build();

                // 必须确保 report 里的 location 和 fix 里的 range 一致
                context.report(ISSUE_VIEWER, element, location,
                        "FormField 缺少必要的 app:slot=\"viewer\" 子组件", fix);
            }
        }
    }

    public static class FormFieldEditorDetector extends LayoutDetector {
        @Override
        public Collection<String> getApplicableElements() {
            // 只对我们的自定义控件感兴趣
            return Arrays.asList("pub.carzy.auto_script.ui_components.components.FormField", "FormField");
        }

        @Override
        public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
            NodeList children = element.getChildNodes();
            boolean hasEditor = false;
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element child = (Element) node;
                    String slot = child.getAttributeNS("http://schemas.android.com/apk/res-auto", "slot");
                    if ("editor".equals(slot)) {
                        hasEditor = true;
                    }
                }
            }
            if (!hasEditor) {
                Location location = context.getLocation(element);
                LintFix fix = fix().name("添加 editor 子组件")
                        .replace()
                        .range(location)
                        .with(insertContent(element,"<EditText android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" app:slot=\"editor\" />"))
                        .reformat(true)
                        .build();
                context.report(ISSUE_EDITOR, element, location,
                        "FormField 应该添加 app:slot=\"editor\" 子组件", fix);
            }
        }
    }
}
