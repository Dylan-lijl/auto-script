package pub.carzy.auto_script.config;

import java.io.Serializable;

import pub.carzy.auto_script.entity.FloatPoint;

/**
 * @author admin
 */
public interface Setting extends Serializable {
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
}
