package test.gov.cdc.izgateway.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import gov.cdc.izgateway.utils.HL7Utils;

class TestHL7Utils {

	@ParameterizedTest
	@MethodSource
	void testMaskSegments(String message, String expected) {
		String actual = HL7Utils.maskSegments(message);
		assertEquals(expected, actual);
	}
	
	public static String[][] testMaskSegments() {
    	return new String[][] {
    		{	"MSH|this is an msh\r followed by some stuff followed by a PID|withsomegunge",
    			"MSH|this is an msh\r followed by some stuff followed by a PID|..."
    		},
    		{	" MSH|this is an msh\r followed by some stuff followed by a PID|withsomegunge\n and some more stuff",
    			" MSH|this is an msh\r followed by some stuff followed by a PID|... and some more stuff"
    		},
    		{	"AMSH|this is an msh\r followed by some stuff followed by a PID|withsomegunge",
    			"AMSH|this is an msh\r followed by some stuff followed by a PID|..."
    		},
    		{	"1MSH|this is an msh\r followed by some stuff followed by a PID|withsomegunge\r",
    			"1MSH|this is an msh\r followed by some stuff followed by a PID|..."
    		},
    		{	"QPD|this is an msh\r followed by some stuff followed by a PID|withsomegunge and \r some more stuff",
    			"QPD|... followed by some stuff followed by a PID|... some more stuff"
    		},
    		{	" QPD|this is an msh\r followed by some stuff followed by a PID|withsomegunge",
    			" QPD|... followed by some stuff followed by a PID|..."
    		},
    		{	"AQPD|this is an msh\r followed by some stuff followed by a PID|withsomegunge \r and some more stuff",
    			"AQPD|this is an msh\r followed by some stuff followed by a PID|... and some more stuff"
    		},
    		{	"1QPD|this is an msh\r followed by some stuff followed by a PID|withsomegunge",
    			"1QPD|this is an msh\r followed by some stuff followed by a PID|..."
    		},
    		{	"ERR|this is an msh\r followed by some stuff followed by a PID|withsomegunge",
    			"ERR|this is an msh\r followed by some stuff followed by a PID|..."
    		},
    		{	"MSA|this is an msh\r followed by some stuff followed by a PID|withsomegunge",
    			"MSA|this is an msh\r followed by some stuff followed by a PID|..."
    		},
    		{	"QAK|this is an msh\r followed by some stuff followed by a PID|withsomegunge",
    			"QAK|this is an msh\r followed by some stuff followed by a PID|..."
    		},
    		{	"ZA1|this is an msh\r followed by some stuff followed by a PID|withsomegunge",
    			"ZA1|... followed by some stuff followed by a PID|..."
    		}
    	};
    }
}
