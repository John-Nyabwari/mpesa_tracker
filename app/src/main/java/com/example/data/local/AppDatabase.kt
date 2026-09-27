package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        MerchantCategoryRuleEntity::class,
        CustomKeywordRuleEntity::class,
        UnparsedSmsEntity::class,
        UserPreferencesEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun ruleDao(): RuleDao
    abstract fun unparsedSmsDao(): UnparsedSmsDao
    abstract fun preferencesDao(): PreferencesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity("Transfers", "#244C4E", true),
            CategoryEntity("Utilities", "#337D63", true),
            CategoryEntity("Shopping", "#89BA4F", true),
            CategoryEntity("Cash Withdrawal", "#804B35", true),
            CategoryEntity("Airtime & Bundles", "#58C411", true),
            CategoryEntity("Fees & Charges", "#C62828", true),
            CategoryEntity("Savings", "#1976D2", true),
            CategoryEntity("Groceries", "#388E3C", true),
            CategoryEntity("Transport", "#F57C00", true),
            CategoryEntity("Food & Dining", "#E64A19", true),
            CategoryEntity("Entertainment", "#7B1FA2", true),
            CategoryEntity("Rent", "#5D4037", true),
            CategoryEntity("Salary/Income", "#2E7D32", true),
            CategoryEntity("Other", "#4C5267", true)
        )

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mpesa_expense_tracker.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            database.categoryDao().insertAll(DEFAULT_CATEGORIES)
                            database.preferencesDao().savePreferences(UserPreferencesEntity())
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
