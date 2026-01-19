package pub.carzy.auto_script.entity;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * @author admin
 */
@Data
public class AcknowledgementImport {
    private List<AcknowledgementEntity> data = new ArrayList<>();
}
