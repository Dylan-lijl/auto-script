package pub.carzy.auto_script.config;

import java.io.Serializable;
import java.util.List;

import pub.carzy.auto_script.entity.FloatPoint;
import pub.carzy.auto_script.entity.Style;

/**
 * @author admin
 */
public interface Setting extends Serializable {
    List<String> keys();
    void setAccepted(boolean accepted);

    void setTick(Integer time);

    Integer getTick();

    boolean isAccepted();

    void reset();

    void setLanguage(String language);

    String getLanguage();

    void setUUID(String uuid);

    String getUUID();

    void setPoint(FloatPoint point);

    FloatPoint getPoint();

    void setShowRecordWindow(Boolean b);

    Boolean getShowRecordWindow();

    void setAutoRunRecord(Integer delay);

    Integer getAutoRunRecord();

    List<Style> getAllStyle();

    void updateStyle(Style style);

    Style getStyle(long id) ;

    void removeStyle(long id);
    void setAutoPlay(Boolean v);
    Boolean getAutoPlay();
}
