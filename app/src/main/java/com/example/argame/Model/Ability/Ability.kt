package com.example.argame.Model.Ability

import android.net.Uri
import androidx.room.TypeConverter
import com.example.argame.Model.CombatControllable.CombatControllable
import com.example.argame.Model.CombatControllable.CombatControllableStatus

/***
 *  Ability data, and animation name that will be used for calling an animation inside .fbx
 */

object AbilityConverter {
    @TypeConverter
    fun fromAbility(ability: Ability) : Int {
        return ability.ordinal
    }
    @TypeConverter
    fun toAbility(int: Int) : Ability {
        return Ability.values()[int]
    }
}

enum class Ability() {
    TEST,
    BEAM;

    override fun toString(): String {
        return when (this) {
            TEST -> "Sphere"
            BEAM -> "Beam"
        }
    }

    interface AbilityCallbackListener {
        fun onAbilityCast(caster: CombatControllable, target: CombatControllable, ability: Ability)
        fun onAbilityHit(caster: CombatControllable, target: CombatControllable, ability: Ability)
    }

    fun uri() : Uri {
        when (this) {
            TEST -> return Uri.parse("untitledv4.sfb")
            BEAM -> return Uri.parse("beam.sfb")
        }
    }

    // returns the base damage of the ability + levels
    // multiplied by some factor + the caster's attack power.
    // (this will probably change in the future)
    fun getDamage(caster: CombatControllableStatus) : Double {
        return when (this) {
            TEST -> caster.level * 0.5 + 200 + caster.attackPower
            BEAM -> caster.level * 0.25 + 200 + caster.attackPower
        }
    }

    fun getCastAnimationString() : String?{
        return when (this) {
            TEST -> "PlayerAttack"
            BEAM -> "PlayerAttack"
        }
    }
}