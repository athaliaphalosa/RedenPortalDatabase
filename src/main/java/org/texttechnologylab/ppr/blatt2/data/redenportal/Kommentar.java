package org.texttechnologylab.ppr.blatt2.data.redenportal;

import org.json.JSONObject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.texttechnologylab.ppr.blatt2.data.database.Neo4jConnection;
import org.texttechnologylab.ppr.blatt2.data.interfaces.KommentarIn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Representation Kommentars für jede Rede
 * Jeder Kommentar besitzt Redner, Fraktion, zugehörige Rede, Kommentartext
 */
public class Kommentar implements KommentarIn {
    private Redner redner;
    private Rede rede;
    private Fraktion fraktion;
    private String textk;

    public Kommentar(Redner redner, Rede rede, Fraktion fraktion, String textk) {
        this.redner = redner;
        this.fraktion = fraktion;
        this.textk = textk;
        this.rede = rede;
    }

    public Redner getRedner() {
        return redner;
    }

    public Fraktion getFraktion() {
        return fraktion;
    }

    public String getTextk() {
        return textk;
    }

    @Override
    public Fraktion fraktion() {
        return this.fraktion;
    }

    public Rede getRede() {
        return rede;
    }

    //toString()
    @Override
    public String toString() {
        // Wenn Redner vorhanden, nehmen Nachname; sonst Not found
        String rednerName = (redner != null) ?
                redner.getVorname() + " " + redner.getNachname() : "Not found";

        // Analog wie in Redner
        String fraktionName = (fraktion != null) ? fraktion.getName() : "Not found";

        // Format for kommentar with Redner and Fraktion
        return String.format("Kommentar von %s (%s): \"%s\"",
                rednerName, fraktionName);
    }

    // toJSON()
    public String toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", "Kommentar");
        json.put("text", textk != null ? textk : "");
        json.put("textLength", textk != null ? textk.length() : 0);

        // Info Redner
        if (redner != null) {
            JSONObject rednerJson = new JSONObject();
            rednerJson.put("id", redner.getId());
            rednerJson.put("name", redner.getVorname() + " " + redner.getNachname());
            json.put("redner", rednerJson);
        }

        // Info Fraktion
        if (fraktion != null) {
            json.put("fraktion", fraktion.getName());
        }

        // Info Rede
        if (rede != null) {
            json.put("redeId", rede.getRid());
        }

        return json.toString(2); // indentasi 2 spasi untuk format rapi
    }


    /**
     * Speichert das Kommentar Objekt als Neo4j Knoten
     * Erstellt Beziehungen zum Redner und zur Rede
     *
     * @param db database
     * @param tx transaction
     * @return kommentarNode
     */
    @Override
    public Node toNode(Neo4jConnection db, Transaction tx) {
        if (db == null || tx == null || textk == null || textk.trim().isEmpty()) return null;

        String uniqueKey = "KOMMENTAR_" + UUID.randomUUID();
        Map<String, Object> props = new HashMap<>();
        props.put("id", uniqueKey);
        props.put("text", textk.trim());

        Node kommentarNode = db.createNode(tx, "Kommentar", props);

        // Beziehungen mit Rede
        if (rede != null) {
            Node redeNode = rede.toNode(db, tx);
            if (redeNode != null) {
                db.createRelationship(tx, kommentarNode, redeNode, "GEHOERT_ZU_REDE");
            }
        }

        // Beziehungen mit Rede
        if (redner != null) {
            Node rednerNode = redner.toNode(db, tx);
            if (rednerNode != null) {
                db.createRelationship(tx, kommentarNode, rednerNode, "VON_REDNER");
            }
        }

        return kommentarNode;
    }


    /**
     * Erstellt ein Kommentar Objekt aus einem Neo4j Knoten.
     * Findet den zugehörigen Redner und die Rede.
     * @param komNode
     * @return new Kommentar
     */
    public static Kommentar fromNode(Node komNode) {
        if (komNode == null) return null;

        String text = (String) komNode.getProperty("text", "");

        Redner redner = null;
        Rede rede = null;
        Fraktion fraktion = null;

        // Such alle Beziehungen des Kommentar Knotens
        for (org.neo4j.graphdb.Relationship rel : komNode.getRelationships()) {
            String relType = rel.getType().name();

            // Falls Beziehung zu einem Redner existiert
            if ("VON REDNER".equals(relType)) {
                redner = Redner.fromNode(rel.getEndNode());
            }

            // Falls Beziehung zu einem Redner existiert
            else if ("ZU REDE".equals(relType)) {
                rede = Rede.fromNode(rel.getEndNode());
            }

        }

        return new Kommentar(redner, rede, fraktion, text);
    }
}