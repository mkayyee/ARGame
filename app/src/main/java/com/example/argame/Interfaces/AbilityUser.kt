package com.example.argame.Interfaces

import android.util.Log
import com.example.argame.Model.Ability
import com.example.argame.Model.CombatControllable
import com.example.argame.Model.ProjectileAnimationData
import com.google.ar.core.Pose
import com.google.ar.sceneform.animation.ModelAnimator

/***
 *  An Interface for casting abilities, that both, the NPC and the Player implement.
 */
interface AbilityUser {

    /***
     *  useAbility() Parameters explained:
     *      -caster & target: The casting GameControllable and the receiving GameControllable
     *      -ability: Holds the data of the ability -> how much damage it does and its name.
     *      -projectileData: Holds the data for the start and end poses, which are used to calculate
     *                      the animated projectiles trajectory.
     */
    fun useAbility(caster: CombatControllable, target: CombatControllable,
                   ability: Ability, projectileData: ProjectileAnimationData) {
        val animator: ModelAnimator?
        val animationData = caster.model?.getAnimationData(ability.animationName)
        if (target.getStatus().isAlive) {
            if (animationData != null) {
                // TODO(?) A completion callback parameter to end the animation, or make some completion animation.
                // The cast animation - should be included in caster's model's ModelRenderable
                animator = ModelAnimator(animationData, caster.model)
                animator.start()
                // The projectile animation - some animation from the caster's pose all the way to the target's pose
                // TODO implement and trigger a projectile animation
                val animationStartPose= projectileData.startPose
                val animationEndPose = projectileData.endPose
                // currently does nothing
                caster.animateProjectile(animationStartPose, animationEndPose)
            }
            Log.d(
                "COMBAT", "${caster.getStatus().name} used" +
                        " ability: ${ability.name} on ${target.getStatus().name} " +
                        "for ${caster.getStatus().attackPower * ability.damage} damage."
            )
            caster.dealDamage(ability.damage, target)
        }
    }
}