package com.yann.supersms;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.SmsMessage;

import java.io.InputStream;
import java.util.LinkedHashMap;

/** Classe gérant la réception de messages
 * Created by yann on 29/08/16.
 */
public class SMSBroadcastReceiver extends BroadcastReceiver {
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
                        String numeroSansPlus = numero.replaceAll("\\+[0-9][0-9]", "0");

                        String messageStr = currentSMS.getDisplayMessageBody();

                        GlobalClass global = (GlobalClass) context.getApplicationContext();

                        LinkedHashMap<String, ContactData> contacts;

                        if(global != null) {
                            if((contacts = global.getContacts()) == null)
                                contacts = getContacts(context);
                        } else {
                            contacts = getContacts(context);
                        }

                        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
                        mBuilder.setContentText(messageStr);
                        mBuilder.setSmallIcon(R.drawable.icone_notification);
                        mBuilder.setVibrate(new long[] { 300, 400, 100, 0, 1000 });
                        mBuilder.setLights(Color.CYAN, 3000, 2000);
                        mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
                        mBuilder.setAutoCancel(true);
                        mBuilder.setColor(Color.parseColor("#b51212"));

                        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        mBuilder.setSound(alarmSound);

                        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
                        style.bigText(messageStr);
                        mBuilder.setStyle(style);

                        if(contacts.get(numero) != null) {
                            mBuilder.setContentTitle(contacts.get(numero).getNom());
                            if(contacts.get(numero).getPhoto() != null) {
                                Uri photo = Uri.parse(contacts.get(numero).getPhoto());
                                mBuilder.setLargeIcon(getContactBitmapFromURI(context, photo));
                            }
                        }else if(contacts.get(numeroSansPlus) != null) {
                            mBuilder.setContentTitle(contacts.get(numeroSansPlus).getNom());
                            if(contacts.get(numeroSansPlus).getPhoto() != null) {
                                Uri photo = Uri.parse(contacts.get(numeroSansPlus).getPhoto());
                                mBuilder.setLargeIcon(getContactBitmapFromURI(context, photo));
                            }
                        } else {
                            mBuilder.setContentTitle(numeroSansPlus);
                        }

                        Intent resultIntent =  new Intent(context, ConversationActivity.class);
                        Bundle b = new Bundle();
                        b.putString("numero", numeroSansPlus);
                        try {
                            b.putInt("notif", Integer.parseInt(numeroSansPlus));
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            b.putInt("notif", 1);
                        }
                        resultIntent.putExtras(b);
                        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                        stackBuilder.addParentStack(MainActivity.class);

                        stackBuilder.addNextIntent(resultIntent);
                        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                        mBuilder.setContentIntent(resultPendingIntent);

                        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(b.getInt("notif"), mBuilder.build());
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

    public Bitmap getContactBitmapFromURI(Context context, Uri uri) {
        InputStream input = null;

        try {
            input = context.getContentResolver().openInputStream(uri);

            if(input == null)
                return null;
        }catch(Exception e){
            e.printStackTrace();
        }

        return BitmapFactory.decodeStream(input);
    }

    public LinkedHashMap<String, ContactData> getContacts(Context context) {
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

        return contacts;
    }
}
