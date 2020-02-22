package com.example.argame.Model
import android.content.Context
import android.content.SharedPreferences
import com.example.argame.Interfaces.PreferenceHelper.defaultPreference



class Level(context: Context) {

    private val saver = defaultPreference(context)
    private val duckNPC = "Ankka"

    private fun getLevelNum(): Int {
       val levelNum =  saver.getInt("levelNum", 1)
        return levelNum
    }

    fun createLevel() : Int {
        val curLevel = getLevelNum()
        return when(curLevel % 10){
           0  -> bossLevelContent(curLevel)
            else -> levelContent(curLevel)
        }
    }

    private fun levelContent(levelNum: Int) : Int {
        // What enemies to spawn, how many
        // Difficulty multiplier
        // For initial testing this will only return the number of ducks to spawn
        return when(levelNum){
            1 -> 3
            else -> 5
        }
    }

    private fun bossLevelContent(levelNum: Int) : Int {
        // What boss to spawn
        // Difficulty multiplier
        return 1
    }
}
