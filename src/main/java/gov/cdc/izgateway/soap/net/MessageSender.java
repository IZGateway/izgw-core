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
import gov.cdc.izgateway.soap.message.ConnectivityTestRequest;
import gov.cdc.izgateway.soap.message.ConnectivityTestResponse;
import gov.cdc.izgateway.soap.message.FaultMessage;
import gov.cdc.izgateway.soap.message.SoapMessage;
import gov.cdc.izgateway.soap.message.SubmitSingleMessageRequest;
import gov.cdc.izgateway.soap.message.SubmitSingleMessageResponse;
import gov.cdc.izgateway.utils.FixedByteArrayOutputStream;
import gov.cdc.izgateway.utils.PreservingOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

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
		this.statusChecker = new IStatusCheckerService() {
			@Override
			public void lookForReset(IDestination dest) {
			}

			@Override
			public void updateStatus(IEndpointStatus s,
					boolean wasCircuitBreakerThrown, Throwable reason) {
			}
			@Override
			public boolean isExempt(String destId) {
				return false;
			}
		};
	}
	public void setStatusChecker(IStatusCheckerService statusChecker) {
		this.statusChecker = statusChecker;
	}
	public IStatusCheckerService getStatusChecker() {
		return statusChecker;
	}
	
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
		if (f.getRetry() != RetryStrategy.CHECK_IIS_STATUS) {
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
		if (status.isCircuitBreakerThrown() && userIsNotAdmin() && !statusChecker.isExempt(dest.getDestId())) {
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

		SoapMessage result = null;
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
			// Mark the buffer so we can reread on error.
			m = new HttpUrlConnectionInputMessage(con, clientConfig.getMaxBufferSize());
			statusCode = m.getStatusCode();
			body = m.getBody();
			m.mark();
			EndPointInfo endPoint = RequestContext.getDestinationInfo();
			if (ACCEPTABLE_RESPONSE_CODES.contains(statusCode)) {
				result = converter.read(m, endPoint);
				if (result instanceof FaultMessage fm) {
					m.reset();
					throw HubClientFault.clientThrewFault(null, dest, statusCode, body, result);
				} 
				return clazz.cast(result);
			} else {
				throw processHttpError(dest, statusCode, con.getErrorStream());
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
		throw HubClientFault.invalidMessage(savedEx, dest, statusCode, body, result);
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

	URL getUrl(IDestination dest) throws DestinationConnectionFault {
		String destUri = StringUtils.substringBefore(dest.getDestUri(), "?"); // Remove any query parameter from the path.
		String errorMsg = "Destination " + dest.getDestId();

		if (StringUtils.startsWith(destUri, "http:")) {
			throw new SecurityException(errorMsg + " is not using https: " + destUri);
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

	public static void logDestinationCertificates(HttpURLConnection con) {
		DestinationInfo destination = RequestContext.getDestinationInfo();
		if (destination.isConnected() && con instanceof HttpsURLConnection conx) {
			try {
				X509Certificate[] certs = (X509Certificate[]) conx.getServerCertificates();
				destination.setCertificate(certs[0]);
				destination.setCipherSuite(conx.getCipherSuite());
				destination.setConnected(true);
			} catch (SSLPeerUnverifiedException | IllegalStateException ex) {
				// Ignore this.
			}
		}
	}
}
