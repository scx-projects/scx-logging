package dev.scx.logging.spi.jdk;

import dev.scx.logging.ScxLogger;

import java.lang.System.Logger;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/// ScxJDKLogger
///
/// @author scx567888
public final class ScxJDKLogger implements Logger {

    private final ScxLogger scxLogger;

    public ScxJDKLogger(ScxLogger scxLogger) {
        this.scxLogger = scxLogger;
    }

    private static String formatMessage(String format, Object... parameters) {
        if (format == null) {
            return null;
        }
        if (parameters == null || parameters.length == 0) {
            return format;
        }
        try {
            return MessageFormat.format(format, parameters);
        } catch (Exception ex) {
            return format;
        }
    }

    private static String getResourceString(ResourceBundle bundle, String key) {
        if (bundle == null || key == null) {
            return key;
        }
        try {
            return bundle.getString(key);
        } catch (MissingResourceException x) {
            return key;
        }
    }

    private static Throwable getThrowableCandidate(Object[] argArray) {
        if (argArray != null && argArray.length != 0) {
            Object lastEntry = argArray[argArray.length - 1];
            return lastEntry instanceof Throwable e ? e : null;
        } else {
            return null;
        }
    }

    @Override
    public String getName() {
        return scxLogger.name();
    }

    @Override
    public boolean isLoggable(Level level) {
        return scxLogger.isLoggable(level);
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable t) {
        scxLogger.log(level, getResourceString(bundle, msg), t);
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) {
        var t = getThrowableCandidate(params);
        if (t != null) {
            params = Arrays.copyOf(params, params.length - 1);
        }
        scxLogger.log(level, formatMessage(getResourceString(bundle, format), params), t);
    }

}
