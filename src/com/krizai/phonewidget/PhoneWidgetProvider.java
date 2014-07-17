package com.krizai.phonewidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
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
    static final String WIDGET_PREFS_NAME_KEY = "WIDGET_PREFS_NAME_KEY";
    static final String WIDGET_PREFS_PHOTO_KEY = "WIDGET_PREFS_PHOTO_KEY";

    static final float WIDGET_CIRCLE_WIDTH = 4;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for( int appWidgetId : appWidgetIds){
            updateWidget(context, appWidgetId);
        }
    }

    static void updateWidget(Context context, int appWidgetId){

        SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS_ID+appWidgetId, Context.MODE_PRIVATE);
        String phone = prefs.getString(WIDGET_PREFS_PHONE_KEY, "");
        String phoneName = prefs.getString(WIDGET_PREFS_NAME_KEY, "");
        String phoneType = prefs.getString(WIDGET_PREFS_TYPE_KEY, "");
        String path  = prefs.getString(WIDGET_PREFS_PHOTO_KEY, null);
        Uri photoUri = path != null ? Uri.parse(path) : null;

        updateWidget(context, appWidgetId, phone, phoneName, phoneType, photoUri);
    }

    private static void updateWidget(Context context, int appWidgetId, String phone, String phoneName,
                                     String phoneType, Uri photoUri) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main);

        if(photoUri != null) {
            Bitmap bm = null;
            try {
                bm = MediaStore.Images.Media.getBitmap(context.getContentResolver(), photoUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            bm = prepareBitmap(bm);
            views.setImageViewBitmap(R.id.photoImage, bm);
        }else{
            views.setImageViewResource(R.id.photoImage, R.drawable.default_userpic);
        }
        views.setTextViewText(R.id.typeText, phoneType);
        views.setTextViewText(R.id.nameText, phoneName);

        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:"+phone));

        PendingIntent actionPendingIntent = PendingIntent.getActivity(context, 0, callIntent, 0);

        views.setOnClickPendingIntent(R.id.plateImage, actionPendingIntent);


        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static Bitmap prepareBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final float halfWidth = canvas.getWidth()/2;
        final float halfHeight = canvas.getHeight()/2;
        final float radius = Math.max(halfWidth, halfHeight) -3;

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
}
