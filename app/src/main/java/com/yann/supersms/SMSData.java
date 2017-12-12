package com.yann.supersms;

/** Class representing a SMS
 * Created by yann on 25/08/16.
 */
public class SMSData extends MessageData {
    public static final int SMS_RECU = 0;
    public static final int SMS_TEST_ENVOI = 1;
    public static final int SMS_ENVOYE = 2;
    public static final int SMS_ENVOYE_RECU = 3;
    public static final int SMS_PROBLEME_ENVOI = 4;

    public SMSData() {
        super();
    }

    public SMSData(long i, String n, String t, String d, int e) {
        super(i, n, t, d, e);
    }

    public String toString() {
        return super.toString();
    }
}