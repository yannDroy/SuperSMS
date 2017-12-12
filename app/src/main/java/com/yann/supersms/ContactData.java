package com.yann.supersms;

/** Classe représentant un contact
 * Created by yann on 27/08/16.
 */
public class ContactData {
    private long id;
    private String numero;
    private String nom;
    private String photo;

    public ContactData() {
        id = -1L;
        numero = "";
        nom = "";
        photo = "";
    }

    public ContactData(long i, String num, String n, String p) {
        id = i;
        numero = num;
        nom = n;
        photo = p;
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

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String toString() {
        String s = numero + ", " + nom + ", " + photo + "\n";

        return s;
    }
}