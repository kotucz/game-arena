package cz.kotu.game.gotfive

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}

fun createDatabase(): AppDatabase {
    val databaseFile = File(System.getenv("DATABASE_PATH") ?: "data/gotfive.db")
    databaseFile.parentFile?.mkdirs()
    return Room.databaseBuilder<AppDatabase>(databaseFile.path)
        .setDriver(BundledSQLiteDriver())
        .build()
}
