package it.demo.fabrick.utils;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

public class Translator {

	private static final String EMPTY_STRING = "";

	private static final Logger LOGGER = LoggerFactory.getLogger(Translator.class);

	private static final int[] offset = new int[20];
	private static final int Nm = 0; // Indice nome campo
	private static final int Nt = 4; // Indice natura campo
	private static final int Ln = 6; // Indice lunghezza campo
	private static final int Ti = 7; // Indice tipo campo
	private static final int Lv = 8; // Indice livello campo
	private static final int Dc = 9; // Indice numero decimali
	private static final int Oc = 10; // Indice occurs campo
	private static final int Ob = 12; // Indice obbligatoriet� campo
	private static final int Ds = 13; // Indice descrizione breve
	private static final int Dl = 14; // Indice descrizione lunga
	static {
		offset[0] = 0; // Vuoto (nome campo)
		offset[1] = 10; // Codice funzione
		offset[2] = 2; // Codice sottofunzione
		offset[3] = 2; // Codice tracciato
		offset[4] = 1; // Natura campo
		offset[5] = 17; // Tabella DB di riferimento
		offset[6] = 5; // Lunghezza campo
		offset[7] = 1; // Tipo campo
		offset[8] = 2; // Livello campo
		offset[9] = 4; // Decimali
		offset[10] = 3; // Numero occurs
		offset[11] = 1; // Attributo di default iniziale
		offset[12] = 1; // Contenuto obbligatorio
		offset[13] = 30; // Descrizione breve
		offset[14] = 256; // Descrizione lunga
		offset[15] = 16; // Codice tab. valori di riferimento
		offset[16] = 10; // Segnalazione di default per 3270 - attributo
		offset[17] = 68; // Segnalazione di default per 3270 - msg
		offset[18] = 16; // Icona di default presentation-www
		offset[19] = 8; // Nome campo di confronto (per validazione)
	}

	private DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
	private DocumentBuilder docBuilder = null;

	/**
	 *
	 * Inizializzazione dell'oggetto. Vengono inizializzati i vari attributi privati.
	 *
	 */
	private void initialize() {
		LOGGER.debug("initialize()");
		LOGGER.debug("initialize() - Fine");
	}

	/**
	 *
	 * Costruttore dell'oggetto.
	 * 
	 */
	public Translator() {
		LOGGER.debug("Translator()");
		initialize();
		try {
			docBuilder = dfactory.newDocumentBuilder();
		} catch (Exception ex) {
			LOGGER.error("Exception", ex);
		}
		LOGGER.debug("Translator() - Fine");
	}

	/**
	 *
	 * Overload del metodo getDOM. Vedi descrizione del metodo completo.
	 * 
	 */
	public Document getDOM(Properties trProp) {
		LOGGER.debug("getDOM(" + trProp + ")");
		LOGGER.debug("getDOM() - Fine");
		return getDOM(trProp, EMPTY_STRING);
	}

	/**
	 *
	 * Overload del metodo getDOM. Vedi descrizione del metodo completo.
	 * 
	 */
	public Document getDOM(Properties trProp, String data) {
		LOGGER.debug("getDOM(" + trProp + "," + data + ")");
		try {
			Document startDoc = docBuilder.newDocument();
			LOGGER.debug("getDOM() - Fine");
			return getDOM(trProp, data, startDoc);
		} catch (Exception ex) {
			LOGGER.error("Exception", ex);
			return null;
		}
	}

	/**
	 *
	 * Partendo dal file di properties, viene creata una struttura DOM. Se viene passata anche una stringa contenente i
	 * dati, il Document � completato con i valori corrispondenti.
	 * 
	 * @param trProp   Il file properties relativo al Document che si vuole reperire.
	 * @param data     Stringa con i dati da associare al Document (la commarea).
	 * @param startDoc Document valorizzato durante lo svolgimento del metodo.
	 * 
	 * @return Il Document XML
	 *
	 */
	public Document getDOM(Properties trProp, String data, Document startDoc) {
		LOGGER.debug("getDOM(" + trProp + "," + data + "," + startDoc + ")");

		try {
			initialize();
			/* modifica */
			Vector occursLst = new Vector();
			Vector redefinesLst = new Vector();
			int[] pLevel = new int[10];
			Arrays.fill(pLevel, 99);
			String[] pName = new String[10];
			Arrays.fill(pName, EMPTY_STRING);
			Document doc = startDoc;
//			doc = startDoc;
			String commData = data;
//			commData = data;
			Vector v = getFields(trProp);
			int cursor = 0;
			for (int i = 0; i < v.size(); i++) {
				String el = (String) v.get(i);
				cursor = el.indexOf('|');
				String[] row = new String[offset.length];
				row[0] = el.substring(4, cursor); // Nome del campo (senza n.progressivo)
				cursor++;
				for (int j = 1; j < offset.length; j++) {
					row[j] = el.substring(cursor, cursor += offset[j]);
				}

				switch (row[Nt].charAt(0)) {

				// Dato
				case 'D': {
					LOGGER.debug("  case D |" + row[Nm] + "| inizio");
					if (row[Lv].equals("01")) {
						doc.appendChild(doc.createElement(row[Nm]));
					} // Radice
					else {
						Element parentNode = getParentNode(row[Lv], row[Nm], doc, pLevel, pName);
						if (Integer.parseInt(row[Oc]) != 0) {
							// Il campo � un occurs.
							// Creo un elemento contenente: nome del parent, nome del nodo,
							// moltiplicatore residuo del nodo (n.occurs - 1) e livello del nodo.
							// L'array occEl andr� inserito nel vettore occursLst in ordine di livello decrescente
							String[] occEl = new String[4];
							occEl[0] = parentNode.getNodeName();
							occEl[1] = row[Nm];
							int occN = Integer.parseInt(row[Oc]);
							occEl[2] = --occN + EMPTY_STRING;
							occEl[3] = row[Lv];
							if (occursLst.size() == 0) {
								occursLst.add(occEl);
							} else {
								/*
								 * int ind = occursLst.size()-1; int aL = Integer.parseInt(occEl[3]); int vL =
								 * Integer.parseInt(((String[])occursLst.get(ind))[3]); if (aL<=vL) {
								 * occursLst.add(occEl); } else { while (ind>=0) { vL =
								 * Integer.parseInt(((String[])occursLst.get(ind))[3]); if (aL>=vL) {
								 * occursLst.insertElementAt(occEl, ind); break; } ind--; } }
								 */
								int ind = occursLst.size() - 1;
								int aL = Integer.parseInt(occEl[3]);
								int vL = Integer.parseInt(((String[]) occursLst.get(ind))[3]);
								if (aL <= vL) {
									occursLst.add(occEl);
								} else {
									ind = 0;
									while (ind < occursLst.size()) {
										vL = Integer.parseInt(((String[]) occursLst.get(ind))[3]);
										if (aL >= vL) {
											occursLst.insertElementAt(occEl, ind);
											break;
										}
										ind++;
									}
								}
							}
						}
						Element actualNode = doc.createElement(row[Nm]);
						actualNode.setAttribute("Ti", row[Ti]);
						if ((row[Ti].equals("N")) && (Integer.parseInt(row[Dc]) != 0)) {
							actualNode.setAttribute("Dc", Integer.parseInt(row[Dc]) + EMPTY_STRING);
						}
						actualNode.setAttribute("Ln", Integer.parseInt(row[Ln]) + EMPTY_STRING);
						actualNode.setAttribute("Ds", row[Ds].trim());
						actualNode.setAttribute("Dl", row[Dl].trim());
						actualNode.setAttribute("Ob", row[Ob]);
						parentNode.appendChild(actualNode);
					}
					LOGGER.debug("  case D |" + row[Nm] + "| fine");
					break;
				}

				// Attributo
				case 'A': {
					LOGGER.debug("  case A |" + row[Nm] + "| inizio");
					String nodeName = row[Nm].substring(0, row[Nm].lastIndexOf("-"));
					Element ele = (Element) doc.getElementsByTagName(nodeName).item(0);
					int atLen = Integer.parseInt(row[Ln]);
					ele.setAttribute("AtLn", atLen + EMPTY_STRING);
					String valStr = EMPTY_STRING;
					for (int l = 0; l < atLen; l++) {
						valStr += " ";
					}
					ele.setAttribute("At", valStr);
					LOGGER.debug("  case A |" + row[Nm] + "| fine");
					break;
				}

				// Redefines
				case 'R': {
					LOGGER.debug("  case R |" + row[Nm] + "| inizio");
					Element parentNode = getParentNode(row[Lv], row[Nm], doc, pLevel, pName);
					Element actualNode = doc.createElement(row[Nm]);
					actualNode.setAttribute("Ti", row[Ti]);
					actualNode.setAttribute("Ln", Integer.parseInt(row[Ln]) + EMPTY_STRING);
					actualNode.setAttribute("Ds", row[Ds].trim());
					actualNode.setAttribute("Dl", row[Dl].trim());
					actualNode.setAttribute("Ob", row[Ob]);
					int trInd = row[Nm].lastIndexOf("-");
					if (trInd != -1) {
						String nodeName = row[Nm].substring(0, trInd);
						String tipoRec = row[Nm].substring(trInd + 1);
						actualNode.setAttribute("Tr", tipoRec);

						Object[] redArray = new Object[3];
						redArray[0] = nodeName;
						redArray[1] = tipoRec;
						// Il terzo elemento � per il nodo stesso, che verr� estratto soltanto in seguito
						redArray[2] = null;
						redefinesLst.add(redArray);
					} else {
						actualNode.setAttribute("Tr", "*");
					}
					parentNode.appendChild(actualNode);
					LOGGER.debug("  case R |" + row[Nm] + "| fine");
					break;
				}

				// Segno
				case 'S': {
					LOGGER.debug("  case S |" + row[Nm] + "| inizio");
					String nodeName = row[Nm].substring(0, row[Nm].lastIndexOf("-"));
					Element ele = (Element) doc.getElementsByTagName(nodeName).item(0);
					ele.setAttribute("SgLn", Integer.parseInt(row[Ln]) + EMPTY_STRING);
					LOGGER.debug("  case S |" + row[Nm] + "| fine");
					break;
				}

				// Dati ulteriori
				case 'M': {
					break;
				}

				// Presentation
				case 'P': {
					break;
				}

				// Validazione
				case 'V': {
					break;
				}

				// Azione
				case 'F': {
					break;
				}
				}
			}

			// Scorro occursLst e completo il doc con le occurs mancanti
			LOGGER.debug("Completamento occurs - inizio");
			if (occursLst.size() != 0) {
				for (int k = 0; k < occursLst.size(); k++) {
					String[] occEl = (String[]) occursLst.get(k);
					Element theParent = (Element) doc.getElementsByTagName(occEl[0]).item(0);
					Element theRefNode = (Element) doc.getElementsByTagName(occEl[1]).item(0);
					int times = Integer.parseInt(occEl[2]);
					for (int t = 0; t < times; t++) {
						Element theNode = (Element) theRefNode.cloneNode(true);
						theParent.insertBefore(theNode, theRefNode);
					}
				}
			}
			LOGGER.debug("Completamento occurs - fine");

			// Tolgo le redefines, lasciando solo l'elemento principale
			LOGGER.debug("Eliminazione redefines - inizio");
			if (redefinesLst.size() != 0) {
				for (int r = 0; r < redefinesLst.size(); r++) {
					Object[] red = (Object[]) redefinesLst.get(r);
					String nodeName = red[0] + "-" + red[1];
					NodeList nl = doc.getElementsByTagName(nodeName);
					if (nl != null) {
						int nlLen = nl.getLength();
						Node[] nodeLst = new Node[nlLen];
						nlLen--;
						for (int n = nlLen; n >= 0; n--) {
							Node theNode = (Node) nl.item(n);
							nodeLst[n] = theNode.getParentNode().removeChild(theNode);
						}
						red[2] = nodeLst;
					}
				}
			}
			LOGGER.debug("Eliminazione redefines - fine");

			if (LOGGER.isDebugEnabled()) {
				TransformerFactory tFactory = TransformerFactory.newInstance();
				Transformer serializer = tFactory.newTransformer();
				StringWriter writer = new StringWriter();
				serializer.transform(new DOMSource(doc), new StreamResult(writer));
				LOGGER.debug("getDom() - Documento xml generato:|" + writer.toString() + "|");
			}

			if (!commData.equals(EMPTY_STRING)) {
				addValues(doc.getDocumentElement(), commData, 0, redefinesLst);
			}
			LOGGER.debug("getDOM() - Fine");
			return doc;
		} catch (Exception ex) {
			LOGGER.error("Exception", ex);
			return null;
		}
	}

	/**
	 *
	 * Metodo Ricorsivo. Completamento del Document (aggiunta dei valori della commarea commData).
	 * 
	 * @param node Il nodo in esame.
	 *
	 */
	private int addValues(Node node, String commData, int dataCursor, Vector redefinesLst) {
		LOGGER.debug("addValues(" + node + ")");

		if (node == null) {
			return dataCursor;
		}

		short nType = node.getNodeType();
		switch (nType) {
		case Node.ELEMENT_NODE: {
			LOGGER.debug("Element |" + node.getNodeName() + "|");
			int len = 0;
			int atLen = 0;
			int sgLen = 0;
			String type = EMPTY_STRING;
			String red = EMPTY_STRING;
			if (node.hasAttributes()) {
				NamedNodeMap attrs = node.getAttributes();
				for (int i = 0; i < attrs.getLength(); i++) {
					Attr attr = (Attr) attrs.item(i);
					String name = attr.getNodeName();
					if (name.equals("Ln")) {
						len = Integer.parseInt(attr.getNodeValue());
					} else if (name.equals("Ti")) {
						type = attr.getNodeValue();
					} else if (name.equals("AtLn")) {
						atLen = Integer.parseInt(attr.getNodeValue());
					} else if (name.equals("SgLn")) {
						sgLen = Integer.parseInt(attr.getNodeValue());
					} else if (name.equals("Tr")) {
						red = attr.getNodeValue();
					}
				}
			}

			LOGGER.debug("    red |" + red + "|");
			if (red.equals("*")) {
				String nodeName = node.getNodeName();
				String tipoRec = commData.substring(dataCursor, dataCursor + 1);
				Node replNode = null;
				for (int i = 0; i < redefinesLst.size(); i++) {
					Object[] redEl = (Object[]) redefinesLst.get(i);
					String redN = redEl[0].toString();
					String trN = redEl[1].toString();
					if ((redN.equals(nodeName)) && (trN.equals(tipoRec))) {
						Node[] nodeLst = (Node[]) redEl[2];
						if ((nodeLst != null) && (nodeLst.length > 0)) {
							replNode = nodeLst[0];
							if (nodeLst.length > 1) {
								int newNodeLstLen = nodeLst.length - 1;
								Node[] newNodeLst = new Node[newNodeLstLen];
								System.arraycopy(nodeLst, 1, newNodeLst, 0, newNodeLstLen);
								redEl[2] = newNodeLst;
							} else {
								redEl[2] = null;
							}
						}
					}
				}
				if (replNode != null) {
					Node redNode = node.getOwnerDocument().importNode(replNode, true);
					Node sibl = node.getNextSibling();
					node.getParentNode().replaceChild(redNode, node);
//						addValues(redNode,commData);
					dataCursor = addValues(redNode, commData, dataCursor, redefinesLst);
					while (sibl != null) {
//							addValues(sibl,commData);
						dataCursor = addValues(sibl, commData, dataCursor, redefinesLst);
						sibl = sibl.getNextSibling();
					}
				} else {
					Node child = node.getFirstChild();
					while (child != null) {
						dataCursor = addValues(child, commData, dataCursor, redefinesLst);
//							addValues(child,commData);
						child = child.getNextSibling();
					}
				}
			} else {
				LOGGER.debug("    len |" + len + "|");
				LOGGER.debug("   type |" + type + "|");
				LOGGER.debug("  atLen |" + atLen + "|");
				LOGGER.debug("  sgLen |" + sgLen + "|");
				if ((len != 0) && (!type.equals(EMPTY_STRING))) {
					Node valNode;
					String valStr = EMPTY_STRING;
					try {
						valStr = UtilGen.rightTrim(commData.substring(dataCursor, (dataCursor += len)));
					} catch (StringIndexOutOfBoundsException e) {
						valStr = EMPTY_STRING;
					}
					LOGGER.debug(" valStr |" + valStr + "|");
					if (!valStr.equals(EMPTY_STRING)) {
						valNode = node.getOwnerDocument().createTextNode(valStr);
						node.appendChild(valNode);
					}
				}
				if (atLen != 0) {
					Element el = (Element) node;
					String valStr = EMPTY_STRING;
					try {
						valStr = UtilGen.rightTrim(commData.substring(dataCursor, (dataCursor += atLen)));
					} catch (StringIndexOutOfBoundsException e) {
						valStr = EMPTY_STRING;
					}

					if (valStr.equals(EMPTY_STRING)) {
						for (int l = 0; l < atLen; l++) {
							valStr += " ";
						}
					}
					el.setAttribute("At", valStr);
				}
				if (sgLen != 0) {
					Element el = (Element) node;
					String valStr = EMPTY_STRING;
					try {
						valStr = UtilGen.rightTrim(commData.substring(dataCursor, (dataCursor += sgLen)));
					} catch (StringIndexOutOfBoundsException e) {
						valStr = EMPTY_STRING;
					}
					if (valStr.equals(EMPTY_STRING)) {
						for (int l = 0; l < sgLen; l++) {
							valStr += " ";
						}
					}
					el.setAttribute("Sg", valStr);
				}

				Node child = node.getFirstChild();
				while (child != null) {
//						addValues(child,commData);
					dataCursor = addValues(child, commData, dataCursor, redefinesLst);
					child = child.getNextSibling();
				}
			}
			break;
		}
		default: {
			break;
		}
		/*
		 * Tipologie di nodo non gestite
		 * 
		 * case Node.DOCUMENT_NODE: { break; }
		 * 
		 * case Node.DOCUMENT_TYPE_NODE: { break; }
		 * 
		 * case Node.ENTITY_REFERENCE_NODE: { break; }
		 * 
		 * case Node.CDATA_SECTION_NODE: { break; }
		 * 
		 * case Node.TEXT_NODE: { break; }
		 * 
		 * case Node.PROCESSING_INSTRUCTION_NODE: { break; }
		 */
		}
		LOGGER.debug("addValues() - Fine");
		return dataCursor;
	}

	/**
	 *
	 * Overload del metodo getCommarea. Vedi descrizione del metodo completo.
	 * 
	 */
	public String getCommarea(Properties trProp) {
		LOGGER.debug("getCommarea(" + trProp + ")");
		try {
			Document startDoc = docBuilder.newDocument();
			LOGGER.debug("getCommarea() - Fine");
			return getCommarea(trProp, startDoc);
		} catch (Exception ex) {
			LOGGER.error("Exception", ex);
			return null;
		}
	}

	/**
	 *
	 * Overload del metodo getCommarea. Vedi descrizione del metodo completo.
	 * 
	 */
	public String getCommarea(Properties trProp, Element el) {
		LOGGER.debug("getCommarea(" + trProp + "," + el + ")");
		try {
			Document startDoc = docBuilder.newDocument();
			Node impNode = startDoc.importNode(el, true);
			startDoc.appendChild(impNode);
			LOGGER.debug("getCommarea() - Fine");
			return getCommarea(trProp, startDoc);
		} catch (Exception ex) {
			LOGGER.error("Exception", ex);
			return null;
		}
	}

	/**
	 *
	 * Partendo dal file di properties e da un Document XML, viene creata la commarea corrispondente.
	 * 
	 * @param trProp   Il file properties relativo alla commarea che si vuole reperire.
	 * @param startDoc Document contenente i dati da convertire in commarea
	 * 
	 * @return La commarea ottenuta
	 *
	 */
	public String getCommarea(Properties trProp, Document startDoc) {
		LOGGER.debug("getCommarea(" + trProp + "," + startDoc + ")");
		try {
			initialize();
			Document doc = startDoc;

			String commData = EMPTY_STRING;

			/* modifica */
			Hashtable h = new Hashtable();
			Vector v = getFields(trProp);
			int cursor = 0;
			/* modifica */
			for (int i = 0; i < v.size(); i++) {
				String el = (String) v.get(i);
				cursor = el.indexOf('|');
				String[] row = new String[offset.length - 1];
				String key = el.substring(4, cursor); // Nome del campo (senza n.progressivo)
				cursor++;
				for (int j = 0; j < (offset.length - 1); j++) {
					row[j] = el.substring(cursor, cursor += offset[j]);
				}
				h.put(key, row);
			}

			commData = iterateDOM(doc, h);
			LOGGER.debug("getCommarea() - Fine");
			return commData;
		} catch (Exception ex) {
                    LOGGER.error("ERRORE in getCommarea nel documento:\n{}\n", UtilGen.xmlToString(startDoc));
			LOGGER.error("Exception", ex);
			return null;
		}
	}

	/**
	 *
	 * Partendo dal file di properties e da un Document XML, viene creata la commarea corrispondente.
	 * 
	 * @param trProp   Il file properties relativo alla commarea che si vuole reperire.
	 * @param startDoc Document contenente i dati da convertire in commarea
	 * 
	 * @return La commarea ottenuta
	 *
	 */
	public String getCommareaIter(Properties trProp, Document startDoc) {
		try {
			initialize();
			Document doc = startDoc;
			String commData = EMPTY_STRING;
			Hashtable h = new Hashtable();
			Vector v = getFields(trProp);
			int cursor = 0;
			for (int i = 0; i < v.size(); i++) {
				String el = (String) v.get(i);
				cursor = el.indexOf('|');
				String[] row = new String[offset.length - 1];
				String key = el.substring(4, cursor); // Nome del campo (senza n.progressivo)
				cursor++;
				for (int j = 0; j < (offset.length - 1); j++) {
					row[j] = el.substring(cursor, cursor += offset[j]);
				}
				h.put(key, row);
			}

			commData = iterateDOM(doc, h);
			return commData;
		} catch (Exception ex) {
                    LOGGER.error("ERRORE in getCommarea nel documento:\n{}\n", UtilGen.xmlToString(startDoc));
			LOGGER.error("Exception", ex);
			return null;
		}
	}

	/**
	 *
	 * Metodo ricorsivo. Scorre tutti i nodi di un documento XML aggiungendo il loro valore alla variabile globale
	 * commData, per comporre la commarea totale.
	 * 
	 * @param node       Il file properties relativo alla commarea che si vuole reperire.
	 * @param parentName Nome del parentNode del nodo in questione (necessario per i nodi di tipo testo o CDATA).
	 *
	 */
	private String traverseDOM(Node node, String parentName, Hashtable h, String commData, int attrLen, String attrVal,
			int signLen, String signVal) {
		LOGGER.debug("traverseDOM(" + node + "," + parentName + ")");
		if (node == null) {
			return commData;
		}

		short nType = node.getNodeType();
		if (nType == Node.ELEMENT_NODE) {
			String name = node.getNodeName();
			LOGGER.debug("  nodo |" + name + "|");
			if (node.hasAttributes()) {
				NamedNodeMap attrs = node.getAttributes();
				attrLen = 0;
				attrVal = EMPTY_STRING;
				signLen = 0;
				signVal = EMPTY_STRING;
				for (int i = 0; i < attrs.getLength(); i++) {
					Attr attr = (Attr) attrs.item(i);
					String attrName = attr.getNodeName();
					if (attrName.equals("AtLn")) {
						attrLen = Integer.parseInt(attr.getNodeValue());
					} else if (attrName.equals("At")) {
						attrVal = attr.getNodeValue();
					} else if (attrName.equals("SgLn")) {
						signLen = Integer.parseInt(attr.getNodeValue());
					} else if (attrName.equals("Sg")) {
						signVal = attr.getNodeValue();
					}
				}
			}

			String[] row = (String[]) h.get(name);
			if ((!row[Ti].equals("*")) && (Integer.parseInt(row[Oc]) == 0) && (!node.hasChildNodes())) {
				char[] empty = new char[Integer.parseInt(row[Ln])];
				if (row[Ti].equals("A")) {
					Arrays.fill(empty, ' ');
				} else if ((row[Ti].equals("N")) || (row[Ti].equals("D"))) {
					Arrays.fill(empty, '0');
				}
				commData += (String.valueOf(empty));
				if (attrLen != 0) {
					commData += attrVal;
				}
			}

			Node child = node.getFirstChild();
			while (child != null) {
				commData = traverseDOM(child, name, h, commData, attrLen, attrVal, signLen, signVal);
				child = child.getNextSibling();
			}
		} else if ((nType == Node.CDATA_SECTION_NODE) || (nType == Node.TEXT_NODE)) {
			String val = node.getNodeValue();
			LOGGER.debug("      valore prima |" + val + "|");
			String[] row = (String[]) h.get(parentName);
			if (val.length() != Integer.parseInt(row[Ln])) {
				char[] empty = new char[Integer.parseInt(row[Ln])];
//				int diff = Integer.parseInt(row[Ln])-val.length();
				if (row[Ti].equals("A")) {
					Arrays.fill(empty, ' ');
					val = (val + new String(empty)).substring(0, Integer.parseInt(row[Ln]));
//					for (int j=0;j<diff;j++) { val+=" "; }
				} else if ((row[Ti].equals("N")) || (row[Ti].equals("D"))) {
					Arrays.fill(empty, '0');

					val = (new String(empty) + val);
					val = val.substring(val.length() - Integer.parseInt(row[Ln]));

//					for (int j=0;j<diff;j++) { val="0"+val; }
				}

			}
			LOGGER.debug("      valore dopo  |" + val + "|");
			commData += val;

			if (attrLen != 0) {
				commData += attrVal;
			}
			if (signLen != 0) {
				commData += signVal;
			}
		}
		LOGGER.debug("traverseDOM() - Fine");
		return commData;
	}

	private String iterateDOM(Document doc, Hashtable h) {

		String commData = EMPTY_STRING;
		DocumentTraversal traversable = (DocumentTraversal) doc;
		NodeIterator iterator = traversable.createNodeIterator(doc, NodeFilter.SHOW_ALL, null, true);
		// Iterate over the comments
		Node node;

		String parentName = EMPTY_STRING;
		int attrLen = 0;
		String attrVal = EMPTY_STRING;
		int signLen = 0;
		String signVal = EMPTY_STRING;
		while ((node = iterator.nextNode()) != null) {
			short nType = node.getNodeType();
			String name = node.getNodeName();

			if (nType == Node.ELEMENT_NODE) {
				if (node.hasAttributes()) {
					NamedNodeMap attrs = node.getAttributes();
					attrLen = 0;
					attrVal = EMPTY_STRING;
					signLen = 0;
					signVal = EMPTY_STRING;
					for (int i = 0; i < attrs.getLength(); i++) {
						Attr attr = (Attr) attrs.item(i);
						String attrName = attr.getNodeName();
						if (attrName.equals("AtLn")) {
							attrLen = Integer.parseInt(attr.getNodeValue());
						} else if (attrName.equals("At")) {
							attrVal = attr.getNodeValue();
						} else if (attrName.equals("SgLn")) {
							signLen = Integer.parseInt(attr.getNodeValue());
						} else if (attrName.equals("Sg")) {
							signVal = attr.getNodeValue();
						}
					}
				}

				String[] row = (String[]) h.get(name);
				if ((!row[Ti].equals("*")) && (Integer.parseInt(row[Oc]) == 0) && (!node.hasChildNodes())) {
					char[] empty = new char[Integer.parseInt(row[Ln])];
					if (row[Ti].equals("A")) {
						Arrays.fill(empty, ' ');
					} else if ((row[Ti].equals("N")) || (row[Ti].equals("D"))) {
						Arrays.fill(empty, '0');
					}
					commData += (String.valueOf(empty));
					if (attrLen != 0) {
						commData += (" " + attrVal).substring((1 + attrVal.length()) - attrLen);
					}
					if (signLen != 0) {
						commData += signVal;
					}
					parentName = EMPTY_STRING;
				} else {
					if (Integer.parseInt(row[Ln]) == 0 && attrLen > 0)
						commData += (" " + attrVal).substring((1 + attrVal.length()) - attrLen);
					parentName = name;
				}
			} else if ((nType == Node.CDATA_SECTION_NODE) || (nType == Node.TEXT_NODE)) {
				String val = node.getNodeValue();
				String[] row = (String[]) h.get(parentName);
				if (row != null) {
					if (val.length() != Integer.parseInt(row[Ln])) {
						char[] empty = new char[Integer.parseInt(row[Ln])];
						// int diff = Integer.parseInt(row[Ln])-val.length();
						if (row[Ti].equals("A")) {
							Arrays.fill(empty, ' ');
							val = (val + new String(empty)).substring(0, Integer.parseInt(row[Ln]));
							// for (int j=0;j<diff;j++) { val+=" "; }
						} else if ((row[Ti].equals("N")) || (row[Ti].equals("D"))) {
							Arrays.fill(empty, '0');

							val = (new String(empty) + val);
							val = val.substring(val.length() - Integer.parseInt(row[Ln]));

							// for (int j=0;j<diff;j++) { val="0"+val; }
						}

					}
					commData += val;

					if (attrLen != 0) {
						commData += (" " + attrVal).substring((1 + attrVal.length()) - attrLen);
					}
					if (signLen != 0) {
						commData += signVal;
					}
					parentName = name;
				}
			}

		}
		return commData;
}

	/**
	 *
	 * Scorre pLevel per vedere se esiste gi� un elemento con lo stesso livello. Se esiste, viene sostituito con
	 * l'elemento attuale. Se non esiste, viene inserito l'elemento attuale in pLevel, mantenendo l'ordinamento. Vengono
	 * cancellati gli elementi di livello inferiore (numero maggiore) all'attuale.
	 * 
	 * @param lv   Livello del campo.
	 * @param name Nome del campo.
	 * 
	 * @return Il nodo relativo all'elemento di livello immediatamente superiore (numero minore) all'attuale.
	 *
	 */
	private Element getParentNode(String lv, String name, Document doc, int[] pLevel, String[] pName) {
		LOGGER.debug("getParentNode(" + lv + "," + name + ")");
		Element n = null;
		int level = Integer.parseInt(lv);
		int pInd = pLevel.length - 1;
		while ((pInd >= 0) && (pLevel[pInd] >= level)) {
			pInd--;
		}
		if (pInd < 0) {
			n = doc.getDocumentElement();
		} else {
			n = (Element) doc.getElementsByTagName(pName[pInd]).item(0);
		}

		pLevel[++pInd] = level;
		pName[pInd] = name;
		Arrays.fill(pLevel, ++pInd, pLevel.length, 99);
		Arrays.fill(pName, pInd, pName.length, EMPTY_STRING);
		LOGGER.debug("getParentNode() - Fine");
		return n;
	}

	/**
	 *
	 * Scorre tutti gli elementi del Properties passato come parametro e carica un Vector di stringhe (key+"|"+val).
	 * 
	 * @param trProp Properties da leggere.
	 * 
	 * @return Il Vector contenente gli elementi.
	 *
	 */
	private Vector getFields(Properties trProp) {
		LOGGER.debug("getFields(" + trProp + ")");
		try {
			Enumeration en = trProp.propertyNames();
			Vector pV = new Vector();
			while (en.hasMoreElements()) {
				String key = (String) en.nextElement();
				pV.add(key + "|" + trProp.getProperty(key));
			}
			Collections.sort(pV);
			LOGGER.debug("getFields() - Fine");
			return pV;
		} catch (Exception ex) {
			LOGGER.error("Exception", ex);
			return null;
		}
	}
}