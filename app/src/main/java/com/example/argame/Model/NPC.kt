package com.example.argame.Model

import android.content.Context
import com.example.argame.Interfaces.AbilityUser
import com.google.ar.sceneform.rendering.ModelRenderable

class NPC(ap: Double, name: String, startHealth: Double, model: ModelRenderable? = null, type: NPCType,
          private var id: Int
)
    : CombatControllable(startHealth, name, ap, model) {

    fun getID() : Int{
        return id
    }
}