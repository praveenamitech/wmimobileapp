package com.example.bhanu.hdevices;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final String TAG = DatabaseHandler.class.getSimpleName();

    private static DatabaseHandler instance = null;

    private FileManager fileManager = FileManager.getInstance();

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "meters_manager";

    // Contacts table name
    private static final String TABLE_METERS = "meters";
    private static final String TABLE_UNDEFINED_METERS = "undefined_meters";

    private DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static DatabaseHandler getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHandler(context);
        }

        return instance;
    }

    public static DatabaseHandler getInstance() {
        return instance;
    }

    private class Column {
        static final int id = 0;
        static final int serial_no = 1;
        static final int make = 2;
        static final int read_status = 3;
        static final int is_read_again = 4;
        static final int upload_status = 5;
        static final int last_read = 6;
    }

    private class ColumnName {
        public static final String id = "id";
        static final String serial_no = "serial_no";
        static final String make = "make";
        static final String read_status = "read_status";
        static final String is_read_again = "is_read_again";
        static final String upload_status = "upload_status";
        static final String last_read = "last_read";
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.e(TAG, "onCreate called");
        String CREATE_METERS_TABLE = "CREATE TABLE `meters` (\n" +
                "\t`id`\tINTEGER,\n" +
                "\t`serial_no`\tTEXT,\n" +
                "\t`make`\tINTEGER,\n" +
                "\t`read_status`\tTEXT,\n" +
                "\t`is_read_again`\tTEXT,\n" +
                "\t`upload_status`\tTEXT,\n" +
                "\t`last_read`\tTEXT,\n" +
                "\tPRIMARY KEY(`id`)\n" +
                ");";

        String METERS_TABLE_INDEX = "CREATE INDEX meters_index ON meters (`id`);";

        String CREATE_UNDEFINED_METERS_TABLE = "CREATE TABLE `undefined_meters` (\n" +
                "\t`id`\tINTEGER,\n" +
                "\t`serial_no`\tTEXT,\n" +
                "\t`make`\tINTEGER,\n" +
                "\t`read_status`\tTEXT,\n" +
                "\t`is_read_again`\tTEXT,\n" +
                "\t`upload_status`\tTEXT,\n" +
                "\t`last_read`\tTEXT,\n" +
                "\tPRIMARY KEY(`id`)\n" +
                ");";

        String UNDEFINED_METERS_TABLE_INDEX = "CREATE INDEX undefined_meters_index ON undefined_meters(`id`)";

        db.execSQL(CREATE_METERS_TABLE);
        db.execSQL(METERS_TABLE_INDEX);

        db.execSQL(CREATE_UNDEFINED_METERS_TABLE);
        db.execSQL(UNDEFINED_METERS_TABLE_INDEX);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.e(TAG, "onUpgrade called");
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_METERS);

        // Create tables again
        onCreate(db);
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */

    synchronized public void truncate(String tableName) {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.e(TAG, "Truncating table " + tableName);
        db.execSQL("delete from "+ tableName);
        Log.e(TAG, "Truncating \'" + tableName + "\' truncated successfully");
    }

    synchronized public void addMeter(String serialNo, int make) {
        Log.d(TAG, "Adding new meter - mtrNo:" + serialNo + ", make: " + make);
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ColumnName.serial_no, serialNo);
        values.put(ColumnName.make, make);
        values.put(ColumnName.read_status, Constants.NA);
        values.put(ColumnName.is_read_again, String.valueOf(false));
        values.put(ColumnName.upload_status, Constants.NA);

        long rowId = db.insert(TABLE_METERS, null, values);
        db.close();

        Log.d(TAG, "Added new meter - rowId: " + rowId);
    }

    synchronized public void deleteMeter(String serialNo) {
        Log.d(TAG, "Deleting meter - mtrNo:" + serialNo);
        SQLiteDatabase db = this.getWritableDatabase();

        int noOfDeletedRows = db.delete(TABLE_METERS, ColumnName.serial_no + " = ?", new String[] { serialNo });
        db.close();

        Log.d(TAG, "Deleted rows: " + noOfDeletedRows);
    }

    synchronized public void deleteUndefinedMeter(String serialNo) {
        Log.d(TAG, "Deleting undefined meter - mtrNo:" + serialNo);
        SQLiteDatabase db = this.getWritableDatabase();

        int noOfDeletedRows = db.delete(TABLE_UNDEFINED_METERS, ColumnName.serial_no + " = ?", new String[] { serialNo });
        db.close();

        Log.d(TAG, "Deleted rows: " + noOfDeletedRows);
    }

    synchronized private void deleteFirstFewRows(int maxRows) {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d(TAG, "maxRows: " + maxRows);
        int limit = Constants.UNDEFINED_TABLE_ROWS_LIMIT;
        if (maxRows > limit) {
            int noOfRows = maxRows - limit;
            Log.d(TAG, "maxRows reached (" + limit + ")");
            Log.d(TAG, "Deleting the first " + noOfRows + " rows to get back into the limit");

            db.execSQL("Delete from `" + TABLE_UNDEFINED_METERS + "` where id IN (Select id from `" + TABLE_UNDEFINED_METERS + "` limit " + noOfRows + ");");

            for (int i = 0; i < noOfRows; i++) {
                ArrayList<EnergyMeter> meters = DataHolder.getInstance().getUndefinedDevicesList();
                meters.remove(0);
                CustomUtils.reorderIndexNo(meters);
            }
        }

        db.close();
    }

    synchronized public void addUndefinedMeter(String serialNo, int make) {

        SQLiteDatabase db = this.getWritableDatabase();

        Log.d(TAG, "Adding undefined meter - mtrNo:" + serialNo + ", make: " + make);
        ContentValues values = new ContentValues();
        values.put(ColumnName.serial_no, serialNo);
        values.put(ColumnName.make, make);
        values.put(ColumnName.read_status, Constants.NA);
        values.put(ColumnName.is_read_again, String.valueOf(false));
        values.put(ColumnName.upload_status, Constants.NA);

        long rowId = db.insert(TABLE_UNDEFINED_METERS, null, values);
        db.close();

        Log.d(TAG, "Added undefined meter - rowId: " + rowId);

        deleteFirstFewRows(getUndefinedMetersCount());
    }

    synchronized public void updateMeter(EnergyMeter currentMeter) {
        Log.d(TAG, "Updating saved meter -> " + currentMeter.getMtrNo());
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        if (currentMeter.getReadStatus().equals(Constants.PROGRESS)) {
            values.put(ColumnName.read_status, Constants.FAILED);
        } else {
            values.put(ColumnName.read_status, currentMeter.getReadStatus());
        }

        if (currentMeter.getUploadStatus().equals(Constants.PROGRESS)) {
            values.put(ColumnName.upload_status, Constants.FAILED);
        } else {
            values.put(ColumnName.upload_status, currentMeter.getUploadStatus());
        }

        values.put(ColumnName.serial_no, String.valueOf(currentMeter.getMtrNo()));
        values.put(ColumnName.make, String.valueOf(currentMeter.getMake()));

        values.put(ColumnName.is_read_again, String.valueOf(currentMeter.isReadAgain()));
        values.put(ColumnName.last_read, String.valueOf(currentMeter.getTimestamp()));

        int affectedRows = db.update(TABLE_METERS, values,"serial_no=\'" + currentMeter.getMtrNo() + "\'", null);
        db.close();

        Log.d(TAG, "Updated saved meter, affectedRows: " + affectedRows);
    }

    synchronized void updateUndefinedMeter(EnergyMeter currentMeter) {
        Log.d(TAG, "Updating undefined meter -> " + currentMeter.getMtrNo());
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        if (currentMeter.getReadStatus().equals(Constants.PROGRESS)) {
            values.put(ColumnName.read_status, Constants.FAILED);
        } else {
            values.put(ColumnName.read_status, currentMeter.getReadStatus());
        }

        if (currentMeter.getUploadStatus().equals(Constants.PROGRESS)) {
            values.put(ColumnName.upload_status, Constants.FAILED);
        } else {
            values.put(ColumnName.upload_status, currentMeter.getUploadStatus());
        }

        values.put(ColumnName.is_read_again, String.valueOf(currentMeter.isReadAgain()));
        values.put(ColumnName.last_read, String.valueOf(currentMeter.getTimestamp()));

        int affectedRows = db.update(TABLE_UNDEFINED_METERS, values,"serial_no=\'" + currentMeter.getMtrNo() + "\'", null);
        db.close();

        Log.d(TAG, "Updated undefined meter, affectedRows: " + affectedRows);
    }


    synchronized public ArrayList<EnergyMeter> getMeters() {
        ArrayList<EnergyMeter> savedMeters = new ArrayList<>();
        String countQuery = "SELECT  * FROM " + TABLE_METERS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        if (cursor.moveToFirst()) {
            do {
                EnergyMeter m = new EnergyMeter();
                m.setMtrNo(cursor.getString(Column.serial_no));
                m.setMake(cursor.getInt(Column.make));
                m.setReadStatus(cursor.getString(Column.read_status));
                m.setReadAgain(Boolean.valueOf(cursor.getString(Column.is_read_again)));
                m.setUploadStatus(cursor.getString(Column.upload_status));
                m.setTimestamp(cursor.getString(Column.last_read));

                savedMeters.add(m);
                m.setIndex(savedMeters.size());
                m.setSavedMeter(true);
            } while (cursor.moveToNext());
        }

        return savedMeters;
    }

    synchronized public ArrayList<EnergyMeter> getUndefinedMeters() {
        ArrayList<EnergyMeter> undefinedMeters = new ArrayList<>();
        String countQuery = "SELECT  * FROM " + TABLE_UNDEFINED_METERS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        if (cursor.moveToFirst()) {
            do {
                EnergyMeter m = new EnergyMeter();
                m.setMtrNo(cursor.getString(Column.serial_no));
                m.setMake(cursor.getInt(Column.make));
                m.setReadStatus(cursor.getString(Column.read_status));
                m.setReadAgain(Boolean.valueOf(cursor.getString(Column.is_read_again)));
                m.setUploadStatus(cursor.getString(Column.upload_status));
                m.setTimestamp(cursor.getString(Column.last_read));

                undefinedMeters.add(m);
                m.setIndex(undefinedMeters.size());
                m.setSavedMeter(false);
            } while (cursor.moveToNext());
        }

        return undefinedMeters;
    }

    synchronized public boolean meterExists(String serialNo) {
        ArrayList<EnergyMeter> savedMeters = new ArrayList<>();
        String countQuery = "SELECT  * FROM " + TABLE_METERS + " WHERE " + ColumnName.serial_no + " = \'" + serialNo + "\'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        return count > 0;
    }

    synchronized public int getMetersCount() {
        String countQuery = "SELECT  * FROM " + TABLE_METERS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    synchronized public int getUndefinedMetersCount() {
        String countQuery = "SELECT  * FROM " + TABLE_UNDEFINED_METERS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }
}
