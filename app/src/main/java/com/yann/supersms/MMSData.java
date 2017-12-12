package com.yann.supersms;

/** Classe représentant un MMS
 * Created by yann on 08/09/16.
 */
public class MMSData extends MessageData {
    public static final int MMS_RECU = 0;
    public static final int MMS_TEST_ENVOI = 1;
    public static final int MMS_ENVOYE = 2;
    public static final int MMS_ENVOYE_RECU = 3;
    public static final int MMS_PROBLEME_ENVOI = 4;


    public static final int INCONNU = 0;
    public static final int IMAGE = 1;
    public static final int VIDEO = 2;
    public static final int SON = 3;

    private int type;

    public MMSData() {
        super();

        type = INCONNU;
    }

    public MMSData(int t) {
        type = t;
    }

    public String toString() {
        return super.toString();
    }
}
