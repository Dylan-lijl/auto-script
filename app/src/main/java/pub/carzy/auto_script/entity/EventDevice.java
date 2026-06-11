package pub.carzy.auto_script.entity;

import java.util.ArrayList;
import java.util.List;


/**
 * @author admin
 */
public class EventDevice {
    private Integer id;
    private String path;
    private String name;
    private List<EventContent> events = new ArrayList<>();
    private List<InputProp>  inputProps = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<EventContent> getEvents() {
        return events;
    }

    public void setEvents(List<EventContent> events) {
        this.events = events;
    }

    public List<InputProp> getInputProps() {
        return inputProps;
    }

    public void setInputProps(List<InputProp> inputProps) {
        this.inputProps = inputProps;
    }
}
