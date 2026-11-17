package ar.edu.um.prog2.fzapata.catalogo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;

// Captura lo que escribe un logger (o todos, con el root) para comprobar el mensaje, el nivel, los campos y la traza.
// Lo propio queda en DEBUG mientras captura, así se ven también las líneas que en producción no salen; lo ajeno
// (Spring, Hibernate...) sigue con su nivel de siempre
public final class LogCapture implements AutoCloseable {

    private static final String OWN_PACKAGE = "ar.edu.um.prog2.fzapata.catalogo";

    private final Logger logger;
    private final Logger loweredLogger;
    private final Level previousLevel;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    private LogCapture(Logger logger, Logger loweredLogger) {
        this.logger = logger;
        this.loweredLogger = loweredLogger;
        this.previousLevel = loweredLogger.getLevel();
        loweredLogger.setLevel(Level.DEBUG);
        // Las vueltas del planificador y el listener de Kafka escriben desde otros hilos mientras el test lee
        appender.list = new CopyOnWriteArrayList<>();
        appender.start();
        logger.addAppender(appender);
    }

    public static LogCapture of(Class<?> type) {
        Logger logger = (Logger) LoggerFactory.getLogger(type);
        return new LogCapture(logger, logger);
    }

    public static LogCapture ofEverything() {
        return new LogCapture((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME),
                (Logger) LoggerFactory.getLogger(OWN_PACKAGE));
    }

    // Los campos de una línea de log (los que salen como campos del JSON)
    public static Map<String, Object> fieldsOf(ILoggingEvent event) {
        Map<String, Object> fields = new LinkedHashMap<>();
        List<KeyValuePair> pairs = event.getKeyValuePairs();
        if (pairs != null) {
            pairs.forEach(pair -> fields.put(pair.key, pair.value));
        }
        return fields;
    }

    // Todo lo que una línea escribe: mensaje, campos, MDC y traza completa con sus causas
    public static String everythingIn(ILoggingEvent event) {
        String trace = event.getThrowableProxy() == null ? ""
                : "\n" + ThrowableProxyUtil.asString(event.getThrowableProxy());
        return event.getLevel() + " " + event.getLoggerName() + " " + event.getFormattedMessage() + " "
                + fieldsOf(event) + " " + event.getMDCPropertyMap() + trace;
    }

    public List<ILoggingEvent> events() {
        return List.copyOf(appender.list);
    }

    public String everything() {
        return String.join("\n", events().stream().map(LogCapture::everythingIn).toList());
    }

    public void clear() {
        appender.list.clear();
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        appender.stop();
        loweredLogger.setLevel(previousLevel);
    }
}
