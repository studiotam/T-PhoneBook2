package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SpamDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<SpamNumberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SpamNumberEntity)

    @Query("DELETE FROM spam_numbers WHERE number IN (:numbers)")
    suspend fun deleteByNumbers(numbers: List<String>): Int

    @Query("DELETE FROM spam_numbers WHERE number = :number")
    suspend fun deleteByNumber(number: String): Int

    @Query("SELECT * FROM spam_numbers WHERE number = :number LIMIT 1")
    suspend fun findByNumber(number: String): SpamNumberEntity?

    @Query("SELECT * FROM spam_numbers WHERE number = :number OR number = :altNumber LIMIT 1")
    suspend fun findByNumberOrAlt(number: String, altNumber: String): SpamNumberEntity?

    @Query("SELECT * FROM spam_numbers ORDER BY risk_score DESC, updated_at DESC")
    suspend fun getAllSpamNumbers(): List<SpamNumberEntity>

    @Query("SELECT * FROM spam_numbers ORDER BY risk_score DESC, updated_at DESC")
    fun getAllFlow(): Flow<List<SpamNumberEntity>>

    @Query("SELECT * FROM spam_numbers WHERE number LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR target_entity LIKE '%' || :query || '%' ORDER BY risk_score DESC LIMIT 100")
    fun searchFlow(query: String): Flow<List<SpamNumberEntity>>

    @Query("SELECT COUNT(*) FROM spam_numbers")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM spam_numbers")
    suspend fun getCount(): Int

    @Query("DELETE FROM spam_numbers")
    suspend fun clearAll(): Int

    @Query("SELECT number FROM spam_numbers WHERE calling_code = :callingCode")
    suspend fun getNumbersByCallingCode(callingCode: String): List<String>

    @Transaction
    suspend fun applyMirrorSync(
        callingCode: String,
        newEntities: List<SpamNumberEntity>,
        explicitRevoked: List<String>
    ): Pair<Int, Int> {
        val existingNumbers = getNumbersByCallingCode(callingCode)
        val newNumberSet = newEntities.map { it.number }.toSet()
        val toDelete = (existingNumbers.filter { it !in newNumberSet } + explicitRevoked).distinct()

        val deletedCount = if (toDelete.isNotEmpty()) {
            deleteByNumbers(toDelete)
        } else {
            0
        }

        if (newEntities.isNotEmpty()) {
            upsertAll(newEntities)
        }

        return Pair(newEntities.size, deletedCount)
    }

    @Transaction
    suspend fun applyDeltaSync(added: List<SpamNumberEntity>, revoked: List<String>): Pair<Int, Int> {
        if (added.isNotEmpty()) {
            upsertAll(added)
        }
        val deletedCount = if (revoked.isNotEmpty()) {
            deleteByNumbers(revoked)
        } else {
            0
        }
        return Pair(added.size, deletedCount)
    }
}
