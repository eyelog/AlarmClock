package ru.eyelog.alarmclock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;

class DataBase extends SQLiteOpenHelper implements BaseColumns {

    private static DataBase instance = null;
    private static SQLiteDatabase database = null;

    private static final String DATABASE_NAME = "database";
    private static final int DATABASE_VERSION = 1;

    public static final String ALARM_TABLE = "alarm_table";
    public static final String COLUMN_ALARM_NAME = "alarm_name";
    public static final String COLUMN_ALARM_ACTIVE = "alarm_active";
    public static final String COLUMN_ALARM_TIME = "alarm_time";
    public static final String COLUMN_ALARM_DAYS = "alarm_days";
    public static final String COLUMN_ALARM_TONE_NAME = "alarm_tone_name";
    public static final String COLUMN_ALARM_DIFF_LEVEL = "difficulty_level";
    public static final String COLUMN_ALARM_NUMB_OF_QUESTS = "number_of_questions";
    public static final String COLUMN_ALARM_TONE_PATH = "alarm_tone_patch";

    private final static String DB_CREATE_SCRIPT = "create table " + ALARM_TABLE + " ("
            + BaseColumns._ID + " integer primary key autoincrement, " + COLUMN_ALARM_NAME + " text not null,"
            +  COLUMN_ALARM_ACTIVE + " integer not null, " +  COLUMN_ALARM_TIME + " text, "  +  COLUMN_ALARM_DAYS + " blob, "
            +  COLUMN_ALARM_TONE_NAME + " text, " +  COLUMN_ALARM_DIFF_LEVEL + " integer, " +  COLUMN_ALARM_NUMB_OF_QUESTS + " integer, "
            +  COLUMN_ALARM_TONE_PATH + " text);";

    public DataBase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DB_CREATE_SCRIPT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ALARM_TABLE);
        onCreate(db);
    }


    public static void init(Context context) {
        if (null == instance) {
            instance = new DataBase(context);
        }
    }

    public static SQLiteDatabase getDatabase() {
        if (null == database) {
            database = instance.getWritableDatabase();
        }
        return database;
    }

    static void deactivate() {
        if (null != database && database.isOpen()) {
            database.close();
        }
        database = null;
        instance = null;
    }

    public static long create(AlarmObject alarmObject) {

        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ALARM_NAME, alarmObject.getAlarmName());
        cv.put(COLUMN_ALARM_ACTIVE, alarmObject.getAlarmActive());
        cv.put(COLUMN_ALARM_TIME, alarmObject.getAlarmTimeString());

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = null;
            oos = new ObjectOutputStream(bos);
            oos.writeObject(alarmObject.getDays());
            byte[] buff = bos.toByteArray();

            cv.put(COLUMN_ALARM_DAYS, buff);

        } catch (Exception e){
            Log.e("bad DB create DAYS", e.toString());
        }

        cv.put(COLUMN_ALARM_TONE_NAME, alarmObject.getAlarmToneName());
        cv.put(COLUMN_ALARM_DIFF_LEVEL, alarmObject.getDiffLevel());
        cv.put(COLUMN_ALARM_NUMB_OF_QUESTS, alarmObject.getNumberOfQuestions());
        cv.put(COLUMN_ALARM_TONE_PATH, alarmObject.getAlarmTonePath());

        return getDatabase().insert(ALARM_TABLE, null, cv);
    }

    public static int update(AlarmObject alarmObject, int id) {

        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ALARM_NAME, alarmObject.getAlarmName());
        cv.put(COLUMN_ALARM_ACTIVE, alarmObject.getAlarmActive());
        cv.put(COLUMN_ALARM_TIME, alarmObject.getAlarmTimeString());

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = null;
            oos = new ObjectOutputStream(bos);
            oos.writeObject(alarmObject.getDays());
            byte[] buff = bos.toByteArray();

            cv.put(COLUMN_ALARM_DAYS, buff);

        } catch (Exception e){
            Log.e("bad DB update DAYS", e.toString());
        }

        cv.put(COLUMN_ALARM_TONE_NAME, alarmObject.getAlarmToneName());
        cv.put(COLUMN_ALARM_DIFF_LEVEL, alarmObject.getDiffLevel());
        cv.put(COLUMN_ALARM_NUMB_OF_QUESTS, alarmObject.getNumberOfQuestions());
        cv.put(COLUMN_ALARM_TONE_PATH, alarmObject.getAlarmTonePath());

        return getDatabase().update(ALARM_TABLE, cv, "_id=" + id, null);
    }


    public static int updateActive(boolean active, int id) {

        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ALARM_ACTIVE, active);

        return getDatabase().update(ALARM_TABLE, cv, "_id=" + id, null);
    }

    public static int deleteEntry(int id){
        return getDatabase().delete(ALARM_TABLE, _ID + "=" + id, null);
    }

    public static AlarmObject getAlarm(int id) {

        String[] columns = new String[] {
                COLUMN_ALARM_NAME,
                COLUMN_ALARM_ACTIVE,
                COLUMN_ALARM_TIME,
                COLUMN_ALARM_DAYS,
                COLUMN_ALARM_TONE_NAME,
                COLUMN_ALARM_DIFF_LEVEL,
                COLUMN_ALARM_NUMB_OF_QUESTS,
                COLUMN_ALARM_TONE_PATH
        };
        Cursor c = getDatabase().query(ALARM_TABLE, columns, _ID+"="+id, null, null, null,
                null);
        AlarmObject alarmObject = null;

        if(c.moveToFirst()){

            alarmObject =  new AlarmObject();
            alarmObject.setAlarmName(c.getString(0));
            alarmObject.setAlarmActive(c.getInt(1)==1);
            alarmObject.setAlarmTime(c.getString(2));

            byte[] repeatDaysBytes = c.getBlob(3);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(repeatDaysBytes);
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                AlarmObject.Day[] repeatDays;
                Object object = objectInputStream.readObject();
                if(object instanceof AlarmObject.Day[]){
                    repeatDays = (AlarmObject.Day[]) object;
                    alarmObject.setDays(repeatDays);
                }
            } catch (StreamCorruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }


            alarmObject.setAlarmToneName(c.getString(4));
            alarmObject.setDiffLevel(c.getInt(5));
            alarmObject.setNumberOfQuestions(c.getInt(6));
            alarmObject.setAlarmTonePath(c.getString(7));

        }
        c.close();
        return alarmObject;
    }

    public static Cursor getCursor() {
        String[] columns = new String[] {
                _ID,
                COLUMN_ALARM_NAME,
                COLUMN_ALARM_ACTIVE,
                COLUMN_ALARM_TIME,
                COLUMN_ALARM_DAYS,
                COLUMN_ALARM_TONE_NAME,
                COLUMN_ALARM_DIFF_LEVEL,
                COLUMN_ALARM_NUMB_OF_QUESTS,
                COLUMN_ALARM_TONE_PATH
        };
        return getDatabase().query(ALARM_TABLE, columns, null, null, null, null,
                null);
    }

    public static List<AlarmObject> getAll() {
        List<AlarmObject> alarms = new ArrayList<AlarmObject>();
        Cursor cursor = DataBase.getCursor();
        if (cursor.moveToFirst()) {

            do {

                AlarmObject alarmObject = new AlarmObject();
                alarmObject.setId(cursor.getInt(0));
                alarmObject.setAlarmName(cursor.getString(1));
                alarmObject.setAlarmActive(cursor.getInt(2) == 1);
                alarmObject.setAlarmTime(cursor.getString(3));

                byte[] repeatDaysBytes = cursor.getBlob(4);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                        repeatDaysBytes);
                try {
                    ObjectInputStream objectInputStream = new ObjectInputStream(
                            byteArrayInputStream);
                    AlarmObject.Day[] repeatDays;
                    Object object = objectInputStream.readObject();
                    if (object instanceof AlarmObject.Day[]) {
                        repeatDays = (AlarmObject.Day[]) object;
                        alarmObject.setDays(repeatDays);
                    }
                } catch (StreamCorruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                alarmObject.setAlarmToneName(cursor.getString(5));
                alarmObject.setDiffLevel(cursor.getInt(6));
                alarmObject.setNumberOfQuestions(cursor.getInt(7));
                alarmObject.setAlarmTonePath(cursor.getString(8));

                alarms.add(alarmObject);

            } while (cursor.moveToNext());
        }
        cursor.close();
        return alarms;
    }
}