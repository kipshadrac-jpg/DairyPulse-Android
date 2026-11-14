package com.dairypulse.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

class Converters {
    @TypeConverter fun fromDate(value: LocalDate?): Long? = value?.toEpochDay()
    @TypeConverter fun toDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)
}

@Database(
    entities = [
        Cow::class, MilkRecord::class, Ingredient::class, Formulation::class, FormulationItem::class,
        FeedRecord::class, Overhead::class, Transaction::class, CalfExpense::class, FarmTask::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cows(): CowDao
    abstract fun milk(): MilkDao
    abstract fun feed(): FeedDao
    abstract fun money(): MoneyDao
    abstract fun tasks(): TaskDao

    companion object {
        const val NAME = "dairypulse.db"

        /** v2: milk fed to calves, and cow photos. Existing records are kept. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE milk_records ADD COLUMN forCalves INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cows ADD COLUMN photo TEXT")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                // Rollback journal keeps the whole database in one file, which makes
                // Android's Wi-Fi Auto Backup capture a consistent copy.
                .setJournalMode(JournalMode.TRUNCATE)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
