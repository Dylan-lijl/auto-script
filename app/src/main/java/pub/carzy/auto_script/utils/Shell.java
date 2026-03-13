package pub.carzy.auto_script.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Optional;

import pub.carzy.auto_script.ex.DeviceNotRootedException;
import pub.carzy.auto_script.ex.ProcessReadOrWriteIOException;
import pub.carzy.auto_script.ex.UnauthorizedRootAccessException;

/**
 * @author admin
 */
public class Shell {
    public static Process getRootProcess() {
        Process process;
        try {
            process = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            throw new DeviceNotRootedException(e);
        }
        try {
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader is = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            os.writeBytes("id\n");
            os.flush();
            String line = is.readLine();
            if (line == null || !line.contains("uid=0")) {
                process.destroy();
                throw new UnauthorizedRootAccessException();
            }
        } catch (IOException e) {
            throw new ProcessReadOrWriteIOException(e);
        }
        return process;
    }

    public static String getEventList(Process process) {
        if (process == null) {
            return null;
        }
        return getEventList(process.getInputStream(), process.getOutputStream());
    }

    public static String getEventList(InputStream input, OutputStream output) {
        if (input == null || output == null) {
            return null;
        }

        try {
            output.write(("getevent -lp\n").getBytes());
            output.write(("echo __END__\n").getBytes());
            output.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if ("__END__".equals(line)) {
                    break;
                }
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (IOException e) {
            throw new ProcessReadOrWriteIOException(e);
        }
    }

    public static void grantOverlayPermissionSilently(Process cmdProcess, String packageName) {
        OutputStream stream = cmdProcess.getOutputStream();
        try {
            stream.write(("appops set " + packageName + " SYSTEM_ALERT_WINDOW allow\n").getBytes());
            stream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
