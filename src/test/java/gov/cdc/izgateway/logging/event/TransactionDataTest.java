package gov.cdc.izgateway.logging.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import gov.cdc.izgateway.configuration.AppProperties;
import gov.cdc.izgateway.logging.LoggingValveBase;

/**
 * Verifies the WSDL/schema (HTTP GET) special-casing added to {@link TransactionData}.
 * A GET on a SOAP endpoint is a WSDL/schema retrieval, not a transaction, so it must
 * produce a distinct message and be logged at DEBUG rather than INFO.
 */
class TransactionDataTest {

    @BeforeAll
    static void initAppProperties() {
        // TransactionData's constructor reads AppProperties.isProduction(); the constructor
        // registers itself as the static instance, which is all this test needs.
        new AppProperties();
    }

    @AfterEach
    void clearMdc() {
        MDC.remove(LoggingValveBase.METHOD);
    }

    private TransactionData wsdlSource() {
        TransactionData t = new TransactionData();
        t.getSource().setCommonName("DocketClosed");
        t.getSource().setIpAddress("10.1.2.3");
        return t;
    }

    @Test
    void getMessageForGetProducesWsdlRetrievalMessage() {
        MDC.put(LoggingValveBase.METHOD, "GET");
        TransactionData t = wsdlSource();
        assertEquals("WSDL/schema retrieval from DocketClosed at 10.1.2.3", t.getMessage());
    }

    @Test
    void getMessageForPostProducesTransactionMessage() {
        MDC.put(LoggingValveBase.METHOD, "POST");
        TransactionData t = wsdlSource();
        String message = t.getMessage();
        assertTrue(message.startsWith("Transaction "),
                "POST must keep the standard transaction message, was: " + message);
    }

    @Test
    void getMessageWithNoMethodProducesTransactionMessage() {
        // No MDC method set (e.g. non-HTTP context) must behave like the pre-existing default.
        TransactionData t = wsdlSource();
        assertTrue(t.getMessage().startsWith("Transaction "));
    }

    @Test
    void logItForGetLogsAtDebug() {
        assertLogLevel("GET", Level.DEBUG);
    }

    @Test
    void logItForPostLogsAtInfo() {
        assertLogLevel("POST", Level.INFO);
    }

    private void assertLogLevel(String method, Level expected) {
        Logger logger = (Logger) LoggerFactory.getLogger(TransactionData.class);
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.DEBUG); // ensure DEBUG events are not filtered out before capture
        logger.addAppender(appender);
        try {
            MDC.put(LoggingValveBase.METHOD, method);
            wsdlSource().logIt();
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(originalLevel);
        }
        List<ILoggingEvent> events = appender.list;
        assertEquals(1, events.size(), "expected exactly one log event");
        assertEquals(expected, events.get(0).getLevel());
    }
}
