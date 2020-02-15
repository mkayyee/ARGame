package com.example.argame.Interfaces

import android.content.Context
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
                   ability: Ability, projectileData: ProjectileAnimationData, context: Context, cb: () -> Unit) {
        val animator: ModelAnimator?
        val animationData = caster.model?.getAnimationData(ability.animationName)
        if (target.getStatus().isAlive) {
            if (animationData != null) {
                // TODO(?) A completion callback parameter to end the animation, or make some completion animation.
                // The cast animation - should be included in caster's model's ModelRenderable
                animator = ModelAnimator(animationData, caster.model)
                animator.start()
            }
            // The projectile animation - some animation from the caster's pose all the way to the target's pose
            // TODO implement and trigger a projectile animation
            // currently does nothing
            caster.instantiateProjectile(projectileData) {
                // retrieved callback that the projectile was fired, so probably safe to deal damage
                caster.dealDamage(ability.damage, target)
                // 1 last callback xD
                cb()
                Log.d(
                    "COMBAT", "${caster.getStatus().name} used" +
                            " ability: ${ability.name} on ${target.getStatus().name} " +
                            "for ${caster.getStatus().attackPower * ability.damage} damage."
                )
            }
        }
    }
}