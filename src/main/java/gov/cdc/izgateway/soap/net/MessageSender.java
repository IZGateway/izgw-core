package gov.cdc.izgateway.soap.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

import gov.cdc.izgateway.configuration.AppProperties;
import gov.cdc.izgateway.configuration.ClientConfiguration;
import gov.cdc.izgateway.configuration.SenderConfig;
import gov.cdc.izgateway.configuration.ServerConfiguration;
import gov.cdc.izgateway.logging.RequestContext;
import gov.cdc.izgateway.logging.event.TransactionData;
import gov.cdc.izgateway.logging.info.DestinationInfo;
import gov.cdc.izgateway.logging.info.EndPointInfo;
import gov.cdc.izgateway.logging.info.MessageInfo;
import gov.cdc.izgateway.logging.info.MessageInfo.Direction;
import gov.cdc.izgateway.logging.info.MessageInfo.EndpointType;
import gov.cdc.izgateway.model.IDestination;
import gov.cdc.izgateway.model.IEndpointStatus;
import gov.cdc.izgateway.model.RetryStrategy;
import gov.cdc.izgateway.security.ClientTlsSupport;
import gov.cdc.izgateway.security.Roles;
import gov.cdc.izgateway.service.IStatusCheckerService;
import gov.cdc.izgateway.service.impl.EndpointStatusService;

import gov.cdc.izgateway.soap.fault.DestinationConnectionFault;
import gov.cdc.izgateway.soap.fault.Fault;
import gov.cdc.izgateway.soap.fault.HubClientFault;
import gov.cdc.izgateway.soap.fault.SecurityFault;
import gov.cdc.izgateway.soap.message.ConnectivityTestRequest;
import gov.cdc.izgateway.soap.message.ConnectivityTestResponse;
import gov.cdc.izgateway.soap.message.FaultMessage;
import gov.cdc.izgateway.soap.message.SoapMessage;
import gov.cdc.izgateway.soap.message.SubmitSingleMessageRequest;
import gov.cdc.izgateway.soap.message.SubmitSingleMessageResponse;
import gov.cdc.izgateway.utils.FixedByteArrayOutputStream;
import gov.cdc.izgateway.utils.PreservingOutputStream;
import gov.cdc.izgateway.utils.SystemUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.xml.stream.XMLStreamException;

/**
 * MessageSender is responsible for sending messages to a given destination,
 * and then returning the response back into a message object.  It uses
 * the SoapMessageConverter to handle marshalling and unmarshalling into and from
 * the wire formats.
 * 
 * @see SoapMessageConverter
 *  
 * @author Audacious Inquiry
 *
 */
@Component
public class MessageSender {
	private static final List<Integer> ACCEPTABLE_RESPONSE_CODES = Arrays.asList(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_BAD_REQUEST, HttpURLConnection.HTTP_INTERNAL_ERROR);
	private final ServerConfiguration serverConfig;
	private final SenderConfig senderConfig;
	private final ClientConfiguration clientConfig;
	private final ClientTlsSupport tlsSupport;
	private final SoapMessageConverter converter;
	private final EndpointStatusService statusService;
	private IStatusCheckerService statusChecker;
	private boolean preserveOutput = true;  // set to true to preserve output for debugging.
	private final boolean isProduction;
	
	/**
	 * Construct a message sender
	 * @param serverConfig	The server
	 * @param senderConfig	The sender configuration
	 * @param clientConfig	The client configuration parameters
	 * @param tlsSupport	Tls configuration
	 * @param statusService	The service that records the status of communications
	 * @param app	The web application configuration
	 */
	@Autowired 
	public MessageSender(
		final ServerConfiguration serverConfig,
		final SenderConfig senderConfig, 
		final ClientConfiguration clientConfig,
		final ClientTlsSupport tlsSupport,
		final EndpointStatusService statusService,
		final AppProperties app
	) {
		this.isProduction = app.isProd();
		this.serverConfig = serverConfig;
		this.senderConfig = senderConfig;
		this.tlsSupport = tlsSupport;
		this.clientConfig = clientConfig;
		this.statusService = statusService;
		this.converter = new SoapMessageConverter(SoapMessageConverter.OUTBOUND);
		// Create a base status Checker
		this.statusChecker = new IStatusCheckerService() {
			@Override
			public void lookForReset(IDestination dest) {
				// Do nothing in testing version
			}

			@Override
			public void updateStatus(IEndpointStatus s,
					boolean wasCircuitBreakerThrown, Throwable reason) {
				// Do nothing in testing version
			}
			@Override
			public boolean isExempt(String destId) {
				return false;
			}
		};
	}
	/**
	 * Set the service used to check status.  This is late bound to avoid
	 * circular class dependencies.
	 * 
	 * @param statusChecker	The status checker service
	 */
	public void setStatusChecker(IStatusCheckerService statusChecker) {
		this.statusChecker = statusChecker;
	}
	/**
	 * Get the status checker service
	 * @return the status checker service
	 */
	public IStatusCheckerService getStatusChecker() {
		return statusChecker;
	}
	
	/**
	 * Send a submitSingleMessage request
	 * @param dest	The destination to send it to
	 * @param submitSingleMessage	The message to send
	 * @return	The response
	 * @throws Fault	If an error occurred while sending or unmarshalling the response
	 */
	public SubmitSingleMessageResponse sendSubmitSingleMessage(
		IDestination dest,
		SubmitSingleMessageRequest submitSingleMessage
	) throws Fault {

		IEndpointStatus status = checkDestinationStatus(dest);
		SubmitSingleMessageRequest toBeSent = 
			new SubmitSingleMessageRequest(submitSingleMessage, getSchemaToUse(dest), true);
		// Clear the hub header, we don't forward that.
		if (!dest.isHub()) {
			toBeSent.getHubHeader().clear();
		}
		copyCredentials(toBeSent, dest);
		int retryCount = 0;
		while (true) {
			try {
				SubmitSingleMessageResponse responseFromClient = sendMessage(SubmitSingleMessageResponse.class, dest, toBeSent);
				SubmitSingleMessageResponse toBeReturned = new SubmitSingleMessageResponse(responseFromClient, submitSingleMessage.getSchema(), true);
				toBeReturned.updateAction(true);  // Now a Hub Response
				RequestContext.getTransactionData().setRetries(retryCount);
				updateStatus(status, dest, true);
				return toBeReturned;
			} catch (Fault f) {
				retryCount++;
				checkRetries(dest, status, retryCount, f);
				// Log the fault and try again.
			} 
		}
	}

	private void checkRetries(IDestination dest, IEndpointStatus status,
			int retryCount, Fault f) throws Fault {
		if (f.getRetry() != RetryStrategy.CHECK_IIS_STATUS || f.getCause() instanceof XMLStreamException) {
			// This is not a retry-able failure.
			RequestContext.getTransactionData().setRetries(retryCount);
			throw f;
		}
		
		// Update retry count and throw circuit break if too many exceeded
		if (retryCount > senderConfig.getMaxRetries()) {
			// Throw the circuit breaker for this endpoint
			RequestContext.getTransactionData().setProcessError(f);
			RequestContext.getTransactionData().setRetries(retryCount);
			updateStatus(status, dest, false);
			throw f;
		}
	}

	/**
	 * Copy credentials from destination to the message to be sent.
	 * @param toBeSent	The message to be sent
	 * @param dest	The credentials
	 */
	private void copyCredentials(SubmitSingleMessageRequest toBeSent, IDestination dest) {
		if (StringUtils.isNotEmpty(dest.getUsername())) {
			toBeSent.setUsername(dest.getUsername());
		}
		if (StringUtils.isNotEmpty(dest.getPassword())) {
			toBeSent.setPassword(dest.getPassword());
		}
	}

	/**
	 * Update status after successful or failed message send. This keeps status fresh and avoids
	 * unnecessary status checks.
	 * @param status	Current status
	 * @param dest		Destination (needed on failure states to look for a reset of the circuit breaker)
	 * @param success	true if the request worked, false if the circuit break should be thrown.
	 */
	private void updateStatus(IEndpointStatus status, IDestination dest, boolean success) {
		if (success) {
			status.connected();
		} else {
			status.setStatus(IEndpointStatus.CIRCUIT_BREAKER_THROWN);
			if (statusChecker != null) {
				statusChecker.lookForReset(dest);
			}
		}
		statusService.save(status);
	}
	
	/**
	 * Check the status of a destination on an inbound request
	 * @param dest	The destination to check.
	 * @return	The current status of the destination
	 * @throws DestinationConnectionFault	If the destination is under maintenance or has had its circuit breaker thrown
	 * 
	 * NOTE: Administrative users skip maintenance and circuit breaker thrown checks  
	 */
	private IEndpointStatus checkDestinationStatus(IDestination dest) throws DestinationConnectionFault {
		// Check for destination under maintenance
		if (dest.isUnderMaintenance() && userIsNotAdmin()) {
			throw DestinationConnectionFault.underMaintenance(dest);
		}
		
		// Check the circuit breaker
		IEndpointStatus status = statusService.getEndpointStatus(dest);
		// Skip endpoints exempt from status checking (b/c they cannot reset) 
		String destId = dest.getDestId();
		if (status.isCircuitBreakerThrown() && userIsNotAdmin() && !statusChecker.isExempt(destId)) {
			throw DestinationConnectionFault.circuitBreakerThrown(dest, status.getDetail());
		}
		return status;
	}

	private boolean userIsNotAdmin() {
		return !RequestContext.getRoles().contains(Roles.ADMIN) ||			// User is not ADMIN 
			RequestContext.getRoles().contains(Roles.NOT_ADMIN_HEADER);		// Admin user requested to be treated as non-admin for testing in header
	}

	private String getSchemaToUse(IDestination dest) {
		if (dest.is2011()) {
			return SoapMessage.IIS2011_NS;
		}
		if (dest.isHub()) {
			return SoapMessage.HUB_NS;
		}
		return SoapMessage.IIS2014_NS;
	}

	/**
	 * Sends a connectivity test request to the destination.
	 * 
	 * Connectivity tests are used to verify connectivity between IZ Gateway and the destination endpoint.  They originated
	 * in the CDC WSDL but IZ Gateway can also route a connectivity test from a sender to a specific destination to verify
	 * that it is alive.  Not all jurisdictions are able to support the connectivity test, either due to security requirements
	 * or technical limitations.  If you get back a response, the jurisdiction endpoint is at least alive and listening.  If
	 * you do not, it MAY be due to jurisdiction limitations on supporting this capability.  All new onboarded jurisdictions
	 * MUST support this capability.
	 * 
	 * @param dest	The destination
	 * @param connectivityTest	The connectivity test message
	 * @return	The response (which should contain the same message body back)
	 * @throws Fault	If an error occurred.
	 */
	public ConnectivityTestResponse sendConnectivityTest(IDestination dest, ConnectivityTestRequest connectivityTest)
			throws Fault {
		String schemaToUse = dest.is2011() ? SoapMessage.IIS2011_NS : SoapMessage.IIS2014_NS;
		ConnectivityTestRequest toBeSent = new ConnectivityTestRequest(connectivityTest, schemaToUse, true);
		toBeSent.getHubHeader().clear();
		ConnectivityTestResponse responseFromClient = sendMessage(ConnectivityTestResponse.class, dest, toBeSent);
		ConnectivityTestResponse toBeReturned = new ConnectivityTestResponse(responseFromClient, connectivityTest.getSchema(), false);
		toBeReturned.updateAction(true);  // Now a Hub Response
		return toBeReturned;
	}

	/**
	 * Send a message and obtain a response of the specified type to the given destination 
	 * @param <T>	represents the type of the response
	 * @param clazz	The class of the expected response message
	 * @param dest	The destination to send the message to
	 * @param toBeSent	The message to send
	 * @return	The response
	 * @throws Fault	If a fault occurs during sending.
	 */
	public <T extends SoapMessage> T sendMessage(Class<T> clazz, IDestination dest, SoapMessage toBeSent)
			throws Fault {
		long started = 0;
		long readStarted = 0;
		HttpURLConnection con = null;
		OutputStream pos = null;
		URL location = getUrl(dest);
		T result = null;
		try { // NOSONAR try with resources not appropriate here
			started = System.currentTimeMillis();
			con = setupConnection(tlsSupport.getSNIEnabledConnection(location));
			MessageInfo messageInfo = new MessageInfo(toBeSent, EndpointType.CLIENT, Direction.OUTBOUND, isProduction);
			RequestContext.getTransactionData().getClientRequest().setWs_request_message(messageInfo);
			toBeSent.updateAction(dest.isHub());  // Sending to non-IZ Gateway endpoint
			String action = toBeSent.getWsaHeaders().getAction();
			con.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/soap+xml;charset=UTF-8;action=\"" + action + "\"");
			messageInfo.setHttpHeaders(con.getRequestProperties());
			
			if (preserveOutput) {
				pos = new PreservingOutputStream(con.getOutputStream(), FixedByteArrayOutputStream.DEFAULT_SIZE);
			} else {
				pos = con.getOutputStream();
			}
			
			converter.write(toBeSent, pos);
			readStarted = System.currentTimeMillis();
			result = readResult(clazz, dest, con, started);
			result.respondingTo(toBeSent);
			// Save the response HttpHeader fields.
			messageInfo = new MessageInfo(result, EndpointType.CLIENT, Direction.INBOUND, isProduction);
			messageInfo.setHttpHeaders(con.getHeaderFields());
			RequestContext.getTransactionData().getClientResponse().setWs_response_message(messageInfo);
			return result;
		} catch (ConnectException ex) {
			throw DestinationConnectionFault.connectError(dest, ex, System.currentTimeMillis() - started);
		} catch (SocketTimeoutException ex) {
			throw DestinationConnectionFault.timedOut(dest, ex, System.currentTimeMillis() - started);
		} catch (UnknownHostException ex) {
			throw DestinationConnectionFault.unknownHost(dest, ex);
		} catch (IOException ex) {
			throw DestinationConnectionFault.writeError(dest, ex);
		} finally {
			long finished = System.currentTimeMillis(); 
			if (result != null) {
				RequestContext.getTransactionData().getClientResponse().setWs_response_message(new MessageInfo(result, EndpointType.CLIENT, Direction.INBOUND, isProduction));
			}
			// Increment elapsed time here in case of retries.
			TransactionData tData = RequestContext.getTransactionData();
			tData.setElapsedTimeIIS(tData.getElapsedTimeIIS() + (finished - started));
			tData.setReadTimeIIS(tData.getReadTimeIIS() + (finished - readStarted));
			logDestinationCertificates(con);
		}
	}

	private HttpURLConnection setupConnection(
			HttpURLConnection con) throws ProtocolException {
		con.setRequestMethod(HttpMethod.POST.name());
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(clientConfig.getConnectTimeout()));
		con.setReadTimeout((int) TimeUnit.SECONDS.toMillis(clientConfig.getReadTimeout()));
		con.setUseCaches(false);
		con.setAllowUserInteraction(false);
		con.setInstanceFollowRedirects(false);
		con.setRequestProperty(HttpHeaders.CONTENT_TYPE, clientConfig.getContentType());
		return con;
	}

	private <T extends SoapMessage> T readResult(Class<T> clazz, IDestination dest, HttpURLConnection con, long started)
			throws Fault {

		int statusCode = -1;
		try {
			statusCode = con.getResponseCode();
			RequestContext.getDestinationInfo().setConnected(true);
		} catch (ConnectException ex) {
			throw DestinationConnectionFault.connectError(dest, ex, System.currentTimeMillis() - started);
		} catch (SocketTimeoutException ex) {
			throw DestinationConnectionFault.timedOut(dest, ex, System.currentTimeMillis() - started);
		} catch (IOException ex) {
			throw DestinationConnectionFault.readError(dest, ex);
		}

		HttpUrlConnectionInputMessage m = null;
		InputStream body = null;
		Exception savedEx;
		try {
			SoapMessage result = null;
			// Mark the buffer so we can reread on error.
			m = new HttpUrlConnectionInputMessage(con, clientConfig.getMaxBufferSize());
			statusCode = m.getStatusCode();
			logDestinationCertificates(con);
			body = m.getBody();
			m.mark();
			EndPointInfo endPoint = RequestContext.getDestinationInfo();
			if (ACCEPTABLE_RESPONSE_CODES.contains(statusCode)) {
				result = converter.read(m, endPoint);
				if (result instanceof FaultMessage) {
					m.reset();
					throw HubClientFault.clientThrewFault(null, dest, statusCode, body, result);
				} 
				return clazz.cast(result);
			} else {
				try (InputStream errStream = con.getErrorStream()) {
					throw processHttpError(dest, statusCode, errStream);
				}
			}
		} catch (ClassCastException ex) {
			savedEx = ex;
		} catch (IOException ex) {
			// There was an IO Exception reading the content
			// We'll call this a destination connection fault of some sort.
			throw DestinationConnectionFault.readError(dest, ex);
		} catch (HttpMessageNotReadableException ex) {
			if (ex.getCause() instanceof Fault f) {
				throw f;
			}
			savedEx = ex;
		}
		if (m != null) {
			m.reset();
		}
		// There can be no result here.
		throw HubClientFault.invalidMessage(savedEx, dest, statusCode, body);
	}

	private HubClientFault processHttpError(IDestination dest, int statusCode, InputStream err) {
		String error = "";
		if (err != null) {
			try {
				error = IOUtils.toString(err, StandardCharsets.UTF_8);
			} catch (IOException e) {
				// We couldn't read the error stream, log it but otherwise
				// proceed normally.
			}
		}
		return HubClientFault.httpError(dest, statusCode, error);
	}

	/**
	 * Get the URL from the destination
	 * @param dest	The destination to get the URL for
	 * @return	A url properly configured for sending, and verified against security rules.
	 * @throws DestinationConnectionFault	If the URL is misconfigured.
	 * @throws SecurityFault	If the system and destination do not agree on the operating environment (e.g., prod, onboarding, dev) 
	 */
	URL getUrl(IDestination dest) throws DestinationConnectionFault, SecurityFault {
		
		String destUri = StringUtils.substringBefore(dest.getDestUri(), "?"); // Remove any query parameter from the path.
		String errorMsg = "Destination " + dest.getDestId();

		if (dest.getDestTypeId() != SystemUtils.getDestType()) {
			throw SecurityFault.generalSecurity("Destination Environment Mismatch", 
					"Expected " + SystemUtils.getDestTypeAsString() + " for " + dest.getDestId() + " != " + dest.getDestType(), null);
		}
		

		if (StringUtils.startsWith(destUri, "http:")) {
			throw SecurityFault.generalSecurity("Destination Not Using HTTPS", destUri + " for " + dest.getDestId(), null);
		}

		try {
			if (StringUtils.startsWith(destUri, "https:")) {
				return new URL(destUri);
			}
			if (StringUtils.startsWith(destUri, "/")) {
				return new URL(serverConfig.getProtocol(), "localhost", serverConfig.getPort(), destUri);
			}
		} catch (MalformedURLException e) {
			throw DestinationConnectionFault.configurationError(dest,
					errorMsg + " is not configured correctly", e);
		}
		throw DestinationConnectionFault.notHttps(dest,
				errorMsg + " is using an unknown or unsupported protocol: " + destUri, null);
	}

	URL getReportedUrl(IDestination dest, URL destUrl) throws DestinationConnectionFault {
		String destUri = dest.getDestUri();
		if (StringUtils.startsWith(destUri, "/")) {
			try {
				return new URL(serverConfig.getBaseUrl(), destUri);
			} catch (MalformedURLException e) {
				throw DestinationConnectionFault.configurationError(dest,
						"Destination " + dest.getDestId() + " is not configured correctly", e);
			}
		}
		return destUrl;
	}

	/**
	 * Capture information about the destination certificates for subsequent logging.
	 * 
	 * This method captures information about
	 * - whether or not a connection was made successfully 
	 * - the jurisdiction certificate used in the connection
	 * - the cipher suite selected for the connection.
	 * 
	 * @param con	The connection used to connect to the jurisdiction.
	 */
	public static void logDestinationCertificates(HttpURLConnection con) {
		DestinationInfo destination = RequestContext.getDestinationInfo();
		if (destination.isConnected() && con instanceof HttpsURLConnection conx) {
			try {
				destination.setCipherSuite(conx.getCipherSuite());
				destination.setConnected(true);
				X509Certificate[] certs = (X509Certificate[]) conx.getServerCertificates();
				destination.setCertificate(certs[0]);
			} catch (SSLPeerUnverifiedException | IllegalStateException ex) {
				// Ignore this.
			}
		}
	}
}
