package ru.eyelog.alarmclock;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class AlarmService extends Service {

    Context context;
    Notification notification;
    public static NotificationManager notificationManager;
    NotificationCompat.Builder builder;
    Intent mIntent;
    PendingIntent pIntent;

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onCreate() {
        Log.d(this.getClass().getSimpleName(),"onCreate()");
        super.onCreate();
    }

    private AlarmObject getNext(){
        Set<AlarmObject> alarmQueue = new TreeSet<AlarmObject>(new Comparator<AlarmObject>() {
            @Override
            public int compare(AlarmObject lhs, AlarmObject rhs) {
                int result = 0;
                long diff = lhs.getAlarmTime().getTimeInMillis() - rhs.getAlarmTime().getTimeInMillis();
                if(diff>0){
                    return 1;
                }else if (diff < 0){
                    return -1;
                }
                return result;
            }
        });

        DataBase.init(getApplicationContext());
        List<AlarmObject> alarms = DataBase.getAll();

        for(AlarmObject alarm : alarms){
            if(alarm.getAlarmActive())
                alarmQueue.add(alarm);
        }
        if(alarmQueue.iterator().hasNext()){
            return alarmQueue.iterator().next();
        }else{
            return null;
        }
    }

    @Override
    public void onDestroy() {
        DataBase.deactivate();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AlarmObject alarmObject = getNext();
        if(null != alarmObject){
            alarmObject.schedule(getApplicationContext());
            Log.d(this.getClass().getSimpleName(),alarmObject.getTimeUntilNextAlarmMessage());

            context = getApplicationContext();
            builder = new NotificationCompat.Builder(context )
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle(context.getResources().getString(R.string.notification_title));

            mIntent = new Intent(context, MainActivity.class);
            mIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            pIntent = PendingIntent.getActivity(context, 1 , mIntent, 0);
            builder.setContentIntent(pIntent);
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            notification = builder.build();
            notificationManager.notify(1, notification);

        }else{
            Intent myIntent = new Intent(getApplicationContext(), SecondBroadcastReceiver.class);
            myIntent.putExtra("alarm", new AlarmObject());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager alarmManager = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);

            if(notificationManager!=null){
                notificationManager.cancel(1);
            }

            alarmManager.cancel(pendingIntent);
        }

        return START_NOT_STICKY;
    }
}
