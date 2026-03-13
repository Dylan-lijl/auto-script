package pub.carzy.auto_script.entity;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * @author admin
 */
@Data
public class EventDevice {
    private Integer id;
    private String path;
    private String name;
    private List<EventContent> events = new ArrayList<>();
    private List<InputProp>  inputProps = new ArrayList<>();
}
