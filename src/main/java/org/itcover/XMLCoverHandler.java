package org.itcover;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLCoverHandler extends DefaultHandler{
	
	private static final String COVERED_ATTRIBUTE = "covered";
	private static final String TYPE_ATTRIBUTE = "type";
	private static final String MISSED_ATTRIBUTE = "missed";
	private static final String SLASH = "/";
	private static final String COUNTER_TAG_XPATH = "/report/counter";
	private static final String COUNTER_TYPE_INSTRUCTION = "INSTRUCTION";
	 
	
	private Deque<String> xpathElements = new LinkedList<String>();
	private double coverage;
	
	@Override
	public void startElement(String namespaceURI, String lname, String qname, Attributes attrs) throws SAXException {

		xpathElements.addLast(qname);
		if (COUNTER_TAG_XPATH.equals(getXPath())) {
			if(COUNTER_TYPE_INSTRUCTION.equals(attrs.getValue(TYPE_ATTRIBUTE))) {
				computeCoverage(attrs);
			}
		}
	}

	private void computeCoverage(Attributes attrs) {
		int missed = Integer.parseInt(attrs.getValue(MISSED_ATTRIBUTE));
		int covered = Integer.parseInt(attrs.getValue(COVERED_ATTRIBUTE));
		
		this.coverage = (covered * 100) / (covered + missed);
	}   

	@Override
	public void endElement (String uri, String localName, String qName) throws SAXException {
		xpathElements.removeLast();
	}

	private String getXPath() {
		StringBuilder xpath = new StringBuilder();
		for (String xpathElement : xpathElements) {
			xpath.append(SLASH).append(xpathElement);
		}
		return xpath.toString();
	}

	public double getCoverage() {
		return coverage;
	}
}
