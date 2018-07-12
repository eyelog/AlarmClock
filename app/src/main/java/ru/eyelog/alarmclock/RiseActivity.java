package ru.eyelog.alarmclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

public class RiseActivity extends Activity {

    TextView tvNote;

    private AlarmObject alarmObject;
    private MediaPlayer mediaPlayer;

    private boolean alarmActive;
    Context context;

    AlertDialog.Builder builder;
    AlertDialog dialog;
    String stOk, stNo;
    String[] dialogMessages = new String[4];

    private static PowerManager.WakeLock wl = null;

    static int clickNumberPosition, rightNumber;

    LinearLayout llManagePanel;

    Button[] buttons = new Button[4];
    int[] res_buttons = new int[]{R.id.id_button_00, R.id.id_button_01, R.id.id_button_02, R.id.id_button_03};
    AnswerClickListener[] answerClickListeners = new AnswerClickListener[4];

    int round_counter=0;
    String[] mainRound = new String[100];

    Random random;
    int difficulty_level;
    int number_of_question;
    int[] answer_order = new int[4];

    String stMainTheme;
    int number_of_rounds = 1;
    int mainThemePosition=0;
    String[] normalThemes = new String[6];

    final int ALERT_DELAY = 30000;

    // Комплект переменных для управляющей кнопки.
    boolean doNext = false, calmActive = false;
    Button btManage;
    String stCalmDown, stNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alert_activity);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Typeface light = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf");

        context = this;

        tvNote = (TextView)findViewById(R.id.id_message);

        Bundle bundle = getIntent().getExtras();
        alarmObject = (AlarmObject) bundle.getSerializable("alarm");

        stOk = getString(R.string.alert_btn_y);
        stNo = getString(R.string.alert_btn_n);
        dialogMessages[0] = getString(R.string.message_cool);
        dialogMessages[1] = getString(R.string.message_good);
        dialogMessages[2] = getString(R.string.message_normal);
        dialogMessages[3] = getString(R.string.message_bad);
        stCalmDown = getString(R.string.alert_calm);
        stNext = getString(R.string.next_round);

        for (int i=0; i<6; i++){
            normalThemes[i] = getString(getResources().getIdentifier("theme_0"+i+"_name", "string", getPackageName()));
        }

        stMainTheme = alarmObject.getAlarmName();
        difficulty_level = alarmObject.getDiffLevel();
        number_of_rounds = alarmObject.getNumberOfQuestions()-1;


        // Определяемся с выбранной темой.
        for (int i=0; i<6; i++){
            if(stMainTheme.equals(normalThemes[i])){
                mainThemePosition = i;
                break;
            }
        }

        Log.e("difficulty_level", String.valueOf(difficulty_level));

        mainRound = getResources().getStringArray(getResources().getIdentifier("theme_0"+mainThemePosition+"_"+difficulty_level, "array", getPackageName()));

        /*
        if (alarmObject != null) {
            tvNote.setText(alarmObject.getAlarmName());
        }
        */

        llManagePanel = (LinearLayout)findViewById(R.id.id_ll_manage_panel);
        llManagePanel.setVisibility(View.VISIBLE);

        for (int i=0; i<4; i++){
            buttons[i] = (Button)findViewById(res_buttons[i]);
            answerClickListeners[i] = new AnswerClickListener(i);
            buttons[i].setOnClickListener(answerClickListeners[i]);
        }

        btManage = (Button)findViewById(R.id.id_button_04);
        btManage.setText(stCalmDown);

        roundBlackSmith();

        startAlarm();

        TextView b1 = (TextView) findViewById(R.id.id_message);
        TextView b2 = (TextView) findViewById(R.id.id_button_00);
        TextView b3 = (TextView) findViewById(R.id.id_button_01);
        TextView b4 = (TextView) findViewById(R.id.id_button_02);
        TextView b5 = (TextView) findViewById(R.id.id_button_03);
        TextView b6 = (TextView) findViewById(R.id.id_button_04);
        //TextView b7 = (TextView) findViewById(R.id.id_button_05);
        b1.setTypeface(light);
        b2.setTypeface(light);
        b3.setTypeface(light);
        b4.setTypeface(light);
        b5.setTypeface(light);
        b6.setTypeface(light);
        //b7.setTypeface(light);
    }

    public void roundBlackSmith(){

        // Выбираем случайным образом один из 20-ти вопросов.
        random = new Random();
        number_of_question =  random.nextInt(20);

        // Сразу проявляем вопрос.
        tvNote.setText(mainRound[number_of_question*5]);

        // Далее расфасовываем ответы по кнопкам.
        // Сначала сгенерируем случайный порядок для четырёх кнопок.
        boolean got = true;
        int a = 1;
        answer_order[0] = random.nextInt(4);
        here: do{
            answer_order[a] = random.nextInt(4);
            for (int i=0; i<a; i++){
                if(answer_order[i]==answer_order[a]){
                    continue here;
                }
            }
            a++;
            if(a==4)got=false;
        }while (got);

        // Теперь расставим ответы по кнопкам.
        // А заодно и представим правильный номер.
        for (int i=0; i<4; i++){
            buttons[answer_order[i]].setText(mainRound[number_of_question*5+i+1]);
        }
        rightNumber = answer_order[0];
    }

    // Метод проверки правильности ответа.
    public void checkAnswer(){

        if(clickNumberPosition==rightNumber){
            // Правильный ответ.
            buttons[clickNumberPosition].setBackgroundColor(Color.GREEN);

            builder = new AlertDialog.Builder(context);
            builder.setTitle(dialogMessages[round_counter]);
            builder.setCancelable(false);

            builder.setPositiveButton(stOk, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    onStopAlert();
                    dialog.cancel();
                }
            });

            dialog = builder.create();
            dialog.show();

        }else {
            // Неправильный ответ.
            buttons[clickNumberPosition].setBackgroundColor(Color.RED);
            buttons[rightNumber].setBackgroundColor(Color.GREEN);

            if(round_counter<number_of_rounds){

                // Проявляем панель управления.
                btManage.setText(stNext);
                llManagePanel.setVisibility(View.VISIBLE);

                // На всякий случай диактивируем кнопки.
                for(int i=0; i<4; i++){
                    buttons[i].setClickable(false);
                }

                doNext=true;
            }else {
                // Вообще ничего не угадали.
                builder = new AlertDialog.Builder(context);
                builder.setTitle(dialogMessages[3]);
                builder.setCancelable(false);

                builder.setPositiveButton(stNo, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onStopAlert();
                        Intent browserIntent = new
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://vk.com/kurs_na_vuz"));
                        startActivity(browserIntent);
                    }
                });

                dialog = builder.create();
                dialog.show();
            }
        }
    }

    // Метод запуска сигнала.
    private void startAlarm() {

        // Будим телефон.
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (wl == null)
            wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Будильник");
        wl.acquire();

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        // Включаем музыку
        if (alarmObject.getAlarmTonePath() != "") {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setVolume(1.0f, 1.0f);
                mediaPlayer.setDataSource(this,
                        Uri.parse(alarmObject.getAlarmTonePath()));
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mediaPlayer.setLooping(true);
                mediaPlayer.prepare();
                mediaPlayer.start();

            } catch (Exception e) {
                mediaPlayer.release();
                alarmActive = false;
            }
        }
    }

    // Остановка будильника.
    public void onStopAlert(){
        alarmActive = false;

        try {
            mediaPlayer.stop();
        } catch (IllegalStateException ise) {
            ise.printStackTrace();
        }
        try {
            mediaPlayer.release();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (wl != null)
                wl.release();
        } catch (Exception e) {
            e.printStackTrace();
        }

        finish();
    }

    // Притушить на нескоько секунд.
    public void onManage(View v){
        if(doNext){
            round_counter++;
            roundBlackSmith();
            for (int i=0; i<4; i++){
                buttons[i].setClickable(true);
                buttons[i].setBackgroundColor(Color.WHITE);
            }
            doNext=false;
            btManage.setText(stCalmDown);
            if(calmActive){
                llManagePanel.setVisibility(View.GONE);
            }
        }else {
            // Сразу приглушаем музыку.
            try {
                mediaPlayer.setVolume(0.1f, 0.1f);
            }catch (Exception e) {
                mediaPlayer.release();
                alarmActive = false;
            }
            calmActive=true;
            llManagePanel.setVisibility(View.GONE);

            // Возарвщение к нормальной громкоскти происходит через нескоько секунд.
            new Handler() {{
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mediaPlayer.setVolume(1.0f, 1.0f);
                        }catch (Exception e) {
                            mediaPlayer.release();
                            alarmActive = false;
                        }
                        calmActive=false;
                        btManage.setText(stCalmDown);
                        llManagePanel.setVisibility(View.VISIBLE);
                    }
                }, ALERT_DELAY);
            }};
        }
    }

    /*
    // Отложить побудку на 5 минут.
    public void onPauseAlert(View v){
        Calendar now = Calendar.getInstance();

        Calendar new_c = (Calendar) now.clone();
        new_c.add(Calendar.MINUTE, 5);

        alarmObject.setAlarmTime(new_c);
        alarmObject.schedule(getApplicationContext());

        try {
            if (wl != null)
                wl.release();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Toast.makeText(context, alarmObject.getTimeUntilNextAlarmMessage(), Toast.LENGTH_LONG).show();
        finish();

    }
    */

    @Override
    public void onBackPressed() {
        if (!alarmActive)
            super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        try {
            mediaPlayer.stop();
        } catch (Exception e) {

        }
        try {
            mediaPlayer.release();
        } catch (Exception e) {

        }
        super.onDestroy();
    }

    class AnswerClickListener implements View.OnClickListener{

        int position;

        AnswerClickListener(int position){
            this.position = position;
        }

        @Override
        public void onClick(View view) {
            clickNumberPosition = position;
            checkAnswer();
        }
    }
}
