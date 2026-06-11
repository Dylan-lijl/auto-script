package pub.carzy.auto_script.entity;

import java.util.ArrayList;
import java.util.List;


/**
 * @author admin
 */
public class WrapperEntity<T> {
    private List<T> data = new ArrayList<>();

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
}
