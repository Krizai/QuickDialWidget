package com.krizai.phonewidget;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: krizai
 * Date: 1/10/14
 * Time: 11:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class PhoneWidgetProvider extends AppWidgetProvider {

    public static final String REFRESH_WIDGET_ACTION = "REFRESH_WIDGET";
    public static final String CALL_ACTION = "CALL_ACTION";

    public static final String WIDGET_IDS_KEY = "WIDGET_IDS_KEY";

    static final String WIDGET_PREFS_ID = "WIDGET_PREFS_ID";

    static final String WIDGET_PREFS_PHONE_KEY = "WIDGET_PREFS_PHONE_KEY";
    static final String WIDGET_PREFS_TYPE_KEY = "WIDGET_PREFS_TYPE_KEY";
    static final String WIDGET_PREFS_NAME_KEY = "WIDGET_PREFS_NAME_KEY";
    static final String WIDGET_PREFS_PHOTO_KEY = "WIDGET_PREFS_PHOTO_KEY";
    static final String WIDGET_PREFS_CONTACT_ID_KEY = "WIDGET_PREFS_CONTACT_ID_KEY";

    static final float WIDGET_CIRCLE_WIDTH = 4;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        switch (action) {
            case REFRESH_WIDGET_ACTION:
                if (intent.hasExtra(WIDGET_IDS_KEY)) {
                    int[] ids = intent.getExtras().getIntArray(WIDGET_IDS_KEY);
                    this.onUpdate(context, AppWidgetManager.getInstance(context), ids);
                }
                break;
            case CALL_ACTION:
                Uri uri = intent.getData();
                if (uri != null) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        requestPermission(Manifest.permission.CALL_PHONE, context);
                    }else{
                        Intent callIntent = new Intent(Intent.ACTION_CALL);
                        callIntent.setData(uri);
                        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(callIntent);
                    }
                }
                break;
            default:
                super.onReceive(context, intent);
                break;
        }
    }

    static void updateWidget(Context context, int appWidgetId){
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.READ_CONTACTS, context);
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS_ID+appWidgetId, Context.MODE_PRIVATE);
        String phone = prefs.getString(WIDGET_PREFS_PHONE_KEY, "");
        String phoneName = prefs.getString(WIDGET_PREFS_NAME_KEY, "");
        String phoneType = prefs.getString(WIDGET_PREFS_TYPE_KEY, "");
        String path  = prefs.getString(WIDGET_PREFS_PHOTO_KEY, null);
        String contactId  = prefs.getString(WIDGET_PREFS_CONTACT_ID_KEY, null);
        Uri photoUri = path != null ? Uri.parse(path) : null;

        if(contactId != null) {
            updateWidget(context, appWidgetId, contactId, phone, phoneType);
        }else{
            updateWidget(context, appWidgetId, phone, phoneName, phoneType, photoUri);
        }
    }

    //New  style ( ver >1.0.3
    private static void updateWidget(Context context, int appWidgetId, String contactId,
                                     String phone, String phoneType) {
        Cursor c = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                ContactsContract.Contacts._ID +" = ?",
                new String[]{contactId}, null);

        if (c != null && c.moveToFirst()) {
            String phoneName = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
            String photoUriString = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));
            Uri photoUri = photoUriString != null ? Uri.parse(photoUriString) : null;
            updateWidget(context, appWidgetId, phone, phoneName, phoneType, photoUri);
        }else{
            updateWidget(context, appWidgetId, "", "Not Found", "Not Found", (Bitmap)null);
        }

        if (c != null) {
            c.close();
        }
    }

    //Old style ( ver <=1.0.3
    private static void updateWidget(Context context, int appWidgetId, String phone, String phoneName,
                                     String phoneType, Uri photoUri) {
        Bitmap bm = null;
        if(photoUri != null) {
            try {
                InputStream input = context.getContentResolver().openInputStream(photoUri);
                bm =  BitmapFactory.decodeStream(input);
                bm = MediaStore.Images.Media.getBitmap(context.getContentResolver(), photoUri);
                bm = prepareBitmap(bm);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        updateWidget(context, appWidgetId, phone, phoneName, phoneType, bm);
    }

    private static void updateWidget(Context context, int appWidgetId, String phone, String phoneName,
                                     String phoneType, Bitmap contactPhoto) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main);

        if(contactPhoto != null) {
            contactPhoto = prepareBitmap(contactPhoto);
            views.setImageViewBitmap(R.id.photoImage, contactPhoto);
        }else{
            views.setImageViewResource(R.id.photoImage, R.drawable.default_userpic);
        }
        views.setTextViewText(R.id.typeText, phoneType);
        views.setTextViewText(R.id.nameText, phoneName);

        Intent callIntent = new Intent(context, PhoneWidgetProvider.class);
        callIntent.setAction(CALL_ACTION);
        callIntent.setData(Uri.parse("tel:"+phone));

        PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context, 0, callIntent, 0);

        views.setOnClickPendingIntent(R.id.plateImage, actionPendingIntent);


        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static Bitmap prepareBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final float halfWidth = canvas.getWidth()/2;
        final float halfHeight = canvas.getHeight()/2;
        final float radius = Math.max(halfWidth, halfHeight) - 2;

        //Draw white circle
        final Paint whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whitePaint.setColor(Color.WHITE);
        canvas.drawCircle(halfWidth, halfHeight, radius, whitePaint);


        // Load the bitmap as a shader to the paint.
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Shader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);

        // Draw a circle with the required radius.
        canvas.drawCircle(halfWidth, halfHeight, radius - WIDGET_CIRCLE_WIDTH, paint);

        return output;
    }

    static private void requestPermission(@NonNull String permission, Context context){
        Intent intent = new Intent(context, PermissionRequestActivity.class);
        intent.putExtra(PermissionRequestActivity.PERMISSIONS_KEY, permission);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
