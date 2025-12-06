package org.texttechnologylab.ppr.blatt2.data.statistik;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.texttechnologylab.ppr.blatt2.data.database.Neo4jConnection;
import org.texttechnologylab.ppr.blatt2.data.redenportal.*;

import java.util.Map;

import static org.texttechnologylab.ppr.blatt2.data.helper.NormalizeFraktion.normalizeFraktion;

/**
 * Führt die statistische Analyse gemäß der Aufgabenstellung durch.
 * Enthält Auswertungen zu Redelängen, Kommentarhäufigkeit sowie zur längsten Sitzung.
 */
public class CypherStatistik {

    //Zugriff auf die Neo4j-Datenbank
    private final GraphDatabaseService graphDb;


    public CypherStatistik(Neo4jConnection db) {
        this.graphDb = db.getGraphDb();
    }


    /**
     * Führt alle statistischen Auswertungen nacheinander aus.
     */
    public void runAllAnalysis() {
        System.out.println("\n" + "=".repeat(180));
        System.out.println("AUFGABE 4 - STATISTISCHE AUSWERTUNG");
        System.out.println("=".repeat(180));

        normalizeAllFraktionen();

        // a. Durchschnittliche Redelänge
        analyseRedelaenge();

        // b. Kommentar-Häufigkeit pro Rede
        analyseKommentarHaeufigkeit();

        // c. Längste Sitzung
        analyseLaengsteSitzung();

    }

    /**
     * Durchschnittliche Redelänge pro Redner/Abgeordneten als auch pro Fraktion
     */
    private void analyseRedelaenge() {
        System.out.println("\n" + "\n" + "A. DURCHSCHNITTLICHE REDELÄNGE");
        System.out.println("═".repeat(180));

        // Pro Abgeordneten
        System.out.println("\n ### PRO ABGEORDNETEN ###");
        System.out.println("-".repeat(90));

        String queryAbgeordnete = """
            MATCH (redner:Redner)<-[:VON_REDNER]-(r:Rede)
            WHERE r.text IS NOT NULL AND r.textLength > 0
            WITH redner.vorname + ' ' + redner.nachname as fullName,
                 redner,
                 r.textLength as laenge
            WITH fullName,
                 avg(laenge) as avgLaenge,
                 count(laenge) as anzahlReden,
                 sum(laenge) as totalLaenge,
                 collect(distinct [(redner)-[:MITGLIED_VON]->(f) | f.name][0]) as fraktionen
            RETURN fullName as name,
                   CASE 
                     WHEN size([f in fraktionen WHERE f <> "Fraktionslos" AND f <> "FRACTIONLESS" AND f <> "FRAKTIONSLOS"]) > 0 
                     THEN [f in fraktionen WHERE f <> "Fraktionslos" AND f <> "FRACTIONLESS" AND f <> "FRAKTIONSLOS"][0]
                     ELSE COALESCE(fraktionen[0], "Fraktionslos")
                   END as fraktion,
                   round(avgLaenge) as durchschnitt,
                   anzahlReden,
                   totalLaenge
            ORDER BY durchschnitt DESC
            LIMIT 15
            """;

        try (Transaction tx = graphDb.beginTx()) {
            Result result = tx.execute(queryAbgeordnete);
            int position = 1;
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                String name = safeGetString(row, "name");
                String rawFraktion = safeGetString(row, "fraktion");
                String fraktion = normalizeFraktionDisplay(rawFraktion);

                System.out.printf("%2d. %-30s (%-20s) %6.0f Zeichen (%d Reden)%n",
                        position++,
                        cleanString(name, 30),
                        cleanString(fraktion, 20),
                        safeGetDouble(row, "durchschnitt"),
                        safeGetLong(row, "anzahlReden"));
            }
            tx.commit();
        }

        // Pro Fraktion
        System.out.println("\n ### PRO FRAKTION ###");
        System.out.println("-".repeat(90));

        String queryFraktionen = """
            MATCH (f:Fraktion)<-[:MITGLIED_VON]-(:Redner)<-[:VON_REDNER]-(r:Rede)
            WHERE r.text IS NOT NULL AND r.textLength > 0
            WITH\s
             CASE\s
               WHEN f.name CONTAINS "CDU" OR f.name CONTAINS "CSU" THEN "CDU/CSU"
               WHEN f.name CONTAINS "BÜNDNIS" OR f.name CONTAINS "GRÜN" OR f.name CONTAINS "GRUEN" THEN "BÜNDNIS 90/DIE GRÜNEN"\s
               WHEN f.name CONTAINS "SPD" THEN "SPD"
               WHEN f.name CONTAINS "FDP" THEN "FDP"\s
               WHEN f.name CONTAINS "LINKE" THEN "DIE LINKE"
               WHEN f.name CONTAINS "AFD" OR f.name CONTAINS "ALTERNATIVE" THEN "AfD"
               WHEN f.name CONTAINS "BSW" THEN "BSW"
               ELSE COALESCE(f.name, "Fraktionslos")
             END as fraktionName,
             avg(r.textLength) as avgLaenge,
             count(r) as anzahlReden,
             sum(r.textLength) as totalLaenge
            RETURN fraktionName as fraktion,
                   round(avgLaenge) as durchschnitt,
                   anzahlReden,
                   totalLaenge
            ORDER BY anzahlReden DESC
            LIMIT 15
            """;


        try (Transaction tx = graphDb.beginTx()) {
            Result result = tx.execute(queryFraktionen);
            int position = 1;
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                String fraktion = normalizeFraktionDisplay(safeGetString(row, "fraktion"));

                System.out.printf("%2d. %-30s %6.0f Zeichen (%d Reden)%n",
                        position++,
                        cleanString(fraktion, 30),
                        safeGetDouble(row, "durchschnitt"),
                        safeGetLong(row, "anzahlReden"));
            }
            tx.commit();
        }
    }

    /**
     * Analyse Kommentarhäufigkeit pro Rede,
     * sowohl pro Redner als auch pro Fraktion.
     */
    private void analyseKommentarHaeufigkeit() {
        System.out.println("\n" + "\n" +"B. KOMMENTAR-HÄUFIGKEIT PRO REDE");
        System.out.println("═".repeat(180));

        // Pro Abgeordnete
        System.out.println("\n ### PRO ABGEORDNETEN ###");
        System.out.println("-".repeat(90));

        String queryAbgeordnete = """
            MATCH (redner:Redner)<-[:VON_REDNER]-(r:Rede)
            OPTIONAL MATCH (r)<-[:GEHOERT_ZU_REDE]-(k:Kommentar)
            WITH redner.vorname + ' ' + redner.nachname as fullName,
                 redner,
                 r,
                 count(k) as kommentare
            WITH fullName,
                 avg(kommentare) as avgKommentare,
                 count(r) as anzahlReden,
                 sum(kommentare) as totalKommentare,
                 collect(distinct [(redner)-[:MITGLIED_VON]->(f) | f.name][0]) as fraktionen
            RETURN fullName as name,
                   CASE 
                     WHEN size([f in fraktionen WHERE f <> "Fraktionslos" AND f <> "FRACTIONLESS" AND f <> "FRAKTIONSLOS"]) > 0 
                     THEN [f in fraktionen WHERE f <> "Fraktionslos" AND f <> "FRACTIONLESS" AND f <> "FRAKTIONSLOS"][0]
                     ELSE COALESCE(fraktionen[0], "Fraktionslos")
                   END as fraktion,
                   round(avgKommentare, 1) as durchschnitt,
                   anzahlReden,
                   totalKommentare
            ORDER BY durchschnitt DESC
            LIMIT 15
            """;

        try (Transaction tx = graphDb.beginTx()) {
            Result result = tx.execute(queryAbgeordnete);
            int position = 1;
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                String name = safeGetString(row, "name");
                String rawFraktion = safeGetString(row, "fraktion");
                String fraktion = normalizeFraktionDisplay(rawFraktion);

                System.out.printf("%2d. %-30s (%-20s) %5.1f Kommentare/Rede (%d total)%n",
                        position++,
                        cleanString(name, 30),
                        cleanString(fraktion, 20),
                        safeGetDouble(row, "durchschnitt"),
                        safeGetLong(row, "totalKommentare"));
            }
            tx.commit();
        }

        // Pro Fraktion
        System.out.println("\n ### PRO FRAKTION ###");
        System.out.println("-".repeat(90));

        String queryFraktionen = """
            MATCH (f:Fraktion)<-[:MITGLIED_VON]-(:Redner)<-[:VON_REDNER]-(r:Rede)
            OPTIONAL MATCH (r)<-[:GEHOERT_ZU_REDE]-(k:Kommentar)
            WITH f, r, count(k) AS kommentare
            
            WITH 
              CASE 
                WHEN f.name IS NULL THEN "Fraktionslos"
                WHEN toLower(f.name) CONTAINS "cdu" OR toLower(f.name) CONTAINS "csu" THEN "CDU/CSU"
                WHEN toLower(f.name) CONTAINS "bündnis" OR toLower(f.name) CONTAINS "grün" OR toLower(f.name) CONTAINS "gruen" THEN "BÜNDNIS 90/DIE GRÜNEN"
                WHEN toLower(f.name) CONTAINS "spd" THEN "SPD"
                WHEN toLower(f.name) CONTAINS "fdp" THEN "FDP"
                WHEN toLower(f.name) CONTAINS "linke" THEN "DIE LINKE" 
                WHEN toLower(f.name) CONTAINS "afd" OR toLower(f.name) CONTAINS "alternative" THEN "AfD"
                WHEN toLower(f.name) CONTAINS "bsw" THEN "BSW"
                ELSE f.name
              END AS fraktionName,
              kommentare
            
            WITH fraktionName,
                 count(*) AS anzahlReden,
                 sum(kommentare) AS totalKommentare,
                 avg(kommentare * 1.0) AS avgKommentare
            
            WHERE fraktionName <> "Fraktionslos"
            RETURN fraktionName AS fraktion,
                   round(avgKommentare, 1) AS durchschnitt,
                   anzahlReden,
                   totalKommentare
            ORDER BY durchschnitt DESC
            LIMIT 15
            """;

        try (Transaction tx = graphDb.beginTx()) {
            Result result = tx.execute(queryFraktionen);
            int position = 1;
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                String fraktion = normalizeFraktionDisplay(safeGetString(row, "fraktion"));

                System.out.printf("%2d. %-30s %5.1f Kommentare/Rede (%d total)%n",
                        position++,
                        cleanString(fraktion, 30),
                        safeGetDouble(row, "durchschnitt"),
                        safeGetLong(row, "totalKommentare"));
            }
            tx.commit();
        }
    }

    /**
     * Bestimmt die längste Sitzung,
     * einmal basierend auf Zeitdauer,
     * und einmal auf Textumfang aller Reden
     */
    private void analyseLaengsteSitzung() {
        System.out.println("\n" + "\n" + "C. LÄNGSTE SITZUNG");
        System.out.println("═".repeat(180));

        // c. Bezüglich der Zeit
        System.out.println("\n ### BEZÜGLICH DER ZEIT ###");
        System.out.println("-".repeat(80));

        try (Transaction tx = graphDb.beginTx()) {

            String queryLaengsteNachZeit = """
               MATCH (s:Sitzung)
               WHERE s.startzeit IS NOT NULL AND s.endzeit IS NOT NULL
               WITH s,\s
                    s.startzeit AS startZeit,
                    s.endzeit AS endZeit
               WITH s,
                    startZeit,
                    endZeit,
                    substring(startZeit, 0, 2) AS startStunde,
                    substring(startZeit, 3, 2) AS startMinute,
                    substring(endZeit, 0, 2) AS endStunde,\s
                    substring(endZeit, 3, 2) AS endMinute
               WITH s,
                    toInteger(startStunde) * 60 + toInteger(startMinute) AS startTotal,
                    toInteger(endStunde) * 60 + toInteger(endMinute) AS endTotal
               WITH s,
                   CASE
                      WHEN endTotal >= startTotal THEN endTotal - startTotal
                      ELSE (24 * 60 - startTotal) + endTotal
                      END AS dauer
               RETURN s.wahlperiode AS wp,
                    s.sitzungNr AS nr,\s
                    s.ort AS ort,
                    s.datum AS datum,
                    s.startzeit AS start,
                    s.endzeit AS ende,
                    dauer AS dauerMinuten
               ORDER BY dauerMinuten DESC
               LIMIT 1
               """;

            Result result = tx.execute(queryLaengsteNachZeit);
            if (result.hasNext()) {
                Map<String, Object> row = result.next();
                System.out.println("LÄNGSTE SITZUNG NACH ZEITDAUER");
                System.out.println(" - Sitzung: WP " + row.get("wp") + ", Nr. " + row.get("nr"));
                System.out.println(" - Ort: " + row.get("ort"));
                System.out.println(" - Zeit: " + row.get("start") + " - " + row.get("ende"));
                System.out.println(" - Datum: " + row.get("datum"));
                System.out.println(" - Dauer: " + safeGetLong(row, "dauerMinuten") + " Minuten");
            } else {
                System.out.println("Keine Sitzungen mit Zeitdaten gefunden");
            }
            tx.commit();
        }

        // Bezüglich der Gesamtlänge aller Reden
        System.out.println("\n ## BEZÜGLICH DER GESAMTLÄNGE ALLER REDEN ##:");
        System.out.println("-".repeat(80));

        try (Transaction tx = graphDb.beginTx()) {
            String queryLaengsteNachLaenge = """
            MATCH (s:Sitzung)<-[:IN_SITZUNG]-(r:Rede)
            WHERE r.text IS NOT NULL AND size(r.text) > 0
            WITH s, 
                 reduce(total = 0, rede IN collect(r) | total + size(rede.text)) as gesamtlaenge,
                 count(r) as anzahlReden
            RETURN s.wahlperiode as wp, 
                   s.sitzungNr as nr,
                   s.ort as ort,
                   gesamtlaenge,
                   anzahlReden
            ORDER BY gesamtlaenge DESC
            LIMIT 1
            """;

            Result result = tx.execute(queryLaengsteNachLaenge);
            if (result.hasNext()) {
                Map<String, Object> row = result.next();
                System.out.println("LÄNGSTE SITZUNG NACH GESAMTTEXTLÄNGE:");
                System.out.printf(" Sitzung: WP %s, Nr. %s (%s)%n",
                        safeGetString(row, "wp"),
                        safeGetString(row, "nr"),
                        safeGetString(row, "ort"));
                System.out.printf(" %,d Zeichen in %d Reden%n",
                        safeGetLong(row, "gesamtlaenge"),
                        safeGetLong(row, "anzahlReden"));
            }
            tx.commit();
        }

    }


    /**
     * Holt einen String Wert aus der Map.
     * @param row  die Map mit den Werten
     * @param key  der Name des gesuchten Feldes
     * @return  der Wert als String oder null
     */
    private String safeGetString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? value.toString() : null;
    }


    /**
     * Holt einen Long Wert aus der Map.
     * Unterstützt auch Integer Werte.
     * @param row  die Map mit den Werten
     * @param key  der Name des gesuchten Feldes
     * @return  der Wert als Long oder 0
     */
    private Long safeGetLong(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        return 0L;
    }


    /**
     * Holt einen Double Wert aus der Map.
     * Unterstützt Double, Long und Integer.
     * @param row  die Map mit den Werten
     * @param key  der Name des gesuchten Feldes
     * @return  der Wert als Double oder 0.0
     */
    private Double safeGetDouble(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Double) return (Double) value;
        if (value instanceof Long) return ((Long) value).doubleValue();
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        return 0.0;
    }

    // Normalisieren Fraktion Name
    private String normalizeFraktionDisplay(String fraktionName) {
        return normalizeFraktion(fraktionName);
    }

    public void normalizeAllFraktionen() {
        System.out.println("Normalizing fraktion names...");

        try (Transaction tx = graphDb.beginTx()) {
            String normalizeQuery = """
            MATCH (f:Fraktion)
            WHERE toUpper(f.name) CONTAINS "FRAKTIONSLOS" 
               OR toUpper(f.name) CONTAINS "FRACTIONLESS"
               OR f.name =~ '(?i).*fraktionslos.*'
            SET f.name = "FRAKTIONSLOS"
            """;

            Result result = tx.execute(normalizeQuery);
            System.out.println("# Fraktion names normalized in database #");
            tx.commit();
        }
    }

    /**
     * Kürzen String für die Ausgabe
     */
    private String cleanString(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}




