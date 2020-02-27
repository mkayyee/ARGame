package com.example.argame.Interfaces

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.util.Log
import com.example.argame.Model.ABILITY_PROJECTILE_SPEED
import com.example.argame.Model.Ability
import com.example.argame.Model.CombatControllable
import com.example.argame.Model.ProjectileAnimationData
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
                   ability: Ability, projectileData: ProjectileAnimationData, cb: () -> Unit) {
        val casterStatus = caster.getStatus()
        val targetStatus = target.getStatus()

        if (targetStatus.isAlive && casterStatus.isAlive) {
            val animator = caster.getModelAnimator()
            if (animator != null) {
                // TODO(?) A completion callback parameter to end the animation, or make some completion animation.
                animator.duration = ABILITY_PROJECTILE_SPEED
                animator.start()
                Handler().postDelayed({animator.end()}, ABILITY_PROJECTILE_SPEED)
            }
            caster.instantiateProjectile(projectileData, ability) {
                // retrieved callback that the projectile was fired, so probably safe to deal damage
                caster.dealDamage(ability.getDamage(casterStatus), target)
                cb()
            }
        } else {
            cb()
        }
    }
}