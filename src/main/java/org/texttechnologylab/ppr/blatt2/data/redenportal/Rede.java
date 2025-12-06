package org.texttechnologylab.ppr.blatt2.data.redenportal;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.texttechnologylab.ppr.blatt2.data.interfaces.RedeIn;
import org.texttechnologylab.ppr.blatt2.data.database.Neo4jConnection;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Repräsentiert einer Rede im Reden Portal
 * Enthält Redner, Sitzung, Text vom Rede, und Kommentar
 */
public class Rede implements RedeIn {
    private Redner redner;
    private Sitzung sitzung;
    private List<Kommentar> kommentar;
    private String rid;
    private String text;

    //Erstellt neue Rede
    public Rede(String id, Redner redner, Sitzung sitzung) {
        this.rid = id;
        this.redner = redner;
        this.sitzung = sitzung;
        this.kommentar = new ArrayList<>();
    }

    public String getRid() {
        return rid;
    }

    public Redner getRedner() {
        return redner;
    }

    public Sitzung getSitzung() {
        return sitzung;
    }

    public List<Kommentar> getKommentar() {
        return kommentar;
    }


    //get und set für Text der Rede
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }


    // Gesamtlänge aller Reden in Zeichen, wird in Aufgabe 4 benutzen
    public int getLaenge() {
        return text != null ? text.length() : 0;
    }


    // Rede ID
    public String getId() {
        return rid;
    }

    // Addieren Kommentare in einer Rede
    public void addKommentar(Kommentar kommentars) {
        this.kommentar.add(kommentars);
    }

    //toString(), hab ich für Debugging benutzen
    @Override
    public String toString() {
        return "Rede{" +
                "rid='" + rid + '\'' +
                ", redner=" + (redner != null ? redner.getVorname() + " " + redner.getNachname() : "null") +
                ", kommentare=" + kommentar.size() +
                ", textLength=" + (text != null ? text.length() : 0) +
                '}' + "\n";
    }

    // toJSON()
    public String toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", "Rede");
        json.put("id", rid != null ? rid : "");
        json.put("textLength", getLaenge());

        // Redners Information
        if (redner != null) {
            JSONObject rednerJson = new JSONObject();
            rednerJson.put("id", redner.getId());
            rednerJson.put("name", redner.getVorname() + " " + redner.getNachname());
            if (redner.getFraktion() != null) {
                rednerJson.put("fraktion", redner.getFraktion().getName());
            }
            json.put("redner", rednerJson);
        }

        // Sitzungs Information
        if (sitzung != null) {
            JSONObject sitzungJson = new JSONObject();
            sitzungJson.put("wahlperiode", sitzung.getWahlperiode());
            sitzungJson.put("sitzungNr", sitzung.getSitzungNr());
            json.put("sitzung", sitzungJson);
        }

        // Kommentars Information
        JSONArray kommentareArray = new JSONArray();
        for (Kommentar kom : kommentar) {
            if (kom != null) {
                JSONObject komJson = new JSONObject();
                komJson.put("text", kom.getTextk() != null ? kom.getTextk() : "");
                komJson.put("textLength", kom.getTextk() != null ? kom.getTextk().length() : 0);
                kommentareArray.put(komJson);
            }
        }
        json.put("kommentare", kommentareArray);
        json.put("kommentareCount", kommentar.size());

        return json.toString(2);
    }

    /**
     * Speichert Rede als Neo4j Knoten-Kanten mit Beziehungen
     * @param db database
     * @param tx transaction
     * @return redeNode
     */
    @Override
    public Node toNode(Neo4jConnection db, Transaction tx) {
        if (rid == null || rid.trim().isEmpty() || db == null || tx == null) return null;

        Map<String, Object> props = new HashMap<>();
        props.put("redeId", rid);
        props.put("type", "Rede");


        // Rede Knoten finden oder erstellen
        Node redeNode = db.findOrCreateNode(tx, "Rede", "id", rid, props);

        // Beziehung mit Redner
        if (redner != null) {
            Node rednerNode = redner.toNode(db, tx);
            db.createRelationship(tx, redeNode, rednerNode, "VON_REDNER");
        }

        // Beziehung mit Sitzung
        if (sitzung != null) {
            Node sitzungNode = sitzung.toNode(db, tx);
            db.createRelationship(tx, redeNode, sitzungNode, "IN_SITZUNG");
        }

        // Beziehung mit Kommentar
        if (kommentar != null) {
            for (Kommentar kom : kommentar) {
                Node komNode = kom.toNode(db, tx);
                db.createRelationship(tx, redeNode, komNode, "HAT_KOMMENTAR");
            }
        }

        return redeNode;
    }



    /**
     * Erstellt Rede Objekt aus einem Neo4j Knoten
     * @param redeNode
     * @return Rede Objekt
     * */
    public static Rede fromNode(Node redeNode) {
        if (redeNode == null) return null;

        // Propertys from Neo4j Knoten lesen
        String rid = (String) redeNode.getProperty("redeId", null);
        String text = (String) redeNode.getProperty("text", "");

        Redner redner = null;
        Sitzung sitzung = null;
        List<Kommentar> kommentare = new ArrayList<>();

        // Iteration der Relationen um Redner, Sitzung, und Kommentar zu holen
        for (org.neo4j.graphdb.Relationship rel : redeNode.getRelationships()) {
            String relType = rel.getType().name();

            // Beziehung zu Redner finden
            if ("VON_REDNER".equals(relType)) {
                Node rednerNode = rel.getEndNode();
                redner = Redner.fromNode(rednerNode);
            }

            // Beziehung zu Sitzung finden
            else if ("IN_SITZUNG".equals(relType)) {
                Node sitzungNode = rel.getEndNode();
                sitzung = Sitzung.fromNode(sitzungNode);
            }

            // Beziehung zu Kommentar finden
            else if ("HAT_KOMMENTAR".equals(relType)) {
                Node komNode = rel.getEndNode();
                Kommentar kom = Kommentar.fromNode(komNode); // Kommentar juga harus ada fromNode
                if (kom != null) {
                    kommentare.add(kom);
                }
            }
        }

        //Erstellen neue Rede Objekt
        Rede rede = new Rede(rid, redner, sitzung);
        rede.setText(text);

        // Add Kommentar
        for (Kommentar kom : kommentare) {
            rede.addKommentar(kom);
        }

        return rede;
    }



}