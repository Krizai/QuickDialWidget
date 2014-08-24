package com.krizai.phonewidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Created with IntelliJ IDEA.
 * User: krizai
 * Date: 1/18/14
 * Time: 8:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigureActivity extends Activity {
    private static final int PICK_CONTACT = 1;

    private static final String FIRST_START_ACTIVITY_FLAG_KEY = "FIRST_START_ACTIVITY_FLAG_KEY";

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
                        final String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                        String hasPhone = c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                        if (hasPhone != null && hasPhone.equalsIgnoreCase("1")) {
                            final Cursor phones = getContentResolver().query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                                    null, null);
                            phones.moveToFirst();
                            if(phones.getCount() == 1){
                                configureWidget(id, phones);
                            }else {
                                (new AlertDialog.Builder(this)).setCursor(phones, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        phones.moveToPosition(which);
                                        configureWidget(id, phones);
                                    }
                                }, "data1").show();

                            }
                        }else{
                            (new AlertDialog.Builder(this)).setMessage(R.string.no_phone).setOnCancelListener(
                                    new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            finishWithCancelResult();
                                        }
                                    }
                            ).show();
                        }
                    }
                }else{
                    finishWithCancelResult();
                }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(FIRST_START_ACTIVITY_FLAG_KEY, this.firstStart);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.firstStart = savedInstanceState.getBoolean(FIRST_START_ACTIVITY_FLAG_KEY, false);
    }


    private void configureWidget(String contactId, Cursor phones) {
        String phone = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA1));
        int phoneTypeId = phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA2));
        String phoneType = (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(getResources(), phoneTypeId, "");
        configureWidget(contactId, phone, phoneType);
    }

    private void configureWidget(String contactId, String phone, String phoneType){
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {

            int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);

            SharedPreferences prefs = getSharedPreferences(PhoneWidgetProvider.WIDGET_PREFS_ID+appWidgetId, Context.MODE_PRIVATE);
            SharedPreferences.Editor prefsEditor = prefs.edit();
            prefsEditor.putString(PhoneWidgetProvider.WIDGET_PREFS_PHONE_KEY, phone);
            prefsEditor.putString(PhoneWidgetProvider.WIDGET_PREFS_TYPE_KEY, phoneType);
            prefsEditor.putString(PhoneWidgetProvider.WIDGET_PREFS_CONTACT_ID_KEY, contactId);
            prefsEditor.commit();

            PhoneWidgetProvider.updateWidget(this,appWidgetId);

            finishWithOkResult(appWidgetId);
        }
    }

    private void finishWithOkResult(int appWidgetId) {
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private void finishWithCancelResult() {
        Intent resultValue = new Intent();
        setResult(RESULT_CANCELED, resultValue);
        finish();
    }


}