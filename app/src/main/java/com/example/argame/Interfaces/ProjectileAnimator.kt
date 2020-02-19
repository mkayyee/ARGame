package com.example.argame.Interfaces

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.util.Log.wtf
import com.example.argame.Model.ABILITY_PROJECTILE_SPEED
import com.example.argame.Model.AnimationAPI
import com.example.argame.Model.ProjectileAnimationData
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import kotlinx.coroutines.delay

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

interface ProjectileAnimator {

    fun instantiateProjectile(projAnimData: ProjectileAnimationData, cb: () -> Unit) {
        val uri: Uri? = projAnimData.modelUri
        // Get 3d model of the projectile if not null
        if (projAnimData.modelUri != null) {
            val renderableFuture = ModelRenderable.builder()
                .setSource(projAnimData.context, uri)
                .build()
            renderableFuture.thenAccept {
                val animData = it.getAnimationData("Sphere|SphereAction.001") // 0 should be the index where cast animation is
                if (animData != null) { // Null check. By default the ability should have at least 1 animation but never know
                    instantiateNodeInScene(projAnimData, it, cb)
                    val animator = ModelAnimator(animData, it)
                    animator.start()
                } else {
                    instantiateDefaultProjectile(projAnimData, cb)
                    wtf("PAERROR", "Projectile animation data was null")
                }
            }
        } else { // some random test sphere to be fired at a target if 3d model is null
            instantiateDefaultProjectile(projAnimData, cb)
        }
    }

    private fun instantiateDefaultProjectile(projAnimData: ProjectileAnimationData, cb: () -> Unit) {
        MaterialFactory
            .makeOpaqueWithColor(
                projAnimData.context,
                com.google.ar.sceneform.rendering.Color(Color.BLUE))
            .thenAccept {
                val renderable: ModelRenderable = ShapeFactory.makeSphere(0.02f, Vector3(0f, 0.1f, 0f), it)
                instantiateNodeInScene(projAnimData, renderable, cb)
            }
    }

    private fun instantiateNodeInScene(projAnimData: ProjectileAnimationData, renderable: ModelRenderable, cb: () -> Unit) {
        val node = TransformableNode(projAnimData.fragment.transformationSystem)
        val scene = projAnimData.fragment.arSceneView.scene
        node.renderable = renderable
        scene.addChild(node)
        animateProjectile(projAnimData, node) {
            Handler().postDelayed({
                cb()
                deleteProjectile(node, projAnimData.fragment.arSceneView.scene)
            }, ABILITY_PROJECTILE_SPEED)
        }
    }

    fun animateProjectile(projectileAnimationData: ProjectileAnimationData, node: TransformableNode, cb: () -> Unit) {
        val startPos = projectileAnimationData.startPos
        val endPos = projectileAnimationData.endPos
        // TODO some logic that separates different types of abilities.
        //  e.g if (projectile.type == ray) *cast something other than AnimationAPI.fireProjectile*
        AnimationAPI.fireProjectile(node, startPos, endPos) {
            // in the makings of a callback hell
            cb()
        }
    }

    // TODO
    // needs a reference to fragment.arSceneView.scene
    fun deleteProjectile(node: TransformableNode, scene: Scene) {
        scene.removeChild(node)
        node.setParent(null)
    }
}