package org.texttechnologylab.ppr.blatt2.data.redenportal;

import org.json.JSONObject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.texttechnologylab.ppr.blatt2.data.database.Neo4jConnection;
import org.texttechnologylab.ppr.blatt2.data.interfaces.RednerIn;

import java.util.HashMap;
import java.util.Map;


/**
 * Repräsentiert einer Rede im Reden Portal
 * Enthält personliche Daten der Redner und Fraktionsugehörigkeit
 */
public class Redner implements RednerIn {
    private String titel;
    private String vorname;
    private String nachname;
    private Fraktion fraktion;
    private String id;

    //Erstellt neue Redner
    public Redner(String titel, String id, String vorname, String nachname, Fraktion fraktion) {
        this.titel = titel;
        this.id = id;
        this.vorname = vorname;
        this.nachname = nachname;
        this.fraktion = fraktion;
    }

    public String getTitel() {
        return titel;
    }

    public String getId() {
        return id;
    }

    public String getVorname() {
        return vorname;
    }

    public String getNachname() {
        return nachname;
    }

    public Fraktion getFraktion() {
        return fraktion;
    }

    public Fraktion getFraktion(String fraktionName) {
        if (fraktionName != null && fraktion != null && fraktion.getName().equals(fraktionName)) {
            return fraktion;
        }
        return null;
    }

    // Vollständiger Name auch mit Titel
    public String getFullName() {
        return (titel != null && !titel.isEmpty() ? titel + " " : "") + vorname + " " + nachname;
    }

    // toString()
    @Override
    public String toString() {
        return getFullName() + " (Fraktion: " + (fraktion != null ? fraktion.getName() : "ohne Fraktion") + ")";
    }

    // toJSON()
    public String toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", "Redner");
        json.put("id", id != null ? id : "");
        json.put("titel", titel != null ? titel : "");
        json.put("vorname", vorname != null ? vorname : "");
        json.put("nachname", nachname != null ? nachname : "");
        json.put("fullName", this.getFullName());

        // Fraktkions Information
        if (fraktion != null) {
            JSONObject fraktionJson = new JSONObject();
            fraktionJson.put("name", fraktion.getName());
            json.put("fraktion", fraktionJson);
        } else {
            json.put("fraktion", JSONObject.NULL);
        }

        return json.toString(2); // indentasi 2 spasi untuk format rapi
    }

    /**
     * Speichert Redner als Neo4j Knoten-Kanten mit Beziehungen
     * @param db database
     * @param tx transaction
     * @return rednerNode
     */
    @Override
    public Node toNode(Neo4jConnection db, Transaction tx) {
        if (db == null || id == null || tx == null) return null;

        Map<String, Object> props = new HashMap<>();
        props.put("id", id);
        props.put("titel", titel != null ? titel : "");
        props.put("vorname", vorname);
        props.put("nachname", nachname);
        props.put("fullName", this.getFullName());

        Node rednerNode = db.findOrCreateNode(tx, "Redner", "id", id, props);


        // Speichern Fraktion nicht als Property, aber speichert in Beziehung
        if (fraktion != null) {
            Node fraktionNode = fraktion.toNode(db, tx);
            db.createRelationship(tx, rednerNode, fraktionNode, "MITGLIED_VON");
        }

        return rednerNode;
    }


    /**
     * Erstellt Redner Objekt aus einem Neo4j Knoten.
     * @param rednerNode
     * @return Redner Objekt
     */
    public static Redner fromNode(Node rednerNode) {
        if (rednerNode == null) return null;

        String id = (String) rednerNode.getProperty("id", null);
        String titel = (String) rednerNode.getProperty("titel", "");
        String vorname = (String) rednerNode.getProperty("vorname", "");
        String nachname = (String) rednerNode.getProperty("nachname", "");
        String fraktionName = (String) rednerNode.getProperty("fraktion", null);

        Fraktion fraktion = fraktionName != null ? new Fraktion(fraktionName) : null;

        return new Redner(titel, id, vorname, nachname, fraktion);
    }

}