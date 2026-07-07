package dev.scx.logging.spi.slf4j;

import dev.scx.logging.ScxLogging;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/// ScxSLF4JLoggerFactory
///
/// @author scx567888
public final class ScxSLF4JLoggerFactory implements ILoggerFactory {

    @Override
    public Logger getLogger(String name) {
        return new ScxSLF4JLogger(ScxLogging.getLogger(name));
    }

}
