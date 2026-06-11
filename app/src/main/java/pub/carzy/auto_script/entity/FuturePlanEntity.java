package pub.carzy.auto_script.entity;

import java.util.Date;


/**
 * @author admin
 */
public class FuturePlanEntity extends BasicFileImport {
    private String title;
    private Date updateTime;
    private String detail;
    private Integer progress;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }
}
