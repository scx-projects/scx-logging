package dev.scx.logging.recorder;

import dev.scx.logging.ScxLogRecord;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import static java.lang.System.Logger.Level;

/// ScxLogRecordHelper
///
/// @author scx567888
public final class ScxLogRecordHelper {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static String formatTimeStamp(TemporalAccessor temporal) {
        return DATE_TIME_FORMATTER.format(temporal);
    }

    public static void appendThrowable(StringBuilder sb, Throwable throwable) {
        var sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        sb.append(sw.getBuffer());
    }

    public static void appendStackTrace(StringBuilder sb, StackTraceElement[] stackTraces) {
        for (var traceElement : stackTraces) {
            sb.append("\t").append(traceElement).append(System.lineSeparator());
        }
    }

    public static String formatLevel(Level level) {
        // 注意长度对齐
        return switch (level) {
            case ALL -> "ALL  ";
            case TRACE -> "TRACE";
            case DEBUG -> "DEBUG";
            case INFO -> "INFO ";
            case WARNING -> "WARN ";
            case ERROR -> "ERROR";
            case OFF -> "OFF  ";
        };
    }

    public static String formatLogRecord(ScxLogRecord logRecord) {
        // 创建初始的 message 格式如下
        // 时间戳                    线程名称  日志级别 日志名称                       具体内容
        // 2020-01-01 11:19:55.356 [main-1] ERROR dev.scx.xxx.TestController - 日志消息 !!!
        var sb = new StringBuilder()
            .append(formatTimeStamp(logRecord.timeStamp()))
            .append(" [").append(logRecord.threadName()).append("] ")
            .append(formatLevel(logRecord.level()))
            .append(" ").append(logRecord.loggerName()).append(" - ")
            .append(logRecord.message()).append(System.lineSeparator());

        // throwable 和 stackTrace 没必要同时出现, throwable 优先级更高
        if (logRecord.throwable() != null) {
            appendThrowable(sb, logRecord.throwable());
        } else if (logRecord.stackTrace() != null) {
            appendStackTrace(sb, logRecord.stackTrace());
        }
        return sb.toString();
    }

}
