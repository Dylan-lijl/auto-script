package pub.carzy.auto_script.entity;

import java.util.List;


/**
 * @author admin
 */
public class AcknowledgementEntity extends BasicFileImport{
    public static final int PEOPLE = 1;
    public static final int ORGANIZATION = 2;
    public static final int LIBRARY = 3;
    public static final int LINK = 4;
    private String title;
    private Integer type;
    private String content;
    private List<String> href;
    private Boolean deleteLine;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getHref() {
        return href;
    }

    public void setHref(List<String> href) {
        this.href = href;
    }

    public Boolean getDeleteLine() {
        return deleteLine;
    }

    public void setDeleteLine(Boolean deleteLine) {
        this.deleteLine = deleteLine;
    }
}
