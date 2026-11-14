package com.dairypulse.app.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.LocalDate

/** An install from version 1 opens on the current schema with its records intact. */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @Test fun version1DatabaseMigrates() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)

        // Build the v1 database exactly as Room exported it.
        val schema = JSONObject(File("schemas/com.dairypulse.app.data.AppDatabase/1.json").readText()).getJSONObject("database")
        val path = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                val table = e.getString("tableName")
                db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
                e.optJSONArray("indices")?.let { idx ->
                    for (j in 0 until idx.length()) db.execSQL(idx.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.version = 1
            val day = LocalDate.of(2026, 11, 1).toEpochDay()
            db.execSQL("INSERT INTO cows (id, tag, name, status, isFemale, notes) VALUES (1, '001', 'Daisy', 'IN_MILK', 1, '')")
            db.execSQL("INSERT INTO milk_records (date, session, cowId, liters, pricePerLiter) VALUES ($day, 'MORNING', 1, 12.0, 50.0)")
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        runBlocking {
            val cow = db.cows().all().first().single()
            assertEquals("Daisy", cow.name)
            assertNull(cow.photo)
            val milk = db.milk().forSession(LocalDate.of(2026, 11, 1), Session.MORNING).single()
            assertEquals(12.0, milk.liters, 1e-9)
            assertFalse(milk.forCalves)
        }
        db.close()
    }
}
