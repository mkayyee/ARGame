package com.example.argame.Model.CombatControllable

import android.content.Context
import android.util.Log
import android.util.Log.wtf
import com.example.argame.Interfaces.AbilityUser
import com.example.argame.Interfaces.ProjectileAnimator
import com.example.argame.Model.Ability.Ability
import com.example.argame.Model.Ability.AbilityModifier
import com.example.argame.Model.Ability.ProjectileAnimationData
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.rendering.ModelRenderable
import kotlin.math.sign

/***
 *  The super class of Player and NPC, containing common
 *  methods and properties for both classes
 */

const val minimumAP = 1.0
const val minimumMaxHealth = 1.0
const val apGrowModifier = 1.25
const val hpGrowModifier = 1.25
const val xpGrowModifier = 1.25


abstract class CombatControllable(
    baseHealth: Double,
    val name: String,
    private var attackPower: Double,
    val model: ModelRenderable? = null,
    context: Context
) : ProjectileAnimator, AbilityUser {
    private var health: Double
    private var maxHealth = baseHealth
    private var isAlive: Boolean = true
    private var isShielded = false
    private var maxShieldAmount = 600.0 * AbilityModifier.getPwrModifier(Ability.SHIELD)
    private var shieldAmount = 600.0 * AbilityModifier.getPwrModifier(Ability.SHIELD)
    private var status: CombatControllableStatus
    private var level = 1
    private var xp = 0.0
    private var xpRequiredForLevel = 1000.0
    private val callback: CombatControllableListener
    private var modelAnimator: ModelAnimator? = null

    interface CombatControllableListener {
        fun onCCDamaged(cc: CombatControllable)
        fun onCCDeath(cc: CombatControllable)
    }

    init {
        callback = context as CombatControllableListener

        var notifyInputError = false

        if (maxHealth in 0.0..minimumMaxHealth) {
            maxHealth =
                minimumMaxHealth
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
        status =
            CombatControllableStatus(
                isAlive,
                health,
                attackPower,
                name,
                maxHealth,
                level,
                xp,
                xpRequiredForLevel,
                maxShieldAmount,
                shieldAmount,
                isShielded
            )
    }

    fun setModelAnimator(animator: ModelAnimator) {
        modelAnimator = animator
    }

    fun getModelAnimator() : ModelAnimator? {
        return modelAnimator
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

    fun useShield() {
        restoreShieldAmountFull()
        isShielded = true
    }

    fun increaseMaxShieldAmount(multiplier: Double) {
        val newMax = maxShieldAmount * multiplier
        // do something only if the multiplier is greater than 1
        if (newMax > maxShieldAmount) {
            maxShieldAmount = newMax
        } else {
            wtf("CCERROR", "Negative value or value less than 1 as increaseMaxHealth() input")
        }
    }

    fun restoreShieldAmountFull() {
        shieldAmount = maxShieldAmount
    }

    private fun reduceShield(amount: Double, cb: (Double) -> Unit) {
        val difference = shieldAmount - amount
        if (difference in 0.0..shieldAmount) {
            if (difference == 0.0) {
                shieldAmount = 0.0
                isShielded = false
                cb(0.0)
            } else {
                shieldAmount -= amount
                cb(0.0)
            }
        }  else if (difference.sign == -1.0) {
            shieldAmount = 0.0
            isShielded = false
            cb(difference * -1.0)
        }
        callback.onCCDamaged(this)
    }

    fun increaseMaxHealth(multiplier: Double) {
        val newMax = maxHealth * multiplier
        // do something only if the multiplier is greater than 1
        if (newMax > maxHealth) {
            maxHealth = newMax
        } else {
            wtf("CCERROR", "Negative value or value less than 1 as increaseMaxHealth() input")
        }
    }

    fun increaseXP(amount: Double) {
        if (amount.sign != -1.0) {
            if (xp + amount >= xpRequiredForLevel) {
                levelUp()
                val newAmount = xp + amount - xpRequiredForLevel
                // recursive call on level up, so the excess xp gets added
                increaseXP(newAmount)
            } else {
                xp += amount
            }
        } else {
            wtf("CCERROR", "Increasing xp with negative values is not increasing it...")
        }
    }

    private fun levelUp() {
        level ++
        xpRequiredForLevel *= xpGrowModifier
        increaseMaxHealth(hpGrowModifier)
        increaseAP(apGrowModifier)
        restoreFullHealth()
    }

    fun increaseAP(multiplier: Double) {
        val newMax = multiplier * attackPower
        // do something only if the multiplier is greater than 1
        if (newMax > attackPower) {
            attackPower = newMax
        } else {
             wtf("CCERROR", "Negative value or value less than 1 as increaseAP() input")
        }
    }

    // Make public, if the CombatControllable may take damage
    // from sources other than another CombatControllable
    private fun takeDamage(damage: Double) {
        Log.d("CCDMG", "$name damaged for $damage. isShielded: $isShielded shieldAmount: $shieldAmount health: $health")
        if (!isShielded) {
            when {
                // Not allowing negative input values
                health - damage in 0.0..health -> {
                    health -= damage
                    callback.onCCDamaged(this)
                }
                health - damage <= 0 -> {
                    health = 0.0
                    isAlive = false
                    callback.onCCDeath(this)
                }
                else -> {
                    wtf("CCERROR", "Negative value as takeDamage() input")
                }
            }
        } else {
            reduceShield(damage) {difference ->
                if (difference != 0.0) {
                    // difference not being 0 means that the shield
                    // was destroyed and there was some leftover damage
                    takeDamage(difference)
                }
            }

        }
    }

    fun dealDamage(damage: Double, target: CombatControllable) {
        if (isAlive) {
            when {
                // Not allowing negative input values
                damage > 0 -> target.takeDamage(damage)
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
        status =
            CombatControllableStatus(
                isAlive,
                health,
                attackPower,
                name,
                maxHealth,
                level,
                xp,
                xpRequiredForLevel,
                maxShieldAmount,
                shieldAmount,
                isShielded
            )
        return status
    }

    fun useAbility(
        ability: Ability, target: CombatControllable, projectileData: ProjectileAnimationData, callback: () -> Unit) {
        super.useAbility(this, target, ability, projectileData) {
            callback()
        }
    }
}