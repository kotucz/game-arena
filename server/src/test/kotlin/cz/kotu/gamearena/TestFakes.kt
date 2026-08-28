package cz.kotu.gamearena

import me.tatarka.inject.annotations.Provides
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.nio.file.Files

private fun createTempDatabase(): AppDatabase {
    val tmp = Files.createTempFile("gamearena-test-", ".db").toFile()
    tmp.deleteOnExit()
    tmp.parentFile?.mkdirs()
    return Room.databaseBuilder<AppDatabase>(tmp.path)
        .setDriver(BundledSQLiteDriver())
        .addMigrations(
            object : androidx.room.migration.Migration(1, 2) {
                override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
                    connection.prepare("CREATE TABLE IF NOT EXISTS sessions (tokenHash TEXT NOT NULL PRIMARY KEY, username TEXT NOT NULL, expiresAt INTEGER NOT NULL)").use {
                        it.step()
                    }
                }
            },
            object : androidx.room.migration.Migration(2, 3) {
                override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
                    connection.prepare(
                        "CREATE TABLE IF NOT EXISTS games (id TEXT NOT NULL PRIMARY KEY, type TEXT NOT NULL, playersJson TEXT NOT NULL, createdAtMillis INTEGER NOT NULL, stateJson TEXT NOT NULL, logsJson TEXT NOT NULL DEFAULT '[]', updatedAtMillis INTEGER NOT NULL)"
                    ).use {
                        it.step()
                    }
                }
            },
        )
        .build()
}

class TestFakes(
    @get:Provides
    val database: AppDatabase = createTempDatabase(),
)
