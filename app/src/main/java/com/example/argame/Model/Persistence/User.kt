package com.example.argame.Model.Persistence

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.room.*

@Entity
data class User(
    @PrimaryKey
    val id: Int,
    val username: String,
    val highScore: Int = 0,
    val numberOfGames: Int = 0,
    val avatar: String? = null)

@Dao
interface UserDao {
    @Query("SELECT * FROM user")
    fun getAll(): LiveData<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: User): Long

    @Query("SELECT * FROM user WHERE user.id = :uid")
    fun getUser(uid: Int): User

    @Query("UPDATE User SET username = :uName WHERE id = :uid")
    fun changeUsername(uid: Int, uName: String)

    @Query("UPDATE User SET highScore = :new WHERE id = :uid")
    fun updateHighScore(new: Int, uid: Int)

    @Query("UPDATE User SET numberOfGames = (numberOfGames + 1) where id = :uid")
    fun incrementNumOfGames(uid: Int)
}

class UserLiveModel(application: Application):
    AndroidViewModel(application) {

    private val users: LiveData<List<User>> =
        AppDatabase.get(getApplication()).userDao().getAll()

    fun getUsers() = users
}


