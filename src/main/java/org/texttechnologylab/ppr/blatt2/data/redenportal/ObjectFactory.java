package org.texttechnologylab.ppr.blatt2.data.redenportal;

import org.texttechnologylab.ppr.blatt2.data.helper.XMLHelper;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.*;

import static org.texttechnologylab.ppr.blatt2.data.helper.NormalizeFraktion.normalizeFraktion;


/**
 * Object Factory Klasse für die Erstellung und Verwaltung der Redenportal
 */
public class ObjectFactory {

    private List<Rede> alleRede = new ArrayList<>();
    private Set<Redner> alleRedner = new HashSet<>();
    private Set<Sitzung> alleSitzung = new HashSet<>();
    private Set<Fraktion> alleFraktion = new HashSet<>();
    private Set<Kommentar> alleKommentar = new HashSet<>();


    // Maps zur Deduplizierung von Rednern und Fraktionen
    private Map<String, Redner> rednerMap = new HashMap<>();
    private Map<String, Fraktion> fraktionMap = new HashMap<>();

    // Getter
    public List<Rede> getAlleRede() { return alleRede; }
    public Set<Redner> getAlleRedner() { return alleRedner; }
    public Set<Sitzung> getAlleSitzung() { return alleSitzung; }
    public Set<Fraktion> getAlleFraktion() { return alleFraktion; }
    public Set<Kommentar> getAlleKommentar() { return alleKommentar; }

    /**
     * Lädt und verarbeitet XML-Daten aus einem angegebenen Ordner
     * @param folderPath
     */
    public void loadXML(String folderPath) {
        File folder = new File(folderPath);

        // Überprüfe ob der Ordner existiert und gültig ist
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Folder not found: " + folderPath);
            return;
        }


        // Finde alle XML-Dateien im Ordner
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
        if (files == null) {
            System.out.println("No XML files in folder: " + folderPath);
            return;
        }

        System.out.println("Founded " + files.length + " XML files");

        // Verarbeite jede XML Datei
        XMLHelper xmlHelper = new XMLHelper();
        for (File file : files) {
            parseXMLFile(file, xmlHelper);
        }
    }

    /**
     * Liest eine einzelne XML-Datei ein und holt alle enthaltenen Reden.
     * @param file   die zu parsende XML-Datei
     * @param xmlHelper XML Helper für Parsing
     */
    private void parseXMLFile(File file, XMLHelper xmlHelper) {
        try {

            // Cache zurücksetzen für neue Datei
            xmlHelper.resetCache();

            // Dokument erstellen und normalize
            javax.xml.parsers.DocumentBuilder builder =
                    javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            // Finde den Sitzungsverlauf im Dokument
            Element root = doc.getDocumentElement();
            Element sitzungsverlauf = (Element) root.getElementsByTagName("sitzungsverlauf").item(0);
            if (sitzungsverlauf == null) return;

            // Finde alle Rede Elemente
            NodeList redeElements = sitzungsverlauf.getElementsByTagName("rede");

            // Parsing Rede Element mit XML Helper
            for (int i = 0; i < redeElements.getLength(); i++) {
                Element redeElement = (Element) redeElements.item(i);

                Rede rede = xmlHelper.parseRede(redeElement, root);

                // Dedupliziere Redner und speichere alle Objekte
                // Duplizieren vermeiden
                if (rede != null && rede.getRedner() != null && rede.getSitzung() != null) {
                    Redner dedupRedner = deduplicateRedner(rede.getRedner());
                    Sitzung sitzung = rede.getSitzung();

                    alleRede.add(rede);
                    alleSitzung.add(sitzung);
                    alleKommentar.addAll(rede.getKommentar());
                }
            }

        } catch (Exception e) {
            System.err.println(" !! Error parsing file !! : " + file.getName());
            e.printStackTrace();
        }
    }


    /**
     * Dedupliziert Redner Objekt anhand ihrer ID
     * Diese Methode zu sicherstellen, dass eine Rede nur einmal din DB existiert
     * @param redner
     * @return deduplizierte Redner
     */
    private Redner deduplicateRedner(Redner redner) {

        String key = redner.getId();

        // Uberrüfe ob Redner bereits existiert
        if (!rednerMap.containsKey(key)) {
            rednerMap.put(key, redner);
            alleRedner.add(redner);

            // Verarbeite Fraktion des Redners
            if (redner.getFraktion() != null) {
                Fraktion fraktion = redner.getFraktion();
                String fraktionKey = fraktion.getName();

                // Normalize fraktion name
                if (fraktionKey == null || fraktionKey.trim().isEmpty()) {
                    fraktionKey = "FRAKTIONSLOS";
                } else {
                    fraktionKey = normalizeFraktion(fraktionKey);
                }

                // Deduplizierte Fraktion
                if (!fraktionMap.containsKey(fraktionKey)) {
                    fraktionMap.put(fraktionKey, fraktion);
                    alleFraktion.add(fraktion);
                }
            } else {
                // Ohne Fraktion
                Fraktion fraktionslos = new Fraktion("FRAKTIONSLOS");
                if (!fraktionMap.containsKey("FRAKTIONSLOS")) {
                    fraktionMap.put("FRAKTIONSLOS", fraktionslos);
                    alleFraktion.add(fraktionslos);
                }
            }
        }
        return rednerMap.get(key);
    }


    /**
     * Prüfe ob die Factory leer ist
     * @return true, wenn keine Objekte vorhanden sind, false sonst
     */
    public boolean isEmpty() {
        return alleRede.isEmpty() && alleRedner.isEmpty() &&
                alleSitzung.isEmpty() && alleFraktion.isEmpty();
    }



}
