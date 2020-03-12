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
 *  Ability data for getting cast animations, models uri's,
 *  image icons and the ability damage.
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
    FBALL,
    BEAM,
    SHIELD,
    TELEPORT,
    DOT,
    ATK;

    override fun toString(): String {
        return when (this) {
            FBALL -> "Fireball"
            BEAM -> "Beam"
            SHIELD -> "Shield"
            TELEPORT -> "Teleport"
            DOT -> "DoT"
            ATK -> "Attack"
        }
    }

    interface AbilityCallbackListener {
        fun onAbilityCast(caster: CombatControllable, target: CombatControllable, ability: Ability)
        fun onAbilityHit(caster: CombatControllable, target: CombatControllable, ability: Ability)
    }

    fun getImage(context: Context) : Drawable? {
        return when (this) {
            FBALL -> ContextCompat.getDrawable(context, R.drawable.icon_attack)
            BEAM -> ContextCompat.getDrawable(context, R.drawable.icon_beam)
            SHIELD -> ContextCompat.getDrawable(context, R.drawable.icon_shield)
            TELEPORT -> ContextCompat.getDrawable(context, R.drawable.icon_teleport)
            DOT -> ContextCompat.getDrawable(context, R.drawable.icon_attack)
            ATK -> ContextCompat.getDrawable(context, R.drawable.icon_attack)
        }
    }

    fun getCooldown() : Long {
        return when (this) {
            FBALL -> 1000 * AbilityModifier.getCdModifier(this).toLong()
            BEAM -> 3000 * AbilityModifier.getCdModifier(this).toLong()
            SHIELD -> 10000 * AbilityModifier.getCdModifier(this).toLong()
            TELEPORT -> 15000 * AbilityModifier.getCdModifier(this).toLong()
            DOT -> 3000 * AbilityModifier.getCdModifier(this).toLong()
            ATK -> 100 * AbilityModifier.getCdModifier(this).toLong()
        }
    }

    fun uri() : Uri {
        return when (this) {
            FBALL -> Uri.parse("untitledv4.sfb")
            BEAM -> Uri.parse("beam.sfb")
            SHIELD -> throw Error("shield doesn't have uri")
            TELEPORT -> throw Error("teleport doesn't have uri")
            DOT -> throw Error("teleport doesn't have uri")
            ATK -> throw Error("teleport doesn't have uri")
        }
    }

    // returns the base damage of the ability + levels
    // multiplied by some factor + the caster's attack power.
    // (this will probably change in the future)
    fun getDamage(caster: CombatControllableStatus) : Double {
        return when (this) {
            FBALL -> caster.level * 0.5 + 200 + caster.attackPower
            BEAM -> caster.level * 0.25 + 200 + caster.attackPower
            SHIELD -> throw Error("ability Shield does not support getDamage casted by $caster")
            TELEPORT -> throw Error("ability Teleport does not support getDamage casted by $caster")
            DOT -> caster.level * 0.25 + 200 + caster.attackPower
            ATK -> caster.level * 0.25 + 200 + caster.attackPower
        }
    }

    fun getCastAnimationString() : String?{
        return when (this) {
            FBALL -> "AlienArmature|Alien_SwordSlash"
            BEAM -> "AlienArmature|Alien_Punch"
            SHIELD -> "AlienArmature|Alien_Clapping"
            TELEPORT -> "AlienArmature|Alien_Jump"
            DOT -> "AlienArmature|Alien_SwordSlash"
            ATK -> "AlienArmature|Alien_SwordSlash"
        }
    }

    fun updateAbility() {
        // TODO: Update ability
    }
}