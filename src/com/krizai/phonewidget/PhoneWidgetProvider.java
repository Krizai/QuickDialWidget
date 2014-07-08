package com.krizai.phonewidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: krizai
 * Date: 1/10/14
 * Time: 11:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class PhoneWidgetProvider extends AppWidgetProvider {

    static final String WIDGET_PREFS_ID = "WIDGET_PREFS_ID";

    static final String WIDGET_PREFS_PHONE_KEY = "WIDGET_PREFS_PHONE_KEY";
    static final String WIDGET_PREFS_TYPE_KEY = "WIDGET_PREFS_TYPE_KEY";
    static final String WIDGET_PREFS_PHOTO_KEY = "WIDGET_PREFS_PHOTO_KEY";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for( int appWidgetId : appWidgetIds){
            updateWidget(context, appWidgetId);
        }
    }

    static void updateWidget(Context context, int appWidgetId){

        SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS_ID+appWidgetId, Context.MODE_PRIVATE);
        String phone = prefs.getString(WIDGET_PREFS_PHONE_KEY, "");
        String phoneType = prefs.getString(WIDGET_PREFS_TYPE_KEY, "");
        String path  = prefs.getString(WIDGET_PREFS_PHOTO_KEY, "");
        Uri photoUri = Uri.parse(path);

        updateWidget(context, appWidgetId, phone, phoneType, photoUri);
    }

    private static void updateWidget(Context context, int appWidgetId, String phone, String phoneType, Uri photoUri) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main);
        Bitmap bm = null;
        try {
            bm = MediaStore.Images.Media.getBitmap(context.getContentResolver(), photoUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        views.setImageViewBitmap(R.id.photoImage, bm);
        views.setTextViewText(R.id.typeText, phoneType);

        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:"+phone));

        PendingIntent actionPendingIntent = PendingIntent.getActivity(context, 0, callIntent, 0);

        views.setOnClickPendingIntent(R.id.photoImage, actionPendingIntent);


        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
