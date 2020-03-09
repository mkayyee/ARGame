package com.example.argame.Model

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.os.CountDownTimer
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.example.argame.Activities.GameActivityPlayground
import com.example.argame.Model.Ability.ProjectileAnimationData
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.TransformableNode
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.sql.Time


/***
 *  This is a helper singleton, that contains static methods
 *  for general animations used in different context's,
 *  so we don't have to re-write everything.
 */

// TODO make enum or something for different abilities
const val ABILITY_PROJECTILE_SPEED: Long = 1000

object AnimationAPI {

    // Potential crash: Node gets removed when trying to update objectValues
    fun fireProjectile(
        model: TransformableNode, startPos: Vector3, endPos: Vector3, targetNode: Node, callback: () -> Unit
    ) {
        val objectAnimation = ObjectAnimator()
        val rotation = Quaternion.lookRotation(startPos, endPos)
        model.localRotation = rotation
        objectAnimation.setAutoCancel(true)
        objectAnimation.target = model
        objectAnimation.setObjectValues(startPos, targetNode.worldPosition)
        objectAnimation.setPropertyName("worldPosition")
        objectAnimation.setEvaluator(Vector3Evaluator())
        objectAnimation.interpolator = LinearInterpolator()
        objectAnimation.duration = (ABILITY_PROJECTILE_SPEED.toDouble() * 2).toLong()
        objectAnimation.addUpdateListener {
            //rotation = Quaternion.lookRotation(it.animatedValue as Vector3, targetNode.worldPosition)
            //model.localRotation = rotation
            objectAnimation.setObjectValues(it.animatedValue, targetNode.worldPosition)
            objectAnimation.setEvaluator(Vector3Evaluator())
            objectAnimation.interpolator = LinearInterpolator()
            objectAnimation.setPropertyName("worldPosition")
        }
        objectAnimation.start()
        callback()
    }

    // Reference:
    // https://stackoverflow.com/questions/53371583/draw-line-between-location-markers-in-arcore
    fun stretchModel(startPos: Vector3, endPos: Vector3, node: TransformableNode, projAnimData: ProjectileAnimationData) {
        var rotation = calculateNewRotation(startPos, endPos)
        var difference = Vector3.subtract(startPos, endPos)
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

        val timer = object: CountDownTimer(ABILITY_PROJECTILE_SPEED, 100) {
            override fun onFinish() {
                Log.d("ANIMATORS", "Timer stopped at ${Time(System.nanoTime())}")
            }
            override fun onTick(millisUntilFinished: Long) {
                difference = Vector3.subtract(node.worldPosition, projAnimData.targetNode.worldPosition)
                Log.d("ANIMATORS", "ticked at ${Time(System.nanoTime())}")
                rotation = calculateNewRotation(node.worldPosition, projAnimData.targetNode.worldPosition)
                node.localScale = Vector3(0.03f, 0.03f, difference.length() * 0.9f)
                node.worldPosition = Vector3.add(node.worldPosition, projAnimData.targetNode.worldPosition).scaled(0.5f)
                node.worldRotation = rotation
            }
        }
        timer.start()
    }

    fun calculateNewRotation(startPos: Vector3, endPos: Vector3) : Quaternion {
        val difference = Vector3.subtract(startPos, endPos)
        val directionFromTopToBottom = difference.normalized()
        val lookRotation = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up())
        return Quaternion(0f, lookRotation.x, 0f, lookRotation.z)
    }


}