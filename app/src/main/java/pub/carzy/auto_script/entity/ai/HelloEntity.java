package pub.carzy.auto_script.entity.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Date;


public class HelloEntity {
    @JsonPropertyDescription("自我介绍的纯文本,非md")
    private String text;
    @JsonPropertyDescription("文本生成的时间")
    private Date time;

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
