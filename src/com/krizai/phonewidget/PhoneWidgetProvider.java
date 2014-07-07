package com.krizai.phonewidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * Created with IntelliJ IDEA.
 * User: krizai
 * Date: 1/10/14
 * Time: 11:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class PhoneWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.main);
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    }

}
