package com.example.argame.Model.Player

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.util.Log.wtf
import com.example.argame.Model.CombatControllable.CombatControllable
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable

/**
 *  The player class that inherits from CombatControllable
 *  Hold all the data required for all combat and game events.
 */

class Player(
        ap: Double,
        name: String,
        startHealth: Double,
        model: ModelRenderable? = null,
        context: Context,
        private var hpBar: ViewRenderable? = null,
        // Initialize points with a value from shared preferences if going to a new level
        private var score: Int = 0)
    : CombatControllable(startHealth, name, ap, model, context) {

    private var ultBar: ViewRenderable? = null
    private var abilitiesUsed = 0
    private var points = 0
    private var ultProgress = 0
    private val ultMax = 1000
    private var ultIsReady = false
    private var health = startHealth

    init {
        Log.d("PLAYER", "Player initialized with score: $score")
    }

    // Add points every time player kills an NPC. amount == NPC maxHealth
    fun addPoints(amount: Int) {
        if (amount > 0) {
            points += amount
            Log.d("PLAYER", "Points added. Points: $points")
        } else {
            wtf("INPUTERR", "Negative input in addPoints(). Value $amount")
        }
    }

    fun clearStatus() {
        ultProgress = 0
        ultIsReady = false
        points = 0
    }

    fun incrementAbilitiesUsed() {
        Log.d("PLAYER", "Amount of abilities used: $abilitiesUsed")
        abilitiesUsed ++
    }

    fun getUltProgress() : Int {
        return ultProgress
    }

    fun getMaxUlt() : Int {
        return ultMax
    }

    fun getMaxHealth(): Double {
        return health
    }

    fun calculateScore() : Int {
        if (points > 0 && abilitiesUsed > 0) {
            return (points / abilitiesUsed) + score
        } else return score
    }

    fun getPoints() : Int {
        return points
    }

    fun ultUsed() {
        ultIsReady = false
        ultProgress = 0
    }

    // Increase ult progress by the amount of damage the ability does
    fun increaseUltProgress(amount: Int) {
        if (amount > 0) {
            if (ultProgress + amount >= ultMax) {
                ultProgress = ultMax
                ultIsReady = true
                Log.d("PLAYER", "Player ult ready")
            } else {
                ultProgress += amount
                Log.d("PLAYER", "Ult increased. Current progress: $ultProgress")
            }
        } else {
            wtf("INPUTERR", "Negative input in increaseUltProgress(). Value $amount")
        }
    }

    fun setHPRenderable(renderable: ViewRenderable) {
        hpBar = renderable
    }

    fun setUltRenderable(renderable: ViewRenderable) {
        ultBar = renderable
    }

    fun getHPBar() : ViewRenderable? {
        return hpBar
    }

    fun getUltBar() : ViewRenderable? {
        return ultBar
    }

}