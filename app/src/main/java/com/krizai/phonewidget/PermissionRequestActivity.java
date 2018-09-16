package com.krizai.phonewidget;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

public class PermissionRequestActivity extends AppCompatActivity {

    public static final String PERMISSIONS_KEY = "PERMISSIONS_KEY";
    public static final int PERMISSIONS_REQUEST = 100;

    private String permission;
    private TextView descriptionLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_request);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        descriptionLabel = findViewById(R.id.description_label);

        permission = getIntent().getStringExtra(PERMISSIONS_KEY);
        if(permission == null){
            return;
        }
        switch (permission){
            case Manifest.permission.READ_CONTACTS:
                descriptionLabel.setText(R.string.contacts_permission_request_text);
                break;
            case Manifest.permission.CALL_PHONE:
                descriptionLabel.setText(R.string.call_permission_request_text);
                break;
        }
    }

    public void onGrant(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, PERMISSIONS_REQUEST);
        }
    }

    public void onCancel(View view) {
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateMyWidgets(this);
                finish();
            }
        }
    }

    private void updateMyWidgets(Context context) {
        AppWidgetManager man = AppWidgetManager.getInstance(context);
        int[] ids = man.getAppWidgetIds(
                new ComponentName(context,PhoneWidgetProvider.class));
        Intent updateIntent = new Intent();
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(PhoneWidgetProvider.WIDGET_IDS_KEY, ids);
        context.sendBroadcast(updateIntent);
    }

}
