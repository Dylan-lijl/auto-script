package pub.carzy.auto_script.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author admin
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FAQEntity extends BasicFileImport{
    private String question;
    private String answer;
}
