package com.example.argame.Model

import android.net.Uri

/***
 *  Ability data, and animation name that will be used for calling an animation inside .fbx
 */

enum class Ability {
    TEST,
    BEAM;

    fun uri() : Uri {
        when (this) {
            TEST-> return Uri.parse("untitledv4.sfb")
            BEAM -> return Uri.parse("Character.sfb")
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
            TEST -> "RightArm|RightArmAction.001"
            BEAM -> "RightArm|RightArmAction.001"
        }
    }
}