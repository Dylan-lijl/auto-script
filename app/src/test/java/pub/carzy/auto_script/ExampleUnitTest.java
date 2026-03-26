package pub.carzy.auto_script;

import org.junit.Test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.function.Consumer;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void writeToFile() {
        File src = new File("D:\\data\\project\\other\\github\\auto-script\\app\\src\\main\\java\\pub\\carzy\\auto_script\\service");
        File dest = new File("C:\\Users\\admin\\Desktop\\out.txt");
        if (dest.exists()) {
            dest.delete();
        }
        try {
            dest.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (PrintWriter writer = new PrintWriter(dest)) {
            Consumer<File> fileConsumer = f -> {
                File[] files = f.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.isFile()) {
                            continue;
                        }
                        try (BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(file)))){
                            writer.println("-----start:"+file.getAbsoluteFile()+"-----");
                            String line;
                            while ((line = reader.readLine()) != null) {
                                writer.println(line);
                            }
                            writer.println("-----end-----");
                            writer.flush();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            };
            fileConsumer.accept(src);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}