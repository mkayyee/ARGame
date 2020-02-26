package com.example.argame.Model

import android.content.Context
import com.example.argame.Interfaces.AbilityUser
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable

class Player(
        ap: Double,
        name: String,
        startHealth: Double,
        model: ModelRenderable? = null,
        context: Context,
        private var hpBar: ViewRenderable? = null)
    : CombatControllable(startHealth, name, ap, model, context) {

    fun setHPRenderable(renderable: ViewRenderable) {
        hpBar = renderable;
    }

    fun getHPBar() : ViewRenderable? {
        return hpBar
    }

}