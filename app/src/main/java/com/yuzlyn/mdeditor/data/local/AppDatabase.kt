package com.yuzlyn.mdeditor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class, NoteDraftEntity::class],
    version = DatabaseMigrations.DB_VERSION,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    abstract fun noteDraftDao(): NoteDraftDao

    companion object {
        const val DB_NAME = "mdeditor_database"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(*DatabaseMigrations.MIGRATIONS)
                    .build()
                    .also { instance = it }
            }
    }
}
