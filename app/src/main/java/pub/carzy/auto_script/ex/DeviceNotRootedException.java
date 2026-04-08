package pub.carzy.auto_script.ex;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * 设备未root
 * @author admin
 */
public class DeviceNotRootedException extends RuntimeException{
    public DeviceNotRootedException() {
        super();
    }

    public DeviceNotRootedException(String message) {
        super(message);
    }

    public DeviceNotRootedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeviceNotRootedException(Throwable cause) {
        super(cause);
    }

    protected DeviceNotRootedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Nullable
    @Override
    public String getMessage() {
        return super.getMessage();
    }

    @Nullable
    @Override
    public String getLocalizedMessage() {
        return super.getLocalizedMessage();
    }

    @Nullable
    @Override
    public synchronized Throwable getCause() {
        return super.getCause();
    }

    @NonNull
    @Override
    public synchronized Throwable initCause(@Nullable Throwable cause) {
        return super.initCause(cause);
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void printStackTrace() {
        super.printStackTrace();
    }

    @Override
    public void printStackTrace(@NonNull PrintStream s) {
        super.printStackTrace(s);
    }

    @Override
    public void printStackTrace(@NonNull PrintWriter s) {
        super.printStackTrace(s);
    }

    @NonNull
    @Override
    public synchronized Throwable fillInStackTrace() {
        return super.fillInStackTrace();
    }

    @NonNull
    @Override
    public StackTraceElement[] getStackTrace() {
        return super.getStackTrace();
    }

    @Override
    public void setStackTrace(@NonNull StackTraceElement[] stackTrace) {
        super.setStackTrace(stackTrace);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj);
    }

    @NonNull
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
