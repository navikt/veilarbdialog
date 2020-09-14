package no.nav.sbl.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.ContextBase;
import no.nav.log.MarkerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.List;

public class LogUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);

    public static final String ROOT = "ROOT";

    public static void setGlobalLogLevel(Level newLevel) {
        LOGGER.info("global log level: {}", newLevel);
        LoggerContext loggerContext = getLoggerContext();
        loggerContext.getLoggerList().forEach(l -> l.setLevel(newLevel));
    }

    public static void setupJULBridge() {
        if (!SLF4JBridgeHandler.isInstalled()) {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
        }
    }

    public static void shutDownLogback() {
        LOGGER.info("shutDownLogback");
        LoggerContext loggerContext = getLoggerContext();
        loggerContext.stop();
    }

    private static LoggerContext getLoggerContext() {
        ContextBase contextBase = (ContextBase) LoggerFactory.getILoggerFactory();
        return (LoggerContext) contextBase;
    }

    public static List<ch.qos.logback.classic.Logger> getAllLoggers() {
        LoggerContext loggerContext = getLoggerContext();
        return loggerContext.getLoggerList();
    }

    public static Level getRootLevel() {
        LoggerContext loggerContext = getLoggerContext();
        return loggerContext.getLogger(ROOT).getLevel();
    }

    public static MarkerBuilder buildMarker() {
        return new MarkerBuilder();
    }

}
