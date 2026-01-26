package pub.carzy.auto_script.entity;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author admin
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AcknowledgementEntity extends BasicFileImport{
    public static final int PEOPLE = 1;
    public static final int ORGANIZATION = 2;
    public static final int LIBRARY = 3;
    public static final int LINK = 4;
    private String title;
    private Integer type;
    private String content;
    private List<String> href;
    private Boolean deleteLine;
}
