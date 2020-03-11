package com.example.argame.Model.Persistence
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

    fun getSelectableAbilities() =
        AppDatabase.get(
            getApplication()
        ).abilitiesDao().getAllUnselectedAbilities()

    fun getSelectedAbilities() =
        AppDatabase.get(
            getApplication()
        ).abilitiesDao().getUserSelectedAbilities()

    fun selectAbility(aid: Int) =
        AppDatabase.get(
            getApplication()
        ).abilitiesDao().selectAbility(aid)

    fun deSelectAbility(aid: Int) =
        AppDatabase.get(
            getApplication()
        ).abilitiesDao().deselectAbility(aid)
}

object Entities {

    @Entity
    data class SelectableAbility(
        @PrimaryKey
        val abilityID: Int,
        val isSelected: Int = 0)

    // Handles two-way communication between the selected and the unselected abilities
    // callback methods for updates, so we can keep track of how many abilities the user
    // currently has selected, and therefor, able to prevent them for selecting too many.

    @Dao
    interface SelectableAbilityDao {
        @Query("UPDATE SelectableAbility SET isSelected = 1 WHERE abilityID = :aid")
        fun selectAbility(aid: Int)

        @Query("UPDATE SelectableAbility SET isSelected = 0 WHERE abilityID = :aid")
        fun deselectAbility(aid: Int)

        @Query("SELECT * FROM SelectableAbility WHERE isSelected = 1")
        fun getUserSelectedAbilities(): LiveData<List<SelectableAbility>>

        @Query("SELECT * FROM SelectableAbility WHERE isSelected = 0")
        fun getAllUnselectedAbilities(): LiveData<List<SelectableAbility>>

        @Query("SELECT * FROM SelectableAbility")
        fun getAllAbilities() : List<SelectableAbility>

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        fun insertAbility(ability: SelectableAbility): Long

        @Query("SELECT * FROM SelectableAbility WHERE isSelected = 0")
        fun checkUnselectedAbilities(): List<SelectableAbility>

         @Query("SELECT COUNT(*) from SelectableAbility where isSelected = 1")
         fun getSelectedAbilitiesCount() : Int

        @Query("SELECT * FROM SelectableAbility WHERE isSelected = 1")
        fun getSelectedAbilities(): List<SelectableAbility>
    }
}
