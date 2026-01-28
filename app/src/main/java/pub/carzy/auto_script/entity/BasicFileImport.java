package pub.carzy.auto_script.entity;

import lombok.Data;

/**
 * @author admin
 */
@Data
public abstract class BasicFileImport {
    private Integer order = Integer.MAX_VALUE;
    private Long id;
}
