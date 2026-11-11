package com.dairypulse.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CowDao {
    @Query("SELECT * FROM cows ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<Cow>>

    @Query("SELECT * FROM cows WHERE id = :id")
    suspend fun get(id: Long): Cow?

    @Upsert
    suspend fun upsert(cow: Cow): Long

    @Delete
    suspend fun delete(cow: Cow)
}

@Dao
interface MilkDao {
    @Query("SELECT * FROM milk_records WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    fun between(from: LocalDate, to: LocalDate): Flow<List<MilkRecord>>

    @Query("SELECT * FROM milk_records WHERE cowId = :cowId ORDER BY date DESC, session DESC LIMIT :limit")
    fun forCow(cowId: Long, limit: Int = 30): Flow<List<MilkRecord>>

    @Query("SELECT * FROM milk_records WHERE date = :date AND session = :session")
    suspend fun forSession(date: LocalDate, session: Session): List<MilkRecord>

    @Query("DELETE FROM milk_records WHERE date = :date AND session = :session")
    suspend fun deleteSession(date: LocalDate, session: Session)

    @Insert
    suspend fun insertAll(records: List<MilkRecord>)

    /** Replaces everything logged for one milking session in one step. */
    @RoomTransaction
    suspend fun replaceSession(date: LocalDate, session: Session, records: List<MilkRecord>) {
        deleteSession(date, session)
        insertAll(records)
    }
}

@Dao
interface FeedDao {
    @Query("SELECT * FROM ingredients ORDER BY name COLLATE NOCASE")
    fun ingredients(): Flow<List<Ingredient>>

    @Upsert
    suspend fun upsertIngredient(ingredient: Ingredient): Long

    @Delete
    suspend fun deleteIngredient(ingredient: Ingredient)

    @Query("SELECT * FROM formulations ORDER BY name COLLATE NOCASE")
    fun formulations(): Flow<List<Formulation>>

    @Query("SELECT * FROM formulation_items")
    fun allItems(): Flow<List<FormulationItem>>

    @Query("SELECT * FROM formulations WHERE id = :id")
    suspend fun formulation(id: Long): Formulation?

    @Query("SELECT * FROM formulation_items WHERE formulationId = :id")
    suspend fun itemsFor(id: Long): List<FormulationItem>

    @Upsert
    suspend fun upsertFormulation(formulation: Formulation): Long

    @Query("DELETE FROM formulation_items WHERE formulationId = :id")
    suspend fun clearItems(id: Long)

    @Insert
    suspend fun insertItems(items: List<FormulationItem>)

    @RoomTransaction
    suspend fun saveFormulation(formulation: Formulation, items: List<FormulationItem>): Long {
        val id = if (formulation.id == 0L) upsertFormulation(formulation) else {
            upsertFormulation(formulation); formulation.id
        }
        clearItems(id)
        insertItems(items.map { it.copy(id = 0, formulationId = id) })
        return id
    }

    @Delete
    suspend fun deleteFormulation(formulation: Formulation)

    @Query("SELECT * FROM feed_records WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    fun recordsBetween(from: LocalDate, to: LocalDate): Flow<List<FeedRecord>>

    @Insert
    suspend fun insertRecord(record: FeedRecord)

    @Delete
    suspend fun deleteRecord(record: FeedRecord)
}

@Dao
interface MoneyDao {
    @Query("SELECT * FROM overheads ORDER BY name COLLATE NOCASE")
    fun overheads(): Flow<List<Overhead>>

    @Upsert
    suspend fun upsertOverhead(overhead: Overhead)

    @Delete
    suspend fun deleteOverhead(overhead: Overhead)

    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    fun transactionsBetween(from: LocalDate, to: LocalDate): Flow<List<Transaction>>

    @Upsert
    suspend fun upsertTransaction(tx: Transaction)

    @Delete
    suspend fun deleteTransaction(tx: Transaction)

    @Query("SELECT * FROM calf_expenses ORDER BY date DESC, id DESC")
    fun calfExpenses(): Flow<List<CalfExpense>>

    @Insert
    suspend fun insertCalfExpense(expense: CalfExpense)

    @Delete
    suspend fun deleteCalfExpense(expense: CalfExpense)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY done, dueDate, id")
    fun all(): Flow<List<FarmTask>>

    @Query("SELECT * FROM tasks WHERE done = 0 AND dueDate <= :date")
    suspend fun dueBy(date: LocalDate): List<FarmTask>

    @Upsert
    suspend fun upsert(task: FarmTask)

    @Delete
    suspend fun delete(task: FarmTask)
}
