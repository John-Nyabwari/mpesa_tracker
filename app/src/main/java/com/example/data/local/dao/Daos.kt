package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE timestamp >= :startDate ORDER BY timestamp DESC, id DESC")
    fun getTransactionsFrom(startDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC, id DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getAllTransactionsDirect(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE mpesaCode = :code LIMIT 1")
    suspend fun getByCode(code: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET category = :newCategory WHERE id = :id")
    suspend fun updateCategory(id: Long, newCategory: String)

    @Query("UPDATE transactions SET category = :newCategory WHERE counterparty = :counterparty")
    suspend fun updateCategoryForMerchant(counterparty: String, newCategory: String)

    @Query("SELECT * FROM transactions WHERE newBalance IS NOT NULL ORDER BY timestamp DESC, id DESC LIMIT 1")
    fun getLatestBalanceTransaction(): Flow<TransactionEntity?>

    @Query("DELETE FROM transactions WHERE timestamp < :trackingStartDate")
    suspend fun pruneBefore(trackingStartDate: Long)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    suspend fun getAllCategoriesDirect(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET name = :newName, colorHex = :newColor WHERE name = :oldName")
    suspend fun renameCategory(oldName: String, newName: String, newColor: String)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM merchant_category_rules WHERE merchantIdentifier = :identifier LIMIT 1")
    suspend fun getRuleForMerchant(identifier: String): MerchantCategoryRuleEntity?

    @Query("SELECT * FROM merchant_category_rules ORDER BY updatedAt DESC")
    fun getAllMerchantRules(): Flow<List<MerchantCategoryRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMerchantRule(rule: MerchantCategoryRuleEntity)

    @Delete
    suspend fun deleteMerchantRule(rule: MerchantCategoryRuleEntity)

    @Query("SELECT * FROM custom_keyword_rules ORDER BY createdAt DESC")
    fun getAllKeywordRules(): Flow<List<CustomKeywordRuleEntity>>

    @Query("SELECT * FROM custom_keyword_rules")
    suspend fun getAllKeywordRulesDirect(): List<CustomKeywordRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeywordRule(rule: CustomKeywordRuleEntity): Long

    @Delete
    suspend fun deleteKeywordRule(rule: CustomKeywordRuleEntity)
}

@Dao
interface UnparsedSmsDao {
    @Query("SELECT * FROM unparsed_sms WHERE status = 'PENDING' ORDER BY receivedTimestamp DESC")
    fun getPendingUnparsed(): Flow<List<UnparsedSmsEntity>>

    @Query("SELECT * FROM unparsed_sms WHERE status = 'PENDING'")
    suspend fun getPendingUnparsedDirect(): List<UnparsedSmsEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(unparsed: UnparsedSmsEntity): Long

    @Query("UPDATE unparsed_sms SET status = 'RESOLVED' WHERE id = :id")
    suspend fun markResolved(id: Long)

    @Delete
    suspend fun delete(unparsed: UnparsedSmsEntity)
}

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    fun getPreferences(): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    suspend fun getPreferencesDirect(): UserPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(prefs: UserPreferencesEntity)
}
