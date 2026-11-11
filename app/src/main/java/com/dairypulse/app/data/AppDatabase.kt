package com.dairypulse.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
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
    version = 1,
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

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                // Rollback journal keeps the whole database in one file, which makes
                // Android's Wi-Fi Auto Backup capture a consistent copy.
                .setJournalMode(JournalMode.TRUNCATE)
                .build()
    }
}
