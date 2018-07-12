package ru.eyelog.alarmclock;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class SecondBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent alarmServiceIntent = new Intent(
                context,
                FirstBroadcastReceiver.class);
        context.sendBroadcast(alarmServiceIntent, null);

        Bundle bundle = intent.getExtras();
        final AlarmObject alarmObject = (AlarmObject) bundle.getSerializable("alarm");

        Intent alarmAlertActivityIntent;

        alarmAlertActivityIntent = new Intent(context, RiseActivity.class);

        alarmAlertActivityIntent.putExtra("alarm", alarmObject);

        alarmAlertActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(alarmAlertActivityIntent);
    }

}