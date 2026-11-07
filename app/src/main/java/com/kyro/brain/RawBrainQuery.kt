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
        // Since you mentioned the DB is in assets, we assume it's pre-populated.
        // We will create a dummy table here just in case it's not copied yet.
        db?.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS commands USING fts4(pattern, action, parameters)")

        // Dummy data
        db?.execSQL("INSERT INTO commands (pattern, action, parameters) VALUES ('open whatsapp', 'OPEN_APP', 'com.whatsapp')")
        db?.execSQL("INSERT INTO commands (pattern, action, parameters) VALUES ('call', 'CALL_CONTACT', '')")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Handle upgrade if needed
    }

    suspend fun findCommand(query: String): BrainResult? = withContext(Dispatchers.IO) {
        var result: BrainResult? = null
        try {
            val db = readableDatabase
            // Simple fuzzy matching via FTS
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
}

data class BrainResult(val action: String, val parameters: String)
