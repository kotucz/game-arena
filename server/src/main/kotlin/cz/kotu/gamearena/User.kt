package cz.kotu.gamearena

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Index
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(
    tableName = "users",
    indices = [Index(value = ["usernameLower"], unique = true)],
)
data class User(
    @PrimaryKey val firebaseUid: String,
    val username: String,
    val usernameLower: String,
    val email: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE usernameLower = :usernameLower LIMIT 1")
    suspend fun findByUsernameLower(usernameLower: String): User?

    @Query("SELECT * FROM users WHERE firebaseUid = :firebaseUid LIMIT 1")
    suspend fun findByFirebaseUid(firebaseUid: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User)
}
