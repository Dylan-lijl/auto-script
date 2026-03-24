package pub.carzy.auto_script.service.sub;

import pub.carzy.auto_script.service.data.ReplayModel;

/**
 * @author admin
 */
public interface Replay {
    void setModel(ReplayModel model);
    void clearCallback();

    void addCallback(Replay.ResultListener listener);

    void removeCallback(ResultListener listener);

    void setRepeatCount(int repeatCount);

    int getRepeatCount();

    void start();

    int getStatus();

    void stop();

    void pause();

    void resume();

    void clear();

    interface ResultListener {
        int SUCCESS = 0;
        int FAIL = -1;
        int EXCEPTION = -2;

        default void stop(int code, String message, Exception e) {

        }

        default void pause(int code, String message, Exception e) {

        }

        default void resume(int code, String message, Exception e) {

        }

        default void start(int code, String message, Exception e) {

        }

        default void before(int status, int count) {

        }

        default void after(int status, int count) {

        }
    }
    interface Payload{
        boolean isEmpty();
    }
}
