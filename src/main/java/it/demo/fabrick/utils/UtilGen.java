package it.demo.fabrick.utils;

import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class UtilGen {

	private static final String EMPTY_STRING = "";

	private static Logger logger = LoggerFactory.getLogger(UtilGen.class);

// Variabili
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

// Metodi	
	public static int count(String str, String tok) {
		logger.debug("count(" + str + "," + tok + ")");
		int counter = 0;
		int start = 0;
		int ind = str.indexOf(tok);
		while (ind != -1) {
			counter++;
			start = ind + 1;
			ind = str.indexOf(tok, start);
		}
		logger.debug("count() - Fine (" + counter + ")");
		return counter;
	}

	public static String dateCalc(String date, int daysToAdd) {
		Date d = stringToDate(date);

		if (d != null) {
			Calendar cal = Calendar.getInstance(Locale.ITALIAN);
			cal.setTime(d);
			cal.add(Calendar.DATE, daysToAdd);
			d = cal.getTime();
			return dateToString(d);
		}
		return EMPTY_STRING;
	}

	public static String dateToString(Date date) {
		return sdf.format(date);
	}

	public static int deleteEmptyNodes(Document theDoc, String pNodeName) {
		return deleteEmptyNodes(theDoc, pNodeName, 0, true);
	}

	public static int deleteEmptyNodes(Document theDoc, String pNodeName, int toDelete) {
		return deleteEmptyNodes(theDoc, pNodeName, toDelete, true);
	}

	public static int deleteEmptyNodes(Document theDoc, String pNodeName, int toDelete, boolean deleteZero) {
		logger.debug("deleteEmptyNodes(" + theDoc + "," + pNodeName + "," + toDelete + "," + deleteZero + ")");
		int deleted = 0;
		try {
			NodeList pNodeList = theDoc.getElementsByTagName(pNodeName);
			Element refEl = (Element) pNodeList.item(0);
			boolean numeric = false;
			if (refEl.getAttribute("Ti").equals("N")) {
				numeric = true;
			}
			int pnlLen = pNodeList.getLength();
			if (toDelete != 0) {
				toDelete = pnlLen - toDelete;
			}
			pnlLen--;
			for (int i = pnlLen; i >= toDelete; i--) {
				Node theNode = pNodeList.item(i);
				if ((theNode == null) || (!theNode.hasChildNodes())) {
					theNode.getParentNode().removeChild(theNode);
					deleted++;
				} else if ((deleteZero) && (numeric) && (isZero(theNode.getFirstChild().getNodeValue()))) {
					theNode.getParentNode().removeChild(theNode);
					deleted++;
				} else {
					break;
				}
			}
		} catch (Exception ex) {
			logger.error("Exception", ex);
		} finally {
			logger.debug("deleteEmptyNodes - Fine (deleted " + deleted + ")");
		}
		return deleted;
	}

	public static int deleteNodes(Document theDoc, String pNodeName, int toDelete) {
		logger.debug("deleteNodes(" + theDoc + "," + pNodeName + "," + toDelete + ")");
		int deleted = 0;
		try {
			NodeList pNodeList = theDoc.getElementsByTagName(pNodeName);
			int pnlLen = pNodeList.getLength();
			if (toDelete != 0) {
				toDelete = pnlLen - toDelete;
			}
			pnlLen--;
			for (int i = pnlLen; i >= toDelete; i--) {
				Node theNode = pNodeList.item(i);
				theNode.getParentNode().removeChild(theNode);
				deleted++;
			}
		} catch (Exception ex) {
			logger.error("Exception", ex);
		} finally {
			logger.debug("deleteNodes - Fine (deleted " + deleted + ")");
		}
		return deleted;
	}

	public static Element getElement(Document theDoc, String elName) {
		logger.debug("getElement(" + theDoc + "," + elName + ")");
		Element retEl = null;
		try {
			retEl = (Element) theDoc.getElementsByTagName(elName).item(0);
		} catch (Exception ex) {
		} finally {
			logger.debug("getElement - Fine");
		}
		return retEl;
	}

	public static Node getNode(Document theDoc, String nodeName) {
		logger.debug("getNode(" + theDoc + "," + nodeName + ")");
		Node retNode = null;
		try {
			retNode = (Node) theDoc.getElementsByTagName(nodeName).item(0);
		} catch (Exception ex) {
		} finally {
			logger.debug("getNode - Fine");
		}
		return retNode;
	}

	public static String getNodeValue(Document theDoc, String nodeName) {
		logger.debug("getNodeValue(" + theDoc + "," + nodeName + ")");
		Node theNode = getNode(theDoc, nodeName);
		logger.debug("getNodeValue - Fine");
		if (theNode != null) {
			return (getNodeValue(theNode));
		} else {
			return EMPTY_STRING;
		}
	}

	public static String getNodeValue(Node theNode) {
		logger.debug("getNodeValue(" + theNode + ")");
		String retVal = EMPTY_STRING;
		try {
			if ((theNode != null) && (theNode.hasChildNodes())) {
				Node textNode = theNode.getFirstChild();
				retVal = textNode.getNodeValue();
			}
		} catch (Exception ex) {
		} finally {
			logger.debug("getNodeValue - Fine (retVal " + retVal + ")");
		}
		return retVal;
	}

	public static boolean isZero(String val) {
		logger.debug("isZero(" + val + ")");
		int retInt = 1;
		try {
			retInt = Integer.parseInt(val);
		} catch (Exception ex) {
		}

		logger.debug("isZero - Fine");
		if (retInt == 0) {
			return true;
		} else {
			return false;
		}
	}

	public static Document parseDoc(DocumentBuilder docBuilder, String xml) {
		logger.debug("parseDoc(" + docBuilder + "," + xml + ")");
		Document doc = null;
		try {
			StringReader r = new StringReader(xml);
			doc = docBuilder.parse(new InputSource(r));
		} catch (Exception ex) {
			logger.error("Exception", ex);
		} finally {
			logger.debug("parseDoc - Fine (" + doc + ")");
		}
		return doc;
	}

	public static String rightTrim(String str) {
		logger.debug("rightTrim(" + str + ")");
		int i = str.length();
		while ((i > 0) && (Character.isWhitespace(str.charAt(i - 1)))) {
			i--;
		}
		String retStr = EMPTY_STRING;
		if (i > 0) {
			retStr = str.substring(0, i);
		}
		logger.debug("rightTrim() - Fine (" + i + "," + retStr + ")");
		return retStr;
	}

	public static void setNodeValue(Document theDoc, String nodeName, String nodeVal) {
		logger.debug("setNodeValue(" + theDoc + "," + nodeName + "," + nodeVal + ")");
		Node theNode = getNode(theDoc, nodeName);
		if (theNode != null) {
			setNodeValue(theDoc, theNode, nodeVal);
		}
		logger.debug("setNodeValue() - Fine");
	}

	public static void setNodeValue(Document theDoc, Node theNode, String nodeVal) {
		logger.debug("setNodeValue(" + theDoc + "," + theNode + "," + nodeVal + ")");
		if (theNode != null) {
			if (nodeVal == null) {
				nodeVal = EMPTY_STRING;
			}
			if (theNode.hasChildNodes()) {
				Node oldNode = theNode.getFirstChild();
				if (oldNode.getNodeType() == Node.TEXT_NODE) {
					theNode.replaceChild(theDoc.createTextNode(nodeVal), oldNode);
				} else {
					theNode.appendChild(theDoc.createTextNode(nodeVal));
				}
			} else {
				theNode.appendChild(theDoc.createTextNode(nodeVal));
			}
		}
		logger.debug("setNodeValue() - Fine");
	}

	public static String[] split(String str, String separator) {
		logger.debug("split(" + str + "," + separator + ")");
		String[] elLst = null;
		int el = count(str, separator);
		elLst = new String[++el];
		if (el != 1) {
			el = 0;
			int start = 0;
			int ind = str.indexOf(separator);
			while (ind != -1) {
				elLst[el++] = str.substring(start, ind);
				start = ind + separator.length();
				ind = str.indexOf(separator, start);
			}
			elLst[el++] = str.substring(start);
		} else {
			elLst[0] = str;
		}
		logger.debug("split() - Fine");
		return elLst;
	}

	public static Date stringToDate(String date) {
		try {
			Date d = sdf.parse(date);
			return d;
		} catch (ParseException pex) {
			return null;
		}
	}

	public static void updateNodeTree(Node refEl, String updString) {
		logger.debug("updateNodeTree(" + refEl.getNodeName() + "," + updString + ")");
		try {
			if (refEl.hasChildNodes()) {
				NodeList nl = refEl.getChildNodes();
				for (int j = 0; j < nl.getLength(); j++)
					updateNodeTree(nl.item(j), updString);
			}
			if (refEl.getNodeType() == Node.TEXT_NODE)
				refEl.setNodeValue(updString);
		} catch (Exception ex) {
			logger.error("Exception", ex);
		} finally {
			logger.debug("updateNodeTree - Fine");
		}
	}

	public static String xmlToString(Node node) {
		return xmlToString(node, "yes");
	}

	public static String xmlToString(Node node, String indent) {
		try {
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer serializer = tFactory.newTransformer();
			serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			serializer.setOutputProperty(OutputKeys.INDENT, indent);
			StringWriter writer = new StringWriter();
			serializer.transform(new DOMSource(node), new StreamResult(writer));
			return writer.toString();
		} catch (Exception e) {
			return EMPTY_STRING;
		}

	}

	public static Node createNode(Document doc, String nodeName, String nodeValue) {
		Node n = doc.createElement(nodeName);
		n.appendChild(doc.createTextNode(nodeValue));
		return n;
	}
}
