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
        .addMigrations(*ALL_MIGRATIONS)
        .build()
}

class TestFakes(
    @get:Provides
    val database: AppDatabase = createTempDatabase(),
)
