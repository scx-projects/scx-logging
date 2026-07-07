package dev.scx.logging.test;

import dev.scx.logging.ScxLogRecord;
import dev.scx.logging.ScxLoggerConfig;
import dev.scx.logging.ScxLogging;
import dev.scx.logging.recorder.ConsoleRecorder;
import dev.scx.logging.recorder.FileRecorder;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

public class ScxLoggerTest {

    public static void main(String[] args) {
        test1();
    }

    @Test
    public static void test1() {
        var logger = LoggerFactory.getLogger("test1");
        for (int i = 0; i < 10; i = i + 1) {
            logger.debug("测试 debug {}", i);
            logger.error("测试 error {}", i);
            logger.error("测试 {}", i, new RuntimeException("错误"));
        }
        var path = getAppRoot();
        ScxLogging.config("test1", new ScxLoggerConfig(
            DEBUG,
            true,
            List.of(
                new ConsoleRecorder() {
                    @Override
                    public String format(ScxLogRecord c) {
                        return c.loggerName() + " : " + c.message() + System.lineSeparator();
                    }
                },
                new FileRecorder(path)
            )));

        for (int i = 0; i < 10; i = i + 1) {
            logger.debug("测试 debug {}", i);
        }
        ScxLogging.config(Pattern.compile("test.*"), new ScxLoggerConfig(ERROR, null, null));
        logger.debug("不应该显示出来");
        logger.error("应该显示出来");
    }

    public static Path getAppRoot() {
        try {
            return Path.of(ScxLoggerTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            return null;
        }
    }

}
