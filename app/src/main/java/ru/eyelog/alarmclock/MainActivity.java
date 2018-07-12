package ru.eyelog.alarmclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener{

    Context context;
    ListView listView;
    TextView tv_noAlarms;
    AlarmsBaseAdapter alarmsBaseAdapter;
    List<AlarmObject> alarmObjects;

    String stNoteDelete, stDelete, stCancel, stAlarmWillSing, stHoleWeek;
    String[] daysOfWeek = new String[7];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Typeface light = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf");

        setContentView(R.layout.activity_main);
        context = this;

        stNoteDelete = getString(R.string.ask_delete);
        stDelete = getString(R.string.delete);
        stCancel = getString(R.string.cancel);
        stAlarmWillSing = getString(R.string.alarm_will_sing);
        stHoleWeek = getString(R.string.hole_week);
        daysOfWeek = getResources().getStringArray(R.array.days_of_week);

        DataBase.init(context);

        listView = (ListView)findViewById(R.id.id_alarms_list);
        tv_noAlarms = (TextView)findViewById(R.id.id_tv_no_alarms);

        alarmsBaseAdapter = new AlarmsBaseAdapter();

        listView.setLongClickable(true);
        listView.setClickable(true);

        callAlarmScheduleService();

        listView.setAdapter(alarmsBaseAdapter);

        // Для редактирования выбранного будильника.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
                AlarmObject alarmObject = (AlarmObject) alarmsBaseAdapter.getItem(position);
                Intent intent = new Intent(context, AlarmCreatorActivity.class);
                intent.putExtra("editionMode", true);
                intent.putExtra("mainId", alarmObject.getId());
                startActivityForResult(intent, 0);
            }
        });

        // Для удаления выбранного будильника с помощью контекстного меню.
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {

                final AlarmObject alarmObject = (AlarmObject)alarmsBaseAdapter.getItem(position);
                AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                dialog.setTitle(stNoteDelete);
                dialog.setPositiveButton(stDelete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        DataBase.init(context);
                        DataBase.deleteEntry(alarmObject.getId());
                        callAlarmScheduleService();

                        updateAlarmList();
                    }
                });
                dialog.setNegativeButton(stCancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                dialog.show();

                return true;
            }
        });

        updateAlarmList();


        TextView b1 = (TextView) findViewById(R.id.id_tv_main_title);
        TextView b2 = (TextView) findViewById(R.id.id_tv_no_alarms);
        TextView b3 = (TextView) findViewById(R.id.button3);
        b1.setTypeface(light);
        b2.setTypeface(light);
        b3.setTypeface(light);

    }

    // Создание нового будильника.
    public void onNew(View v){
        Intent intent = new Intent(context, AlarmCreatorActivity.class);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        updateAlarmList();
    }

    public Context getContext(){
        return context;
    }

    // Запуск службы.
    protected void callAlarmScheduleService() {
        Intent alarmServiceIntent = new Intent(this, FirstBroadcastReceiver.class);
        sendBroadcast(alarmServiceIntent, null);
    }

    // Обновление списка будильников.
    public void updateAlarmList(){
        DataBase.init(getApplicationContext());
        alarmObjects = DataBase.getAll();
        alarmsBaseAdapter.setAlarms(alarmObjects);

        alarmsBaseAdapter.notifyDataSetChanged();
        if(alarmObjects.size() > 0){
            tv_noAlarms.setVisibility(View.GONE);
        }else{
            tv_noAlarms.setVisibility(View.VISIBLE);
        }
    }

    // Адаптер списка будильников.
    class AlarmsBaseAdapter extends BaseAdapter{
        LayoutInflater adapterInflater;
        TextView tvNote, tvTime, tvDays;
        CheckBox checkBox;
        View row;


        private List<AlarmObject> alarms = new ArrayList<AlarmObject>();

        public void setAlarms(List<AlarmObject> alarms) {
            this.alarms = alarms;
        }

        @Override
        public int getCount() {
            return alarms.size();
        }

        @Override
        public Object getItem(int position) {
            return alarms.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {

            row = view;
            if(view == null){
                adapterInflater = getLayoutInflater();
                row = adapterInflater.inflate(R.layout.item_alarm, viewGroup, false);
            }

            tvNote = (TextView)row.findViewById(R.id.tv_alarm_note);
            tvTime = (TextView)row.findViewById(R.id.tv_alarm_time);
            tvDays = (TextView)row.findViewById(R.id.tv_alarm_days);

            tvNote.setText(alarms.get(position).getAlarmName());
            tvTime.setText(alarms.get(position).getAlarmTimeString());

            StringBuilder daysStringBuilder = new StringBuilder();
            if(alarms.get(position).getDays().length == AlarmObject.Day.values().length){
                daysStringBuilder.append(stHoleWeek);
            }else{

                Arrays.sort(alarms.get(position).getDays(), new Comparator<AlarmObject.Day>() {
                    @Override
                    public int compare(AlarmObject.Day lhs, AlarmObject.Day rhs) {

                        return lhs.ordinal() - rhs.ordinal();
                    }
                });

                for(AlarmObject.Day d : alarms.get(position).getDays()){
                    switch(d){
                        case MONDAY:
                            daysStringBuilder.append(daysOfWeek[0]);
                            break;
                        case TUESDAY:
                            daysStringBuilder.append(daysOfWeek[1]);
                            break;
                        case WEDNESDAY:
                            daysStringBuilder.append(daysOfWeek[2]);
                            break;
                        case THURSDAY:
                            daysStringBuilder.append(daysOfWeek[3]);
                            break;
                        case FRIDAY:
                            daysStringBuilder.append(daysOfWeek[4]);
                            break;
                        case SATURDAY:
                            daysStringBuilder.append(daysOfWeek[5]);
                            break;
                        case SUNDAY:
                            daysStringBuilder.append(daysOfWeek[6]);
                            break;
                    }
                    daysStringBuilder.append(", ");
                }
                daysStringBuilder.setLength(daysStringBuilder.length()-2);
            }

            tvDays.setText(daysStringBuilder.toString());

            checkBox = (CheckBox) row.findViewById(R.id.cb_alarm_active);
            checkBox.setChecked(alarms.get(position).getAlarmActive());

            // Через метку вида передаём слушателю позицию выбранного.
            checkBox.setTag(position);
            checkBox.setOnClickListener(MainActivity.this);

            return row;
        }
    }

    // Сам класс является слушателем.
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.cb_alarm_active) {
            CheckBox checkBox = (CheckBox) view;

            DataBase.updateActive(checkBox.isChecked(), alarmObjects.get((int)view.getTag()).getId());
            callAlarmScheduleService();
            if (checkBox.isChecked()) {
                Toast.makeText(context, alarmObjects.get((int)view.getTag()).getAlarmName()
                        + " " + stAlarmWillSing + " " + alarmObjects.get((int)view.getTag()).getTimeUntilNextAlarmMessage(),
                        Toast.LENGTH_LONG).show();
            }else {
                AlarmService.notificationManager.cancel(1);
            }
        }
    }
}