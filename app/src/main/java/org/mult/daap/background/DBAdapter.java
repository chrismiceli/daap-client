package org.mult.daap.background;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBAdapter {
    public static final String KEY_ROWID = "_id";
    public static final String KEY_SERVER_NAME = "server_name";
    public static final String KEY_SERVER_ADDRESS = "address";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_LOGIN_REQUIRED = "login_required";
    @SuppressWarnings("unused")
    private static final String TAG = "DBAdapter";
    private static final String DATABASE_NAME = "servs";
    public static final String DATABASE_TABLE = "servers";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_CREATE = "create table servers (_id integer primary key autoincrement, "
            + "server_name text not null, address text not null, "
            + "password text not null, " + "login_required integer not null);";
    private final Context context;
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public DBAdapter(Context ctx) {
        this.context = ctx;
        DBHelper = new DatabaseHelper(context);
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS servers");
            onCreate(db);
        }
    }

    public void reCreate() {
        db.execSQL("DROP TABLE IF EXISTS servers");
        db.execSQL(DATABASE_CREATE);
    }

    // ---opens the database---
    public DBAdapter open() throws SQLException {
        db = DBHelper.getWritableDatabase();
        return this;
    }

    // ---closes the database---
    public void close() {
        DBHelper.close();
    }

    // ---insert a title into the database---
    public long insertServer(String name, String address, String password,
            boolean login_required) {
        int login_required_local;
        if (login_required)
            login_required_local = 1;
        else
            login_required_local = 0;
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_SERVER_NAME, name);
        initialValues.put(KEY_SERVER_ADDRESS, address);
        initialValues.put(KEY_PASSWORD, password);
        initialValues.put(KEY_LOGIN_REQUIRED, login_required_local);
        return db.insert(DATABASE_TABLE, null, initialValues);
    }

    // ---deletes a particular title---
    public boolean deleteServer(int rowId) {
        return db.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    public void deleteAllServer() {
        db.execSQL("DROP TABLE IF EXISTS servers");
        db.execSQL(DATABASE_CREATE);
    }

    public Cursor getAllServers() {
        return db.query(DATABASE_TABLE, new String[] { KEY_ROWID,
                KEY_SERVER_NAME, KEY_SERVER_ADDRESS, KEY_PASSWORD,
                KEY_LOGIN_REQUIRED }, null, null, null, null, null);
    }

    // ---retrieves a particular title---
    public Cursor getServer(int rowId) throws SQLException {
        Cursor mCursor = db.query(true, DATABASE_TABLE, new String[] {
                KEY_ROWID, KEY_SERVER_NAME, KEY_SERVER_ADDRESS, KEY_PASSWORD,
                KEY_LOGIN_REQUIRED }, KEY_ROWID + "=" + rowId, null, null,
                null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    // ---checks if a server already exists in the database---
    public boolean serverExists(String name, String address, String password,
            boolean login_required) {
        StringBuilder selectionBuilder = new StringBuilder();
        ArrayList<String> selectionValuesList = new ArrayList<String>();
        boolean exists = false;

        selectionBuilder.append(KEY_SERVER_NAME).append(" = ?");
        selectionBuilder.append(" AND ");
        selectionBuilder.append(KEY_SERVER_ADDRESS).append(" = ?");
        selectionBuilder.append(" AND ");
        selectionBuilder.append(KEY_PASSWORD).append(" = ?");
        selectionBuilder.append(" AND ");
        selectionBuilder.append(KEY_LOGIN_REQUIRED).append(" = ?");

        selectionValuesList.add(name);
        selectionValuesList.add(address);
        selectionValuesList.add(password);

        if (login_required)
            selectionValuesList.add("1");
        else
            selectionValuesList.add("0");

        String selectionValues[] = new String[selectionValuesList.size()];
        selectionValuesList.toArray(selectionValues);
        selectionValuesList = null;

        synchronized (db) {
            Cursor c = db.query(DATABASE_TABLE, null,
                    selectionBuilder.toString(), selectionValues, null, null,
                    null);

            if (c.moveToNext())
                exists = true;

            c.close();
        }

        return exists;
    }

    // ---updates a title---
    public boolean updateServer(int rowId, String name, String address,
            String login, String password, boolean login_required) {
        ContentValues args = new ContentValues();
        int login_required_local;
        if (login_required)
            login_required_local = 1;
        else
            login_required_local = 0;
        args.put(KEY_SERVER_NAME, name);
        args.put(KEY_SERVER_ADDRESS, address);
        args.put(KEY_PASSWORD, password);
        args.put(KEY_LOGIN_REQUIRED, login_required_local);
        return db.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }

    public boolean updateServer(ContentValues update, int rowId) {
        return (db.update(DATABASE_TABLE, update, "_id = ?",
                new String[] { String.valueOf(rowId) }) != 0);
    }
}