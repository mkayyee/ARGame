package com.example.argame.Model.NPC

import android.content.Context
import android.net.Uri
import com.example.argame.Model.CombatControllable.apGrowModifier
import com.example.argame.Model.CombatControllable.hpGrowModifier
import com.google.ar.sceneform.rendering.ModelRenderable
import kotlin.math.pow

const val MELEE_BASE_HP = 500.0
const val MELEE_BASE_AP = 5.0
const val RANGED_BASE_HP = 250.0
const val RANGED_BASE_AP = 10.0
const val SUPPORT_BASE_HP = 100.0
const val SUPPORT_BASE_AP = 10.0
const val BOSSLVL10_BASE_HP  = 5000.0
const val BOSSLVL10_BASE_AP  = 15.0
const val BOSSLVL20_BASE_HP  = 15000.0
const val BOSSLVL20_BASE_AP  = 25.0
const val BOSSLVL30_BASE_HP  = 25000.0
const val BOSSLVL30_BASE_AP  = 35.0

enum class NPCType {
    MELEE,
    RANGED,
    SUPPORT,
    BOSSLVL10,
    BOSSLVL20,
    BOSSLVL30;
    // etc

    fun getNPCObject(level: Int, renderable: ModelRenderable, npcID: Int, context: Context) : NPC {
        // hpGrowModifier and apGrowModifier defined in CombatControllable (how much AP scales per level, how much HP scales per level)
        val hpMultiplier = hpGrowModifier.pow(level.toDouble())
        val apMultiplier = apGrowModifier.pow(level.toDouble())

        return when (this) {
            MELEE -> NPC(
                MELEE_BASE_AP * apMultiplier,
                "Hurdur",
                MELEE_BASE_HP * hpMultiplier,
                renderable,
                type = this,
                id = npcID,
                context = context
            )
            RANGED -> NPC(
                RANGED_BASE_AP * apMultiplier,
                "Hurdur",
                RANGED_BASE_HP * hpMultiplier,
                renderable,
                type = this,
                id = npcID,
                context = context
            )
            SUPPORT -> NPC(
                SUPPORT_BASE_AP * apMultiplier,
                "Hurdur",
                SUPPORT_BASE_HP * hpMultiplier,
                renderable,
                type = this,
                id = npcID,
                context = context
            )
            BOSSLVL10 -> NPC(
                BOSSLVL10_BASE_AP,
                "Hurdur",
                BOSSLVL10_BASE_HP, renderable, type = this, id = npcID, context = context
            )
            BOSSLVL20 -> NPC(
                BOSSLVL20_BASE_AP,
                "Hurdur",
                BOSSLVL20_BASE_HP, renderable, type = this, id = npcID, context = context
            )
            BOSSLVL30 -> NPC(
                BOSSLVL30_BASE_AP,
                "Hurdur",
                BOSSLVL30_BASE_HP, renderable, type = this, id = npcID, context = context
            )
        }
    }

    fun hitAnimationString() : String {
        return when (this) {
            MELEE -> "SpiderArmature|Spider_Jump"
            RANGED -> "DragonArmature|Dragon_Hit"
            SUPPORT -> "BatArmature|Bat_Hit"
            BOSSLVL10 -> "DragonArmature|Dragon_Hit"
            else -> "No animation"
        }
    }

    fun idleAnimationString() : String {
        return when (this) {
            MELEE -> "SpiderArmature|Spider_Idle"
            RANGED -> "DragonArmature|Dragon_Flying"
            SUPPORT -> "BatArmature|Bat_Flying"
            BOSSLVL10 -> "DragonArmature|Dragon_Flying"
            else -> "No animation"
        }
    }

    fun walkAnimationString() : String {
        return when (this) {
            MELEE -> "SpiderArmature|Spider_Walk"
            RANGED -> "DragonArmature|Dragon_Flying"
            SUPPORT -> "BatArmature|Bat_Flying"
            BOSSLVL10 -> "DragonArmature|Dragon_Flying"
            else -> "No animation"
        }
    }

    fun deathAnimationString() : String {
        return when (this) {
            MELEE -> "SpiderArmature|Spider_Death"
            RANGED -> "DragonArmature|Dragon_Death"
            SUPPORT -> "BatArmature|Bat_Death"
            BOSSLVL10 -> "DragonArmature|Dragon_Death"
            else -> "No animation"
        }
    }

    fun attackAnimationString() : String {
        return when (this) {
            MELEE -> "SpiderArmature|Spider_Attack"
            RANGED -> "DragonArmature|Dragon_Hit"
            SUPPORT -> "BatArmature|Bat_Hit"
            BOSSLVL10 -> "DragonArmature|Dragon_Hit"
            else -> "No animation"
        }
    }

    fun modelUri() : Uri {
        return when (this) {
            MELEE -> Uri.parse("Spider.sfb")
            RANGED -> Uri.parse("Dragon.sfb")
            SUPPORT -> Uri.parse("Bat.sfb")
            BOSSLVL10 -> Uri.parse("Dragon.sfb")
            else -> Uri.parse("duck.sfb")
        }
    }
}