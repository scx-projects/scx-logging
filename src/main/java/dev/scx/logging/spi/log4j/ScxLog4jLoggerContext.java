package dev.scx.logging.spi.log4j;

import dev.scx.logging.ScxLogging;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;

/// ScxLog4jLoggerContext
///
/// @author scx567888
public final class ScxLog4jLoggerContext implements LoggerContext {

    @Override
    public Object getExternalContext() {
        return null;
    }

    @Override
    public ExtendedLogger getLogger(String name) {
        return getLogger(name, null);
    }

    @Override
    public ExtendedLogger getLogger(String name, MessageFactory messageFactory) {
        return new ScxLog4jLogger(ScxLogging.getLogger(name));
    }

    @Override
    public boolean hasLogger(String name) {
        return false;
    }

    @Override
    public boolean hasLogger(String name, Class<? extends MessageFactory> messageFactoryClass) {
        return false;
    }

    @Override
    public boolean hasLogger(String name, MessageFactory messageFactory) {
        return false;
    }

}
