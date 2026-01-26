package pub.carzy.auto_script.entity;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author admin
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TroubleshootingEntity extends BasicFileImport{
    /**
     * 问题
     */
    private String question;
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
