package pub.carzy.auto_script.core.sub;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

import pub.carzy.auto_script.core.data.ReplayModel;

/**
 * @author admin
 */
public interface Replay {
    void setModel(ReplayModel model);

    void clearCallback();

    void addCallback(Replay.ResultListener listener);

    void setRepeatCount(int repeatCount);
    void setTick(Integer tick);

    void start();

    int getStatus();

    void stop();

    void pause();

    void resume();

    void clear();

    void close();

    int getTick();

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

    interface Payload {
        boolean isEmpty();

        int size();

        Collection<Number[]> getData();

        void binding(DataOutputStream writer);

        void write(byte[] bytes) throws IOException;

        void flush() throws IOException;
    }

    abstract class AbstractPayload implements Payload {
        protected DataOutputStream writer;

        @Override
        public void binding(DataOutputStream writer) {
            this.writer = writer;
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            if (writer != null) {
                writer.write(bytes);
            }
        }

        @Override
        public void flush() throws IOException {
            if (writer != null) {
                writer.flush();
            }
        }

        @Override
        public Collection<Number[]> getData() {
            return null;
        }
    }
    class NullPayload extends AbstractPayload{
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public int size() {
            return 0;
        }
    }
    class BreakPayload extends AbstractPayload{
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public int size() {
            return 0;
        }
    }
}
