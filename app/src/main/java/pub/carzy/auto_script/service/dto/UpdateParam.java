package pub.carzy.auto_script.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author admin
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateParam extends BasicParam{
    public UpdateParam() {
    }

    public UpdateParam(Object data) {
        super(data);
    }
}
