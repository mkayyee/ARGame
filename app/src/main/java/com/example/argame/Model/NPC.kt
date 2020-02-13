package com.example.argame.Model

import com.example.argame.Interfaces.AbilityUser
import com.google.ar.core.Pose
import com.google.ar.sceneform.rendering.ModelRenderable

class NPC(ap: Double, name: String, startHealth: Double, model: ModelRenderable? = null)
    : AbilityUser, CombatControllable(startHealth, name, ap, model){

    fun useAbility(ability: Ability, target: CombatControllable, projectileData: ProjectileAnimationData) {
        super.useAbility(this, target, ability, projectileData)
    }

    override fun animateProjectile(startPose: Pose, endPose: Pose) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}