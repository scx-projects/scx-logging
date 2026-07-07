package dev.scx.logging;

import java.util.List;

import static java.lang.System.Logger.Level;

/// ScxLoggerConfig
///
/// @param level      允许为 null
/// @param stackTrace 允许为 null
/// @param recorders  允许为 null
/// @author scx567888
public record ScxLoggerConfig(
    Level level,
    Boolean stackTrace,
    List<ScxLogRecorder> recorders
) {

    public ScxLoggerConfig {
        if (recorders != null) {
            recorders = List.copyOf(recorders);
        }
    }

}
