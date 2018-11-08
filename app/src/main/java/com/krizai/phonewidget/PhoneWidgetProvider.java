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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
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

    static final int WIDGET_WIDTH = 70;

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
            String photoUriString = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI));
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
            contactPhoto = prepareBitmapWithUserPic(context, contactPhoto);
            views.setImageViewBitmap(R.id.photoImage, contactPhoto);
        }else{
            contactPhoto = prepareBitmapWithDefaultUserpic(context);
            views.setImageViewBitmap(R.id.photoImage, contactPhoto);
        }
        views.setTextViewText(R.id.typeText, phoneType);
        views.setTextViewText(R.id.nameText, phoneName);

        Intent callIntent = new Intent(context, PhoneWidgetProvider.class);
        callIntent.setAction(CALL_ACTION);
        callIntent.setData(Uri.parse("tel:"+phone));

        PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context, 0, callIntent, 0);

        views.setOnClickPendingIntent(R.id.overlay_button, actionPendingIntent);


        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static Bitmap prepareBitmapWithUserPic(Context context, Bitmap bitmap) {
        int widgetWidth = dpToPx(context, WIDGET_WIDTH - 10);
        Size bitmapSize = new Size(widgetWidth, widgetWidth);
        Bitmap output = Bitmap.createBitmap(bitmapSize.width, bitmapSize.height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint xferPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xferPaint.setColor(Color.RED);

        canvas.drawRoundRect(new RectF(0,0,bitmapSize.width,bitmapSize.height),
                dpToPx(context, 3),
                dpToPx(context, 3),
                xferPaint);

        xferPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        canvas.drawBitmap(bitmap,
                new Rect((bitmap.getWidth() - bitmap.getHeight())/2,0, bitmap.getWidth(),bitmap.getHeight()),
                new Rect(0,0,bitmapSize.width, bitmapSize.height),
                xferPaint);

        Bitmap iconBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.call);
        Paint plainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawBitmap(iconBitmap,
                bitmapSize.width - iconBitmap.getWidth() - dpToPx(context, 4),
                dpToPx(context, 4),
                plainPaint);

        return output;
    }


    private static Bitmap prepareBitmapWithDefaultUserpic(Context context) {
        int widgetWidth = dpToPx(context, WIDGET_WIDTH - 10);
        Size bitmapSize = new Size(widgetWidth, widgetWidth);
        Bitmap output = Bitmap.createBitmap(bitmapSize.width, bitmapSize.height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint xferPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xferPaint.setColor(Color.rgb(203, 203, 203));

        canvas.drawRoundRect(new RectF(0,0,bitmapSize.width,bitmapSize.height),
                             dpToPx(context, 3),
                             dpToPx(context, 3),
                             xferPaint);

        Paint plainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Bitmap placeholderBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.default_userpic);
        canvas.drawBitmap(placeholderBitmap,
                          new Rect(0,0, placeholderBitmap.getWidth(),placeholderBitmap.getHeight()),
                          new Rect(5,10, bitmapSize.width - 5, bitmapSize.height),
                          xferPaint);
        Bitmap iconBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.call);
        canvas.drawBitmap(iconBitmap,
                          bitmapSize.width - iconBitmap.getWidth() - dpToPx(context, 4),
                          dpToPx(context, 4),
                          plainPaint);

        return output;
    }

    static private void requestPermission(@NonNull String permission, Context context){
        Intent intent = new Intent(context, PermissionRequestActivity.class);
        intent.putExtra(PermissionRequestActivity.PERMISSIONS_KEY, permission);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    private static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        Log.d("WidgetTest", "dp"+dp+ " px "+ px);
        return px;
    }
}
