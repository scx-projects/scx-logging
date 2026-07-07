package dev.scx.logging;

import java.time.LocalDateTime;

import static dev.scx.logging.ScxLoggerHelper.getFilteredStackTrace;
import static java.lang.System.Logger.Level;

/// ScxLogger
///
/// @author scx567888
public final class ScxLogger {

    private final String name;

    private volatile CachedConfig cachedConfig;

    ScxLogger(String name) {
        this.name = name;
        // 这里用 -1 初始化. 因为 ScxLogging 的版本默认是 0.
        this.cachedConfig = new CachedConfig(-1, null);
    }

    /// level 允许是 null
    public boolean isLoggable(Level level) {
        if (level == null) {
            return false;
        }

        var config = config();

        return level.getSeverity() >= config.level().getSeverity();
    }

    /// message, t 都允许为 null
    public void log(Level level, String message, Throwable t) {
        if (level == null) {
            return;
        }

        var config = config();

        // log 自己必须检查 level, 不能只依赖外部调用 isLoggable
        if (level.getSeverity() < config.level().getSeverity()) {
            return;
        }

        var now = LocalDateTime.now();

        var stackTrace = config.stackTrace() ? getFilteredStackTrace(new Throwable()) : null;

        var logRecord = new ScxLogRecord(now, level, name, message, Thread.currentThread().getName(), t, stackTrace);

        for (var r : config.recorders()) {
            try {
                r.record(logRecord);
            } catch (Exception _) {
                // 防止 recorder 异常传播到 用户代码.
            }
        }
    }

    public String name() {
        return name;
    }

    /// 轻量级配置缓存。
    /// 并发语义：
    /// 1. 不保证正在执行中的本次日志一定使用最新配置。
    /// 2. 只保证 ScxLogging 的配置版本变化后，后续调用会重新解析配置。
    /// 3. 依赖 ScxLogging 按「先发布新配置，再更新版本号」的顺序更新。
    /// 4. 在上述前提下，如果读到旧 version + 新 config，最多导致下一次多 resolve 一次，不影响正确性。
    public ScxLoggerConfig config() {
        var version = ScxLogging.configVersion();

        var cache = cachedConfig;

        if (cache.version() == version) {
            return cache.config();
        }

        var config = ScxLogging.resolveConfig(name);

        cachedConfig = new CachedConfig(version, config);

        return config;
    }

    private record CachedConfig(long version, ScxLoggerConfig config) {

    }

}
