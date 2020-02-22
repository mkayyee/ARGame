package com.example.argame.Model

import android.content.Context
import android.net.Uri
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
//class NPC(ap: Double, name: String, startHealth: Double, model: ModelRenderable? = null, type: NPCType)
//    : CombatControllable(startHealth, name, ap, model)class NPC(ap: Double, name: String, startHealth: Double, model: ModelRenderable? = null, type: NPCType)
//    : CombatControllable(startHealth, name, ap, model)

    // TODO add modelrenderable into parameters when NPC models are done
    fun getNPCObject(level: Int, renderable: ModelRenderable) : NPC {
        // hpGrowModifier and apGrowModifier defined in CombatControllable (how much AP scales per level, how much HP scales per level)
        val hpMultiplier = hpGrowModifier.pow(level.toDouble())
        val apMultiplier = apGrowModifier.pow(level.toDouble())

        return when (this) {
            MELEE -> NPC(MELEE_BASE_AP * apMultiplier, "Hurdur", MELEE_BASE_HP * hpMultiplier, renderable, type = this)
            RANGED -> NPC(RANGED_BASE_AP * apMultiplier, "Hurdur", RANGED_BASE_HP * hpMultiplier, renderable, type = this)
            SUPPORT -> NPC(SUPPORT_BASE_AP * apMultiplier, "Hurdur", SUPPORT_BASE_HP * hpMultiplier, renderable,  type = this)
            BOSSLVL10 -> NPC(BOSSLVL10_BASE_AP, "Hurdur", BOSSLVL10_BASE_HP, renderable, type = this)
            BOSSLVL20 -> NPC(BOSSLVL20_BASE_AP, "Hurdur", BOSSLVL20_BASE_HP, renderable, type = this)
            BOSSLVL30 -> NPC(BOSSLVL30_BASE_AP, "Hurdur", BOSSLVL30_BASE_HP, renderable, type = this)
        }
    }

    fun modelUri() : Uri {
        return when (this) {
            MELEE -> Uri.parse("duck.sfb")
            // TODO more models
            else -> Uri.parse("duck.sfb")
        }
    }
}