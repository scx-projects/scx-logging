package dev.scx.logging;

import java.util.ArrayList;

/// ScxLoggerHelper
///
/// @author scx567888
final class ScxLoggerHelper {

    public static boolean isLoggerClass(String className) {
        return className.startsWith("dev.scx.logging") ||
            className.startsWith("org.slf4j.helpers") ||
            className.startsWith("org.apache.logging.log4j") ||
            className.startsWith("java.lang.System$Logger");
    }

    public static StackTraceElement[] getFilteredStackTrace(Throwable e) {
        var stackTrace = e.getStackTrace();
        var list = new ArrayList<StackTraceElement>(stackTrace.length);
        for (var traceElement : stackTrace) {
            if (!isLoggerClass(traceElement.getClassName())) {
                list.add(traceElement);
            }
        }
        return list.toArray(StackTraceElement[]::new);
    }

}
