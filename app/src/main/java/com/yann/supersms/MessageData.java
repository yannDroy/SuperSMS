package com.yann.supersms;

/** Classe représentant un message
 * Created by yann on 08/09/16.
 */
public class MessageData {
    private static final int RIEN = -2;
    private long id;
    private String numero;
    private String texte;
    private String date;
    private int etat;

    public MessageData() {
        id = -1L;
        numero = "";
        texte = "";
        date = "";
        etat = RIEN;
    }

    public MessageData(long i, String n, String t, String d, int e) {
        id = i;
        numero = n;
        texte = t;
        date = d;
        etat = e;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getTexte() {
        return texte;
    }

    public void setTexte(String texte) {
        this.texte = texte;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getEtat() {
        return etat;
    }

    public void setEtat(int etat) {
        this.etat = etat;
    }

    public String toString() {
        String s = "(" + id + ") ";

        if(etat == SMSData.SMS_RECU)
            s += "SMS/MMS recu de ";
        else
            s += "SMS/MMS envoyé à ";

        s += numero;
        s += " le/à " + ConversationActivity.longToDate(date, "0");
        s += " : " + this.texte;
        s += "\n";

        return s;
    }
}
