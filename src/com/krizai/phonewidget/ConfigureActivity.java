package com.krizai.phonewidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.RemoteViews;

import java.io.IOException;


/**
 * Created with IntelliJ IDEA.
 * User: krizai
 * Date: 1/18/14
 * Time: 8:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigureActivity extends Activity {
    private static final int PICK_CONTACT = 1;

    private boolean firstStart = true;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
    }
    public void onResume() {
        super.onResume();
        if(firstStart) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
            startActivityForResult(intent, PICK_CONTACT);
            firstStart = false;
        }
    }
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (PICK_CONTACT):
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    Cursor c = getContentResolver().query(contactData, null, null, null, null);
                    if (c != null && c.moveToFirst()) {
                        String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                        String photoUriString = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));
                        final Uri photoUri = photoUriString != null ? Uri.parse(photoUriString) : null;
                        String hasPhone = c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                        if (hasPhone != null && hasPhone.equalsIgnoreCase("1")) {
                            final Cursor phones = getContentResolver().query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                                    null, null);
                            phones.moveToFirst();
                            if(phones.getCount() == 1){
                                configureWidget(photoUri, phones);
                            }else {
                                (new AlertDialog.Builder(this)).setCursor(phones, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        phones.moveToPosition(which);
                                        configureWidget(photoUri, phones);
                                    }
                                }, "data1").show();

                            }
                        }else{
                            (new AlertDialog.Builder(this)).setMessage(R.string.no_phone).show();
                        }
                    }
                }
        }
    }

    private void configureWidget(Uri photoUri, Cursor phones) {
        String phone = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA1));
        int phoneTypeId = phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA2));
        String phoneType = (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(getResources(), phoneTypeId, "");
        configureWidget(phone,phoneType, photoUri);
    }

    private void configureWidget(String phone, String phoneType, Uri photoUri){
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {

            int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);

            SharedPreferences prefs = getSharedPreferences(PhoneWidgetProvider.WIDGET_PREFS_ID+appWidgetId, Context.MODE_PRIVATE);
            SharedPreferences.Editor prefsEditor = prefs.edit();
            prefsEditor.putString(PhoneWidgetProvider.WIDGET_PREFS_PHONE_KEY, phone);
            prefsEditor.putString(PhoneWidgetProvider.WIDGET_PREFS_TYPE_KEY, phoneType);
            prefsEditor.putString(PhoneWidgetProvider.WIDGET_PREFS_PHOTO_KEY, photoUri.toString());
            prefsEditor.commit();

            PhoneWidgetProvider.updateWidget(this,appWidgetId);

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    }
}