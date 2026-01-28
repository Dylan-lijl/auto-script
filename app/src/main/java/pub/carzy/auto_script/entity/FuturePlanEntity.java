package pub.carzy.auto_script.entity;

import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author admin
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FuturePlanEntity extends BasicFileImport {
    private String title;
    private Date updateTime;
    private String detail;
    private Integer progress;
}
