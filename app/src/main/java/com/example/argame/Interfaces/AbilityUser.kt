package com.example.argame.Interfaces

import android.os.Handler
import com.example.argame.Model.ABILITY_PROJECTILE_SPEED
import com.example.argame.Model.Ability.Ability
import com.example.argame.Model.CombatControllable.CombatControllable
import com.example.argame.Model.Ability.ProjectileAnimationData
import com.google.ar.sceneform.rendering.ModelRenderable

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
            caster.instantiateProjectile(projectileData, ability) {
                // retrieved callback that the projectile was fired, so probably safe to deal damage
                caster.dealDamage(ability.getDamage(casterStatus), target)
                animator?.end()
                cb()
            }

        } else {
            cb()
        }
    }
}