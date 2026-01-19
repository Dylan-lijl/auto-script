package pub.carzy.auto_script.entity;

import lombok.Data;

/**
 * @author admin
 */
@Data
public class AcknowledgementEntity {
    public static final int PEOPLE = 1;
    public static final int ORGANIZATION = 2;
    public static final int LIBRARY = 3;
    public static final int LINK = 4;
    private String title;
    private Integer type;
    private String content;
    private Integer order;
}
