package com.krizai.phonewidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;


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
    private static final String GLOBAL_WIDGET_PREFS = "com.krizai.phonewidget.GLOBAL_WIDGET_PREFS";
    private static final String WELCOME_SHOWN = "com.krizai.phonewidget.GLOBAL_WIDGET_PREFS.WELCOME_SHOWN";
    private static final String PRIVACY_POLICY_PATH = "http://krizai.github.io/QuickDialWidget/privacy.html";

    private boolean contactPickerStarted = false;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
    }
    public void onResume() {
        super.onResume();
        if(!contactPickerStarted) {
            if(isWelcomeShown()) {
                startContactPicker();
            }else{
                showWelcome();
            }
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
        outState.putBoolean(FIRST_START_ACTIVITY_FLAG_KEY, this.contactPickerStarted);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.contactPickerStarted = savedInstanceState.getBoolean(FIRST_START_ACTIVITY_FLAG_KEY, false);
    }

    private boolean isWelcomeShown(){
        SharedPreferences prefs = getSharedPreferences(GLOBAL_WIDGET_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(WELCOME_SHOWN, false);
    }

    private void showWelcome(){
        (new AlertDialog.Builder(this))
                .setMessage(R.string.welcome_text)
                .setNegativeButton(R.string.privacy_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        showPrivacyPolicy();
                    }
                })
                .setPositiveButton(R.string.continue_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startContactPicker();
                    }
                })
                .show();


        SharedPreferences prefs = getSharedPreferences(GLOBAL_WIDGET_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putBoolean(WELCOME_SHOWN, true);
        prefsEditor.commit();
    }

    private void showPrivacyPolicy() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_PATH));
        startActivity(browserIntent);
    }

    private void startContactPicker() {
        contactPickerStarted = true;
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
        startActivityForResult(intent, PICK_CONTACT);
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