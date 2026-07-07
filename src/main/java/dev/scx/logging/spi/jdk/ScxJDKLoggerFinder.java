package dev.scx.logging.spi.jdk;

import dev.scx.logging.ScxLogging;

import java.lang.System.Logger;
import java.lang.System.LoggerFinder;

/// ScxJDKLoggerFinder
///
/// @author scx567888
public final class ScxJDKLoggerFinder extends LoggerFinder {

    @Override
    public Logger getLogger(String name, Module module) {
        return new ScxJDKLogger(ScxLogging.getLogger(name));
    }

}


