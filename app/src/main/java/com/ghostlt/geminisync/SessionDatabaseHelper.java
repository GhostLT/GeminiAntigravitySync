package com.ghostlt.geminisync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "gemini_sessions.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_SESSIONS = "chat_sessions";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SESSION_ID = "session_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_UPDATED_AT = "updated_at";
    public static final String COLUMN_MESSAGES_JSON = "messages_json";
    public static final String COLUMN_MODE_TAG = "mode_tag";

    private final Gson gson = new Gson();

    public static class SessionItem {
        public long id;
        public String sessionId;
        public String title;
        public String createdAt;
        public String updatedAt;
        public int messageCount;
        public String modeTag;
        public String messagesJson;

        public SessionItem(long id, String sessionId, String title, String createdAt, String updatedAt, int messageCount, String modeTag, String messagesJson) {
            this.id = id;
            this.sessionId = sessionId;
            this.title = title;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.messageCount = messageCount;
            this.modeTag = modeTag;
            this.messagesJson = messagesJson;
        }
    }

    public SessionDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_SESSIONS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SESSION_ID + " TEXT UNIQUE, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_CREATED_AT + " TEXT, " +
                COLUMN_UPDATED_AT + " TEXT, " +
                COLUMN_MESSAGES_JSON + " TEXT, " +
                COLUMN_MODE_TAG + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
        onCreate(db);
    }

    public synchronized void saveOrUpdateSession(String sessionId, String title, List<ChatMessage> messages, String modeTag) {
        if (sessionId == null || messages == null || messages.isEmpty()) return;

        SQLiteDatabase db = this.getWritableDatabase();
        String json = gson.toJson(messages);
        String now = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

        ContentValues values = new ContentValues();
        values.put(COLUMN_SESSION_ID, sessionId);
        values.put(COLUMN_TITLE, title != null && !title.isEmpty() ? title : "Conversación sin título");
        values.put(COLUMN_UPDATED_AT, now);
        values.put(COLUMN_MESSAGES_JSON, json);
        values.put(COLUMN_MODE_TAG, modeTag != null ? modeTag : "Modo Estándar");

        // Verificar si ya existe
        Cursor cursor = db.query(TABLE_SESSIONS, new String[]{COLUMN_ID},
                COLUMN_SESSION_ID + " = ?", new String[]{sessionId}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            db.update(TABLE_SESSIONS, values, COLUMN_SESSION_ID + " = ?", new String[]{sessionId});
            cursor.close();
        } else {
            if (cursor != null) cursor.close();
            values.put(COLUMN_CREATED_AT, now);
            db.insert(TABLE_SESSIONS, null, values);
        }
    }

    public synchronized List<SessionItem> getAllSessions() {
        List<SessionItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_SESSIONS, null, null, null, null, null, COLUMN_ID + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String sId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SESSION_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
                String updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT));
                String modeTag = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MODE_TAG));
                String json = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGES_JSON));

                int msgCount = 0;
                try {
                    Type listType = new TypeToken<List<ChatMessage>>() {}.getType();
                    List<ChatMessage> msgs = gson.fromJson(json, listType);
                    if (msgs != null) msgCount = msgs.size();
                } catch (Exception ignored) {}

                list.add(new SessionItem(id, sId, title, createdAt, updatedAt, msgCount, modeTag, json));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public synchronized List<ChatMessage> getSessionMessages(String sessionId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SESSIONS, new String[]{COLUMN_MESSAGES_JSON},
                COLUMN_SESSION_ID + " = ?", new String[]{sessionId}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String json = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGES_JSON));
            cursor.close();
            try {
                Type listType = new TypeToken<List<ChatMessage>>() {}.getType();
                return gson.fromJson(json, listType);
            } catch (Exception ignored) {}
        }
        if (cursor != null) cursor.close();
        return new ArrayList<>();
    }

    public synchronized void deleteSession(String sessionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SESSIONS, COLUMN_SESSION_ID + " = ?", new String[]{sessionId});
    }

    public synchronized void clearAllSessions() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SESSIONS, null, null);
    }
}
