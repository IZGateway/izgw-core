package gov.cdc.izgateway.soap.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import gov.cdc.izgateway.security.crypto.CryptoException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;

import gov.cdc.izgateway.logging.info.EndPointInfo;
import gov.cdc.izgateway.soap.message.SoapMessage;
import gov.cdc.izgateway.utils.FixedByteArrayOutputStream;
import gov.cdc.izgateway.utils.IndentingXMLStreamWriter;
import lombok.Getter;
import lombok.Setter;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stax.StAXSource;

public class SoapMessageConverter implements HttpMessageConverter<SoapMessage> {
    private static List<MediaType> mediaTypes = Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_PLAIN, new MediaType("application", "soap+xml"));

	// Retain up to 8K of the input message for error handling.
	private static final int MAX_RETAINED_INPUT = FixedByteArrayOutputStream.DEFAULT_SIZE;
	private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newDefaultFactory();
	static {
		XML_INPUT_FACTORY.setProperty("javax.xml.stream.isValidating", false);
		XML_INPUT_FACTORY.setProperty("javax.xml.stream.isNamespaceAware", true);
		XML_INPUT_FACTORY.setProperty("javax.xml.stream.isCoalescing", true);
		XML_INPUT_FACTORY.setProperty("javax.xml.stream.isReplacingEntityReferences", true);
		XML_INPUT_FACTORY.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		XML_INPUT_FACTORY.setProperty("javax.xml.stream.supportDTD", false);
	}
	private static boolean filterXmlDecl(XMLStreamReader reader) {
		return reader.getEventType() != XMLStreamReader.PROCESSING_INSTRUCTION;
	}
	
	public static final String INBOUND = "Inbound";
	public static final String OUTBOUND = "Outbound";
	private SourceHttpMessageConverter<StAXSource> staxSource = new SourceHttpMessageConverter<>();
	private final String type;
	@Getter
	@Setter
	private boolean isHub;
	
	public static class SoapConversionException extends HttpMessageNotReadableException {
		private static final long serialVersionUID = 1L;

		public SoapConversionException(String msg, Throwable cause, HttpInputMessage httpInputMessage) {
			super(msg, cause, httpInputMessage);
		}
	}
	
	/**
	 * This constructor is called by Application.configureMessageConverters
	 * @param type The type of converter
	 */
	public SoapMessageConverter(String type) {
		staxSource.setSupportDtd(false);
		staxSource.setDefaultCharset(StandardCharsets.UTF_8);
		staxSource.setProcessExternalEntities(false);
		staxSource.setSupportedMediaTypes(mediaTypes);
		this.type = type;
	}
	/**
	 * See if the MediaType is json format.
	 * @param mediaType	The media type to check
	 * @return	true if mediaType matches any/json or any/any+json 
	 */
	private boolean isJson(MediaType mediaType) {
		return mediaType != null && MediaType.APPLICATION_JSON.isCompatibleWith(mediaType);
	}
	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		// We will try to read anything, except application/json, which we'll let Spring/Jackson combo handle
		return SoapMessage.class.isAssignableFrom(clazz) && !isJson(mediaType);
	}
	
	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		// We will try to write anything, except application/json, which we'll let Spring/Jackson combo handle
		return SoapMessage.class.isAssignableFrom(clazz) && !isJson(mediaType);
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return staxSource.getSupportedMediaTypes();
	}

	@Override
	public SoapMessage read(Class<? extends SoapMessage> clazz, HttpInputMessage message)
			throws IOException, HttpMessageNotReadableException {
		return read(message, null);
	}
	
	public SoapMessage read(HttpInputMessage message, EndPointInfo endpoint)
			throws IOException, HttpMessageNotReadableException {
		BufferedInputStream b = IOUtils.buffer(message.getBody());
		HttpInputMessage inputMessage = new HttpInputMessage() {
			@Override public HttpHeaders getHeaders() { return message.getHeaders(); }
			@Override public InputStream getBody() throws IOException { return b; }
		};
		
		inputMessage.getBody().mark(MAX_RETAINED_INPUT);
		StAXSource source = staxSource.read(StAXSource.class, inputMessage);
		try {

			XMLStreamReader xmlReader = getReader(source);
			SoapMessageReader r = new SoapMessageReader(xmlReader, getReadType(), null);
			r.setHub(isHub());
			r.setEndpoint(endpoint);
			SoapMessage m = r.read();  // NOSONAR, enables debugging
			return m;
		} catch (Exception e) {
			inputMessage.getBody().reset();
			throw new SoapConversionException(e.getMessage(), e, inputMessage);
		}
	}
	
	private XMLStreamReader getReader(StAXSource source) throws XMLStreamException {
		return XML_INPUT_FACTORY.createFilteredReader(
			source.getXMLStreamReader(),
			SoapMessageConverter::filterXmlDecl
		);
		// return 
	}
	private String getReadType() {
		switch (type) {
		case INBOUND:
			return "Inbound Request";
		case OUTBOUND:
			return "Inbound Response";
		default:
			return type;
		}
	}
	@Override
	public void write(SoapMessage message, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		outputMessage.getHeaders().set(HttpHeaders.CONTENT_TYPE, getContentType(message, contentType));
		write(message, outputMessage.getBody());
	}
	
	private String getContentType(SoapMessage message,
			MediaType contentType) {
		StringBuilder ct = new StringBuilder("application/soap+xml");
		message.updateAction(isHub());
		String action = message.getWsaHeaders().getAction();
		ct.append("; charset=UTF-8");
		if (StringUtils.isNotEmpty(action)) {
			ct.append("; action=\"").append(action).append("\"");
		}
		return ct.toString();
	}

	public void write(SoapMessage message, OutputStream body) {
		try {
			new SoapMessageWriter(message, IndentingXMLStreamWriter.createInstance(body)).write();
		} catch (XMLStreamException | CryptoException e) {
			throw new HttpMessageNotWritableException(e.getMessage(), e);
		}
	}
}
