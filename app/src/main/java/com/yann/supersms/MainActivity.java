package com.yann.supersms;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsMessage;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.provider.ContactsContract;
import android.widget.Toast;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    int divide = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if(fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final GlobalClass global = (GlobalClass) getApplicationContext();
                    global.setDivide(divide);

                    Intent i = new Intent(MainActivity.this, NouveauMessageActivity.class);
                    startActivity(i);
                    finish();
                }
            });
        }

        final GlobalClass global = (GlobalClass) getApplicationContext();
        divide = global.getDivide();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(MainSMSReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(MainSMSReceiver, filter);

        getContacts(getApplicationContext());
        getConversations(getApplicationContext());

        afficherListeConversations();
    }

    public static void getContacts(Context context) {
        LinkedHashMap<String, ContactData> contacts = null;
        Cursor numeros = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

        if(numeros != null) {
            contacts = new LinkedHashMap<String, ContactData>();

            while(numeros.moveToNext()) {
                ContactData c = new ContactData();

                c.setId(numeros.getLong(numeros.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)));
                c.setNom(numeros.getString(numeros.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                c.setPhoto(numeros.getString(numeros.getColumnIndex(ContactsContract.Contacts.Photo.PHOTO_URI)));

                String n = numeros.getString(numeros.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                n = n.replaceAll(" ", "");
                c.setNumero(n);

                if(!contacts.containsKey(c.getNumero()) || c.getPhoto() != null)
                    contacts.put(c.getNumero(), c);
            }

            numeros.close();
        }

        final GlobalClass global = (GlobalClass) context.getApplicationContext();
        global.setContacts(contacts);
    }

    public static void getConversations(Context context) {
        final GlobalClass global = (GlobalClass) context.getApplicationContext();

        LinkedHashMap<String, ConversationData> conversations = new LinkedHashMap<String, ConversationData>();

        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse("content://mms-sms/conversations?simple=true");
        Cursor c = contentResolver.query(uri, null, null, null, "date DESC");

        if(c != null) {
            if(c.moveToFirst()) {
                for(int i = 0; i < c.getCount(); i++) {
                    ConversationData conv = new ConversationData();

                    conv.setId(c.getLong(c.getColumnIndexOrThrow("_id")));
                    conv.setDate(c.getLong(c.getColumnIndexOrThrow("date")));
                    conv.setNbMessages(c.getInt(c.getColumnIndexOrThrow("message_count")));
                    conv.setNbNonLus(c.getInt(c.getColumnIndexOrThrow("unread_count")));

                    if(c.getInt(c.getColumnIndexOrThrow("has_attachment")) == 0)
                        conv.setMMSpresent(false);
                    else
                        conv.setMMSpresent(true);

                    Uri uriNum = Uri.parse("content://mms-sms/conversations/" + conv.getId());
                    Cursor cNum = contentResolver.query(uriNum, new String[] { "_id", "address", "date", "ct_t" }, null, null, "date DESC");
                    if (cNum != null) {
                        if(cNum.moveToFirst()) {
                            do {
                                String s = cNum.getString(cNum.getColumnIndex("ct_t"));
                                if ("application/vnd.wap.multipart.related".equals(s)) {
                                    String selection = "msg_id=" + cNum.getLong(cNum.getColumnIndexOrThrow("_id"));
                                    String uriStr = MessageFormat.format("content://mms/{0}/addr", cNum.getLong(cNum.getColumnIndexOrThrow("_id")));
                                    Uri uriNumMMS = Uri.parse(uriStr);

                                    Cursor cNumMMS = context.getContentResolver().query(uriNumMMS, null, selection, null, null);

                                    if (cNumMMS != null) {
                                        if(cNumMMS.moveToFirst()) {
                                            conv.setNumeroAssocie(cNumMMS.getString(cNumMMS.getColumnIndexOrThrow("address")));
                                        }
                                        cNumMMS.close();
                                    }
                                } else {
                                    conv.setNumeroAssocie(cNum.getString(cNum.getColumnIndexOrThrow("address")));
                                }
                            } while (conv.getNumeroAssocie() == null && cNum.moveToNext());
                        }

                        cNum.close();
                    }

                    conversations.put(("" + conv.getId()), conv);
                    c.moveToNext();
                }
            }

            c.close();
        }

        global.setConversations(conversations);
    }

    public void afficherListeConversations() {
        final GlobalClass global = (GlobalClass) getApplicationContext();

        LinkedHashMap<String, ConversationData> conversations = global.getConversations();

        LinearLayout layout = (LinearLayout) findViewById(R.id.liste_conv);
        if(layout != null){
            layout.removeAllViews();

            final Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            int nombreLignes = 0;
            int nombreCellules = 0;

            if(conversations != null) {
                ArrayList<TextView> liste = getConvTextView(conversations);

                LinearLayout line = new LinearLayout(this);
                line.setOrientation(LinearLayout.HORIZONTAL);

                for(int i = 0; i < liste.size(); i++) {
                    nombreCellules++;

                    final String id = liste.get(i).getHint().toString();
                    final String numero = liste.get(i).getText().toString();
                    String numeroSansPlus = numero.replaceAll("\\+33", "0");

                    ContactData cd = global.getContacts().get(numero);
                    ContactData cd2 = global.getContacts().get(numeroSansPlus);

                    if(cd != null) {
                        if(cd.getPhoto() != null) {
                            Uri photoUri = Uri.parse(cd.getPhoto());
                            ImageView photo = getImageViewContact(photoUri, numero, id);

                            line.addView(photo);
                        } else {
                            liste.set(i, getTextViewContact(numero, cd.getNom(), id));
                            line.addView(liste.get(i));
                        }

                        liste.get(i).setText(global.getContacts().get(numero).getNom());
                    } else if(cd2 != null){
                        if(cd2.getPhoto() != null) {
                            Uri photoUri = Uri.parse(cd2.getPhoto());
                            ImageView photo = getImageViewContact(photoUri, numeroSansPlus, id);

                            line.addView(photo);
                        } else {
                            liste.set(i, getTextViewContact(numeroSansPlus, cd2.getNom(), id));
                            line.addView(liste.get(i));
                        }

                        liste.get(i).setText(global.getContacts().get(numeroSansPlus).getNom());
                    } else {
                        liste.set(i, getTextViewContact(numero, liste.get(i).getText().toString(), id));
                        liste.get(i).setText(numero);

                        line.addView(liste.get(i));
                    }

                    if ((i + 1) % divide == 0) {
                        layout.addView(line);
                        line = new LinearLayout(this);
                        line.setOrientation(LinearLayout.HORIZONTAL);

                        nombreLignes++;
                    }

                    if ((i + 1) == liste.size()) {
                        if (nombreCellules > nombreLignes * divide) {
                            for (int j = 0; j < ((nombreLignes + 1) * divide) - nombreCellules; j++) {
                                TextView tv = getTextViewVide();
                                line.addView(tv);
                            }
                            nombreLignes++;
                        }
                        layout.addView(line);
                    }
                }

                switch (divide) {
                    case 2:
                        ajouterLignesVides(layout, nombreLignes, 2);
                        break;
                    case 3:
                        ajouterLignesVides(layout, nombreLignes, 3);
                        break;
                    case 4:
                        ajouterLignesVides(layout, nombreLignes, 5);
                        break;
                    case 5:
                        ajouterLignesVides(layout, nombreLignes, 6);
                        break;
                }
            }
        }
    }

    public ArrayList<TextView> getConvTextView(LinkedHashMap<String, ConversationData> conversations) {
        ArrayList<TextView> liste = new ArrayList<TextView>();

        for(Map.Entry<String, ConversationData> e : conversations.entrySet()) {
            String numero = e.getValue().getNumeroAssocie();
            String id = "" + e.getValue().getId();

            TextView tvConv = new TextView(this);
            tvConv.setText(numero.replaceAll("\\+33", "0"));
            tvConv.setHint(id);

            liste.add(tvConv);
        }

        return liste;
    }

    public TextView getTextViewContact(final String numero, String nom, final String id) {
        final Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        TextView tv = new TextView(this);

        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, getTailleTexte());
        //tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.parseColor("#efefef"));
        tv.setWidth(size.x / divide);
        tv.setHeight(size.x / divide);

        LinearLayout.LayoutParams paramsSms = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        paramsSms.gravity = Gravity.START;
        tv.setLayoutParams(paramsSms);

        tv.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
            public void onSwipeRight() {
                if(divide < 5){
                    divide++;
                    afficherListeConversations();
                }
            }
            public void onSwipeLeft() {
                if(divide > 1){
                    divide--;
                    afficherListeConversations();
                }
            }
            public void singleTap() {
                Bundle b = new Bundle();

                b.putString("thread", id);
                b.putString("numero", numero);
                b.putInt("notif", -1);

                final GlobalClass global = (GlobalClass) getApplicationContext();
                global.setDivide(divide);

                Intent conv = new Intent(MainActivity.this, ConversationActivity.class);
                conv.putExtras(b);
                startActivity(conv);
                finish();
            }
            public void doubleTap() {
                try {
                    final GlobalClass global = (GlobalClass) getApplicationContext();
                    global.setDivide(divide);

                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel: " + numero));
                    startActivity(callIntent);
                } catch (ActivityNotFoundException | SecurityException e) {
                    e.printStackTrace();
                }
            }
            public void longTap() {
                confirmationSuppression(id);
            }
        });

        tv.setBackgroundColor(getContactColor(nom));

        return tv;
    }

    public ImageView getImageViewContact(Uri photoUri, final String numero, final String id) {
        final Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        ImageView photo = new ImageView(this);

        photo.setImageURI(photoUri);

        LinearLayout.LayoutParams paramsPhoto = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        paramsPhoto.gravity = Gravity.START;
        paramsPhoto.width = size.x / divide;
        paramsPhoto.height = size.x / divide;
        photo.setLayoutParams(paramsPhoto);

        photo.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
            public void onSwipeRight() {
                if(divide < 5){
                    divide++;
                    afficherListeConversations();
                }
            }
            public void onSwipeLeft() {
                if(divide > 1){
                    divide--;
                    afficherListeConversations();
                }
            }
            public void singleTap() {
                Bundle b = new Bundle();

                b.putString("thread", id);
                b.putString("numero", numero);
                b.putInt("notif", -2);

                final GlobalClass global = (GlobalClass) getApplicationContext();
                global.setDivide(divide);

                Intent conv = new Intent(MainActivity.this, ConversationActivity.class);
                conv.putExtras(b);
                startActivity(conv);
                finish();
            }
            public void doubleTap() {
                try {
                    final GlobalClass global = (GlobalClass) getApplicationContext();
                    global.setDivide(divide);

                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel: " + numero));
                    startActivity(callIntent);
                } catch (ActivityNotFoundException | SecurityException e) {
                    e.printStackTrace();
                }
            }
            public void longTap() {
                confirmationSuppression(id);
            }
        });

        return photo;
    }

    public void ajouterLignesVides(LinearLayout content, int nbl, int n) {
        while (nbl <= n) {
            LinearLayout line = ligneVide();
            for (int j = 0; j < divide; j++) {
                TextView tv = getTextViewVide();
                line.addView(tv);
            }
            content.addView(line);
            nbl++;
        }
    }

    public LinearLayout ligneVide() {
        LinearLayout line = new LinearLayout(this);

        for (int j = 0; j < divide; j++) {
            TextView tv = getTextViewVide();
            line.addView(tv);
        }

        return line;
    }

    public TextView getTextViewVide() {
        final Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        TextView tv = new TextView(this);

        tv.setWidth(size.x / divide);
        tv.setHeight(size.x / divide);
        tv.setBackgroundColor(Color.parseColor("#eeeeee"));

        tv.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
            public void onSwipeRight() {
                if(divide < 5){
                    divide++;
                    afficherListeConversations();
                }
            }
            public void onSwipeLeft() {
                if(divide > 1){
                    divide--;
                    afficherListeConversations();
                }
            }
        });

        return tv;
    }

    public int getTailleTexte() {
        switch(divide) {
            case 1:
                return 45;
            case 2:
                return 24;
            case 3:
                return 17;
            case 4:
                return 13;
            case 5:
                return 10;
            default:
                return 15;
        }
    }

    public int getContactColor(String s) {
        if((s.charAt(0) >= '0' && s.charAt(0) <= '9') || s.charAt(0) == '+')
            return Color.parseColor("#808080");

        switch (s.toUpperCase().charAt(0)) {
            case 'A':
                return Color.parseColor("#a603e3");
            case 'B':
                return Color.parseColor("#35b201");
            case 'C':
                return Color.parseColor("#0497f1");
            case 'D':
                return Color.parseColor("#d45656");
            case 'E':
                return Color.parseColor("#a6503c");
            case 'F':
                return Color.parseColor("#7da341");
            case 'G':
                return Color.parseColor("#67108a");
            case 'H':
                return Color.parseColor("#674587");
            case 'I':
                return Color.parseColor("#aba5f2");
            case 'J':
                return Color.parseColor("#d006a1");
            case 'K':
                return Color.parseColor("#c48d9d");
            case 'L':
                return Color.parseColor("#ce7e01");
            case 'M':
                return Color.parseColor("#cac700");
            case 'N':
                return Color.parseColor("#7ecfad");
            case 'O':
                return Color.parseColor("#34f671");
            case 'P':
                return Color.parseColor("#9aa6b4");
            case 'Q':
                return Color.parseColor("#47c2b8");
            case 'R':
                return Color.parseColor("#b4aaa0");
            case 'S':
                return Color.parseColor("#7e08cd");
            case 'T':
                return Color.parseColor("#e4ad59");
            case 'U':
                return Color.parseColor("#0020a2");
            case 'V':
                return Color.parseColor("#ae36bf");
            case 'W':
                return Color.parseColor("#897ec2");
            case 'X':
                return Color.parseColor("#bbb3a9");
            case 'Y':
                return Color.parseColor("#5e6abb");
            case 'Z':
                return Color.parseColor("#cacefd");
            default:
                return Color.parseColor("#806070");
        }
    }

    public void supprimerConversation(String id) {
        getContentResolver().delete(Uri.parse("content://mms-sms/conversations/" + id), null, null);

        getConversations(getApplicationContext());

        afficherListeConversations();
    }

    public void confirmationSuppression(final String idSup) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Supprimer la conversation ?");

        alertDialogBuilder.setCancelable(true);

        alertDialogBuilder.setPositiveButton("Oui", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                supprimerConversation(idSup);
            }
        });

        alertDialogBuilder.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final AlertDialog alertDialog = alertDialogBuilder.create();

        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }

    private BroadcastReceiver MainSMSReceiver = new BroadcastReceiver() {
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

                            final GlobalClass global = (GlobalClass) getApplicationContext();
                            LinkedHashMap<String, ContactData> contacts = global.getContacts();

                            try {
                                Thread.sleep(800);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if(contacts.containsKey(numero))
                                Toast.makeText(MainActivity.this, "Nouveau message de " + contacts.get(numero).getNom(), Toast.LENGTH_LONG).show();
                            else if(contacts.containsKey(numeroSansPLus))
                                Toast.makeText(MainActivity.this, "Nouveau message de " + contacts.get(numeroSansPLus).getNom(), Toast.LENGTH_LONG).show();
                            else
                                Toast.makeText(MainActivity.this, "Nouveau message de " + numeroSansPLus, Toast.LENGTH_LONG).show();

                            getConversations(context);
                            afficherListeConversations();
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
