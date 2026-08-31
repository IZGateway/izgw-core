package gov.cdc.izgateway.soap.fault;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import gov.cdc.izgateway.utils.XmlUtils;

class HubClientFaultTests {

	@Test
	void documentElementIsErrorReturnsFalseForNullDocument() {
		assertFalse(HubClientFault.documentElementIsError(null));
	}

	@Test
	void documentElementIsErrorReturnsFalseForChildlessDocument() throws Exception {
		// A Document with no children reproduces the shape that was previously
		// causing a NullPointerException on getFirstChild().getNodeName().
		Document childless = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

		assertDoesNotThrow(() -> HubClientFault.documentElementIsError(childless));
		assertFalse(HubClientFault.documentElementIsError(childless));
	}

	@Test
	void documentElementIsErrorReturnsTrueForErrorTextFallbackDocument() {
		// This is the shape XmlUtils.parseDocument() produces for unparseable input.
		Document errorTextDoc = XmlUtils.parseDocument("not valid xml <<<");

		assertTrue(HubClientFault.documentElementIsError(errorTextDoc));
	}
}
