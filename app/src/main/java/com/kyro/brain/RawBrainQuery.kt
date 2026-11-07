package com.kyro.brain

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RawBrainQuery(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "kyro_brain.db"
        private const val DB_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS commands USING fts4(pattern, action, parameters)")

        // Base initial commands
        db?.execSQL("INSERT INTO commands (pattern, action, parameters) VALUES ('open whatsapp', 'OPEN_APP', 'com.whatsapp')")
        db?.execSQL("INSERT INTO commands (pattern, action, parameters) VALUES ('call', 'CALL_CONTACT', '')")
        db?.execSQL("INSERT INTO commands (pattern, action, parameters) VALUES ('scroll down', 'ACCESSIBILITY_ACTION', 'SCROLL_DOWN')")
        db?.execSQL("INSERT INTO commands (pattern, action, parameters) VALUES ('read screen', 'ACCESSIBILITY_ACTION', 'READ_SCREEN')")
        db?.execSQL("INSERT INTO commands (pattern, action, parameters) VALUES ('message bhejo', 'WHATSAPP_MSG', '')")
        db?.execSQL("INSERT INTO commands (pattern, action, parameters) VALUES ('youtube par', 'YOUTUBE_SEARCH', '')")
        db?.execSQL("INSERT INTO commands (pattern, action, parameters) VALUES ('play reels', 'REELS_SCROLL', '')")
        db?.execSQL("INSERT INTO commands (pattern, action, parameters) VALUES ('fake call', 'FAKE_CALL', '')")
        db?.execSQL("INSERT INTO commands (pattern, action, parameters) VALUES ('auto scroll', 'REELS_SCROLL', '')")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Handle upgrade if needed
    }

    suspend fun findCommand(query: String): BrainResult? = withContext(Dispatchers.IO) {
        var result: BrainResult? = null
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT action, parameters FROM commands WHERE pattern MATCH ? LIMIT 1",
                arrayOf("$query*")
            )

            if (cursor.moveToFirst()) {
                val action = cursor.getString(0)
                val params = cursor.getString(1)
                result = BrainResult(action, params)
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("KYRO_BRAIN", "Database query failed", e)
        }
        result
    }

    suspend fun learnCommand(pattern: String, actions: String) = withContext(Dispatchers.IO) {
        try {
            val db = writableDatabase
            val statement = db.compileStatement("INSERT INTO commands (pattern, action, parameters) VALUES (?, 'MACRO', ?)")
            statement.bindString(1, pattern)
            statement.bindString(2, actions)
            statement.executeInsert()
            Log.d("KYRO_BRAIN", "Learned new macro: $pattern -> $actions")
        } catch (e: Exception) {
            Log.e("KYRO_BRAIN", "Failed to learn macro", e)
        }
    }
}

data class BrainResult(val action: String, val parameters: String)
