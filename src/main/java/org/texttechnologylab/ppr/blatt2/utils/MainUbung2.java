package org.texttechnologylab.ppr.blatt2.utils;

import org.texttechnologylab.ppr.blatt2.data.database.Neo4jConnection;
import org.texttechnologylab.ppr.blatt2.data.redenportal.ObjectFactory;
import org.texttechnologylab.ppr.blatt2.data.redenportal.Rede;
import org.texttechnologylab.ppr.blatt2.data.redenportal.Redner;
import org.texttechnologylab.ppr.blatt2.data.redenportal.Sitzung;
import org.texttechnologylab.ppr.blatt2.data.statistik.CypherStatistik;

import java.util.Scanner;


/**
 * Lädt XML-Daten, zeigt Parsing-Ergebnisse an und führt statistische Auswertungen durch
 * Benutzer kann über das Menü wählen, ob er Parsing-Ergebnisse, Statistiken oder beides sehen möchte
 */
public class MainUbung2 {

    /**
     * Lädt XML-Dateien aus dem angegebenen Ordner, zeigt eine Datenübersicht
     * und bietet ein Menü für detaillierte Parsing Ergebnisse oder statistische Auswertung oder beides
     * @param args
     */
    public static void main(String[] args) {
        String folderPath = "src/main/resources/20";
        Scanner scanner = new Scanner(System.in);

        System.out.println("-".repeat(180));
        System.out.println("XML-Daten werden geladen von: " + folderPath);
        System.out.println("Bitte kurz warten ;)");
        System.out.println("-".repeat(180));

        ObjectFactory factory = new ObjectFactory();
        factory.loadXML(folderPath);

        // Datenübersicht
        System.out.println();
        System.out.println("=".repeat(180));
        System.out.println(" ".repeat(60) + "DATENÜBERSICHT");
        System.out.println("=".repeat(180));
        System.out.printf(" ".repeat(60) + "Anzahl Reden: %d%n", factory.getAlleRede().size());
        System.out.printf(" ".repeat(60) + "Anzahl Redner: %d%n", factory.getAlleRedner().size());
        System.out.printf(" ".repeat(60) + "Anzahl Sitzungen: %d%n", factory.getAlleSitzung().size());
        System.out.printf(" ".repeat(60) + "Anzahl Fraktionen: %d%n", factory.getAlleFraktion().size());
        System.out.printf(" ".repeat(60) + "Anzahl Kommentare: %d%n", factory.getAlleKommentar().size());
        System.out.println("=".repeat(180));


        boolean exit = false;
        while (!exit) {
            System.out.println("\n" + "═".repeat(180));
            System.out.println(" ".repeat(60) + "WAS MÖCHTEN SIE ANGÜCKEN?");
            System.out.println("═".repeat(180));
            System.out.println(" ".repeat(60) + "1. Detaillierte Parsing-Ergebnisse anzeigen");
            System.out.println(" ".repeat(60) + "2. Statistische Auswertung (Aufgabe 4) ausführen");
            System.out.println(" ".repeat(60) + "3. Beides anzeigen");
            System.out.println(" ".repeat(60) + "4. Programm beenden");
            System.out.print(" ".repeat(60) + "\n Ihre Auswahl Bitte (1 oder 2 oder 3 oder 4): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    showDetailedParsing(factory);
                    break;
                case "2":
                    runStatisticalAnalysis(folderPath);
                    break;
                case "3":
                    showDetailedParsing(factory);
                    runStatisticalAnalysis(folderPath);
                    break;
                case "4":
                    exit = true;
                    System.out.println("\n Yeyyyyyy! Vielen Dank, ciaooooooo !!!!");
                    break;
                default:
                    System.out.println("Oh Nein! Ungültige Eingabe! Bitte 1-4 wählen.");
            }

            if (!exit && !choice.equals("4")) {
                System.out.print("\n Drücken Sie Enter um zum Menü zurückzukehren...");
                scanner.nextLine();
            }
        }

        scanner.close();

        System.out.println("\n" + "=".repeat(180));
        System.out.println( " ".repeat(60)+ "PROGRAMM WURDE BEENDET");
        System.out.println("=".repeat(180));
    }

    /**
     * Zeigt detaillierte Parsing-Ergebnisse an
     * @param factory  ObjectFactory, die die geladenen Daten enthält
     */
    private static void showDetailedParsing(ObjectFactory factory) {
        System.out.println("\n" + "═".repeat(120));
        System.out.println("VERARBEITETE DATEN (PARSING-ERGEBNISSE)");
        System.out.println("═".repeat(120));

        int count = 0;
        for (Rede rede : factory.getAlleRede()) {
            if (count >= 214) break;

            System.out.println();
            System.out.println((count + 1) + " .  REDE ID: " + rede.getRid());
            System.out.println("-".repeat(80));

            // Redner Information
            if (rede.getRedner() != null) {
                Redner redner = rede.getRedner();
                System.out.printf("  Redner: %s %s %s%n",
                        redner.getTitel() != null ? redner.getTitel() + "." : "",
                        redner.getVorname(),
                        redner.getNachname());

                if (redner.getFraktion() != null) {
                    System.out.printf("  Fraktion: %s%n", redner.getFraktion().getName());
                }
            } else {
                System.out.println("  Redner: Keine Daten");
            }

            // Sitzung Information
            if (rede.getSitzung() != null) {
                Sitzung sitzung = rede.getSitzung();

                System.out.printf("  Sitzung: WP %s, Sitzung %s%n",
                        sitzung.getWahlperiode(), sitzung.getSitzungNr());

                if (sitzung.getSitzungDatum() != null) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy");
                    String d = sdf.format(sitzung.getSitzungDatum());
                    System.out.printf("  Datum: %s%n", d);
                }

                if (sitzung.getStartZeit() != null && sitzung.getEndZeit() != null) {
                    System.out.printf("  Zeit: %s bis %s%n",
                            sitzung.getStartZeit(), sitzung.getEndZeit());
                }
            }

            // Kommentar
            System.out.printf("Kommentare: %d%n", rede.getKommentar().size());

            if (!rede.getKommentar().isEmpty()) {
                System.out.println("Die Kommentare:");
                for (int i = 0; i < Math.min(50, rede.getKommentar().size()); i++) {
                    String text = rede.getKommentar().get(i).getTextk();
                    String preview = text.length() > 100 ? text.substring(0, 100) + "..." : text;
                    System.out.printf("      %d. %s%n", i + 1, preview);
                }
            }

            count++;
        }

        System.out.println("\n !!!! PARSING ERGEBNISSE ANGEZEIGT !!!!");
    }

    /**
     * Führt die statistische Auswertung mit Neo4j durch
     * @param folderPath   Pfad zum Ordner mit XML
     */
    private static void runStatisticalAnalysis(String folderPath) {
        System.out.println("\n" + "═".repeat(180));
        System.out.println("STATISTISCHE AUSWERTUNG (NEO4J)");
        System.out.println("═".repeat(180));

        System.out.println("XML-Daten werden in Neo4j importiert...");

        Neo4jConnection db = new Neo4jConnection("neo4j-database");

        System.out.println("Vorhandene Daten werden entfernt...");
        db.deleteAllData();

        db.importDataComplete(folderPath);

        // Statistik ausführen
        CypherStatistik analysis = new CypherStatistik(db);
        analysis.runAllAnalysis();

        System.out.println("\n !!! STATISTISCHE AUSWERTUNG ABGESCHLOSSEN !!!");

        // Database shutdown
        db.shutdown();
    }
}
