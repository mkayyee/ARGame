package com.example.argame.Model

import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator
import com.google.ar.sceneform.ux.TransformableNode

/***
 *  This is a helper singleton, that contains static methods
 *  for general animations used in different context's,
 *  so we don't have to re-write everything.
 */

// TODO make enum or something for different abilities
const val ABILITY_PROJECTILE_SPEED: Long = 2000

object AnimationAPI {

    fun fireProjectile(model: TransformableNode, startPos: Vector3, endPos: Vector3, callback: () -> Unit) {
        val objectAnimation = ObjectAnimator()
        objectAnimation.setAutoCancel(true)
        objectAnimation.setTarget(model)
        objectAnimation.setObjectValues(startPos, endPos)
        objectAnimation.setPropertyName("worldPosition")
        objectAnimation.setEvaluator(Vector3Evaluator())
        objectAnimation.setInterpolator(LinearInterpolator())
        objectAnimation.setDuration(ABILITY_PROJECTILE_SPEED)
        objectAnimation.start()
        // currently executed immediately --
        // could implement some logic to see if it reached the target
        callback()
    }
}