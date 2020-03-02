package com.example.argame.Model.NPC

import android.content.Context
import com.example.argame.Model.CombatControllable.CombatControllable
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable

class NPC(ap: Double,
          name: String,
          startHealth: Double,
          model: ModelRenderable? = null,
          private var type: NPCType,
          private var id: Int,
          context: Context,
          private var hpBar: ViewRenderable? = null)
    : CombatControllable(startHealth, name, ap, model, context) {

    fun getID() : Int{
        return id
    }

    fun getType() : NPCType {
        return type
    }

    fun setHPRenderable(renderable: ViewRenderable) {
        hpBar = renderable;
    }

    fun getHPBar() : ViewRenderable? {
        return hpBar
    }
}