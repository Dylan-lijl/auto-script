package pub.carzy.auto_script.entity;

import lombok.Data;

/**
 * @author admin
 */
@Data
public class KeyEntity {
    private Integer code;
    private Long downTime;
    private Long eventTime;
    private Long upTime;
}
