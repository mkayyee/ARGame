package com.example.argame.Model.Ability

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.room.TypeConverter
import com.example.argame.Model.CombatControllable.CombatControllable
import com.example.argame.Model.CombatControllable.CombatControllableStatus
import com.example.argame.R

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
    BEAM,
    SHIELD,
    TELEPORT;

    override fun toString(): String {
        return when (this) {
            TEST -> "Fireball"
            BEAM -> "Beam"
            SHIELD -> "Shield"
            TELEPORT -> "Teleport"
        }
    }

    interface AbilityCallbackListener {
        fun onAbilityCast(caster: CombatControllable, target: CombatControllable, ability: Ability)
        fun onAbilityHit(caster: CombatControllable, target: CombatControllable, ability: Ability)
    }

    fun getImage(context: Context) : Drawable? {
        return when (this) {
            TEST -> ContextCompat.getDrawable(context, R.drawable.icon_attack)
            BEAM -> ContextCompat.getDrawable(context, R.drawable.icon_beam)
            SHIELD -> ContextCompat.getDrawable(context, R.drawable.icon_shield)
            TELEPORT -> ContextCompat.getDrawable(context, R.drawable.icon_teleport)
        }
    }

    fun getCooldown() : Long {
        return when (this) {
            TEST -> 1000
            BEAM -> 3000
            SHIELD -> 10000
            TELEPORT -> 15000
        }
    }

    fun uri() : Uri {
        return when (this) {
            TEST -> Uri.parse("untitledv4.sfb")
            BEAM -> Uri.parse("beam.sfb")
            SHIELD -> throw Error("shield doesn't have uri")
            TELEPORT -> throw Error("teleport doesn't have uri")
        }
    }

    // returns the base damage of the ability + levels
    // multiplied by some factor + the caster's attack power.
    // (this will probably change in the future)
    fun getDamage(caster: CombatControllableStatus) : Double {
        return when (this) {
            TEST -> caster.level * 0.5 + 200 + caster.attackPower
            BEAM -> caster.level * 0.25 + 200 + caster.attackPower
            SHIELD -> throw Error("ability Shield does not support getDamage casted by $caster")
            TELEPORT -> throw Error("ability Teleport does not support getDamage casted by $caster")
        }
    }

    fun getCastAnimationString() : String?{
        return when (this) {
            TEST -> "AlienArmature|Alien_SwordSlash"
            BEAM -> "AlienArmature|Alien_Punch"
            SHIELD -> "AlienArmature|Alien_Clapping"
            TELEPORT -> "AlienArmature|Alien_Jump"
        }
    }
}