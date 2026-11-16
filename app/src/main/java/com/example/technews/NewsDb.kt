package com.example.technews

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(items: List<NewsItem>)

    @Query("SELECT * FROM NewsItem ORDER BY time DESC")
    fun all(): Flow<List<NewsItem>>


    @Query("DELETE FROM NewsItem WHERE link NOT IN (SELECT link FROM NewsItem ORDER BY time DESC LIMIT 100)")
    suspend fun trim()

    @Query("DELETE FROM NewsItem WHERE time < :since")
    suspend fun deleteOld(since: Long)
}

@Database(entities = [NewsItem::class], version = 1, exportSchema = false)
abstract class NewsDb : RoomDatabase() {
    abstract fun dao(): NewsDao
}