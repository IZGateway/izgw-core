package gov.cdc.izgateway.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ServiceConfigurationError;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class XmlUtils {
	private static DocumentBuilder documentBuilder = getDocumentBuilder();
	private XmlUtils() {}
	/**
     * Get a properly configured DocumentBuilder that is not subject to
     * 1000 laughs attack.
     * 
     */
    static DocumentBuilder getDocumentBuilder() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setNamespaceAware(true);
            factory.setCoalescing(true);
            return factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            return null;                
        }
    }

    /**
     * Parse a String as an XML Document using an safe XML Builder.
     * @param s	The string to parse
     * @return	The parsed document, or null if the string was empty, or there was any sort of error
     */
    public static Document parseDocument(String s) {
        if (documentBuilder == null || StringUtils.isBlank(s)) {
            return null;
        }
        try {
        	if (!StringUtils.isEmpty(s)) {
                return documentBuilder.parse(new InputSource(new StringReader(s)));
        	}
        	
            Document d = documentBuilder.newDocument();
            d.appendChild(d.createElement("ErrorText"));
            d.getDocumentElement().appendChild(d.createTextNode(s));
            return d;
            
        } catch (Exception e) {
        	// Swallow any exception
        }
        Document d = documentBuilder.newDocument();
        d.appendChild(d.createElement("ErrorText"));
        d.getDocumentElement().appendChild(d.createTextNode(s));
        return d;
    }
    
    /**
     * Convert an input stream to a string 
     * @param is	The stream
     * @return	The string, or null if any sort of error occurred.
     */
    public static String toString(InputStream is) {
        try {
        	if (is != null) {
        		return IOUtils.toString(is, StandardCharsets.UTF_8);
        	}
        } catch (IOException e) {
        	// Ignore IOException and return empty string
        }
        return null;
    }
    
    public static Document parseDocument(InputStream is) {
        if (is == null) {
            return null;
        }
        return parseDocument(toString(is));
    }

	public static Document copy(Document msgPayloadDoc) {
		Transformer tx;
		try {
			TransformerFactory tfactory = TransformerFactory.newInstance();  // NOSONAR Necessary code is below
			tfactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			tx = tfactory.newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new ServiceConfigurationError(e.getMessage(), e);
		}
		DOMSource source = new DOMSource(msgPayloadDoc);
		DOMResult result = new DOMResult();
		try {
			tx.transform(source,result);
		} catch (TransformerException e) {
			throw new ServiceConfigurationError(e.getMessage(), e);
		}
		return (Document)result.getNode();
	}

	public static boolean isTextNode(Node child) {
		if (child == null) {
			return false;
		}
		int type = child.getNodeType();
		return type == Node.CDATA_SECTION_NODE || type == Node.TEXT_NODE;
	}

	public static void copyEvent(XMLStreamReader reader, XMLStreamWriter writer, boolean ignoringSpaces)
			throws XMLStreamException {
		switch (reader.getEventType()) {
		/* Indicates an event is a start element */
		case XMLStreamConstants.START_ELEMENT:
			copyStartElement(reader, writer);
			break;

		/* Indicates an event is an end element */
		case XMLStreamConstants.END_ELEMENT:
			writer.writeEndElement();
			break;

		/* Indicates an event is a processing instruction */
		case XMLStreamConstants.PROCESSING_INSTRUCTION:
			writer.writeProcessingInstruction(reader.getPITarget(), reader.getPIData());
			break;

		case XMLStreamConstants.SPACE, /* The characters are white space */
			 XMLStreamConstants.CHARACTERS: /* Indicates an event is characters */
			String text = reader.getText();
			if (!StringUtils.isAllBlank(text) || !ignoringSpaces) {
				writer.writeCharacters(text);
			}
			break;

		/* Indicates an event is a comment */
		case XMLStreamConstants.COMMENT:
			writer.writeComment(reader.getText());
			break;

		/* Indicates an event is a start document */
		case XMLStreamConstants.START_DOCUMENT:
			writer.writeStartDocument(reader.getEncoding(), reader.getVersion());
			break;

		/* Indicates an event is an end document */
		case XMLStreamConstants.END_DOCUMENT:
			writer.writeEndDocument();
			break;

		/* Indicates an event is an entity reference */
		case XMLStreamConstants.ENTITY_REFERENCE:
			writer.writeEntityRef(reader.getLocalName());
			break;

		/* Indicates an event is a CDATA section */
		case XMLStreamConstants.CDATA:
			writer.writeCData(reader.getText());
			break;

		/* Indicates an event is an attribute */
		case XMLStreamConstants.ATTRIBUTE:
			for (int i = 0; i < reader.getAttributeCount(); i++) {
				writer.writeAttribute(reader.getAttributePrefix(i), reader.getAttributeLocalName(i),
						reader.getAttributeNamespace(i), reader.getAttributeValue(i));
			}
			break;
		/* Indicates the event is a namespace declaration */
		case XMLStreamConstants.NAMESPACE:
			for (int i = 0; i < reader.getNamespaceCount(); i++) {
				writer.writeNamespace(reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
			}
			break;

		/* Indicates a Entity Declaration */
		case XMLStreamConstants.ENTITY_DECLARATION,
			/* Indicates a Notation */
			XMLStreamConstants.NOTATION_DECLARATION,
			/* Indicates an event is a DTD */
			XMLStreamConstants.DTD:
			// Skipping DTD and realted content at this time.
		default:
			break;
		}
	}

	public static void copyStartElement(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
		if (writer == null) {
			return;
		}
		QName name = reader.getName();
		writer.writeStartElement(name.getPrefix(), name.getLocalPart(), name.getNamespaceURI());
		for (int i = 0; i < reader.getAttributeCount(); i++) {
			writer.writeAttribute(
					reader.getAttributePrefix(i), reader.getAttributeLocalName(i),
					reader.getAttributeNamespace(i), reader.getAttributeValue(i)
				);
		}
		for (int i = 0; i < reader.getNamespaceCount(); i++) {
			writer.writeNamespace(reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
		}
	}
	public static Document getErrorDocument(InputStream is) {
		if (is == null) {
			return null;
		}
		try {
			return parseDocument(IOUtils.toString(is, StandardCharsets.UTF_8));
		} catch (IOException e) {
			return null;
		}
	}

}
