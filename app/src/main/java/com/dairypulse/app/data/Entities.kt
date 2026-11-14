package com.dairypulse.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class Session(val label: String) { MORNING("Morning"), MIDDAY("Midday"), EVENING("Evening") }

enum class CowStatus(val label: String) {
    IN_MILK("In Milk"), DRY("Dry"), HEIFER("Heifer"), CALF("Calf"), SOLD("Sold / Gone");

    val isYoungStock get() = this == HEIFER || this == CALF
}

enum class Recurrence(val label: String) { DAILY("Daily"), MONTHLY("Monthly") }

enum class TxType { INCOME, EXPENSE }

enum class CalfCostType(val label: String) { FEED("Feed"), UPKEEP("Upkeep") }

object Categories {
    const val LABOUR = "Labour & Salaries"
    const val MACHINERY = "Machinery & Fuel"
    const val TRANSPORT = "Transport & Delivery"
    const val VET = "Veterinary & Health"
    const val UTILITIES = "Utilities"
    const val OTHER_EXPENSE = "Other Expenses"

    val expense = listOf(LABOUR, MACHINERY, TRANSPORT, VET, UTILITIES, OTHER_EXPENSE)
    val income = listOf("Animal Sales", "Manure Sales", "Other Income")
}

@Entity(tableName = "cows")
data class Cow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tag: String,
    val name: String,
    val status: CowStatus,
    val isFemale: Boolean = true,
    val birthDate: LocalDate? = null,
    val lastCalvingDate: LocalDate? = null,
    val expectedCalvingDate: LocalDate? = null,
    val damId: Long? = null,
    val notes: String = "",
    /** File name of the cow's photo in the app's private photos folder, if one was added. */
    val photo: String? = null,
) {
    val displayName get() = if (tag.isBlank()) name else "$name (#$tag)"
}

@Entity(tableName = "milk_records", indices = [Index("date"), Index("cowId")])
data class MilkRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val session: Session,
    /** Null when the litres were recorded as a herd bulk total. */
    val cowId: Long?,
    val liters: Double,
    /** Price per litre at the time of logging, so price changes don't rewrite history. */
    val pricePerLiter: Double,
    /** Milk fed to calves: produced on the farm but not sold, so it is taken off the sales. */
    @ColumnInfo(defaultValue = "0") val forCalves: Boolean = false,
)

@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val packKg: Double,
    val packPrice: Double,
) {
    val pricePerKg get() = if (packKg > 0) packPrice / packKg else 0.0
}

@Entity(tableName = "formulations")
data class Formulation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String = "",
)

@Entity(
    tableName = "formulation_items",
    indices = [Index("formulationId"), Index("ingredientId")],
    foreignKeys = [
        ForeignKey(Formulation::class, ["id"], ["formulationId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Ingredient::class, ["id"], ["ingredientId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class FormulationItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val formulationId: Long,
    val ingredientId: Long,
    val kg: Double,
)

@Entity(tableName = "feed_records", indices = [Index("date")])
data class FeedRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val session: Session,
    /** Name of the ration or ingredient, kept so the record survives a deleted recipe. */
    val description: String,
    val kg: Double,
    val costPerKg: Double,
) {
    val cost get() = kg * costPerKg
}

@Entity(tableName = "overheads")
data class Overhead(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val amount: Double,
    val recurrence: Recurrence,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
)

@Entity(tableName = "transactions", indices = [Index("date")])
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val type: TxType,
    val category: String,
    val amount: Double,
    val note: String = "",
)

@Entity(
    tableName = "calf_expenses",
    indices = [Index("calfId"), Index("date")],
    foreignKeys = [ForeignKey(Cow::class, ["id"], ["calfId"], onDelete = ForeignKey.CASCADE)],
)
data class CalfExpense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val calfId: Long,
    val date: LocalDate,
    val type: CalfCostType,
    val amount: Double,
    val note: String = "",
)

@Entity(tableName = "tasks", indices = [Index("dueDate")])
data class FarmTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueDate: LocalDate,
    val cowId: Long? = null,
    val done: Boolean = false,
    val notes: String = "",
)
