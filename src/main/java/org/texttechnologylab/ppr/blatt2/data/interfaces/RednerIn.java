package org.texttechnologylab.ppr.blatt2.data.interfaces;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.texttechnologylab.ppr.blatt2.data.database.Neo4jConnection;
import org.texttechnologylab.ppr.blatt2.data.redenportal.Fraktion;


/**
 * Interface für Redner in Redeportal
 */
public interface RednerIn {

    /**
     * Get Titel des Redners
     * @return der Titel
     */
    String getTitel();

    /**
     * Get Id des Redners
     * @return die Id
     */
    String getId();

    /**
     * Get Vorname des Redners
     * @return der Vorname
     */
    String getVorname();

    /**
     * Get Nachanme des Redners
     * @return der Nachame
     */
    String getNachname();


    /**
     * Get Fraktion des Redners
     * @return der Titel
     */
    Fraktion getFraktion(String FraktionName);

    /**
     * Gibt den vollständigen Namen zurück
     * @return der volständige Name
     */
    String getFullName();

    /**
     * To convert to Neo4j Node
     * @param db database
     * @param tx transaction
     * @return node
     */
    Node toNode(Neo4jConnection db, Transaction tx);

}
