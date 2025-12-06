package org.texttechnologylab.ppr.blatt2.data.redenportal;

import org.json.JSONObject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.texttechnologylab.ppr.blatt2.data.database.Neo4jConnection;
import org.texttechnologylab.ppr.blatt2.data.interfaces.SitzungIn;

import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Repräsentiert Sitzung im Reden Portal
 * Enthält der Ort, Datum, Zeitbeginn und Zeitende, Sitzungnummer, und WP(Wahlperiode)
 */
public class Sitzung implements SitzungIn {
    private String sitzungOrt;
    private Date sitzungDatum;
    private LocalTime startZeit;
    private LocalTime endZeit;
    private String wahlperiode;
    private String sitzungNr;

    public Sitzung(String wahlperiode, String sitzungNr, String sitzungOrt, Date sitzungDatum, LocalTime startZeit, LocalTime endZeit) {
        this.wahlperiode = wahlperiode;
        this.sitzungNr = sitzungNr;
        this.sitzungOrt = sitzungOrt;
        this.sitzungDatum = sitzungDatum;
        this.startZeit = startZeit;
        this.endZeit = endZeit;
    }

    // Method zu generate sitzungId
    private String genSitzungId() {
        String dateStr = (sitzungDatum != null) ? sitzungDatum.toString().replace("-", "").replace(":", "") : "unknown";
        return wahlperiode + "_" + sitzungNr + "_" + dateStr;
    }

    // Getter methods
    public String getWahlperiode() { return wahlperiode; }
    public String getSitzungNr() { return sitzungNr; }
    public String getSitzungOrt() { return sitzungOrt; }
    public Date getSitzungDatum() { return sitzungDatum; }
    public LocalTime getStartZeit() { return startZeit; }
    public LocalTime getEndZeit() { return endZeit; }

    // toString()
    @Override
    public String toString() {
        return "Sitzung " + sitzungNr + " (WP " + wahlperiode + ") am " + sitzungDatum + " in " + sitzungOrt +
                "\nBeginn: " + startZeit + "; Ende: " + endZeit;
    }

    // toJSON()
    public String toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", "Sitzung");
        json.put("sitzungId", genSitzungId());
        json.put("wahlperiode", wahlperiode != null ? wahlperiode : "");
        json.put("sitzungNr", sitzungNr != null ? sitzungNr : "");
        json.put("ort", sitzungOrt != null ? sitzungOrt : "");

        // Date Format
        if (sitzungDatum != null) {
            json.put("datum", sitzungDatum.toString());
        } else {
            json.put("datum", "");
        }

        // Zeit
        json.put("startZeit", startZeit != null ? startZeit.toString() : "");
        json.put("endZeit", endZeit != null ? endZeit.toString() : "");

        // Zeitdauer
        if (startZeit != null && endZeit != null) {
            long durationMinutes = java.time.Duration.between(startZeit, endZeit).toMinutes();
            json.put("dauerMinuten", durationMinutes);
        } else {
            json.put("dauerMinuten", JSONObject.NULL);
        }

        return json.toString(2); // indentasi 2 spasi untuk format rapi
    }


    /**
     * Speichert Sitzung als Neo4j Knoten-Kanten mit Beziehungen
     * @param db database
     * @param tx transaction
     * @return
     */
    @Override
    public Node toNode(Neo4jConnection db, Transaction tx) {
        if (db == null || sitzungOrt == null) {
            return null;
        }

        String sitzungId = genSitzungId();

        Map<String, Object> props = new HashMap<>();
        props.put("sitzungId", sitzungId);
        props.put("wahlperiode", wahlperiode);
        props.put("sitzungNr", sitzungNr);
        props.put("ort", sitzungOrt);
        props.put("datum", sitzungDatum != null ? sitzungDatum.toString() : "");
        props.put("startZeit", startZeit != null ? startZeit.toString() : "");
        props.put("endZeit", endZeit != null ? endZeit.toString() : "");

        // Zeitdauer
        if (startZeit != null && endZeit != null) {
            long durationMinutes = java.time.Duration.between(startZeit, endZeit).toMinutes();
            props.put("dauerMinuten", durationMinutes);
        }

        return db.findOrCreateNode(tx, "Sitzung", "sitzungId", sitzungId, props);
    }

    /**
     * Erstellt Sitzung Objekt aus einem Neo4j Knoten.
     * @param sitzungNode
     * @return Sitzung Objekt
     */
    public static Sitzung fromNode(Node sitzungNode) {
        if (sitzungNode == null) return null;

        //Property aus Neo4j Knoten lesen
        String wahlperiode = (String) sitzungNode.getProperty("wahlperiode", "");
        String sitzungNr = (String) sitzungNode.getProperty("sitzungNr", "");
        String sitzungOrt = (String) sitzungNode.getProperty("sitzungOrt", "");

        Date datum = (Date) sitzungNode.getProperty("datum", null);

        LocalTime startZeit = null;
        LocalTime endZeit = null;
        try {
            if (sitzungNode.hasProperty("startZeit")) {
                startZeit = LocalTime.parse((String) sitzungNode.getProperty("startZeit"));
            }
            if (sitzungNode.hasProperty("endZeit")) {
                endZeit = LocalTime.parse((String) sitzungNode.getProperty("endZeit"));
            }
        } catch (Exception e) {
            //
        }

        return new Sitzung(wahlperiode, sitzungNr, sitzungOrt, datum, startZeit, endZeit);
    }


}