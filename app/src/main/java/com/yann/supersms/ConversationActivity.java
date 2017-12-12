package com.yann.supersms;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConversationActivity extends AppCompatActivity {
    private LinkedHashMap<String, MessageData> listeSmsConversation = null;
    private Bundle b = null;
    private String thread = "";
    private String numeroContact = "";
    private String nomContact = "";
    private boolean chargerContacts = false;
    private boolean pause = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        b = getIntent().getExtras();

        if(b != null)
            init();

        afficherMessages(true);
    }

    public void onBackPressed() {
        Intent menu = new Intent(ConversationActivity.this, MainActivity.class);
        startActivity(menu);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(ConversationSMSReceiver);

        pause = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(ConversationSMSReceiver, filter);

        supprimerNotifications();

        if(chargerContacts){
            MainActivity.getContacts(getApplicationContext());
            changerTitre();

            chargerContacts = false;
        }

        if(pause) {
            getMessagesConversation();

            pause = false;
        }

        afficherMessages(true);
    }

    public void supprimerNotifications() {
        NotificationManager nMgr = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String num = numeroContact;
        num = num.replaceAll("\\+33", "0");

        try {
            nMgr.cancel(Integer.parseInt(num));
        } catch(NumberFormatException e) {
            e.printStackTrace();
            nMgr.cancel(1);
        }
    }

    public void init() {
        numeroContact = b.getString("numero");

        if(b.getString("thread") == null) {
            final GlobalClass global = (GlobalClass) getApplicationContext();

            MainActivity.getContacts(getApplicationContext());
            MainActivity.getConversations(getApplicationContext());

            LinkedHashMap<String, ConversationData> conversations = global.getConversations();

            for(Map.Entry<String, ConversationData> e : conversations.entrySet()) {
                String tmp = e.getValue().getNumeroAssocie();
                String tmp2 = tmp.replaceAll("\\+33", "0");

                if(tmp.equals(numeroContact) || tmp2.equals(numeroContact)) {
                    thread = e.getKey();
                    break;
                }
            }

        } else {
            thread = b.getString("thread");
        }

        getMessagesConversation();

        changerTitre();

        EditText etSms = (EditText) findViewById(R.id.nouveau_sms);
        if(etSms != null) {
            etSms.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            if(getSupportActionBar() != null)
                etSms.setHint("Message à " + getSupportActionBar().getTitle());
        }

        Button envoyer = (Button) findViewById(R.id.bouton_envoyer);
        if(envoyer != null) {
            envoyer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    essaiEnvoiMessage();
                }
            });
        }
    }

    public void changerTitre() {
        final GlobalClass global = (GlobalClass) getApplicationContext();

        if(numeroContact != null) {
            String tmp = numeroContact.replaceAll(" ", "");
            String tmp2 = tmp.replaceAll("\\+33", "0");

            if(getSupportActionBar() != null) {
                if(global.getContacts().get(tmp) != null)
                    getSupportActionBar().setTitle(global.getContacts().get(tmp).getNom());
                else if(global.getContacts().get(tmp2) != null)
                    getSupportActionBar().setTitle(global.getContacts().get(tmp2).getNom());
                else
                    getSupportActionBar().setTitle(numeroContact);

                getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
                getSupportActionBar().setHideOnContentScrollEnabled(false);

                if(getSupportActionBar() != null)
                    nomContact = getSupportActionBar().getTitle().toString();
            }
        }
    }

    public void getMessagesConversation() {
        listeSmsConversation = new LinkedHashMap<String, MessageData>();

        Uri uriNum = Uri.parse("content://mms-sms/conversations/" + thread);
        Cursor c = getContentResolver().query(uriNum, new String[] { "_id", "address", "body", "date", "ct_t", "date_sent" }, null, null, "date ASC");

        if(c != null) {
            if(c.moveToFirst()) {
                for(int i = 0; i < c.getCount(); i++) {
                    String s = c.getString(c.getColumnIndex("ct_t"));
                    if ("application/vnd.wap.multipart.related".equals(s)) {
                        MMSData mms = new MMSData();
                    } else {
                        SMSData sms = new SMSData();

                        sms.setId(c.getLong(c.getColumnIndexOrThrow("_id")));
                        sms.setNumero(c.getString(c.getColumnIndexOrThrow("address")));
                        sms.setTexte(c.getString(c.getColumnIndexOrThrow("body")));
                        sms.setDate(c.getString(c.getColumnIndexOrThrow("date")));

                        if(c.getLong(c.getColumnIndexOrThrow("date_sent")) != 0)
                            sms.setEtat(SMSData.SMS_RECU);
                        else
                            sms.setEtat(SMSData.SMS_ENVOYE_RECU);

                        listeSmsConversation.put(("" + sms.getId()), sms);
                    }

                    c.moveToNext();
                }
            }

            c.close();
        }
    }

    /*public static LinkedHashMap<String, SMSData> triParDate(LinkedHashMap<String, SMSData> lhm) {
        if(lhm != null) {
            ArrayList<Map.Entry<String, SMSData>> l = new ArrayList<Map.Entry<String, SMSData>>(lhm.entrySet());

            Collections.sort(l, new Comparator<Map.Entry<String, SMSData>>() {
                @Override
                public int compare(Map.Entry<String, SMSData> sms1, Map.Entry<String, SMSData> sms2) {
                    return sms1.getValue().getDate().compareTo(sms2.getValue().getDate());
                }
            });

            LinkedHashMap<String, SMSData> sortedMap = new LinkedHashMap<String, SMSData>();
            for(Map.Entry<String, SMSData> entry : l)
                sortedMap.put(entry.getKey(), entry.getValue());

            return sortedMap;
        }

        return null;
    }*/

    public static String longToDate(String s, String previous) {
        long l = Long.parseLong(s);
        long prev = Long.parseLong(previous);

        Date date = new Date(l);
        SimpleDateFormat sdf;

        if(DateUtils.isToday(l)) {
            if(l - prev >= 120000)
                sdf = new SimpleDateFormat("HH:mm");
            else
                sdf = new SimpleDateFormat("");
        } else {
            if(l - prev >= 900000)
                sdf = new SimpleDateFormat("dd/MM/yy HH:mm");
            else
                sdf = new SimpleDateFormat("");
        }

        return sdf.format(date);
    }

    public void afficherMessages(boolean scroll) {
        if(listeSmsConversation != null) {
            LinearLayout layout = (LinearLayout) findViewById(R.id.liste_sms);

            if(layout != null) {
                layout.removeAllViews();

                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);

                String previousDate = "0";

                for(Map.Entry<String, MessageData> e : listeSmsConversation.entrySet()){
                    if(e.getValue() instanceof SMSData) {
                        final SMSData value = (SMSData) e.getValue();

                        TextView tvSms = new TextView(this);
                        TextView tvDate = new TextView(this);

                        tvSms.setMaxWidth(size.x * 3 / 4);
                        tvSms.setPaddingRelative(20, 20, 20, 20);
                        tvSms.setLinksClickable(true);
                        tvSms.setAutoLinkMask(Linkify.ALL);

                        String date = longToDate(value.getDate(), previousDate);
                        tvDate.setText(date);
                        previousDate = value.getDate();

                        tvSms.setOnTouchListener(new OnSwipeTouchListener(ConversationActivity.this) {
                            public void longTap() {
                                choixDialog(value);
                            }

                            public void doubleTap() {
                                listeSmsConversation.remove("" + value.getId());
                                getContentResolver().delete(Uri.parse("content://sms/" + value.getId()), null, null);

                                afficherMessages(false);
                            }
                        });

                        int etat = value.getEtat();

                        if (etat == SMSData.SMS_RECU) {
                            LinearLayout.LayoutParams paramsSms = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            paramsSms.setMargins(20, 20, 10, 3);
                            paramsSms.gravity = Gravity.START;
                            tvSms.setLayoutParams(paramsSms);

                            LinearLayout.LayoutParams paramsDate = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            paramsDate.setMargins(20, 3, 10, 20);
                            paramsDate.gravity = Gravity.START;
                            tvDate.setLayoutParams(paramsDate);

                            tvSms.setTextColor(Color.parseColor("#ffffff"));
                            tvSms.setLinkTextColor(Color.parseColor("#feae01"));
                            tvSms.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

                            tvDate.setTextColor(Color.parseColor("#cacaca"));
                            tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

                            tvSms.setBackgroundResource(R.drawable.coins_arrondis_recu);
                        } else {
                            LinearLayout.LayoutParams paramsSms = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            paramsSms.setMargins(20, 20, 20, 3);
                            paramsSms.gravity = Gravity.END;
                            tvSms.setLayoutParams(paramsSms);

                            LinearLayout.LayoutParams paramsDate = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            paramsDate.setMargins(10, 3, 20, 20);
                            paramsDate.gravity = Gravity.END;
                            tvDate.setLayoutParams(paramsDate);

                            tvSms.setLinkTextColor(Color.parseColor("#0447c1"));
                            switch (etat) {
                                case SMSData.SMS_TEST_ENVOI:
                                    tvSms.setTextColor(Color.parseColor("#ffffff"));
                                    tvSms.setBackgroundResource(R.drawable.coins_arrondis_attente);
                                    break;
                                case SMSData.SMS_ENVOYE:
                                    tvSms.setTextColor(Color.parseColor("#ffffff"));
                                    tvSms.setBackgroundResource(R.drawable.coins_arrondis_parti);
                                    break;
                                case SMSData.SMS_ENVOYE_RECU:
                                    tvSms.setTextColor(Color.parseColor("#ffffff"));
                                    tvSms.setBackgroundResource(R.drawable.coins_arrondis_envoye);
                                    break;
                                case SMSData.SMS_PROBLEME_ENVOI:
                                    tvSms.setTextColor(Color.parseColor("#ffffff"));
                                    tvSms.setBackgroundResource(R.drawable.coins_arrondis_erreur);
                                    break;
                            }

                            tvSms.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

                            tvDate.setTextColor(Color.parseColor("#cacaca"));
                            tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                        }

                        tvSms.setText(value.getTexte());

                        layout.addView(tvSms);

                        if(!tvDate.getText().equals(""))
                            layout.addView(tvDate);
                    } else {
                        System.out.println("MMS!");
                    }
                    //TODO affichage MMS
                }

                TextView vide = new TextView(this);
                vide.setText("");
                layout.addView(vide);
            }
        }

        if(scroll)
            scrollDown();
    }

    public void scrollDown() {
        final ScrollView nsv = (ScrollView) findViewById(R.id.nsv);
        if(nsv != null) {
            nsv.post(new Runnable() {
                @Override
                public void run() {
                    nsv.fullScroll(NestedScrollView.FOCUS_DOWN);
                }
            });
        }
    }

    public void choixDialog(final SMSData sms) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Que voulez-vous faire ?");

        alertDialogBuilder.setCancelable(true);

        String[] options = getListeOptionsSms();

        alertDialogBuilder.setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("message", sms.getTexte());
                                clipboard.setPrimaryClip(clip);

                                Toast.makeText(ConversationActivity.this, "Message copié dans le presse-papier", Toast.LENGTH_SHORT).show();
                                break;
                            case 1:
                                Intent nouveauMessage = new Intent(ConversationActivity.this, NouveauMessageActivity.class);

                                Bundle b = new Bundle();
                                b.putString("transfert", sms.getTexte());
                                nouveauMessage.putExtras(b);

                                startActivity(nouveauMessage);

                                break;
                            case 2:
                                EditText etSms = (EditText) findViewById(R.id.nouveau_sms);
                                if(etSms != null)
                                    etSms.setText(sms.getTexte());

                                break;
                            case 3:
                                listeSmsConversation.remove("" + sms.getId());
                                getContentResolver().delete(Uri.parse("content://sms/" + sms.getId()), null, null);

                                afficherMessages(false);

                                break;
                            case 4:
                                try {
                                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                                    callIntent.setData(Uri.parse("tel: " + numeroContact));
                                    startActivity(callIntent);
                                } catch (ActivityNotFoundException | SecurityException e) {
                                    e.printStackTrace();
                                }

                                break;
                            case 5:
                                Intent intent = new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, Uri.parse("tel: " + numeroContact));
                                intent.putExtra(ContactsContract.Intents.EXTRA_FORCE_CREATE, true);
                                startActivity(intent);

                                chargerContacts = true;

                                break;
                        }
                    }
                }
            );

        final AlertDialog alertDialog = alertDialogBuilder.create();

        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }

    public String[] getListeOptionsSms() {
        String[] options;

        if(nomContact.matches("^[0-9]+$") || nomContact.matches("^\\+[0-9]*")) {
            options = new String[]{
                    "Copier le message dans le presse-papier",
                    "Transférer le message",
                    "Renvoyer le message",
                    "Supprimer le message",
                    "Appeler le " + numeroContact + "",
                    "Enregistrer le " + numeroContact
            };
        } else {
            final GlobalClass global = (GlobalClass) getApplicationContext();

            String nom = null;
            String nom2 = null;

            if(global.getContacts().get(numeroContact) != null)
                nom = global.getContacts().get(numeroContact).getNom();

            if(global.getContacts().get(numeroContact.replaceAll("\\+33", "0")) != null)
                nom2 = global.getContacts().get(numeroContact.replaceAll("\\+33", "0")).getNom();

            if (nom != null) {
                options = new String[]{
                        "Copier le message",
                        "Transférer le message",
                        "Renvoyer le message",
                        "Supprimer le message",
                        "Appeler " + nom,
                        "Voir la fiche de " + nom
                };
            } else if(nom2 != null){
                options = new String[]{
                        "Copier le message",
                        "Transférer le message",
                        "Renvoyer le message",
                        "Supprimer le message",
                        "Appeler " + nom2,
                        "Voir la fiche de " + nom2
                };
            } else {
                options = new String[]{
                        "Copier le message",
                        "Transférer le message",
                        "Renvoyer le message",
                        "Supprimer le message",
                        "Appeler " + numeroContact,
                        "Voir la fiche de " + numeroContact
                };
            }
        }

        return options;
    }

    public void essaiEnvoiMessage() {
        String message;

        final EditText etSms = (EditText) findViewById(R.id.nouveau_sms);

        if(etSms != null) {
            message = etSms.getText().toString();

            if(!message.matches(" *") && !message.matches("\n*")) {
                SmsManager smsManager = SmsManager.getDefault();

                Date d = new Date();
                final SMSData newSms = new SMSData(d.getTime(), numeroContact, message, ("" + d.getTime()), SMSData.SMS_TEST_ENVOI);

                listeSmsConversation.put("" + d.getTime(), newSms);

                final String SMS_SENT = "SMS_SENT";
                final String SMS_DELIVERED = "SMS_DELIVERED";

                PendingIntent spi = PendingIntent.getBroadcast(this, 0, new Intent(SMS_SENT), 0);
                PendingIntent dpi = PendingIntent.getBroadcast(this, 0, new Intent(SMS_DELIVERED), 0);

                ArrayList<String> pages = smsManager.divideMessage(message);
                ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
                ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();

                for (int i = 0; i < pages.size(); i++) {
                    sentPendingIntents.add(spi);
                    deliveredPendingIntents.add(dpi);
                }

                //TODO messages non envoyés
                registerReceiver(new BroadcastReceiver() {
                    private String id = "" + newSms.getId();

                    @Override
                    public void onReceive(Context context, Intent intent) {
                        switch (getResultCode()) {
                            case Activity.RESULT_OK:
                                if(listeSmsConversation.get(id).getEtat() == SMSData.SMS_TEST_ENVOI)
                                    listeSmsConversation.get(id).setEtat(SMSData.SMS_ENVOYE);
                                Toast.makeText(ConversationActivity.this, "Message envoyé", Toast.LENGTH_SHORT).show();
                                break;
                            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                                listeSmsConversation.get(id).setEtat(SMSData.SMS_PROBLEME_ENVOI);
                                Toast.makeText(ConversationActivity.this, "Erreur d'envoi inconnue", Toast.LENGTH_SHORT).show();
                                break;
                            case SmsManager.RESULT_ERROR_NO_SERVICE:
                                listeSmsConversation.get(id).setEtat(SMSData.SMS_PROBLEME_ENVOI);
                                Toast.makeText(ConversationActivity.this, "Service indisponible", Toast.LENGTH_SHORT).show();
                                break;
                            case SmsManager.RESULT_ERROR_NULL_PDU:
                                listeSmsConversation.get(id).setEtat(SMSData.SMS_PROBLEME_ENVOI);
                                Toast.makeText(ConversationActivity.this, "PDU introuvable", Toast.LENGTH_SHORT).show();
                                break;
                            case SmsManager.RESULT_ERROR_RADIO_OFF:
                                listeSmsConversation.get(id).setEtat(SMSData.SMS_PROBLEME_ENVOI);
                                Toast.makeText(ConversationActivity.this, "Mode hors ligne activé", Toast.LENGTH_SHORT).show();
                                break;
                        }

                        context.unregisterReceiver(this);
                        afficherMessages(true);
                    }
                }, new IntentFilter(SMS_SENT));

                registerReceiver(new BroadcastReceiver() {
                    private String id = "" + newSms.getId();

                    @Override
                    public void onReceive(Context context, Intent intent) {
                        switch (getResultCode()) {
                            case Activity.RESULT_OK:
                                if(listeSmsConversation.get(id).getEtat() == SMSData.SMS_ENVOYE)
                                    listeSmsConversation.get(id).setEtat(SMSData.SMS_ENVOYE_RECU);
                                Toast.makeText(ConversationActivity.this, "Message reçu", Toast.LENGTH_SHORT).show();
                                break;
                            case Activity.RESULT_CANCELED:
                                listeSmsConversation.get(id).setEtat(SMSData.SMS_PROBLEME_ENVOI);
                                Toast.makeText(ConversationActivity.this, "Erreur de réception", Toast.LENGTH_SHORT).show();
                                break;
                        }

                        context.unregisterReceiver(this);
                        afficherMessages(true);
                    }
                }, new IntentFilter(SMS_DELIVERED));

                smsManager.sendMultipartTextMessage(numeroContact, null, pages, sentPendingIntents, deliveredPendingIntents);

                afficherMessages(true);

                etSms.setText("");
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etSms.getWindowToken(), 0);
                etSms.clearFocus();
            }else{
                Toast.makeText(ConversationActivity.this, "Votre message est vide", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private BroadcastReceiver ConversationSMSReceiver = new BroadcastReceiver() {
        public static final String SMS_BUNDLE = "pdus";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {

                Bundle bundle = intent.getExtras();

                if(bundle != null) {
                    SmsMessage currentSMS;
                    Object[] pduObjects = (Object[]) bundle.get(SMS_BUNDLE);

                    if(pduObjects != null) {
                        for(Object o : pduObjects) {
                            currentSMS = getIncomingMessage(o, bundle);

                            String numero = currentSMS.getDisplayOriginatingAddress();
                            numero = numero.replaceAll(" ", "");
                            String numeroSansPLus = numero.replaceAll("\\+33", "0");

                            if(numero.equals(numeroContact) || numeroSansPLus.equals(numeroContact)) {
                                String messageStr = currentSMS.getDisplayMessageBody();
                                long date = currentSMS.getTimestampMillis();

                                SMSData newSms = new SMSData(date, numeroContact, messageStr, ("" + date), SMSData.SMS_RECU);

                                listeSmsConversation.put("" + date, newSms);
                                afficherMessages(true);
                            }
                        }

                        this.abortBroadcast();
                    }
                }
            }
        }

        public SmsMessage getIncomingMessage(Object aObject, Bundle bundle) {
            SmsMessage sms;

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String format = bundle.getString("format");
                sms = SmsMessage.createFromPdu((byte[]) aObject, format);
            } else {
                sms = SmsMessage.createFromPdu((byte[]) aObject);
            }
            return sms;
        }
    };
}