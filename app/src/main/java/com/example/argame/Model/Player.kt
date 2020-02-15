package com.example.argame.Model

import android.content.Context
import com.example.argame.Interfaces.AbilityUser
import com.google.ar.sceneform.rendering.ModelRenderable

class Player(ap: Double, name: String, startHealth: Double, model: ModelRenderable? = null)
    : AbilityUser,  CombatControllable(startHealth, name, ap, model){

    fun useAbility(
        ability: Ability, target: CombatControllable, projectileData: ProjectileAnimationData, context: Context, callback: () -> Unit) {
        super.useAbility(this, target, ability, projectileData, context) {
            callback()
        }
    }
}