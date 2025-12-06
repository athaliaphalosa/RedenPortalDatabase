package org.texttechnologylab.ppr.blatt2.data.helper;

/**
 * zu Normalize der Fraktion
 */
public class NormalizeFraktion {

    // Standardized fraktion names
    public static final String FRAKTIONSLOS = "FRAKTIONSLOS";
    public static final String CDU_CSU = "CDU/CSU";
    public static final String SPD = "SPD";
    public static final String GRUENE = "BÜNDNIS 90/DIE GRÜNEN";
    public static final String AFD = "AfD";
    public static final String FDP = "FDP";
    public static final String LINKE = "DIE LINKE";
    public static final String BSW = "BSW";

    /**
     * Helper Method zu Normalize der Fraktion
     */
    public static String normalizeFraktion(String oriName) {
        if (oriName == null || oriName.trim().isEmpty()) {
            return FRAKTIONSLOS;
        }

        String upper = oriName.toUpperCase()
                .replace("Ü", "U").replace("Ö", "O").replace("Ä", "A").replace("ß", "SS")
                .trim();

        if (upper.contains("FRAKTIONSLOS") || upper.contains("FRACTIONLESS")) {
            return FRAKTIONSLOS;
        }
        if (upper.contains("CDU") || upper.contains("CSU")) {
            return CDU_CSU;
        }
        if (upper.contains("SPD")) {
            return SPD;
        }
        if (upper.contains("GRUEN") || upper.contains("BUNDNIS") || upper.contains("GRÜN")) {
            return GRUENE;
        }
        if (upper.contains("AFD") || upper.contains("ALTERNATIVE")) {
            return AFD;
        }
        if (upper.contains("FDP")) {
            return FDP;
        }
        if (upper.contains("LINKE")) {
            return LINKE;
        }
        if (upper.contains("BSW")) {
            return BSW;
        }

        return oriName;
    }
}