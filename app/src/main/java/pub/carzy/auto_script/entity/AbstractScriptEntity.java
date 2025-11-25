package pub.carzy.auto_script.entity;

import lombok.Data;

/**
 * @author admin
 */
@Data
public class AbstractScriptEntity {
    private Integer type;
    private String version;
    private Long timestamp;
    public String className;
    public String contentDescription;
}
