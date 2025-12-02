package pub.carzy.auto_script.service.dto;

import lombok.Data;

/**
 * @author admin
 */
@Data
public class OpenParam {
    private Object data;

    public OpenParam() {
    }

    public OpenParam(Object data) {
        this.data = data;
    }
}
