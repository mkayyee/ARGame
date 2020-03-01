package com.example.argame.Model
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.room.*

@Database(entities = arrayOf(User::class, Entities.SelectableAbility::class), version = 2)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun abilitiesDao(): Entities.SelectableAbilityDao

    companion object {
        private var sInstance: AppDatabase? = null
        @Synchronized
        fun get(context: Context): AppDatabase {
            if (sInstance == null) {
                sInstance =
                    Room.databaseBuilder(context.applicationContext,
                        AppDatabase::class.java, "abilities.db")
                        .fallbackToDestructiveMigration()
                        .build()
            }
            return sInstance!!
        }
    }
}

class AbilitiesLiveModel(application: Application): AndroidViewModel(application) {

    fun getSelectableAbilities(uid: Int) =
        AppDatabase.get(getApplication()).abilitiesDao().getAllUnselectedAbilities()

    fun getSelectedAbilities(uid: Int) =
        AppDatabase.get(getApplication()).abilitiesDao().getUserSelectedAbilities(uid)

    fun selectAbility(uid: Int, aid: Int) =
        AppDatabase.get(getApplication()).abilitiesDao().selectAbility(uid, aid)

    fun deSelectAbility(uid: Int, aid: Int) =
        AppDatabase.get(getApplication()).abilitiesDao().deselectAbility(uid, aid)
}

object Entities {

    @Entity(foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userID"]
    )])
    data class SelectableAbility(
        @PrimaryKey
        val abilityID: Int,
        val userID: Int? = null)

    // Handles two-way communication between the selected and the unselected abilities
    // callback methods for updates, so we can keep track of how many abilities the user
    // currently has selected, and therefor, able to prevent them for selecting too many.

    @Dao
    interface SelectableAbilityDao {
        @Query("UPDATE SelectableAbility SET userID = :uid WHERE abilityID = :aid")
        fun selectAbility(uid: Int, aid: Int)

        @Query("UPDATE SelectableAbility SET userID = NULL WHERE userID = :uid AND abilityID = :aid")
        fun deselectAbility(uid: Int, aid: Int)

        @Query("SELECT * FROM SelectableAbility WHERE userID = :uid")
        fun getUserSelectedAbilities(uid: Int): LiveData<List<SelectableAbility>>

        @Query("SELECT * FROM SelectableAbility WHERE userID IS NULL")
        fun getAllUnselectedAbilities(): LiveData<List<SelectableAbility>>

        @Query("SELECT * FROM SelectableAbility")
        fun getAllAbilities() : List<SelectableAbility>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insertAbility(ability: SelectableAbility): Long

        @Query("SELECT * FROM SelectableAbility WHERE userID IS NULL")
        fun checkUnselectedAbilities(): List<SelectableAbility>
    }
}
