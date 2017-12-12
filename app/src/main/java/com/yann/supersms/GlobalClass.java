package com.yann.supersms;

import android.app.Application;

import java.util.LinkedHashMap;

/** Classe de variables globales
 * Created by yann on 27/08/16.
 */
public class GlobalClass extends Application {
    private LinkedHashMap<String, ContactData> contacts;
    private LinkedHashMap<String, ConversationData> conversations;
    private int divide;

    public GlobalClass() {
        conversations = null;
        contacts = null;
        divide = 2;
    }

    public LinkedHashMap<String, ContactData> getContacts() {
        return contacts;
    }

    public void setContacts(LinkedHashMap<String, ContactData> contacts) {
        this.contacts = contacts;
    }

    public LinkedHashMap<String, ConversationData> getConversations() {
        return conversations;
    }

    public void setConversations(LinkedHashMap<String, ConversationData> conversations) {
        this.conversations = conversations;
    }

    public int getDivide() {
        return divide;
    }

    public void setDivide(int divide) {
        this.divide = divide;
    }
}