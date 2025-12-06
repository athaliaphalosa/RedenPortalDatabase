package org.texttechnologylab.ppr.blatt2.data.interfaces;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.texttechnologylab.ppr.blatt2.data.database.Neo4jConnection;
import org.texttechnologylab.ppr.blatt2.data.redenportal.Fraktion;
import org.texttechnologylab.ppr.blatt2.data.redenportal.Rede;
import org.texttechnologylab.ppr.blatt2.data.redenportal.Redner;


/**
 * Interface für Kommentar jeder Rede in Redeportal
 */

public interface KommentarIn {

    /**
     * Gibt den Redner, der diese Kommentar abgegeben hat
     * @return der Redner
     */
    Redner getRedner();

    /**
     * Gibt die Rede, der zu diese Kommentar gehört
     * @return die Rede
     */
    Rede getRede();

    /**
     * Gibt der Text von Kommentar
     * @return die Textkommentare
     */
    String getTextk();


    /**
     * Gibt die Fraktion des Redners dieses Kommentars
     * @return die Fraktion des Redners
     */
    Fraktion fraktion();

    /**
     * To convert to Neo4j Node
     * @param db database
     * @param tx transaction
     * @return node
     */
    Node toNode(Neo4jConnection db, Transaction tx);
}
