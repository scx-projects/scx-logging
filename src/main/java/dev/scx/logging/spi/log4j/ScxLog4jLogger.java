package dev.scx.logging.spi.log4j;

import dev.scx.logging.ScxLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.AbstractLogger;

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.*;
import static org.apache.logging.log4j.spi.StandardLevel.getStandardLevel;

/// ScxLog4jLogger
///
/// @author scx567888
public final class ScxLog4jLogger extends AbstractLogger {

    private final ScxLogger scxLogger;

    public ScxLog4jLogger(ScxLogger scxLogger) {
        super(scxLogger.name());
        this.scxLogger = scxLogger;
    }

    private static Logger.Level toJDKLevel(Level level) {
        if (level == null) {
            return null;
        }
        var standardLevel = getStandardLevel(level.intLevel());
        return switch (standardLevel) {
            case OFF -> OFF;
            case FATAL, ERROR -> ERROR;
            case WARN -> WARNING;
            case INFO -> INFO;
            case DEBUG -> DEBUG;
            case TRACE -> TRACE;
            case ALL -> ALL;
        };
    }

    private static Level toLog4jLevel(Logger.Level level) {
        return switch (level) {
            case OFF -> Level.OFF;
            case ERROR -> Level.ERROR;
            case WARNING -> Level.WARN;
            case INFO -> Level.INFO;
            case DEBUG -> Level.DEBUG;
            case TRACE -> Level.TRACE;
            case ALL -> Level.ALL;
        };
    }

    private boolean isLoggable(Level level) {
        return scxLogger.isLoggable(toJDKLevel(level));
    }

    @Override
    public Level getLevel() {
        return toLog4jLevel(scxLogger.config().level());
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, Message message, Throwable t) {
        return isLoggable(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, CharSequence message, Throwable t) {
        return isLoggable(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, Object message, Throwable t) {
        return isLoggable(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Throwable t) {
        return isLoggable(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message) {
        return isLoggable(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object... params) {
        return isLoggable(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object p0) {
        return isLoggable(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1) {
        return isLoggable(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
        return isLoggable(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
        return isLoggable(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
        return isLoggable(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        return isLoggable(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        return isLoggable(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        return isLoggable(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        return isLoggable(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
        return isLoggable(level);
    }

    @Override
    public void logMessage(String fqcn, Level level, Marker marker, Message message, Throwable t) {
        scxLogger.log(toJDKLevel(level), message != null ? message.getFormattedMessage() : null, t);
    }

}
