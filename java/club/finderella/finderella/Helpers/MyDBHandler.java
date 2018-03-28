package club.finderella.finderella.Helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MyDBHandler extends SQLiteOpenHelper {

    private static final String DB_NAME = "finderella.db";
    String query;

    public MyDBHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DB_NAME, factory, version);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // user data
        query = "CREATE TABLE IF NOT EXISTS user_data(" +
                "user_id INTEGER PRIMARY KEY," +
                "password VARCHAR(10) DEFAULT \"\"," +
                "start MEDIUMINT DEFAULT 0)";
        db.execSQL(query);

        // meta_data
        query = "CREATE TABLE IF NOT EXISTS meta_data(" +
                "first_name VARCHAR(255) DEFAULT NULL," +
                "last_name VARCHAR(255) DEFAULT NULL," +
                "status TEXT DEFAULT NULL, " +
                "gender VARCHAR(255) DEFAULT NULL," +
                "age INTEGER DEFAULT 0," +
                "location VARCHAR(255) DEFAULT NULL)";
        db.execSQL(query);

        // user media
        query = "CREATE TABLE IF NOT EXISTS user_media(" +
                "profile_image TEXT DEFAULT NULL," +
                "exp_type INTEGER DEFAULT 0," +
                "exp_image TEXT DEFUALT NULL," +
                "exp_video TEXT DEFAULT NULL)";
        db.execSQL(query);

        // social_networks_data
        query = "CREATE TABLE IF NOT EXISTS social_networks_data(" +
                "type INTEGER PRIMARY KEY," +
                "url TEXT DEFAULT NULL)";
        db.execSQL(query);

        // pokes
        query = "CREATE TABLE IF NOT EXISTS pokes(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "account_id INTEGER DEFAULT 0," +
                "session INTEGER DEFAULT 0," +
                "read INTEGER DEFAULT 1)";
        db.execSQL(query);

        // bookmarks
        query = "CREATE TABLE IF NOT EXISTS bookmarks(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "account_id INTEGER DEFAULT 0," +
                "session INTEGER DEFAULT 0)";
        db.execSQL(query);

        // block list
        query = "CREATE TABLE IF NOT EXISTS block_list(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "account_id INTEGER DEFAULT 0," +
                "name TEXT DEFAULT NULL)";
        db.execSQL(query);

        // server sync
        query = "CREATE TABLE IF NOT EXISTS server_sync(" +
                "exp_sync INTEGER DEFAULT 0," +
                "text_sync INTEGER DEFAULT 0," +
                "profile_pic_sync INTEGER DEFAULT 0)";
        db.execSQL(query);


        // mac_data
        query = "CREATE TABLE IF NOT EXISTS mac_data(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "address VARCHAR(20) DEFAULT \"\")";
        db.execSQL(query);


        // cache_data
        query = "CREATE TABLE IF NOT EXISTS cache_data(" +
                "mac_data_id INTEGER PRIMARY KEY," +
                "account_id INTEGER DEFAULT 0," +
                "server_addition INTEGER DEFAULT 0)";
        db.execSQL(query);


        // main_data
        query = "CREATE TABLE IF NOT EXISTS main_data(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "account_id INTEGER DEFAULT 0," +
                "uploaded INTEGER DEFAULT 0," +
                "days INTEGER DEFAULT 0," +
                "shown_id INTEGER DEFAULT 0," +
                "session INTEGER DEFAULT 0)";
        db.execSQL(query);

        // shown_data
        query = "CREATE TABLE IF NOT EXISTS shown_data(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "account_id INTEGER DEFAULT 0," +
                "session INTEGER DEFAULT 0)";
        db.execSQL(query);

        Log.i("mTag", "ON DATABASE CREATE executed");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }


}
