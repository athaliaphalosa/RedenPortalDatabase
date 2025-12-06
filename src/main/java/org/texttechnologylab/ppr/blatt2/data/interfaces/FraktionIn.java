package org.texttechnologylab.ppr.blatt2.data.interfaces;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.texttechnologylab.ppr.blatt2.data.database.Neo4jConnection;

/**
 * Interface für Fraktion in Redeportal
 */


public interface FraktionIn {

    /**
     * Gibt den Namen der Fraktion zurück
     * @return name
     */
    String getName();

    /**
     * Konvertiert zu Neo4j Node
     * @param db database
     * @param tx transaction
     * @return node
     */
    Node toNode(Neo4jConnection db, Transaction tx);

}
