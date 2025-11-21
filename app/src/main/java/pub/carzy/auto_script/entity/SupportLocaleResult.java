package pub.carzy.auto_script.entity;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import lombok.Data;

/**
 * @author admin
 */
@Data
public class SupportLocaleResult {
    private String currentLocale;
    private final Map<String, Locale> locales = new LinkedHashMap<>();
}
