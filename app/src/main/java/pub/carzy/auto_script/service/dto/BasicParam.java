package pub.carzy.auto_script.service.dto;

import lombok.Data;

/**
 * @author admin
 */
@Data
public class BasicParam {
    protected Object data;
    public BasicParam() {
        this(null);
    }
    public BasicParam(Object data) {
        this.data = data;
    }
}
