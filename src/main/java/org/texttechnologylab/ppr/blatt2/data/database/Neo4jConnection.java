package org.texttechnologylab.ppr.blatt2.data.database;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.*;
import org.texttechnologylab.ppr.blatt2.data.helper.NormalizeFraktion;
import org.texttechnologylab.ppr.blatt2.data.helper.XMLHelper;
import org.texttechnologylab.ppr.blatt2.data.redenportal.Sitzung;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.UUID;


/**
 * Neo4j Connection Database
 * Diese Klasse verwaltet die Verbindung zur Neo4j-Datenbank und bietet Methode:
 * 1. Import von XML-Daten in Neo4j
 * 2. CRUD-Operationen auf der Datenbank durchführen
 * 3. Transaktionen und Caching handhaben
 * 4. Helper Methoden für Node und Relationship rstellung
 *
 */
public class Neo4jConnection {

    /** Neo4j Database Interface */
    public final GraphDatabaseService graphDb;

    /** Database Management Service for lifecycle control */
    private final DatabaseManagementService managementService;

    /**
     * Gibt den GraphDatabaseService zurück
     * @return GraphDatabaseService Instanz (graphDb)
     */
    public GraphDatabaseService getGraphDb() {
        return graphDb;
    }

    /**
     * Einfach Knotruktor für Neo4j
     * @param databasePath  Pfad/Path zur Neo4j Datenbank
     */
    public Neo4jConnection(String databasePath) {
        this.managementService =
                new DatabaseManagementServiceBuilder(new File(databasePath).toPath()).build();
        this.graphDb = managementService.database("neo4j");
        registerShutdownHook();
    }

    /**
     * Stellt sicher, dass die Datenbank beim Beenden sauber geschlossen wird
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    /**
     * Beendet die Datenbankverbindung ordentlich
     */
    public void shutdown() {
        managementService.shutdown();
    }



    /**
     * Erstellt einen neuen Knoten (Node) in der Neo4j-Datenbank mit den angegebenen Eigenschaften (Properties)
     * @param tx  aktuelle Transaktion, in der der Node erstellt wird
     * @param label  das Label, das dem Node zugewiesen werden soll
     * @param props eine Map von Properties (Key-Value Paaren), die dem Node hinzugefügt werden
     * @return der erstellte Node
     */
    public Node createNode(Transaction tx, String label, Map<String, Object> props) {
        Node n = tx.createNode(Label.label(label));
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            if (entry.getValue() != null) {
                n.setProperty(entry.getKey(), entry.getValue());
            }
        }
        return n;
    }

    /**
     * Sucht einen Node anhand eines Schlüsselwerts
     * Oder erstellt ihn falls er nicht existiert.
     * @param tx  aktuelle Neo4j-Transaktion
     * @param label  das Label, das dem Node zugewiesen werden soll
     * @param key   der Schlüssel, nach dem gesucht werden soll
     * @param keyValue  der Wert des Schlüssels für die Suche
     * @param props Eigenschaften (Properties) für die Erstellung des Nodes, falls er nicht existiert
     * @return  der gefundene oder neu erstellte Node
     */
    public Node findOrCreateNode(Transaction tx, String label, String key, String keyValue, Map<String, Object> props) {

        ResourceIterator<Node> nodes = tx.findNodes(Label.label(label), key, keyValue);
        if (nodes.hasNext()) {
            Node node = nodes.next();
            nodes.close();
            return node;
        }
        nodes.close();

        return createNode(tx, label, props);
    }


    /**
     * Erstellt eine Relationship zwischen zwei Nodes
     * @param tx Die Neo4j Transaction
     * @param fromNode Start-Node
     * @param toNode End-Node
     * @param relationshipType Typ der Relationship
     */
    public void createRelationship(Transaction tx, Node fromNode, Node toNode, String relationshipType) {
        fromNode.createRelationshipTo(toNode, RelationshipType.withName(relationshipType));
    }

    /**
     * Löschen alle Daten aus der Datenbank
     */
    public void deleteAllData() {
        try (Transaction tx = graphDb.beginTx()) {

            tx.execute("MATCH (n) DETACH DELETE n");
            tx.commit();
        }
    }


    /**
     * Import Data in Hoch Perfomance
     * @param xmlFolderPath  Pfad/Path zum Ordner mit XML-Dateien
     */
    public void importDataComplete(String xmlFolderPath) {
        File folder = new File(xmlFolderPath);
        File[] files = folder.listFiles((d, n) -> n.endsWith(".xml"));

        if (files == null) {
            System.out.println("No XML files found");
            return;
        }

        System.out.println("COMPLETE IMPORT: " + files.length + " files");
        System.out.println("Bitte kurz Warten :)");
        long startTime = System.currentTimeMillis();

        // Datenbank vorher erstmal leeren
        deleteAllData();

        // Batch Verarbeitung
        int batchSize = 10;
        int totalProcessed = 0;
        int totalReden = 0;
        int totalKommentare = 0;

        for (int batchStart = 0; batchStart < files.length; batchStart += batchSize) {
            int batchEnd = Math.min(batchStart + batchSize, files.length);

            try (Transaction tx = graphDb.beginTx()) {

                Map<String, Node> sitzungCache = new HashMap<>();
                Map<String, Node> rednerCache = new HashMap<>();
                Map<String, Node> fraktionCache = new HashMap<>();
                Map<String, Node> redeCache = new HashMap<>();

                for (int i = batchStart; i < batchEnd; i++) {
                    File file = files[i];
                    try {
                        int[] counts = processFileComplete(tx, file, sitzungCache, rednerCache, fraktionCache, redeCache);
                        totalReden += counts[0];
                        totalKommentare += counts[1];
                        totalProcessed++;
                    } catch (Exception e) {
                        System.err.println("!!! Error in " + file.getName());
                    }
                }
                tx.commit();
            }

        }

        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        System.out.printf("IMPORT COMPLETED in %.1f seconds%n", duration);
        System.out.printf("Files: %d/%d | Reden: %,d | Kommentare: %,d%n",
                totalProcessed, files.length, totalReden, totalKommentare);
    }


    /**
     * Verarbeitet eine einzelne Datei im High Performance
     */
    private int[] processFileComplete(Transaction tx, File file,
                                      Map<String, Node> sitzungCache,
                                      Map<String, Node> rednerCache,
                                      Map<String, Node> fraktionCache,
                                      Map<String, Node> redeCache) throws Exception {

        int redeCount = 0;
        int kommentarCount = 0;

        // Schnelles XML parsing
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
        Element root = doc.getDocumentElement();

        // Parse Sitzungsinformationen
        String wahlperiode = root.getAttribute("wahlperiode");
        String sitzungNr = root.getAttribute("sitzung-nr");
        String ort = root.getAttribute("sitzung-ort");

        if (wahlperiode.isEmpty() || sitzungNr.isEmpty()) {
            return new int[]{0, 0};
        }

        // Sitzungs-Node finden oder erstellen
        String sitzungKey = wahlperiode + "_" + sitzungNr;
        Node sitzungNode = sitzungCache.get(sitzungKey);
        if (sitzungNode == null) {
            String sitzungId = "SITZUNG_" + sitzungKey;
            sitzungNode = tx.createNode(Label.label("Sitzung"));
            sitzungNode.setProperty("sitzungId", sitzungId);
            sitzungNode.setProperty("wahlperiode", wahlperiode);
            sitzungNode.setProperty("sitzungNr", sitzungNr);
            sitzungNode.setProperty("ort", ort != null && !ort.isEmpty() ? ort : "Berlin");

            // Zeitdaten parsen und speichern
            parseTimeDataFast(root, sitzungNode);

            sitzungCache.put(sitzungKey, sitzungNode);
        }


        // Alle Reden in der Datei verarbeiten
        NodeList redeElements = root.getElementsByTagName("rede");
        for (int i = 0; i < redeElements.getLength(); i++) {
            Element redeElement = (Element) redeElements.item(i);
            int[] counts = processRedeComplete(tx, redeElement, sitzungNode, rednerCache, fraktionCache, redeCache);
            redeCount += counts[0];
            kommentarCount += counts[1];
        }

        return new int[]{redeCount, kommentarCount};
    }


    /**
     * Parsing Zeitdaten schnell aus XML
     * Und setzt die auf Sitzungs Node
     */
    private void parseTimeDataFast(Element root, Node sitzungNode) {
        try {

            Sitzung sitzung = XMLHelper.parseSitzung(root);

            if (sitzung != null) {

                if (sitzung.getSitzungDatum() != null) {
                    String datum = new SimpleDateFormat("dd.MM.yyyy").format(sitzung.getSitzungDatum());
                    sitzungNode.setProperty("datum", datum);
                }
                if (sitzung.getStartZeit() != null) {
                    sitzungNode.setProperty("startzeit", sitzung.getStartZeit().toString());
                }
                if (sitzung.getEndZeit() != null) {
                    sitzungNode.setProperty("endzeit", sitzung.getEndZeit().toString());
                }

            }


        } catch (Exception e) {
            //
        }
    }


    /**
     * Verarbeitet eine einzelne Rede im High Performance (Schnell)
     */
    private int[] processRedeComplete(Transaction tx, Element redeElement, Node sitzungNode,
                                      Map<String, Node> rednerCache,
                                      Map<String, Node> fraktionCache,
                                      Map<String, Node> redeCache) {

        int kommentarCount = 0;

        try {
            String redeId = redeElement.getAttribute("id");
            if (redeId == null || redeId.isEmpty()) {
                return new int[]{0, 0};
            }

            String redeKey = "REDE_" + redeId;
            if (redeCache.containsKey(redeKey)) {
                return new int[]{0, 0};
            }

            // Rede-Node erstellen
            Node redeNode = tx.createNode(Label.label("Rede"));
            redeNode.setProperty("id", redeKey);
            redeNode.setProperty("redeId", redeId);
            redeCache.put(redeKey, redeNode);

            // Add Redetext
            String redeText = extractRedeTextSimple(redeElement);
            if (!redeText.isEmpty()) {
                redeNode.setProperty("text", redeText);
                redeNode.setProperty("textLength", redeText.length());
            }

            // Erstellen Relationships zur Sitzung
            redeNode.createRelationshipTo(sitzungNode, RelationshipType.withName("IN_SITZUNG"));

            // Redner Verarbeiten
            Element rednerElement = (Element) redeElement.getElementsByTagName("redner").item(0);
            if (rednerElement != null) {
                Node rednerNode = processRednerFast(tx, rednerElement, rednerCache, fraktionCache);
                if (rednerNode != null) {
                    redeNode.createRelationshipTo(rednerNode, RelationshipType.withName("VON_REDNER"));
                }
            }

            // Kommentar mit Nodes
            kommentarCount = processKommentareWithNodes(tx, redeElement, redeNode);

            return new int[]{1, kommentarCount};

        } catch (Exception e) {
            return new int[]{0, 0};
        }
    }


    /**
     * Verarbeitet Kommentare und erstellt Kommentar Nodes
     */
    private int processKommentareWithNodes(Transaction tx, Element redeElement, Node redeNode) {
        int kommentarCount = 0;

        try {
            // Kommentare innerhalb des Rede Elements
            NodeList kommentarElements = redeElement.getElementsByTagName("kommentar");
            for (int i = 0; i < kommentarElements.getLength(); i++) {
                Element kommentarElement = (Element) kommentarElements.item(i);
                String kommentarText = kommentarElement.getTextContent().trim();
                if (!kommentarText.isEmpty()) {
                    createKommentarNode(tx, kommentarText, redeNode);
                    kommentarCount++;
                }
            }

            // Benchbarte Kimmentar
            Element parent = (Element) redeElement.getParentNode();
            if (parent != null) {
                NodeList siblings = parent.getChildNodes();
                boolean foundRede = false;
                for (int i = 0; i < siblings.getLength(); i++) {
                    if (siblings.item(i) == redeElement) {
                        foundRede = true;
                        continue;
                    }
                    if (foundRede && siblings.item(i) instanceof Element) {
                        Element sibling = (Element) siblings.item(i);
                        if ("kommentar".equals(sibling.getTagName())) {
                            String kommentarText = sibling.getTextContent().trim();
                            if (!kommentarText.isEmpty()) {
                                createKommentarNode(tx, kommentarText, redeNode);
                                kommentarCount++;
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            //
        }

        return kommentarCount;
    }

    /**
     * Erstellt einen Kommentar Node
     */
    private void createKommentarNode(Transaction tx, String kommentarText, Node redeNode) {
        try {
            String kommentarId = "KOMMENTAR_" + UUID.randomUUID().toString();
            Node kommentarNode = tx.createNode(Label.label("Kommentar"));
            kommentarNode.setProperty("id", kommentarId);
            kommentarNode.setProperty("text", kommentarText);
            kommentarNode.setProperty("textLength", kommentarText.length());

            // Relationship zur Rede erstellen
            kommentarNode.createRelationshipTo(redeNode, RelationshipType.withName("GEHOERT_ZU_REDE"));
        } catch (Exception e) {
            //
        }
    }

    /**
     * Extrahiert Redetext aus XML-Element
     */
    private String extractRedeTextSimple(Element redeElement) {
        StringBuilder text = new StringBuilder();

        String fullText = redeElement.getTextContent().trim();

        // Entferne Redner Text
        NodeList rednerElements = redeElement.getElementsByTagName("redner");
        for (int i = 0; i < rednerElements.getLength(); i++) {
            String rednerText = rednerElements.item(i).getTextContent().trim();
            fullText = fullText.replace(rednerText, "");
        }

        // Entferne Kommentar Text
        NodeList kommentarElements = redeElement.getElementsByTagName("kommentar");
        for (int i = 0; i < kommentarElements.getLength(); i++) {
            String kommentarText = kommentarElements.item(i).getTextContent().trim();
            fullText = fullText.replace(kommentarText, "");
        }

        return fullText.replaceAll("\\s+", " ").trim();
    }


    /**
     * Verarbeitet Redner schnell
     */
    private Node processRednerFast(Transaction tx, Element rednerElement,
                                   Map<String, Node> rednerCache,
                                   Map<String, Node> fraktionCache) {

        // 1. BACA DATA DULU
        String rednerId = rednerElement.getAttribute("id");
        Element nameElement = (Element) rednerElement.getElementsByTagName("name").item(0);
        String vorname = getTextContentFast(nameElement, "vorname");
        String nachname = getTextContentFast(nameElement, "nachname");

        if (vorname == null || nachname == null) return null;

        // 2. BUAT CACHE KEY YANG BENER
        String nameKey = (vorname + "_" + nachname).toLowerCase().trim();
        String cacheKey = (rednerId != null && !rednerId.isEmpty()) ?
                rednerId : "NAME_" + nameKey;

        // 3. SEARCH BY NAME JIKA ID TIDAK ADA
        if ((rednerId == null || rednerId.isEmpty()) && !rednerCache.containsKey(cacheKey)) {
            // Coba cari di cache yang sudah ada berdasarkan nama
            for (Map.Entry<String, Node> entry : rednerCache.entrySet()) {
                Node existing = entry.getValue();
                if (existing.hasProperty("vorname") && existing.hasProperty("nachname")) {
                    String existingVorname = (String) existing.getProperty("vorname");
                    String existingNachname = (String) existing.getProperty("nachname");
                    if (vorname.equalsIgnoreCase(existingVorname) &&
                            nachname.equalsIgnoreCase(existingNachname)) {
                        return existing; // Pakai yang sudah ada
                    }
                }
            }
        }

        // 4. CHECK CACHE
        Node rednerNode = rednerCache.get(cacheKey);
        if (rednerNode != null) return rednerNode;

        // 5. BUAT NODE BARU
        rednerNode = tx.createNode(Label.label("Redner"));
        if (rednerId != null && !rednerId.isEmpty()) {
            rednerNode.setProperty("id", rednerId);
        }
        rednerNode.setProperty("vorname", vorname);
        rednerNode.setProperty("nachname", nachname);
        rednerNode.setProperty("fullName", vorname + " " + nachname);

        // 6. UPDATE CACHE DENGAN SEMUA KEY YANG MUNGKIN
        rednerCache.put(cacheKey, rednerNode);
        rednerCache.put("NAME_" + nameKey, rednerNode); // Cache by name juga

        // 7. PROCESS FRAKTION...
        String fraktionName = getTextContentFast(nameElement, "fraktion");
        if (fraktionName != null && !fraktionName.isEmpty()) {
            Node fraktionNode = getOrCreateFraktionFast(tx, fraktionName, fraktionCache);
            if (fraktionNode != null) {
                rednerNode.createRelationshipTo(fraktionNode, RelationshipType.withName("MITGLIED_VON"));
            }
        }

        return rednerNode;
    }


    /**
     * Findet oder erstellt Fraktion schnell
     * @param tx   Neo4j Transaction für Datenbankoperationen
     * @param fraktionName   der Raw Name der Fraktion aus XML
     * @param fraktionCache  cache-Map für Fraktions-Nodes
     * @return  gefundene Fraktion im Cache oder neu erstellt wurde
     */
    private Node getOrCreateFraktionFast(Transaction tx, String fraktionName, Map<String, Node> fraktionCache) {
        try {
            // Normalize name
            String normalized = normalizeFraktionNameFast(fraktionName);

            // Check cache
            Node fraktionNode = fraktionCache.get(normalized);
            if (fraktionNode != null) {
                return fraktionNode;
            }

            // Neue Fraktion erstellen
            fraktionNode = tx.createNode(Label.label("Fraktion"));
            fraktionNode.setProperty("name", normalized);
            fraktionCache.put(normalized, fraktionNode);

            return fraktionNode;

        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Normalize Fraktionname
     * @param oriName original FraktionName
     * @return Fraktionname, die schon normalisiert ist
     */
    private String normalizeFraktionNameFast(String oriName) {
        return NormalizeFraktion.normalizeFraktion(oriName);
    }


    /**
     * Hilfsmethode zum Extrahieren von Textinhalt aus XML-Elementen
     * @param parent  das übergeordnete XML-Element
     * @param tagName  der Name des gesuchten Tags
     * @return  den extrahierten Text als String oder null
     */
    private String getTextContentFast(Element parent, String tagName) {
        try {
            NodeList elements = parent.getElementsByTagName(tagName);
            if (elements.getLength() > 0) {
                return elements.item(0).getTextContent().trim();
            }
        } catch (Exception e) {
            //
        }
        return null;
    }

}
