package org.texttechnologylab.ppr.blatt2.data.redenportal;

import org.json.JSONObject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.texttechnologylab.ppr.blatt2.data.database.Neo4jConnection;
import org.texttechnologylab.ppr.blatt2.data.interfaces.FraktionIn;

import java.util.HashMap;
import java.util.Map;


/**
 * Implementierung einer Fraktion
 */
public class Fraktion implements FraktionIn {
    private String namef;

    /**
     * Erstellt eine neue Fraktion
     * @param namef
     */
    public Fraktion(String namef) {
        this.namef = namef;
    }

    // Name der Fraktion
    @Override
    public String getName() {
        return namef;
    }


    /**
     * Wandelt die Fraktion in Neo4j mit to Node
     * @param db database
     * @param tx transaction
     * @return
     */
    @Override
    public Node toNode(Neo4jConnection db, Transaction tx) {
        if (db == null) { return null; }

        Map<String, Object> props = new HashMap<>();
        props.put("name", namef);

        return db.findOrCreateNode(tx, "Fraktion", "namef", namef, props);
    }

    // to String()
    @Override
    public String toString() {
        return String.format("Fraktion(name='%s')", namef);
    }


    /**
     * Exportiert die Fraktionsdaten als JSON String
     * @return JSON Repräsentation der Fraktion
     */
    public String toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("namef", namef != null ? namef : "null");
        obj.put("type", "Fraktion");
        return(obj.toString());
    }

}
