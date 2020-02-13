package com.example.argame.Model

import android.util.Log.wtf
import kotlin.math.sign

/***
 *  The super class of Player and NPC, containing common
 *  methods and properties for both classes
 */

const val minimumAP = 1.0
const val minimumMaxHealth = 1.0


abstract class CombatControllable(
    baseHealth: Double, private val name: String, private var attackPower: Double) {

    private var health: Double
    private var maxHealth = baseHealth
    private var isAlive: Boolean = true
    private var status: CombatControllableStatus

    init {
        var notifyInputError = false

        if (maxHealth in 0.0..minimumMaxHealth) {
            maxHealth = minimumMaxHealth
            wtf("CCERROR", "0 or value < minMaxHP as CombatControllable constructor input")
        }

        if (attackPower in 0.0..minimumAP) {
            attackPower = minimumAP
            wtf("CCERROR", "0 or value < minAP as CombatControllable constructor input")
        }

        if (maxHealth.sign == -1.0) {
            maxHealth *= -1
            notifyInputError = true
        }
        if (attackPower.sign == -1.0) {
            attackPower *= -1
            notifyInputError = true
        }

        if (notifyInputError) {
            wtf("CCERROR", "Negative value as CombatControllable " +
                    "constructor value -- automatically correcting. " +
                    "Use positive values in the future. CC Name: $name")
        }
        health = maxHealth
        status = CombatControllableStatus(isAlive, health, attackPower, name, maxHealth)
    }

    fun restoreFullHealth() {
        if (isAlive) {
            health = maxHealth
        } else {
            wtf("CCERROR", "trying to restore health of a fallen CombatControllable")
        }
    }

    fun restoreHealth(amount: Double) {
        if (isAlive) {
            if (amount > 0) {
                if (health + amount > maxHealth) {
                    health = maxHealth
                } else {
                    health += amount
                }
            }
        } else {
            wtf("CCERROR", "trying to restore health of a fallen CombatControllable")
        }
    }

<<<<<<< HEAD
    fun increaseMaxHealth(multiplier: Double) {
        val newMax = maxHealth * multiplier
        // do something only if the multiplier is greater than 1
        if (newMax > maxHealth) {
            maxHealth = newMax
        } else {
            wtf("CCERROR", "Negative value or value less than 1 as increaseMaxHealth() input")
        }
    }

    fun increaseAP(multiplier: Double) {
        val newMax = multiplier * attackPower
        // do something only if the multiplier is greater than 1
        if (newMax > attackPower) {
            attackPower = newMax
        } else {
             wtf("CCERROR", "Negative value or value less than 1 as increaseAP() input")
=======
    fun increaseMaxHealth(multiplier: Int) {
        if (maxHealth * multiplier > maxHealth) {
            maxHealth *= multiplier
        } else {
            wtf("CCERROR", "Negative value, or value less than 1 as increaseMaxHealth() input")
        }
    }

    fun increaseAP(multiplier: Int) {
        if (attackPower * multiplier > attackPower) {
            attackPower *= multiplier
        } else {
             wtf("CCERROR", "Negative value, or value less than 1 as increaseAP() input")
>>>>>>> 27d728f3ae9afaa736835d673db000214397fbfe
        }
    }

    // Make public, if the CombatControllable may take damage
    // from sources other than another CombatControllable
    private fun takeDamage(damage: Double) {
        when {
            // Not allowing negative input values
            health - damage in 0.0..health -> {
                health -= damage
            }
            health - damage <= 0 -> {
                health = 0.0
                isAlive = false
            }
            else -> {
                wtf("CCERROR", "Negative value as takeDamage() input")
            }
        }
    }

    fun dealDamage(damage: Double, target: CombatControllable) {
        if (isAlive) {
            when {
                // Not allowing negative input values
                damage > 0 -> target.takeDamage(damage * attackPower)
                else -> {
                    wtf("CCERROR", "Negative value or 0 as dealDamage() input")
                }
            }
        } else {
            // A dead CombatControllable should not be able to deal damage.
            wtf("CCERROR", "I am dead.")
        }
    }

    fun getStatus() : CombatControllableStatus {
        status = CombatControllableStatus(isAlive, health, attackPower, name, maxHealth)
        return status
    }
}