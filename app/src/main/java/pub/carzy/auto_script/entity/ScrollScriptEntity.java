package pub.carzy.auto_script.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author admin
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ScrollScriptEntity extends AbstractScriptEntity {
    public Integer scrollX;
    public Integer scrollY;
    public Integer scrollDeltaX;
    public Integer scrollDeltaY;
    public boolean scrollable;
}
