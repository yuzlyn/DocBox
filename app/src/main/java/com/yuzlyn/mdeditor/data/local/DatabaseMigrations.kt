package com.yuzlyn.mdeditor.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    const val DB_VERSION = 8

    /** v2：新增 createdAt 欄位；既有資料以 lastModified 作為建立時間的近似值。 */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE ${NoteEntity.TABLE_NAME} " +
                        "ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "UPDATE ${NoteEntity.TABLE_NAME} SET createdAt = lastModified WHERE createdAt = 0"
            )
        }
    }

    /** v3：新增每篇筆記自己的排版；NULL 表示沿用設定裡的預設排版（既有筆記保持原樣）。 */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE ${NoteEntity.TABLE_NAME} ADD COLUMN verticalLayout INTEGER"
            )
        }
    }

    /** v4：新增自動保存草稿表；升級前的筆記沒有草稿，行為不變。 */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `${NoteDraftEntity.TABLE_NAME}` (" +
                        "`noteId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`content` TEXT NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`noteId`))"
            )
        }
    }

    /** v5：新增來源格式；既有筆記一律當成 App 內新建（空字串），不影響任何行為。 */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE ${NoteEntity.TABLE_NAME} " +
                        "ADD COLUMN sourceFormat TEXT NOT NULL DEFAULT ''"
            )
        }
    }

    /** v6：新增來源檔 Uri；既有筆記沒有原始檔可回開，維持空字串。 */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE ${NoteEntity.TABLE_NAME} " +
                        "ADD COLUMN sourceUri TEXT NOT NULL DEFAULT ''"
            )
        }
    }

    /** v7：新增每篇筆記自己的書寫輔助線；NULL 表示沿用設定裡的預設（既有筆記保持原樣）。 */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE ${NoteEntity.TABLE_NAME} ADD COLUMN writingGuide TEXT"
            )
        }
    }

    /**
     * v8：新增每篇筆記自己的底紙、字色與字體。
     *
     * 三欄都是 NULL＝沿用設定裡的預設，所以既有筆記的畫面完全不變；
     * 只有使用者在某一篇的「樣式」面板動過，才寫進那一篇。
     */
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE ${NoteEntity.TABLE_NAME} ADD COLUMN backgroundPreset TEXT"
            )
            db.execSQL(
                "ALTER TABLE ${NoteEntity.TABLE_NAME} ADD COLUMN backgroundInk TEXT"
            )
            db.execSQL(
                "ALTER TABLE ${NoteEntity.TABLE_NAME} ADD COLUMN noteFont TEXT"
            )
        }
    }

    val MIGRATIONS: Array<Migration> =
        arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8
        )
}
