package pub.carzy.auto_script.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author admin
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ClickScriptEntity extends AbstractScriptEntity{
    private Float x;
    private Float y;
}
