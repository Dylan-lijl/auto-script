package pub.carzy.auto_script.config;

import java.io.Serializable;

public interface Setting extends Serializable {
    void setAccepted(boolean accepted);

    void setTick(Integer time);
    Integer getTick();

    boolean isAccepted();
    void reset();
}
