package pub.carzy.auto_script.entity;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;


/**
 * @author admin
 */
public class SupportLocaleResult {
    private String currentLocale;
    private final Map<String, Locale> locales = new LinkedHashMap<>();

    public String getCurrentLocale() {
        return currentLocale;
    }

    public void setCurrentLocale(String currentLocale) {
        this.currentLocale = currentLocale;
    }

    public Map<String, Locale> getLocales() {
        return locales;
    }
}
