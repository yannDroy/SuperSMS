package com.yann.supersms;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class NouveauMessageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nouveau_message);

        if(getSupportActionBar() != null)
            getSupportActionBar().setTitle("Nouveau message");

        final AutoCompleteTextView recherche = (AutoCompleteTextView) findViewById(R.id.contact);
        if(recherche != null) {
            recherche.setSingleLine(true);

            String[] tab = getNomContacts();
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, tab);
            recherche.setAdapter(adapter);
        }

        final EditText newSms = (EditText) findViewById(R.id.message);
        if(newSms != null) {
            newSms.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

            Bundle b = getIntent().getExtras();
            if(b != null) {
                if(b.getString("transfert") != null)
                    newSms.setText(b.getString("transfert"));
            }
        }

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if(fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    envoyerMessage();
                }
            });
        }
    }

    public void onBackPressed() {
        Intent menu = new Intent(NouveauMessageActivity.this, MainActivity.class);
        startActivity(menu);
        finish();
    }

    public String[] getNomContacts() {
        final GlobalClass global = (GlobalClass) getApplicationContext();
        LinkedHashMap<String, ContactData> liste = global.getContacts();

        String[] tab = new String[liste.size()];

        int i = 0;
        for(LinkedHashMap.Entry<String, ContactData> e : liste.entrySet()) {
            ContactData cd = e.getValue();

            tab[i] = cd.getNom();
            i++;
        }

        return tab;
    }

    public void envoyerMessage() {
        AutoCompleteTextView contact = (AutoCompleteTextView) findViewById(R.id.contact);
        EditText etSms = (EditText) findViewById(R.id.message);

        if(etSms != null && contact != null) {
            etSms.clearFocus();

            String message = etSms.getText().toString();
            String contactStr = contact.getText().toString();
            String numero = "";

            //TODO '0x xx xx xx xx'

            boolean ok = false;

            if(!message.matches(" *") && !message.matches("\n*")) {
                if(contactStr.matches(" *") || contactStr.matches("\n*")) {
                    Toast.makeText(NouveauMessageActivity.this, "Aucun destinataire", Toast.LENGTH_LONG).show();
                } else if(contactStr.matches("\\d+")) {
                    numero = contactStr;
                    ok = true;
                } else {
                    final GlobalClass global = (GlobalClass) getApplicationContext();
                    LinkedHashMap<String, ContactData> listeContacts = global.getContacts();

                    for(Map.Entry<String, ContactData> e : listeContacts.entrySet()) {
                        String num = e.getKey();
                        ContactData cd = e.getValue();

                        if(cd.getNom().equalsIgnoreCase(contactStr)) {
                            numero = num;
                            ok = true;
                            break;
                        }
                    }

                    if(!ok)
                        Toast.makeText(NouveauMessageActivity.this, "Contact inexistant", Toast.LENGTH_LONG).show();
                }

                if(ok) {
                    SmsManager smsManager = SmsManager.getDefault();

                    ArrayList<String> pages = smsManager.divideMessage(message);
                    smsManager.sendMultipartTextMessage(numero, null, pages, null, null);

                    try {
                        Thread.sleep(800);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Intent menu = new Intent(NouveauMessageActivity.this, MainActivity.class);
                    startActivity(menu);
                    finish();
                }
            }else{
                Toast.makeText(NouveauMessageActivity.this, "Votre message est vide", Toast.LENGTH_LONG).show();
            }

            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(etSms.getWindowToken(), 0);
        }
    }
}
