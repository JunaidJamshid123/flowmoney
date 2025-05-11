package com.example.flowmoney.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.flowmoney.database.converters.DateConverters
import com.example.flowmoney.database.dao.AccountDao
import com.example.flowmoney.database.dao.CategoryDao
import com.example.flowmoney.database.dao.TransactionDao
import com.example.flowmoney.database.entities.AccountEntity
import com.example.flowmoney.database.entities.CategoryEntity
import com.example.flowmoney.database.entities.TransactionEntity

/**
 * Main database class for the application
 */
@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        AccountEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flow_money_database"
                )
                .fallbackToDestructiveMigration() // For simplicity in this example
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 