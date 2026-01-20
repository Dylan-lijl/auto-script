package pub.carzy.auto_script.entity;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * @author admin
 */
@Data
public class WrapperEntity<T> {
    private List<T> data = new ArrayList<>();
}
