package pub.carzy.auto_script.components_rules;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.ApiKt;
import com.android.tools.lint.detector.api.Issue;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import pub.carzy.auto_script.components_rules.detector.FormFieldSlotDetector;


/**
 * @author admin
 */
public class CustomerIssueRegistry extends IssueRegistry {
    @NotNull
    @Override
    public List<Issue> getIssues() {
        return Collections.singletonList(FormFieldSlotDetector.ISSUE_VIEWER);
    }

    @Override
    public int getApi() {
        return ApiKt.CURRENT_API;
    }
    @Override
    public int getMinApi() {
        return 10;
    }
}