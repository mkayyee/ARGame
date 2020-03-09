package com.example.argame.Interfaces

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.util.Log.wtf
import com.example.argame.Model.ABILITY_PROJECTILE_SPEED
import com.example.argame.Model.Ability.Ability
import com.example.argame.Model.AnimationAPI
import com.example.argame.Model.Ability.ProjectileAnimationData
import com.example.argame.R
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.TransformableNode

/***
 *  Instantiates a projectile from a 3D model and checks that it has an animation.
 *  If either: the 3D model is null, or the animation is null -> calls for another
 *  method: instantiateDefaultProjectile() with its parameters, which instantiates
 *  a default 3d model (a sphere) in the scene. If there is no need to call for the
 *  default projectile, then starts the animation attached to the provided 3D model.
 *
 *  Both of these methods call for another method: instantiateNodeInScene(),
 *  which instantiates the node in the AR scene and attaches the renderable to it and
 *  calls animateProjectile(), which sends the projectile toward its target in the scene.
 *  It also sets a timer that removes the model from the scene after the same duration
 *  it takes for the model to reach its target, and sends a callback, which is used to
 *  track the health of the target after the ability is hit.
 *
 */

// The delay before instantiating the projectile
const val CAST_TIME: Long = 350

interface ProjectileAnimator {

    fun instantiateProjectile(projAnimData: ProjectileAnimationData, ability: Ability, cb: () -> Unit) {
        Handler().postDelayed({
            if (ability == Ability.BEAM) {
                instantiateNodeInScene(projAnimData, cb, true)
            } else {
                instantiateNodeInScene(projAnimData, cb)
            }
        },CAST_TIME)
    }

    private fun instantiateNodeInScene(projAnimData: ProjectileAnimationData, cb: () -> Unit, isBeam: Boolean = false) {
        val animationNode = TransformableNode(projAnimData.fragment.transformationSystem)
        val scene = projAnimData.fragment.arSceneView.scene
        scene.addChild(animationNode)
        if (!isBeam) {
            animationNode.renderable = projAnimData.gifRenderable
            animationNode.scaleController.isEnabled = false
            animationNode.localScale = Vector3(0.65f, 0.65f, 0.65f)
            animationNode.renderable?.isShadowCaster = false
            animateProjectile(projAnimData, animationNode) {
                Handler().postDelayed({
                    cb()
                    deleteProjectile(animationNode, projAnimData.fragment.arSceneView.scene)
                }, ABILITY_PROJECTILE_SPEED)
            }
        } else {
            // Beam
            val casterPos = projAnimData.startPos
            val targetPos = projAnimData.endPos
            AnimationAPI.stretchModel(casterPos, targetPos, animationNode, projAnimData)
            Handler().postDelayed({
                cb()
                deleteProjectile(animationNode, projAnimData.fragment.arSceneView.scene)
            }, ABILITY_PROJECTILE_SPEED)
        }
    }

    fun animateProjectile(projectileAnimationData: ProjectileAnimationData, node: TransformableNode, cb: () -> Unit) {
        val startPos = projectileAnimationData.startPos
        val endPos = projectileAnimationData.endPos
        // TODO some logic that separates different types of abilities.
        //  e.g if (projectile.type == ray) *cast something other than AnimationAPI.fireProjectile*
        AnimationAPI.fireProjectile(node, startPos, endPos, projectileAnimationData.targetNode) {
            cb()
        }
    }

    fun deleteProjectile(node: TransformableNode, scene: Scene) {
        scene.removeChild(node)
        node.setParent(null)
    }
}