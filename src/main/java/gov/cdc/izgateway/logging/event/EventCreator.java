package gov.cdc.izgateway.logging.event;

import java.util.Date;



import jakarta.servlet.http.HttpSession;

import javax.net.ssl.SSLSession;


public interface EventCreator {
    public interface Event {
        String getId();
        Date getDate();
    }
    /**
     * Create an event that can be matched to the session Id once
     * @param session   The session to match
     * @return  The event Id.
     */
    String createEvent(SSLSession session);

    /**
     * Get the event associated with the specified session
     * @param sessionId The session
     * @return  The event
     */
    Event getEvent(HttpSession sessionId);

    /**
     * Utility function to convert SSLSession id values to a String
     * @param sessionId The session id
     * @return  A hexidecimal string representing the session id.
     */
    static String toHex(byte[] sessionId) {
        StringBuilder sessId = new StringBuilder();
        for (byte c : sessionId) {
            sessId.append(String.format("%02x", c & 0xff));
        }
        return sessId.toString();
    }
}
