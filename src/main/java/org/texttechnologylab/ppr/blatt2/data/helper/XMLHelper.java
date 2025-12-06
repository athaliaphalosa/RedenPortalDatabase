package org.texttechnologylab.ppr.blatt2.data.helper;

import org.texttechnologylab.ppr.blatt2.data.redenportal.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * XML Helper für das Parsing von Parlamentsdaten aus XML-Dateien
 * Verarbeitet Redner, Sitzungen, Reden und Kommentare
 */
public class XMLHelper {

    private Sitzung aktuelleSitzung = null;
    private String aktuellerRootHash = null;


    /**
     * Parsing Redner Daten aus XML-Element
     */
    public static Redner parseRedner(Element rednerElement) {
        if (rednerElement == null) return null;

        String id = rednerElement.getAttribute("id");
        Element nameElement = (Element) rednerElement.getElementsByTagName("name").item(0);
        if (nameElement == null) return null;

        String titel = getTextContent(nameElement, "titel");
        String vorname = getTextContent(nameElement, "vorname");
        String nachname = getTextContent(nameElement, "nachname");
        String fraktionsName = getTextContent(nameElement, "fraktion");

        String normalisierterFraktionsName = NormalizeFraktion.normalizeFraktion(fraktionsName);
        Fraktion fraktion = (normalisierterFraktionsName != null && !normalisierterFraktionsName.isEmpty()) ?
                new Fraktion(normalisierterFraktionsName) : null;

        return new Redner(titel, id, vorname, nachname, fraktion);
    }

    /**
     * Hilfsmethode zum Extrahieren von Textinhalt
     */
    private static String getTextContent(Element parent, String tagName) {
        if (parent == null) return null;
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) return null;
        return nodeList.item(0).getTextContent().trim();
    }

    /**
     * Parsing Sitzungsdaten aus XML Element
     */
    public static Sitzung parseSitzung(Element rootElement) {
        if (rootElement == null) return null;

        String wahlperiode = rootElement.getAttribute("wahlperiode");
        String sitzungsNr = rootElement.getAttribute("sitzung-nr");
        String sitzungsOrt = rootElement.getAttribute("sitzung-ort");
        String datumString = rootElement.getAttribute("sitzung-datum");
        String startZeitString = rootElement.getAttribute("sitzung-start-uhrzeit");
        String endZeitString = rootElement.getAttribute("sitzung-ende-uhrzeit");

        if (wahlperiode == null || wahlperiode.isEmpty() || sitzungsNr == null || sitzungsNr.isEmpty()) {
            return null;
        }

        // Parsing Datum
        Date datum = null;
        try {
            if (datumString != null && !datumString.isEmpty()) {
                datum = new SimpleDateFormat("dd.MM.yyyy").parse(datumString);
            }
        } catch (ParseException e) {
            System.err.println("Fehler beim Parsen des Datums: " + datumString);
        }

        // Parsing der Zeiten
        DateTimeFormatter zeitFormat = DateTimeFormatter.ofPattern("H:mm");
        LocalTime startZeit = null, endZeit = null;

        try {
            if (startZeitString != null && !startZeitString.isEmpty()) {
                startZeit = LocalTime.parse(startZeitString, zeitFormat);
            }
            if (endZeitString != null && !endZeitString.isEmpty()) {
                endZeit = LocalTime.parse(endZeitString, zeitFormat);
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Parsen der Zeit: " + e.getMessage());
        }

        return new Sitzung(wahlperiode, sitzungsNr, sitzungsOrt, datum, startZeit, endZeit);
    }

    /**
     * Parser einer Rede mit Cache
     */
    public Rede parseRede(Element redeElement, Element rootElement) {
        if (redeElement == null) return null;

        String redeId = redeElement.getAttribute("id");

        // Cache für Sitzungsdaten
        if (aktuelleSitzung == null || !istGleicherRoot(rootElement)) {
            aktuelleSitzung = parseSitzung(rootElement);
            aktuellerRootHash = generiereRootHash(rootElement);
        }

        Redner redner = parseRednerAusRede(redeElement);
        Rede rede = new Rede(redeId, redner, aktuelleSitzung);

        // Extrahieren Redetext und Kommentar
        String redeText = extrahiereRedeText(redeElement);
        rede.setText(redeText);
        parseKommentareforRede(redeElement, rede);

        return rede;
    }

    /**
     * Parsing einer Rede ohne Cache
     */
    public static Rede parseRedeSimple(Element redeElement, Element rootElement) {
        if (redeElement == null) return null;

        String redeId = redeElement.getAttribute("id");

        // Parse Sitzung direkt ohne Cache
        Sitzung sitzung = parseSitzung(rootElement);
        Redner redner = parseRednerAusRede(redeElement);

        Rede rede = new Rede(redeId, redner, sitzung);
        String redeText = extrahiereRedeText(redeElement);
        rede.setText(redeText);
        parseKommentareforRede(redeElement, rede);

        return rede;
    }

    /**
     * Parsing Redner Daten aus Rede Element
     */
    private static Redner parseRednerAusRede(Element redeElement) {
        // Versuchen Redner direkt zu finden
        Element rednerElement = (Element) redeElement.getElementsByTagName("redner").item(0);
        if (rednerElement != null) return parseRedner(rednerElement);

        // Suche Redner in p-Elementen
        NodeList pElemente = redeElement.getElementsByTagName("p");
        for (int i = 0; i < pElemente.getLength(); i++) {
            Element pElement = (Element) pElemente.item(i);
            if ("redner".equals(pElement.getAttribute("klasse"))) {
                Element rednerInP = (Element) pElement.getElementsByTagName("redner").item(0);
                if (rednerInP != null) return parseRedner(rednerInP);
            }
        }

        return null;
    }

    /**
     * Extrahiert den Text einer Rede aus dem XML Element
     */
    public static String extrahiereRedeText(Element redeElement) {
        if (redeElement == null) return "";

        StringBuilder textBuilder = new StringBuilder();
        NodeList alleKinder = redeElement.getChildNodes();

        for (int i = 0; i < alleKinder.getLength(); i++) {
            Node kind = alleKinder.item(i);
            if (kind.getNodeType() == Node.ELEMENT_NODE) {
                Element kindElement = (Element) kind;
                String tagName = kindElement.getTagName();
                String className = kindElement.getAttribute("klasse");

                // Ignoriere Redner- und Kommentar-Elemente
                if (!"redner".equals(tagName) && !"kommentar".equals(tagName) &&
                        !"redner".equals(className) && !"kommentar".equals(className)) {

                    String elementText = kindElement.getTextContent().trim();
                    if (!elementText.isEmpty()) {
                        textBuilder.append(elementText).append(" ");
                    }
                }
            }
        }
        return textBuilder.toString().trim();
    }

    /**
     * Parsing Kommentare für eine Rede
     */
    private static void parseKommentareforRede(Element redeElement, Rede rede) {
        parseKommentareInElement(redeElement, rede);
        parseAlleKommentare(redeElement, rede);
    }

    /**
     * Parsing Kommentar innerhalb des Rede Elements
     */
    private static void parseKommentareInElement(Element element, Rede rede) {
        NodeList kommentarElemente = element.getElementsByTagName("kommentar");
        for (int i = 0; i < kommentarElemente.getLength(); i++) {
            Element kommentarElement = (Element) kommentarElemente.item(i);
            String text = kommentarElement.getTextContent().trim();
            if (!text.isEmpty()) {
                erstelleKommentarforRede(text, rede);
            }
        }
    }

    /**
     * Parsing alle Kommentare (alle benachbare Elemente)
     */
    private static void parseAlleKommentare(Element redeElement, Rede rede) {
        Element parent = (Element) redeElement.getParentNode();
        if (parent == null) return;

        NodeList alleKinder = parent.getChildNodes();
        boolean redeGefunden = false;

        for (int i = 0; i < alleKinder.getLength(); i++) {
            if (!(alleKinder.item(i) instanceof Element)) continue;
            Element kind = (Element) alleKinder.item(i);

            if (kind == redeElement) {
                redeGefunden = true;
                continue;
            }

            if (redeGefunden && "kommentar".equals(kind.getTagName())) {
                String text = kind.getTextContent().trim();
                if (!text.isEmpty()) {
                    erstelleKommentarforRede(text, rede);
                }
            } else if (redeGefunden) {
                break;
            }
        }
    }

    /**
     * Erstellt Kommentar Objekt für eine Rede
     */
    private static void erstelleKommentarforRede(String text, Rede rede) {
        Kommentar kommentar = new Kommentar(
                rede.getRedner(),
                rede,
                rede.getRedner() != null ? rede.getRedner().getFraktion() : null,
                text
        );
        rede.addKommentar(kommentar);
    }

    /**
     * Prüft ob gleiches Root Element wie im Cache
     */
    private boolean istGleicherRoot(Element root) {
        if (aktuellerRootHash == null) return false;
        return aktuellerRootHash.equals(generiereRootHash(root));
    }

    /**
     * Generiert Hash für Root-Element zur Cache Identifikation
     */
    private static String generiereRootHash(Element root) {
        String key = root.getAttribute("wahlperiode") + "|" +
                root.getAttribute("sitzung-nr") + "|" +
                root.getAttribute("sitzung-datum");
        return String.valueOf(key.hashCode());
    }

    /**
     * Reset Cache (Setzt Cache zurück)
     */
    public void resetCache() {
        aktuelleSitzung = null;
        aktuellerRootHash = null;
    }
}
