package com.example.argame.Model

import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator
import com.google.ar.sceneform.ux.TransformableNode


/***
 *  This is a helper singleton, that contains static methods
 *  for general animations used in different context's,
 *  so we don't have to re-write everything.
 */

// TODO make enum or something for different abilities
const val ABILITY_PROJECTILE_SPEED: Long = 1500

object AnimationAPI {

    fun fireProjectile(model: TransformableNode, startPos: Vector3, endPos: Vector3, callback: () -> Unit) {
        val objectAnimation = ObjectAnimator()
        val rotation = Quaternion.lookRotation(startPos, endPos)
        model.localRotation = rotation
        objectAnimation.setAutoCancel(true)
        objectAnimation.target = model
        objectAnimation.setObjectValues(startPos, endPos)
        objectAnimation.setPropertyName("worldPosition")
        objectAnimation.setEvaluator(Vector3Evaluator())
        objectAnimation.interpolator = LinearInterpolator()
        objectAnimation.duration = ABILITY_PROJECTILE_SPEED
        objectAnimation.start()
        // currently executed immediately --
        // could implement some logic to see if it reached the target
        callback()
    }

    // Reference:
    // https://stackoverflow.com/questions/53371583/draw-line-between-location-markers-in-arcore
    fun stretchModel(startPos: Vector3, endPos: Vector3, node: TransformableNode) {
        val rotation = calculateNewRotation(startPos, endPos)
        node.worldPosition = Vector3.add(startPos, endPos).scaled(0.5f)
        node.worldRotation = rotation
    }

    fun calculateNewRotation(startPos: Vector3, endPos: Vector3) : Quaternion {
        val difference = Vector3.subtract(startPos, endPos)
        val directionFromTopToBottom = difference.normalized()
        return Quaternion.lookRotation(directionFromTopToBottom, Vector3.up())
    }


}