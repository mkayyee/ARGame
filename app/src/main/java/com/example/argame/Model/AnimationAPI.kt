package com.example.argame.Model

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.view.animation.LinearInterpolator
import com.example.argame.Activities.GameActivityPlayground
import com.example.argame.Model.Ability.ProjectileAnimationData
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.TransformableNode


/***
 *  This is a helper singleton, that contains static methods
 *  for general animations used in different context's,
 *  so we don't have to re-write everything.
 */

// TODO make enum or something for different abilities
const val ABILITY_PROJECTILE_SPEED: Long = 600

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

//    MaterialFactory.makeTransparentWithColor(context, com.google.ar.sceneform.rendering.Color(Color.BLUE))
//    .thenAccept { material: Material? ->
//        lineRenderable = ShapeFactory.makeCube(
//            Vector3(.01f, .01f, difference.length() * 0.9f),
//            Vector3.zero(), material
//        )
//        lineRenderable.isShadowCaster = false
//        node.renderable = lineRenderable
//        node.localPosition = Vector3.add(startPos, endPos).scaled(.5f)
//    }

    // Reference:
    // https://stackoverflow.com/questions/53371583/draw-line-between-location-markers-in-arcore
    fun stretchModel(startPos: Vector3, endPos: Vector3, node: TransformableNode, projAnimData: ProjectileAnimationData) {
        val rotation = calculateNewRotation(startPos, endPos)
        val difference = Vector3.subtract(startPos, endPos)
        node.rotationController.isEnabled = false
        node.scaleController.isEnabled = false
        node.rotationController.isEnabled = false
        node.worldPosition = Vector3.add(startPos, endPos).scaled(0.5f)
        node.worldRotation = rotation
        node.localScale = Vector3(0.03f, 0.03f, difference.length() * 0.9f)
        node.localPosition = Vector3.add(startPos, endPos).scaled(.5f)
        node.setParent(projAnimData.fragment.arSceneView.scene)
        node.renderable = projAnimData.abilityRenderable
        node.renderable?.isShadowCaster = false
    }

    fun calculateNewRotation(startPos: Vector3, endPos: Vector3) : Quaternion {
        val difference = Vector3.subtract(startPos, endPos)
        val directionFromTopToBottom = difference.normalized()
        return Quaternion.lookRotation(directionFromTopToBottom, Vector3.up())
    }


}