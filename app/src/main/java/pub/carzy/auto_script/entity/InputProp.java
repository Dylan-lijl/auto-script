package pub.carzy.auto_script.entity;

import lombok.Data;

/**
 * @author admin
 */
@Data
public class InputProp {
    private String name;

    public InputProp(String name) {
        this.name = name;
    }
}
