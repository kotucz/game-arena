package cz.kotu.game.gotfive

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

@Database(entities = [User::class, Session::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
}

fun createDatabase(): AppDatabase {
    val databaseFile = File(System.getenv("DATABASE_PATH") ?: "data/gotfive.db")
    databaseFile.parentFile?.mkdirs()
    return Room.databaseBuilder<AppDatabase>(databaseFile.path)
        .setDriver(BundledSQLiteDriver())
        .addMigrations(object : Migration(1, 2) {
            override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
                connection.prepare("CREATE TABLE IF NOT EXISTS sessions (tokenHash TEXT NOT NULL PRIMARY KEY, username TEXT NOT NULL, expiresAt INTEGER NOT NULL)").use {
                    it.step()
                }
            }
        })
        .build()
}
