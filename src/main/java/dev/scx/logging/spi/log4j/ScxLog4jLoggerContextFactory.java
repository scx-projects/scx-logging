package dev.scx.logging.spi.log4j;

import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerContextFactory;

import java.net.URI;

/// ScxLog4jLoggerContextFactory
///
/// @author scx567888
public final class ScxLog4jLoggerContextFactory implements LoggerContextFactory {

    private static final LoggerContext LOGGER_CONTEXT = new ScxLog4jLoggerContext();

    @Override
    public LoggerContext getContext(String fqcn, ClassLoader loader, Object externalContext, boolean currentContext) {
        return LOGGER_CONTEXT;
    }

    @Override
    public LoggerContext getContext(String fqcn, ClassLoader loader, Object externalContext, boolean currentContext, URI configLocation, String name) {
        return LOGGER_CONTEXT;
    }

    @Override
    public void removeContext(LoggerContext context) {

    }

}
