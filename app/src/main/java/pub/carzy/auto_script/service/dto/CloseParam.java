package pub.carzy.auto_script.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author admin
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CloseParam extends BasicParam{
    public CloseParam() {
    }

    public CloseParam(Object data) {
        super(data);
    }
}
