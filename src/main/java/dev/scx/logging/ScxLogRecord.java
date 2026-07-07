package dev.scx.logging;

import java.lang.System.Logger.Level;
import java.time.LocalDateTime;

/// 日志记录
///
/// @author scx567888
public record ScxLogRecord(
    LocalDateTime timeStamp,
    Level level,
    String loggerName,
    String message,
    String threadName,
    Throwable throwable,
    StackTraceElement[] stackTrace
) {

}
