package gov.cdc.izgateway.utils;

import java.io.OutputStream;



import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Based on the design of the IndentingXMLStreamWriter of the same name developed by Sun, but written from scratch without including
 * other classes the Sun implementation used, as the original class includes variants on Core java classes and could not be used as is (the
 * most recent release was in 2007). 
 *
 */
public class IndentingXMLStreamWriter implements XMLStreamWriter {
	private static final XMLOutputFactory outputFactory = XMLOutputFactory.newDefaultFactory();

	private final XMLStreamWriter delegate;
	int level = 0;
	private String indent = "    ";
	boolean isOnNewLine = false;
	boolean hasData = false;
	
	public IndentingXMLStreamWriter(XMLStreamWriter delegate) {
		this.delegate = delegate;
	}
	
	public IndentingXMLStreamWriter(XMLStreamWriter delegate, String indent) {
		this.delegate = delegate;
		this.indent = indent;
	}
	
	public static IndentingXMLStreamWriter createInstance(OutputStream os) throws XMLStreamException {
		return new IndentingXMLStreamWriter(outputFactory.createXMLStreamWriter(os));
	}
	
	@Override
	public void writeStartElement(String localName) throws XMLStreamException {
		beforeStartElement();
		delegate.writeStartElement(localName);
		afterStartElement();
	}

	@Override
	public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
		beforeStartElement();
		delegate.writeStartElement(namespaceURI, localName);
		afterStartElement();
	}

	@Override
	public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		beforeStartElement();
		delegate.writeStartElement(prefix, localName, namespaceURI);
		afterStartElement();
	}

	@Override
	public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
		beforeStartElement();
		delegate.writeEmptyElement(namespaceURI, localName);
		afterStartElement();
	}

	@Override
	public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		beforeStartElement();
		delegate.writeEmptyElement(prefix, namespaceURI, localName);
		afterStartElement();
	}

	@Override
	public void writeEmptyElement(String localName) throws XMLStreamException {
		beforeStartElement();
		delegate.writeEmptyElement(localName);
		afterStartElement();
	}

	@Override
	public void writeEndElement() throws XMLStreamException {
		beforeEndElement();
		delegate.writeEndElement();
		afterEndElement();
	}

	@Override
	public void writeEndDocument() throws XMLStreamException {
		beforeEndDocument();
		delegate.writeEndDocument();
		afterEndDocument();
	}

	@Override
	public void close() throws XMLStreamException {
		delegate.close();
	}

	@Override
	public void flush() throws XMLStreamException {
		delegate.flush();
	}

	@Override
	public void writeAttribute(String localName, String value) throws XMLStreamException {
		delegate.writeAttribute(localName, value);
	}

	@Override
	public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
			throws XMLStreamException {
		delegate.writeAttribute(prefix, namespaceURI, localName, value);
	}

	@Override
	public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
		delegate.writeAttribute(namespaceURI, localName, value);
	}

	@Override
	public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
		delegate.writeNamespace(prefix, namespaceURI);
	}

	@Override
	public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
		delegate.writeDefaultNamespace(namespaceURI);
	}

	@Override
	public void writeComment(String data) throws XMLStreamException {
		beforeComment();
		delegate.writeComment(data);
		afterComment();
	}

	@Override
	public void writeProcessingInstruction(String target) throws XMLStreamException {
		beforeProcessingInstruction();
		delegate.writeProcessingInstruction(target);
		afterProcessingInstruction();
	}

	@Override
	public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
		beforeProcessingInstruction();
		delegate.writeProcessingInstruction(target, data);
		afterProcessingInstruction();
	}

	@Override
	public void writeCData(String data) throws XMLStreamException {
		beforeCData();
		delegate.writeCData(data);
		afterCData();
	}

	@Override
	public void writeDTD(String dtd) throws XMLStreamException {
		beforeDTD();
		delegate.writeDTD(dtd);
		afterDTD();
	}

	@Override
	public void writeEntityRef(String name) throws XMLStreamException {
		delegate.writeEntityRef(name);
	}

	@Override
	public void writeStartDocument() throws XMLStreamException {
		beforeStartDocument();
		delegate.writeStartDocument();
		afterStartDocument();
	}

	@Override
	public void writeStartDocument(String version) throws XMLStreamException {
		beforeStartDocument();
		delegate.writeStartDocument(version);
		afterStartDocument();
	}

	@Override
	public void writeStartDocument(String encoding, String version) throws XMLStreamException {
		beforeStartDocument();
		delegate.writeStartDocument(encoding, version);
		afterStartDocument();
	}

	@Override
	public void writeCharacters(String text) throws XMLStreamException {
		delegate.writeCharacters(text);
		isOnNewLine  = text.length() > 0 && text.charAt(text.length() - 1) == '\n';
		hasData = true;
	}

	@Override
	public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
		delegate.writeCharacters(text, start, len);
		isOnNewLine = text.length > 0 && text[text.length - 1] == '\n'; 
		hasData = true;
	}

	@Override
	public String getPrefix(String uri) throws XMLStreamException {
		return delegate.getPrefix(uri);
	}

	@Override
	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		delegate.setPrefix(prefix, uri);
	}

	@Override
	public void setDefaultNamespace(String uri) throws XMLStreamException {
		delegate.setDefaultNamespace(uri);
	}

	@Override
	public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
		delegate.setNamespaceContext(context);
	}


	@Override
	public NamespaceContext getNamespaceContext() {
		return delegate.getNamespaceContext();
	}

	@Override
	public Object getProperty(String name) throws IllegalArgumentException {
		return delegate.getProperty(name);
	}
	
	public void beforeStartDocument() {
		isOnNewLine = true;
	}
	public void afterStartDocument() {
		isOnNewLine = false;
	}
	public void beforeEndDocument() {
	}
	public void afterEndDocument() {
		isOnNewLine = false;
	}
	public void beforeStartElement() throws XMLStreamException {
		writeIndent();
	}
	public void afterStartElement() {
		isOnNewLine = false;
		level++;
	}
	public void beforeEndElement() throws XMLStreamException {
		--level;
		if (!hasData) {
			writeIndent();
		}
	}
	public void afterEndElement() throws XMLStreamException {
		writeNewline();
	}
	public void beforeComment() throws XMLStreamException {
		writeIndent();
	}
	public void afterComment() throws XMLStreamException {
		writeNewline();
	}
	public void beforeProcessingInstruction() throws XMLStreamException {
		writeIndent();
	}
	public void afterProcessingInstruction() throws XMLStreamException {
		writeNewline();
	}
	public void beforeCData() throws XMLStreamException {
		writeIndent();
	}
	public void afterCData() throws XMLStreamException {
		writeNewline();
	}
	public void beforeDTD() {
	}
	public void afterDTD() throws XMLStreamException {
		writeNewline();
	}
	public void writeIndent() throws XMLStreamException {
		if (indent != null && indent.length() != 0) {
			if (level > 0) {
				if (!isOnNewLine) {
					writeNewline();
				}
				for (int i = 0; i < level; i++) {
					writeCharacters(indent);
				}
				isOnNewLine = false;
			}
		}
	}
	public void writeNewline() throws XMLStreamException {
		writeCharacters("\n");
		hasData = false;
	}
	public void setIndent(String indent) {
		this.indent = indent;
	}
	public String getIndent() {
		return indent;
	}
}
