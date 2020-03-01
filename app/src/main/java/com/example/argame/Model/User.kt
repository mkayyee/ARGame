package com.example.argame.Model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.room.*

@Entity
data class User(
    @PrimaryKey
    val id: Int,
    val username: String,
    val highScore: Int? = null,
    var numberOfGames: Int = 0,
    val avatar: String? = null)

@Dao
interface UserDao {
    @Query("SELECT * FROM user")
    fun getAll(): LiveData<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: User): Long

    @Query("SELECT * FROM user WHERE user.id = :uid")
    fun getUser(uid: Int): User

    @Query("UPDATE User SET highScore = :new")
    fun updateHighScore(new: Int)

    @Query("UPDATE User SET highScore = highScore + 1")
    fun incrementNumOfGames()
}

class UserLiveModel(application: Application):
    AndroidViewModel(application) {

    private val users: LiveData<List<User>> =
        AppDatabase.get(getApplication()).userDao().getAll()

    fun getUsers() = users
}


