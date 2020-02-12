package com.example.argame.Model

import android.util.Log.wtf

/***
 *  The super class of Player and NPC, containing common
 *  methods and properties for both classes
 */

abstract class CombatControllable(baseHealth: Int,
     private val name: String,
     private var attackPower: Int,
     private var health: Int = baseHealth) {

    private var maxHealth = baseHealth
    private var isAlive: Boolean = true
    private var status = CombatControllableStatus(isAlive, health, attackPower, name)


    fun restoreFullHealth() {
        health = maxHealth
    }

    fun restoreHealth(amount: Int) {
        if (amount > 0) {
            if (health + amount > maxHealth) {
                health = maxHealth
            } else {
                health += amount
            }
        }
    }

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
        }
    }

    fun takeDamage(damage: Int) {
        when {
            // Not allowing negative input values
            health - damage in 1 until health -> {
                health -= damage
            }
            health - damage <= 0 -> {
                health = 0
                isAlive = false
            }
            else -> {
                wtf("CCERROR", "Negative value as takeDamage() input")
            }
        }
    }

    fun dealDamage(damage: Int, target: CombatControllable) {
        if (isAlive) {
            when {
                // Not allowing negative input values
                damage > 0 -> target.takeDamage(damage)
                else -> {
                    wtf("CCERROR", "Negative value as dealDamage() input")
                }
            }
        } else {
            // A dead CombatControllable should not be able to deal damage.
            wtf("CCERROR", "I am dead.")
        }
    }

    fun getStatus() : CombatControllableStatus {
        status = CombatControllableStatus(isAlive, health, attackPower, name)
        return status
    }
}