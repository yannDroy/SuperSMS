package com.yann.supersms;

/** Classe représentant une conversation
 * Created by yann on 07/09/16.
 */
public class ConversationData {
    private long id;
    private String numeroAssocie;
    private int nbMessages;
    private int nbNonLus;
    private long date;
    private boolean MMSpresent;

    public ConversationData() {
        id = -1L;
        numeroAssocie = "";
        nbMessages = 0;
        nbNonLus = 0;
        date = 0L;
        MMSpresent = false;
    }

    public ConversationData(long i, String num, int nb, int nbnl, long d, boolean mms) {
        id = i;
        numeroAssocie = num;
        nbMessages = nb;
        nbNonLus = nbnl;
        date = d;
        MMSpresent = mms;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNumeroAssocie() {
        return numeroAssocie;
    }

    public void setNumeroAssocie(String numeroAssocie) {
        this.numeroAssocie = numeroAssocie;
    }

    public int getNbMessages() {
        return nbMessages;
    }

    public void setNbMessages(int nbMessages) {
        this.nbMessages = nbMessages;
    }

    public int getNbNonLus() {
        return nbNonLus;
    }

    public void setNbNonLus(int nbNonLus) {
        this.nbNonLus = nbNonLus;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public boolean isMMSpresent() {
        return MMSpresent;
    }

    public void setMMSpresent(boolean MMSpresent) {
        this.MMSpresent = MMSpresent;
    }

    public String toString() {
        String s = "" + id + " (" + numeroAssocie + "), " + nbMessages + ", " + nbNonLus + ", " + ConversationActivity.longToDate("" + date, "0") + ", " + MMSpresent + "\n";

        return s;
    }
}
