package dev.scx.logging.spi.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

import static org.slf4j.helpers.NOP_FallbackServiceProvider.REQUESTED_API_VERSION;

/// ScxSLF4JServiceProvider
///
/// @author scx567888
public final class ScxSLF4JServiceProvider implements SLF4JServiceProvider {

    private static final ILoggerFactory LOGGER_FACTORY = new ScxSLF4JLoggerFactory();
    private static final IMarkerFactory MARKER_FACTORY = new BasicMarkerFactory();
    private static final MDCAdapter MDC_ADAPTER = new BasicMDCAdapter();

    @Override
    public ILoggerFactory getLoggerFactory() {
        return LOGGER_FACTORY;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return MARKER_FACTORY;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return MDC_ADAPTER;
    }

    @Override
    public String getRequestedApiVersion() {
        return REQUESTED_API_VERSION;
    }

    @Override
    public void initialize() {

    }

}
