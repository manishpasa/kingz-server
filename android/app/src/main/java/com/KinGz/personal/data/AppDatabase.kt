
package com.KinGz.personal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Task::class,
        MoneyTransaction::class,
        Account::class,
        MoneyTransfer::class,
        Debt::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun moneyDao(): MoneyDao
    abstract fun accountDao(): AccountDao
    abstract fun moneyTransferDao(): MoneyTransferDao
    abstract fun debtDao(): DebtDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS money_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        isIncome INTEGER NOT NULL,
                        amountPaisa INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        note TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS money_accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        initialBalancePaisa INTEGER NOT NULL,
                        isArchived INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT OR IGNORE INTO money_accounts
                    (id, name, initialBalancePaisa, isArchived, createdAt)
                    VALUES
                    (1, 'Cash', 0, 0, strftime('%s','now') * 1000)
                """.trimIndent())

                db.execSQL("""
                    ALTER TABLE money_transactions
                    ADD COLUMN accountId INTEGER NOT NULL DEFAULT 1
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS money_transfers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fromAccountId INTEGER NOT NULL,
                        toAccountId INTEGER NOT NULL,
                        amountPaisa INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS money_debts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        person TEXT NOT NULL,
                        amountPaisa INTEGER NOT NULL,
                        isOwedToMe INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        isSettled INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    ALTER TABLE money_transactions
                    ADD COLUMN debtId INTEGER
                """.trimIndent())
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
            ALTER TABLE money_accounts
            ADD COLUMN excludeFromTotal INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
            }
        }
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kingz_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
