package org.texttechnologylab.ppr.blatt2.data.interfaces;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.texttechnologylab.ppr.blatt2.data.database.Neo4jConnection;

import java.time.LocalTime;
import java.util.Date;


/**
 * Interface für Sitzung in Redeportal
 */
public interface SitzungIn {

    /**
     * Get den Ort der Sitzung
     * @return der Sitzungort
     */
    String getSitzungOrt();

    /**
     * Get dieNummer der Sitzung
     * @return die Sitzungnummer
     */
    String getSitzungNr();

    /**
     * Get die Wahlperiode
     * @return Get die Wahlperiode
     */
    String getWahlperiode();

    /**
     * Get das Datum der Sitzung
     * @return Sitzungdatum
     */
    Date getSitzungDatum();

    /**
     * Get die Beginnzeit der Sitzung
     * @return die Startzeit
     */
    LocalTime getStartZeit();

    /**
     * Get die Endezeit der Sitzung
     * @return die Endzeit
     */
    LocalTime getEndZeit();

    /**
     * To convert to Neo4j Node
     * @param db database
     * @param tx transaction
     * @return node
     */
    Node toNode(Neo4jConnection db, Transaction tx);
}
