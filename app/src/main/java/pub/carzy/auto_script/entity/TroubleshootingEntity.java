package pub.carzy.auto_script.entity;

import java.util.List;


/**
 * @author admin
 */
public class TroubleshootingEntity extends BasicFileImport{
    /**
     * 问题
     */
    private String question;

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<AcknowledgementEntity> getUsers() {
        return users;
    }

    public void setUsers(List<AcknowledgementEntity> users) {
        this.users = users;
    }

    /**
     * 答案
     */
    private String answer;
    /**
     * 是否已解决
     */
    private Boolean resolved;
    /**
     * 问题的链接
     */
    private String url;
    /**
     * 致谢列表
     */
    private List<AcknowledgementEntity> users;

}
