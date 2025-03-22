package com.example.chalkitup.ui.offline

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "appointments.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "appointments"
        private const val COLUMN_ID = "id"
        private const val COLUMN_STUDENT_ID = "studentID"
        private const val COLUMN_TUTOR_ID = "tutorID"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_TIME = "time"
        private const val COLUMN_SUBJECT = "subject"
        private const val COLUMN_MODE = "mode"
        private const val COLUMN_COMMENTS = "comments"

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableStatement = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_STUDENT_ID TEXT,
                $COLUMN_TUTOR_ID TEXT,
                $COLUMN_DATE TEXT,
                $COLUMN_TIME TEXT,
                $COLUMN_SUBJECT TEXT,
                $COLUMN_MODE TEXT,
                $COLUMN_COMMENTS TEXT
            )
        """.trimIndent()
        db.execSQL(createTableStatement)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun getAppointments(): List<Map<String, String>> {
        val db = readableDatabase
        val cursor = db.query(TABLE_NAME, null, null, null, null, null, null)
        val appointments = mutableListOf<Map<String, String>>()

        while (cursor.moveToNext()) {
            val appointment = mapOf(
                COLUMN_ID to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)).toString(),
                COLUMN_STUDENT_ID to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STUDENT_ID)),
                COLUMN_TUTOR_ID to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TUTOR_ID)),
                COLUMN_DATE to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                COLUMN_TIME to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME)),
                COLUMN_SUBJECT to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBJECT)),
                COLUMN_MODE to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MODE)),
                COLUMN_COMMENTS to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENTS))
            )
            appointments.add(appointment)
        }
        cursor.close()
        return appointments
    }

    fun insertAppointment(appointment: Map<String, String>): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_STUDENT_ID, appointment[COLUMN_STUDENT_ID])
            put(COLUMN_TUTOR_ID, appointment[COLUMN_TUTOR_ID])
            put(COLUMN_DATE, appointment[COLUMN_DATE])
            put(COLUMN_TIME, appointment[COLUMN_TIME])
            put(COLUMN_SUBJECT, appointment[COLUMN_SUBJECT])
            put(COLUMN_MODE, appointment[COLUMN_MODE])
            put(COLUMN_COMMENTS, appointment[COLUMN_COMMENTS])
        }
        return db.insert(TABLE_NAME, null, values)
    }

    fun updateAppointment(appointment: Map<String, String>): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_STUDENT_ID, appointment[COLUMN_STUDENT_ID])
            put(COLUMN_TUTOR_ID, appointment[COLUMN_TUTOR_ID])
            put(COLUMN_DATE, appointment[COLUMN_DATE])
            put(COLUMN_TIME, appointment[COLUMN_TIME])
            put(COLUMN_SUBJECT, appointment[COLUMN_SUBJECT])
            put(COLUMN_MODE, appointment[COLUMN_MODE])
            put(COLUMN_COMMENTS, appointment[COLUMN_COMMENTS])
        }
        return db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(appointment[COLUMN_ID]))
    }

    fun deleteAllAppointments(): Int {
        val db = writableDatabase
        return db.delete(TABLE_NAME, null, null)
    }

    fun deleteAppointment(id: String): Int {
        val db = writableDatabase
        return db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id))
    }
}