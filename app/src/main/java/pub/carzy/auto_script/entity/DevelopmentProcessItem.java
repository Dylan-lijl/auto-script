package pub.carzy.auto_script.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author admin
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DevelopmentProcessItem extends BasicFileImport{
    private String title;
    private String href;
}
