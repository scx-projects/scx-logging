package dev.scx.logging.test;

import dev.scx.logging.ScxLoggerConfig;
import dev.scx.logging.ScxLogging;
import org.testng.Assert;
import org.testng.annotations.Test;

import static java.lang.System.Logger.Level.TRACE;

//并发测试
public class ScxLoggerConcurrentTest {

    public static void main(String[] args) throws InterruptedException {
        test1();
    }

    @Test
    public static void test1() throws InterruptedException {
        var scxLoggerConfig = new ScxLoggerConfig(TRACE, null, null);

        // 先启动配置更新线程
        Thread thread1 = Thread.ofPlatform().start(() -> {
            for (int i = 0; i < 1000; i = i + 1) {
                ScxLogging.config(i + "", scxLoggerConfig);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("全部设置完成");
        });

        Thread thread2 = Thread.ofPlatform().start(() -> {
            // 线程创建 Logger
            for (int i = 0; i < 1000; i = i + 1) {
                var logger = ScxLogging.getLogger(i + "");
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("全部获取完成");
        });

        // 理论上 所有日志都应该是 TRACE 级别

        Thread.sleep(4000);// 等待运行完成

        for (int i = 0; i < 1000; i = i + 1) {
            var logger = ScxLogging.getLogger(i + "");
            //这里应该全部都是  TRACE
            var level = logger.config().level();
            if (level != TRACE) {
                Assert.fail();
            }
        }

    }

}
