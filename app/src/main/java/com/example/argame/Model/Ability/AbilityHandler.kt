package com.example.argame.Model.Ability

import android.content.Context
import android.os.Handler
import com.example.argame.Model.ABILITY_PROJECTILE_SPEED
import com.example.argame.Model.CombatControllable.CombatControllable
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.ArFragment

/***
 *  Handles building the projectile and the cast animations,
 *  and using an ability given the right input.
 */
class AbilityHandler(private val context: Context, private val fragment: ArFragment) : Ability.AbilityCallbackListener {

    fun useAbility(caster: CombatControllable, target: CombatControllable,
                   ability: Ability, startPos: Vector3, endPos: Vector3, callback: () -> Unit) {
        if (caster.getStatus().isAlive && target.getStatus().isAlive) {
            val projAnimData = ProjectileAnimationData(startPos, endPos, context, fragment, ability.uri()
            )
            caster.useAbility(ability, target, projAnimData) { callback() }
            val animator = caster.getModelAnimator()
            if (animator != null) {
                // TODO(?) A completion callback parameter to end the animation, or make some completion animation.
                animator.duration = ABILITY_PROJECTILE_SPEED
                animator.start()
                Handler().postDelayed({animator.end()}, ABILITY_PROJECTILE_SPEED)
            }
        }
    }

    private fun cancelAnimator(cc: CombatControllable) {
        val animator = cc.getModelAnimator()
        if (animator != null) {
            if (animator.isRunning) {
                animator.cancel()
            }
        }
    }

    override fun onAbilityCast(caster: CombatControllable, target: CombatControllable, ability: Ability) {
        // cancel any running animation
        cancelAnimator(caster)
        // new cast animation
        val castAnimData = caster.model?.getAnimationData(ability.getCastAnimationString())
        caster.setModelAnimator(ModelAnimator(castAnimData, caster.model))
    }

    override fun onAbilityHit(caster: CombatControllable, target: CombatControllable, ability: Ability) {
        cancelAnimator(caster)
    }
}