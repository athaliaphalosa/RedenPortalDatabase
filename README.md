### Rede - Portal App
Bundestagdaten stastitischen Analyse mit Neo4j als Datenbanken

## Description
Eine Java-Anwendung zur Verarbeitung und Auswertung von Bundestagsdaten.
Die XML-Dateien der Sitzungen werden eingelesen, in Java-Objekte umgewandelt und in einer Neo4j Graphdatenbank gespeichert. 
Anschließend kann man verschiedene statistische Auswertungen direkt auf diesen Objekten durchführen.

Das Projekt ist objektorientiert aufgebaut:
- Jede Rede, Redner, Sitzung, Fraktion Kommentar hat ihre eigene Klasse.
- Hilfsklassen (Helper) sorgen für das Einlesen der XML-Dateien oder die Vereinheitlichung der Fraktionsnamen.

Die Datenbankzugriffe sind in eigenen Klassen gekapselt, damit alles übersichtlich bleibt und sich gut wiederverwenden lässt.
- XML Dateien vom Bundestag parsen und in Objekte umwandeln
- Daten in Neo4j speichern und verwalten
- Statistiken zu Redelängen und Kommentarhäufigkeiten erstellen
- Auswertungen nach Abgeordnetem oder Fraktion durchführen
- Die längsten Sitzungen finden sowohl nach Zeit als auch nach Gesamtlänge der Reden

## Badges
On some READMEs, you may see small images that convey metadata, such as whether or not all the tests are passing for the project. You can use Shields to add some to your README. Many services also have instructions for adding a badge.


## Installation
Hier benutze ich:
- Java JDK 21
- Maven 4,0
- Neo4j Community Edition

# Program Starten
In Utils Klasse, mach in MainUbung2 run (org.texttechnologylab.ppr.blatt2.utils.MainUbung2).
Nach dem Start werden die XML-Daten automatisch eingelesen, in Java-Objekte umgewandelt und
eine Übersicht der geladenen Daten angezeigt (Anzahl Reden, Redner, Sitzungen, Fraktionen und Kommentare).

# Menu Optionen
Menü kan man auswählen, welche Informationen man sehen möchte:
1. Detaillierte Parsing-Ergebnisse anzeigen
Zeigt alle Reden mit Informationen zu Redner, Fraktion, Sitzung und den Kommentaren an. 

2. Statistische Auswertung (Aufgabe 4) ausführen
Führt die statistische Analyse auf Basis der geladenen Daten durch.
Details sieh Abschnitt "Statitische Auswertung".

3. Beides anzeigen
Zeigt sowohl die Parsing-Ergebnisse als auch die statistischen Auswertungen gleichzeitig an.

4. Programm beenden
Schließt das Programm sauber und beendet die Datenbankverbindung.


## Stastitische Auswertungen
Es ist die Lösung von Aufgabe 4, ich habe mit Cypher Query benutzen

A. Durchschnittliche Redelänge
Abgeordnete nach durchschnittlicher Redelänge (ich hab nur top 15 gezeigt).
Vergleich der Redelängen auf Fraktions.

B. Kommentar Häufigkeit pro Rede
Abgeordnete mit den meisten Kommentaren (ich hab nur top 15 gezeigt).
Vergleich der Kommentare Häufigkeit zwischen den Fraktionen.

C. Längste Sitzung
Nach Zeitdauer der Sitzung.
Nach Gesamtlänge aller Reden in der Sitzung.


## Die Ausgabe
Die Ausgabe und alle verarbeiteten Daten werden in der Konsole angezeigt


## Support
Häufige Probleme:
1. Neo4j würde nicht gestartet
Es ist ein Problem, wenn Neo4j nicht läuft, 
kann sich die Anwendung nicht mit der Datenbank verbinden.

2. XML-Daten wurden nicht gefunden
Die Anwendung erwartet die Bundestags XML Dateien im Ordner: src/main/resources/20/
- Ich habe aber die Dateien in src/main/resources/20/ auf Gitlab gepusht
- !! Die XML-Datei, die ich für die Abgabe auf OLAT hochgeladen habe, enthält bereits alle benötigten XML-Dateien in src/main/resources/20/.

4. Parsing-Fehler in den XML-Dateien
Ungültige Tags oder nicht komplett heruntergeladene XML können beim Einlesen Fehler verursachen.


## Roadmap
Weitere Funktionen könnten in Zukunft ergänzt werden.

## Contributing
Dieses Projekt ist Teil einer Uni-Aufgabe von Blatt 2 ProgrammierPraktikum (PPR)/

## Authors and acknowledgment
Authors: Athalia Bernice Phalosa

## Project status
Das Projekt ist für die Universitätsaufgabe abgeschlossen.

