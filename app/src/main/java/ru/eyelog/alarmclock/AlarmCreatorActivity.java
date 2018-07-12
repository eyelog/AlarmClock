package ru.eyelog.alarmclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

import ru.eyelog.alarmclock.util.IabBroadcastReceiver;
import ru.eyelog.alarmclock.util.IabBroadcastReceiver.IabBroadcastListener;
import ru.eyelog.alarmclock.util.IabHelper;
import ru.eyelog.alarmclock.util.IabResult;
import ru.eyelog.alarmclock.util.Inventory;
import ru.eyelog.alarmclock.util.Purchase;

public class AlarmCreatorActivity extends Activity implements IabBroadcastListener {

    final String[] qweNamesArray = {"1", "2", "3", "4", "5", "6"};
    String[] arrayDifficulty = new String[3];

    SharedPreferences ADB;
    public static final String SETTINGS = "settings";
    public static final String ADBCOUNT = "adbcount";
    boolean adb = true;

    Context context;
    Intent intent;

    AlarmObject gotAlarmObject, sendAlarmObject;

    AlertDialog.Builder builder;
    AlertDialog dialog;
    LayoutInflater inflater;
    View view;
    MediaPlayer mediaPlayer;
    CountDownTimer alarmToneTimer;

    boolean editionMode;
    int mainId;
    int numberOfQuestions = 1;
    int difficultyLevel = 0;

    TimePicker timePicker;
    TextView tvDays[] = new TextView[7];
    int res_tsDays[] = new int[]{R.id.id_day_06, R.id.id_day_00, R.id.id_day_01, R.id.id_day_02, R.id.id_day_03,
            R.id.id_day_04, R.id.id_day_05};
    DaysOnClickListener[] daysOnClickListeners = new DaysOnClickListener[7];
    boolean daysActive[] = new boolean[7];
    Button buttonNote, buttonDifficulty, buttonNumbOfQwst, buttonRing, buttonStartOrStop, buttonDelete;
    String stNote, stMelody, stRington, stChNote,
            stOk, stCancel, stSilent, stDelete, stSimpleName;

    String stNoteValue;


    // Комплект переменных для механизма оплаты.
    // Progress Dialog
    ProgressDialog pDialog;

    // Класс для связи с сервером google для покупки.
    IabHelper mHelper;

    // Поучатель широкоформатных сообщений..
    IabBroadcastReceiver mBroadcastReceiver;
    IntentFilter broadcastFilter;

    // Серверный идентификатор доступа.
    static final String SKU_KEY_HARD = "key_hard";

    // Пакет покупки.
    Purchase purchase_app;

    // Статус покупки.
    boolean pdm = false;

    // Переменная для хранения статуса покупки в системном файле.
    public static final String PAIDMODE = "paidmode";

    // Код запроса для потока покупки.
    static final int RC_REQUEST = 10001;

    // Открытый ключ приложения.
    final String publicKey = "ВНИМАНИЕ! Сюда нужно вставить публичный ключ из консоли проекта в Google";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alert_creator);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ADB = getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);

        loadText();
        showAlert();

        Typeface light = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf");

        context = this;

        timePicker = (TimePicker)findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        for(int i=0; i<7; i++){
            tvDays[i] = (TextView)findViewById(res_tsDays[i]);
            daysOnClickListeners[i] = new DaysOnClickListener(i);
            tvDays[i].setOnClickListener(daysOnClickListeners[i]);
            daysActive[i] = true;
            tvDays[i].setTextColor(Color.BLUE);
            tvDays[i].setTypeface(null, Typeface.BOLD);
        }
        buttonNote = (Button)findViewById(R.id.id_btNote);
        buttonDifficulty = (Button)findViewById(R.id.id_btDifclt);
        buttonNumbOfQwst = (Button)findViewById(R.id.id_btQwe);
        buttonRing = (Button)findViewById(R.id.id_btRing);
        buttonStartOrStop = (Button)findViewById(R.id.id_btStartOrStop);
        buttonDelete = (Button)findViewById(R.id.id_btDelete);

        stNote = getString(R.string.value);
        stMelody = getString(R.string.alert_ring);
        stRington = getString(R.string.alert_ring_def);
        buttonRing.setText(stMelody + " " + stRington);
        stChNote = getString(R.string.choose_value);
        stOk = getString(android.R.string.ok);
        stCancel = getString(R.string.cancel);
        stSilent = getString(R.string.silent);
        stDelete = getString(R.string.ask_delete);
        stSimpleName = getString(R.string.simple_alarm);
        stNoteValue = getString(R.string.theme_00_name);

        // Выбираем платный режим или нет.
        if(pdm){
            arrayDifficulty = getResources().getStringArray(R.array.diff_paid);
        }else {
            arrayDifficulty = getResources().getStringArray(R.array.diff_free);
        }

        sendAlarmObject = new AlarmObject();

        intent = getIntent();
        editionMode = intent.getBooleanExtra("editionMode", false);

        // Если активность запущена в режиме редактирования.
        if(editionMode){
            mainId = intent.getIntExtra("mainId", 0);
            DataBase.init(context);
            gotAlarmObject = DataBase.getAlarm(mainId);

            // Установка времени на циферблат.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                timePicker.setHour(gotAlarmObject.getAlarmTime().get(Calendar.HOUR_OF_DAY));
                timePicker.setMinute(gotAlarmObject.getAlarmTime().get(Calendar.MINUTE));
            }else {
                timePicker.setCurrentHour(gotAlarmObject.getAlarmTime().get(Calendar.HOUR_OF_DAY));
                timePicker.setCurrentMinute(gotAlarmObject.getAlarmTime().get(Calendar.MINUTE));
            }

            // Распределение дней повторения.
            for(int i=0; i<7; i++){
                for(int ii=0; ii<gotAlarmObject.getDays().length; ii++){
                    if(AlarmObject.Day.values()[i].toString().equals(gotAlarmObject.getDays()[ii].toString())){
                        daysActive[i] = true;
                        tvDays[i].setTextColor(Color.BLUE);
                        tvDays[i].setTypeface(null, Typeface.BOLD);
                        sendAlarmObject.addDay(AlarmObject.Day.values()[i]);
                        break;
                    }else{
                        daysActive[i] = false;
                        tvDays[i].setTextColor(Color.GRAY);
                        tvDays[i].setTypeface(null, Typeface.NORMAL);
                        sendAlarmObject.removeDay(AlarmObject.Day.values()[i]);
                    }
                }
            }

            // Подтверждение сообщения.
            sendAlarmObject.setAlarmName(gotAlarmObject.getAlarmName());
            buttonNote.setText(stNote + " " + gotAlarmObject.getAlarmName());

            // Уровень сложности.
            sendAlarmObject.setDiffLevel(gotAlarmObject.getDiffLevel());
            difficultyLevel = gotAlarmObject.getDiffLevel();
            buttonDifficulty.setText(arrayDifficulty[difficultyLevel]);

            // Количество вопросов.
            sendAlarmObject.setNumberOfQuestions(gotAlarmObject.getNumberOfQuestions());
            numberOfQuestions = gotAlarmObject.getNumberOfQuestions();

            // Подтверждение мелодии.
            sendAlarmObject.setAlarmToneName(gotAlarmObject.getAlarmToneName());
            sendAlarmObject.setAlarmTonePath(gotAlarmObject.getAlarmTonePath());
            buttonRing.setText(stMelody + " " + gotAlarmObject.getAlarmToneName());

            buttonDelete.setVisibility(View.VISIBLE);

        }

        TextView b1 = (TextView) findViewById(R.id.id_btNote);
        TextView b2 = (TextView) findViewById(R.id.id_btDifclt);
        TextView b3 = (TextView) findViewById(R.id.id_btQwe);
        TextView b4 = (TextView) findViewById(R.id.id_btRing);
        TextView b5 = (TextView) findViewById(R.id.id_btStartOrStop);
        TextView b6 = (TextView) findViewById(R.id.id_btDelete);
        b1.setTypeface(light);
        b2.setTypeface(light);
        b3.setTypeface(light);
        b4.setTypeface(light);
        b5.setTypeface(light);
        b6.setTypeface(light);

        mHelper = new IabHelper(this, publicKey);

        mHelper.enableDebugLogging(false);

        // Запуск настройки.
        // Запускается фоном, один раз.
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {

                if (!result.isSuccess()) {
                    complain("Problem setting up in-app billing: " + result);
                    return;
                }

                if (mHelper == null) return;

                // Динамическая регистрация для широковещательных сообщений об обновлениях покупок.
                // Мы регистрируем приемник здесь вместо того, чтобы зарегистрировать как <receiver> в манифесте
                // потому что мы всегда вызываем getPurchases() при запуске, поэтому мы можем игнорировать
                // любые трансляции в то время как приложение не работает.
                mBroadcastReceiver = new IabBroadcastReceiver(AlarmCreatorActivity.this);
                broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB (In-APP-Billing) настроен. Проверка позиций в наличии.
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error querying inventory. Another async operation in progress.");
                }
            }
        });
    }


    void showAlert() {
        if (adb) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(AlarmCreatorActivity.this);

            // Указываем Title
            alertDialog.setTitle("Как правильно настроить будильник?");

            // Указываем текст сообщение
            alertDialog.setMessage("1) Выбери нужное время.\n2) Выбери нужный день (обрати внимание, что активные дни выделены голубым цветом)\n3) Выбери предмет (на разные дни можно выбрать разные предметы, просто создай новый будильник)\n4) Выбери мелодию\n5) Жми \"сохранить\"");

            // Обработчик на нажатие ДА
            alertDialog.setPositiveButton("Я понял", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // User pressed YES button. Write Logic Here
                    adb=false;
                    saveText();

                }
            });

            // показываем Alert
            alertDialog.show();
        }
    }

    void saveText() {
        SharedPreferences.Editor editor = ADB.edit();
        editor.putBoolean(ADBCOUNT, adb);
        editor.apply();
    }
    void loadText() {
        ADB = getPreferences(MODE_PRIVATE);
        adb = ADB.getBoolean(ADBCOUNT, adb);
        pdm = ADB.getBoolean(PAIDMODE, pdm);
    }

    class DaysOnClickListener implements View.OnClickListener {

        int position;

        DaysOnClickListener(int position){
            this.position = position;
        }

        @Override
        public void onClick(View view) {

            AlarmObject.Day thisDay = AlarmObject.Day.values()[position];

            // Если день уже активирован.
            if(daysActive[position]){

                sendAlarmObject.removeDay(thisDay);
                tvDays[position].setTextColor(Color.GRAY);
                tvDays[position].setTypeface(null, Typeface.NORMAL);
                daysActive[position] = false;

            }else { // Если день не активен.

                sendAlarmObject.addDay(thisDay);
                tvDays[position].setTextColor(Color.BLUE);
                tvDays[position].setTypeface(null, Typeface.BOLD);
                daysActive[position] = true;
            }
        }
    }

    // Выбор уровня сложности.
    public void onChooseDiff(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Выбирите уровень сложности");

        // переключатели
        builder.setSingleChoiceItems(arrayDifficulty, difficultyLevel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                // Забираем значение.
                difficultyLevel = item;
                Log.e("Difficulty choise", String.valueOf(item));
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // Если в бесплатном режиме выбран платный пункт.
                if(!pdm&&difficultyLevel==2){

                    // Запускаем дилог оплаты.
                    // Включаем ожидалку.
                    setWaitScreen(true);

                    // Верификационный ключ (null).
                    String payload = "";

                    // Запуск покупки.
                    try {
                        mHelper.launchPurchaseFlow(AlarmCreatorActivity.this, SKU_KEY_HARD, RC_REQUEST, mPurchaseFinishedListener, payload);
                    } catch (IabHelper.IabAsyncInProgressException e) {
                        complain("Error launching purchase flow. Another async operation in progress.");
                        setWaitScreen(false);
                    }

                    /*
                    AlertDialog.Builder subBuilder = new AlertDialog.Builder(context);
                    subBuilder.setTitle("Оплатить доступ?");
                    subBuilder.setPositiveButton("Оплатить", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            SharedPreferences.Editor editor = ADB.edit();
                            editor.putBoolean(PAIDMODE, true);
                            editor.apply();
                            pdm=true;
                            arrayDifficulty = getResources().getStringArray(R.array.diff_paid);
                            sendAlarmObject.setDiffLevel(difficultyLevel);
                            buttonDifficulty.setText(arrayDifficulty[difficultyLevel]);
                        }
                    });
                    subBuilder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
                    subBuilder.show();

                    */
                }else {
                    // Если доступ к платной версии открыт, просто добавляем его в базу.
                    // Добавляем значение в объект sendAlarmObject.
                    sendAlarmObject.setDiffLevel(difficultyLevel);
                    buttonDifficulty.setText(arrayDifficulty[difficultyLevel]);
                }
            }
        });

        builder.show();
    }

    // Выбор количества вопросов.
    public void onChooseQwe(View V){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите количество вопросов");

        // переключатели
        builder.setSingleChoiceItems(qweNamesArray, numberOfQuestions-1, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                // Забираем значение.
                numberOfQuestions = Integer.parseInt(qweNamesArray[item]);
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // Добавляем значение в объект sendAlarmObject.
                sendAlarmObject.setNumberOfQuestions(numberOfQuestions);
            }
        });

        builder.show();
    }

    // Выбор сообщения.
    public void onChooseNote(View v){
        builder = new AlertDialog.Builder(context);
        builder.setTitle(stChNote);
        inflater = getLayoutInflater();
        view = inflater.inflate(R.layout.dialog_notes, null);
        final RadioGroup radioGroup = (RadioGroup)view.findViewById(R.id.id_dialogRadioGroup);
        builder.setCancelable(true);
        builder.setView(view);

        builder.setPositiveButton(stOk, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (radioGroup.getCheckedRadioButtonId()){
                    case R.id.id_rb_00:
                        stNoteValue = getString(R.string.theme_00_name);
                        break;
                    case R.id.id_rb_01:
                        stNoteValue = getString(R.string.theme_01_name);
                        break;
                    case R.id.id_rb_02:
                        stNoteValue = getString(R.string.theme_02_name);
                        break;
                    case R.id.id_rb_03:
                        stNoteValue = getString(R.string.theme_03_name);
                        break;
                    case R.id.id_rb_04:
                        stNoteValue = getString(R.string.theme_04_name);
                        break;
                    case R.id.id_rb_05:
                        stNoteValue = getString(R.string.theme_05_name);
                        break;
                }

                buttonNote.setText(stNote + " " + stNoteValue);

                sendAlarmObject.setAlarmName(stNoteValue);
            }
        });
        builder.setNegativeButton(stCancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialog.cancel();
            }
        });

        dialog = builder.create();
        dialog.show();
    }

    // Выбор мелодии.
    public void onChooseRing(View v){

        builder = new AlertDialog.Builder(context);
        builder.setTitle("Выбери мелодию");
        builder.setCancelable(false);

        inflater = getLayoutInflater();
        view = inflater.inflate(R.layout.rings_dialog, null);
        final ListView listView = (ListView)view.findViewById(R.id.id_ringsList);
        final RingsAdapter ringsAdapter = new RingsAdapter();
        listView.setAdapter(ringsAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                // Предрослушивание мелодии.
                sendAlarmObject.setAlarmToneName(ringsAdapter.getItem(i).toString());
                sendAlarmObject.setAlarmTonePath(ringsAdapter.getItemPatch(i).toString());
                if (sendAlarmObject.getAlarmTonePath() != null) {
                    if (mediaPlayer == null) {
                        mediaPlayer = new MediaPlayer();
                    } else {
                        if (mediaPlayer.isPlaying())
                            mediaPlayer.stop();
                        mediaPlayer.reset();
                    }
                    try {
                        mediaPlayer.setVolume(0.3f, 0.3f);
                        mediaPlayer.setDataSource(context, Uri.parse(sendAlarmObject.getAlarmTonePath()));
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                        mediaPlayer.setLooping(false);
                        mediaPlayer.prepare();
                        mediaPlayer.start();

                        // Проигрыватель работает 5 секунд.
                        if (alarmToneTimer != null)
                            alarmToneTimer.cancel();
                        alarmToneTimer = new CountDownTimer(5000, 5000) {
                            @Override
                            public void onTick(long millisUntilFinished) {

                            }

                            @Override
                            public void onFinish() {
                                try {
                                    if (mediaPlayer.isPlaying())
                                        mediaPlayer.stop();
                                } catch (Exception e) {

                                }
                            }
                        };
                        alarmToneTimer.start();
                    } catch (Exception e) {
                        try {
                            if (mediaPlayer.isPlaying())
                                mediaPlayer.stop();
                        } catch (Exception e2) {

                        }
                    }
                }
            }
        });

        builder.setPositiveButton(stOk, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                buttonRing.setText(stMelody + " " + sendAlarmObject.getAlarmToneName());
                dialog.cancel();
            }
        });

        builder.setNegativeButton(stCancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialog.cancel();
            }
        });

        builder.setView(view);
        dialog = builder.create();
        dialog.show();
    }

    class RingsAdapter extends BaseAdapter{

        View row;
        LayoutInflater adapterInflater;
        TextView tvRing;

        String [] alarmTones;
        String [] alarmTonePaths;

        RingsAdapter(){

            RingtoneManager ringtoneMgr = new RingtoneManager(context);

            ringtoneMgr.setType(RingtoneManager.TYPE_ALARM);

            Cursor alarmsCursor = ringtoneMgr.getCursor();

            alarmTones = new String[alarmsCursor.getCount()+1];
            alarmTones[0] = stSilent;
            alarmTonePaths = new String[alarmsCursor.getCount()+1];
            alarmTonePaths[0] = "";

            if (alarmsCursor.moveToFirst()) {
                do {
                    alarmTones[alarmsCursor.getPosition()+1] = ringtoneMgr.getRingtone(alarmsCursor.getPosition()).getTitle(context);
                    alarmTonePaths[alarmsCursor.getPosition()+1] = ringtoneMgr.getRingtoneUri(alarmsCursor.getPosition()).toString();
                }while(alarmsCursor.moveToNext());
            }

            alarmsCursor.close();
        }

        @Override
        public int getCount() {
            return alarmTones.length;
        }

        @Override
        public Object getItem(int i) {
            return alarmTones[i];
        }

        public Object getItemPatch(int i) {
            return alarmTonePaths[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            row = view;
            if(view == null){
                adapterInflater = getLayoutInflater();
                row = adapterInflater.inflate(android.R.layout.simple_list_item_1, viewGroup, false);
            }

            tvRing = (TextView)row.findViewById(android.R.id.text1);

            tvRing.setText(alarmTones[i]);

            return row;
        }
    }

    // Изменение активности/сохранение.
    public void onSave(View v){

        // Собираем данные с TimePicker.
        int hours, minutes;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hours = timePicker.getHour();
            minutes = timePicker.getMinute();
        }else {
            hours = timePicker.getCurrentHour();
            minutes = timePicker.getCurrentMinute();
        }
        Calendar newAlarmTime = Calendar.getInstance();
        newAlarmTime.set(Calendar.HOUR_OF_DAY, hours);
        newAlarmTime.set(Calendar.MINUTE, minutes);
        newAlarmTime.set(Calendar.SECOND, 0);
        sendAlarmObject.setAlarmTime(newAlarmTime);

        // Если не выбрано имя.
        // Присваиваем по умолчанию.
        if(sendAlarmObject.getAlarmName().equals("")){
            sendAlarmObject.setAlarmName(stNoteValue);
        }

        // Если не выбран уровень сложности.
        // Вообще он по умолчанию = 0, т.е. "Лёгкий"
        //sendAlarmObject.setDiffLevel(gotAlarmObject.getDiffLevel());

        // Если не выбрано количество вопросов.
        // По умолчанию ставим - 1.
        if(sendAlarmObject.getNumberOfQuestions()==0){
            sendAlarmObject.setNumberOfQuestions(numberOfQuestions);
        }

        // Если не выбрана мелодия.
        // Используем системную, по умолчанию.
        if(sendAlarmObject.getAlarmToneName()==null){
            sendAlarmObject.setAlarmToneName(RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)).getTitle(context));
        }

        // Остальные данные уже присвоены.
        DataBase.init(context);
        if (!editionMode) {
            DataBase.create(sendAlarmObject);
        } else {
            DataBase.update(sendAlarmObject, mainId);
        }

        Intent alarmServiceIntent = new Intent(context, FirstBroadcastReceiver.class);
        sendBroadcast(alarmServiceIntent, null);

        Toast.makeText(context, sendAlarmObject.getTimeUntilNextAlarmMessage(), Toast.LENGTH_LONG).show();
        finish();

    }

    // Удаление будильника
    public void onDelete(View v){

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(stDelete);
        dialog.setPositiveButton(stOk, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                DataBase.init(context);
                DataBase.deleteEntry(mainId);
                finish();
            }
        });
        dialog.setNegativeButton(stCancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }



    // Комплект слушателей.
    // Слушатель запроса купленных позиций.
    // В данном случае используется при покупках вне приложения.
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {

            if (mHelper == null) return;
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }

            // Сначала проверяем не куплено ли всё приложение.
            purchase_app = inventory.getPurchase(SKU_KEY_HARD);
            pdm = (purchase_app != null && verifyDeveloperPayload(purchase_app));
            if(pdm){
                // Если есть ключ от всего приложения, применим его.
                try {
                    mHelper.consumeAsync(inventory.getPurchase(SKU_KEY_HARD), mConsumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error consuming app. Another async operation in progress.");
                }
                //return;
            }

            //setWaitScreen(false);
        }
    };

    // Слушатель завершения потока покупки.
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (mHelper == null){
                return;
            }

            if (result.isFailure()) {
                return;
            }

            if (!verifyDeveloperPayload(purchase)) {
                return;
            }

            // Применяем ключ.
            try {
                mHelper.consumeAsync(purchase, mConsumeFinishedListener);
            } catch (IabHelper.IabAsyncInProgressException e) {
                complain("Error consuming key. Another async operation in progress.");
                return;
            }

            setWaitScreen(false);
        }
    };

    // Слушатель применения ключа.
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            if (mHelper == null) return;

            if (result.isSuccess()) {

                pdm=true;
                SharedPreferences.Editor editor = ADB.edit();
                editor.putBoolean(PAIDMODE, pdm);
                editor.apply();
                arrayDifficulty = getResources().getStringArray(R.array.diff_paid);
                sendAlarmObject.setDiffLevel(difficultyLevel);
                buttonDifficulty.setText(arrayDifficulty[difficultyLevel]);

                setResult(RESULT_OK);
                finish();
            }
            else {
                complain("Error while consuming: " + result);
            }

            setWaitScreen(false);
        }
    };


    // Комплект служебных методов.

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mHelper == null) return;

        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }else {
            Log.d("Pay", "onActivityResult handled by IABUtil.");
        }

        setWaitScreen(false);
    }

    // Приёмник широкоформатных сообщений.
    @Override
    public void receivedBroadcast() {
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error querying inventory. Another async operation in progress.");
        }
        setWaitScreen(false);
    }

    // Отключение служебных классов.
    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }

        // very important:
        Log.d("Destroy", "Destroying helper.");
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }

        setWaitScreen(false);
    }

    // Ожидательное сообщение.
    void setWaitScreen(boolean set) {
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Идёт оплата");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(true);
        if(set){
            pDialog.show();
        }else {
            pDialog.dismiss();
        }
    }

    // Верификация покупки.
    // В данный момент отключена.
    // Если необходимо использование, нужно прописать ключи транзакций.
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        return true;
    }

    // Всплывающие сообщения об ошибках.
    void complain(String message) {
        alert("Error: " + message);
    }
    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        bld.create().show();
    }
}
