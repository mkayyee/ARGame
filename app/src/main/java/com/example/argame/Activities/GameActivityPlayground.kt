package com.example.argame.Activities

import android.animation.ObjectAnimator
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import com.example.argame.Fragments.CustomArFragment
import com.example.argame.Fragments.MenuFragmentController
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.Interfaces.PreferenceHelper
import com.example.argame.Interfaces.PreferenceHelper.customPreference
import com.example.argame.Interfaces.PreferenceHelper.defaultPreference
import com.example.argame.Interfaces.PreferenceHelper.setGson
import com.example.argame.Model.*
import com.example.argame.R
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.TransformableNode
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_game_playground.*
import kotlinx.android.synthetic.main.healthbar.view.*

class GameActivityPlayground : AppCompatActivity(), FragmentCallbackListener {

    private val menuFragController = MenuFragmentController()
    private lateinit var fragment: CustomArFragment
    private lateinit var duckUri: Uri
    private lateinit var tposeUri: Uri
    private var renderedDuck: ModelRenderable? = null
    private var renderedTpose: ModelRenderable? = null
    private var protoTargetNode: TransformableNode? = null
    private var anchorList = ArrayList<AnchorNode>()

    // MARK: Testing-abilities-related stuff
    private var playerTarget: PlayerTargetData? = null
    private var hpRenderableDuck: ViewRenderable? = null
    private var hpRenderableTpose: ViewRenderable? = null
    private var duckNPC = NPC(1.0, "duck", 5000.0)
    private var tposeNPC = NPC(1.0, "duck 2", 5000.0)
    private var player = Player(5.0, "player", 5000.0)
    var ducksInScene = false

    // GSON and SHAREDPREFERENCE

    private lateinit var saver: SharedPreferences
    private val gson = Gson()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_playground)
        fragment = supportFragmentManager.findFragmentById(R.id.playground_sceneform_fragment) as CustomArFragment
        saver = defaultPreference(this)
        initButtons()
        prepareModels()
        // MARK: Testing-abilities-related stuff
        initHPRenderables()
        playground_targetTxt.text = "No target"
    }

    override fun onPause() {
        super.onPause()
        val positionList = ArrayList<Vector3>()
        anchorList.forEach {
            positionList.add(it.worldPosition)
        }
        val saveGame = GameVault(positionList)
        val saveToGson = gson.toJson(saveGame)

        saver.setGson = saveToGson

    }

    override fun onResume() {
        super.onResume()
        if (saver.setGson != null) {
            Log.d("JATKUU", saver.setGson)
            // TODO: Put everything back to their places. Redo spawn functions to work with onPause and onResume
        }
    }

    override fun onButtonPressed(btn: Button) {
        // The callback's are forwarded to MenuFragmentController,
        // that handles all the logic for these events
        menuFragController.onButtonPressed(btn)
    }

    private fun prepareModels() {

        duckUri = Uri.parse("duck.sfb")
        tposeUri = Uri.parse("duck.sfb")
        val renderableFuture = ModelRenderable.builder()
            .setSource(this, duckUri)
            .build()
        renderableFuture.thenAccept{renderedDuck = it}
        val renderableFuture2 = ModelRenderable.builder()
            .setSource(this, tposeUri)
            .build()
        renderableFuture2.thenAccept{renderedTpose = it}
    }

    private fun initButtons() {

        val menuBtn = findViewById<Button>(R.id.playground_menuBtn)
        menuBtn.setOnClickListener {
            callMenuFragment()
        }
        val spawnBtn = findViewById<Button>(R.id.playground_spawnBtn)
        spawnBtn.setOnClickListener {
            spawnObjects()
        }
        // MARK: Testing-abilities-related stuff
        playground_attackDuckBtn.setOnClickListener {
            attackTarget()
        }
        val destroyBtn = findViewById<Button>(R.id.playground_destroyBtn)
        destroyBtn.setOnClickListener {
            clearModels()
        }
    }

    private fun callMenuFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.playground_main_menu_container, MenuFragmentController())
            .addToBackStack(null)
            .commit()

        /*val resume = this.findViewById<Button>(R.id.button_resume_game)
        if (resume != null) {
            resume.isEnabled = true
        }*/
    }

    // MARK: Testing-abilities-related stuff
    private fun initHPRenderables() {
        val renderableFuture = ViewRenderable.builder()
            .setView(this, R.layout.healthbar)
            .build()
        renderableFuture.thenAccept { hpRenderableDuck = it }
        hpRenderableDuck?.view?.textView_healthbar?.text = duckNPC.getStatus().currentHealth.toString()

        val renderableFuture2 = ViewRenderable.builder()
            .setView(this, R.layout.healthbar)
            .build()
        renderableFuture2.thenAccept { hpRenderableTpose = it }
        hpRenderableTpose?.view?.textView_healthbar?.text = tposeNPC.getStatus().currentHealth.toString()
    }

    // MARK: Testing-abilities-related stuff
    private fun attackTarget() {
        // disable attack button for the animation duration
        if (playerTarget != null) {
            playground_attackDuckBtn.isEnabled = false
            player.dealDamage(5.0, playerTarget!!.model)
            val ability = Ability("Sphere", 100.0, "I have no cast animation")
            val animData = ProjectileAnimationData(
                Vector3(0.5f, 0f, 0f),
                playerTarget!!.node.worldPosition,
                this,
                fragment
            )
            player.useAbility(ability, playerTarget!!.model, animData) {
                if (playerTarget!!.healthBar != null) {
                    updateHPBar(playerTarget!!.healthBar, playerTarget!!.model)
                }
                //beamTarget(playerTarget!!)

                // enable attack button after in callback
                playground_attackDuckBtn.isEnabled = true
                if (!playerTarget!!.model.getStatus().isAlive) {
                    playerTarget!!.node.localRotation = Quaternion(0f, 0f, 1f, 0f)
                }
            }
        } else {
            Toast.makeText(this, "You don't have a target", Toast.LENGTH_SHORT).show()
        }
    }

    private fun beamTarget(playerTarget: PlayerTargetData) {
        anchorList.forEach() {
            // TODO: Calculate a good offset for worldposition difference. 0.1f is just a placeholder.
            if (it.worldPosition.x - playerTarget.node.worldPosition.x < 0.1f && it.worldPosition.y - playerTarget.node.worldPosition.y < 0.1f) {
                // TODO: Needs a collection from where to look for other targets
            }
        }


    }

    private fun clearPlayerTarget() {
        playerTarget = null
    }

    // MARK: Testing-abilities-related stuff
    private fun createHPBar(anchor: AnchorNode, renderable: ViewRenderable?) {
        val hpNode = Node()
        hpNode.setParent(anchor)
        hpNode.renderable = renderable
        hpNode.localPosition = Vector3(0f,0.5f,0f)
        hpNode.localScale = Vector3(0.5f, 0.5f, 0.5f)
    }

    private fun destroyTpose(anchor: AnchorNode, target: TransformableNode) {
        if (anchorList.isNotEmpty()) { // voi myös pitää listaa vaikka specifeistä anchornodeista scenessä
            Log.d("DESTROY", "Function called with node: " + target.toString())
            removeAnchorNode(anchor)
        }
    }

    private fun clearModels() {
        for (anchor in anchorList) {
            removeAnchorNode(anchor)
        }
        anchorList.clear()
        ducksInScene = false
    }

    private fun moveToTarget(model: TransformableNode, targetModel: TransformableNode) {
        val objectAnimation = ObjectAnimator()
        objectAnimation.setAutoCancel(true)
        objectAnimation.setTarget(model)
        objectAnimation.setObjectValues(model.getWorldPosition(), targetModel.getWorldPosition())
        objectAnimation.setPropertyName("worldPosition")
        objectAnimation.setEvaluator(Vector3Evaluator())
        objectAnimation.setInterpolator(LinearInterpolator())
        objectAnimation.setDuration(5000)
        objectAnimation.start()
    }

    private fun spawnObjects() {
        if (!ducksInScene) {
            duckNPC = NPC(1.0, "duck", 5000.0)
            tposeNPC = NPC(1.0, "duck 2", 5000.0)
            // For prototyping only
            val frame = fragment.arSceneView.arFrame
            val pt = getScreenCenter()
            val hits: List<HitResult>
            if (frame != null && renderedDuck != null) {
                hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
                for (hit in hits) {
                    val trackable = hit.trackable
                    if (trackable is Plane) {
                        val duckAnchor = hit!!.createAnchor()
                        val duckAnchorNode = AnchorNode(duckAnchor)
                        anchorList.add(duckAnchorNode)
                        val tposeAnchor = (frame.hitTest(
                            (pt.x.toFloat() - 420.0f),
                            (pt.y.toFloat() + 420.0f)
                        ))[0].createAnchor()
                        val tposeAnchorNode = AnchorNode(tposeAnchor)
                        anchorList.add(tposeAnchorNode)
                        duckAnchorNode.setParent(fragment.arSceneView.scene)
                        tposeAnchorNode.setParent(fragment.arSceneView.scene)
                        val duckNode = TransformableNode(fragment.transformationSystem)
                        val tposeNode = TransformableNode(fragment.transformationSystem)
                        duckNode.scaleController.isEnabled = false
                        tposeNode.scaleController.isEnabled = false
                        duckNode.localScale = Vector3(0.1f, 0.1f, 0.1f)
                        tposeNode.localScale = Vector3(0.1f, 0.1f, 0.1f)
                        duckNode.setParent(duckAnchorNode)
                        tposeNode.setParent(tposeAnchorNode)

                        duckNode.renderable = renderedDuck
                        //duckNode.setLookDirection(tposeNode.worldPosition)
                        //tposeNode.setLookDirection(duckNode.worldPosition)
                        duckNode.select()
                        tposeNode.renderable = renderedTpose
                        protoTargetNode = tposeNode

                        // MARK: Testing-abilities-related stuff
                        createHPBar(duckAnchorNode, hpRenderableDuck)
                        createHPBar(tposeAnchorNode, hpRenderableTpose)

                        // tap listeners for ability usage
                        // TODO -> move hpRenderable out of this function. Could be stored inside every model..?
                        createOnTapListenerForAttack(
                            // TODO -> create separate functions for acquiring player target, and for setting up tap listeners
                            tposeNode,
                            tposeAnchorNode,
                            duckAnchorNode,
                            tposeNPC,
                            hpRenderableDuck,
                            duckNPC,
                            hpRenderableTpose
                        )
                        createOnTapListenerForAttack(
                            duckNode,
                            duckAnchorNode,
                            tposeAnchorNode,
                            duckNPC,
                            hpRenderableTpose,
                            tposeNPC,
                            hpRenderableDuck
                        )
                        ducksInScene = true
                        break
                    }
                }
            }
        }
    }

    private fun updateHPBar(tv: TextView?, model: CombatControllable) {
        tv?.text = model.getStatus().currentHealth.toString()
    }

    private fun createOnTapListenerForAttack(
        node: Node,
        casterAnchor: AnchorNode,
        targetAnchor: AnchorNode,
        casterModel: CombatControllable,
        hpRend: ViewRenderable?,
        target: CombatControllable,
        playerTargetHpRend: ViewRenderable?) {

            node.setOnTapListener {_, _ ->
//                val animationData = ProjectileAnimationData(
//                    casterAnchor.worldPosition,
//                    targetAnchor.worldPosition,
//                this,
//                fragment)
//                casterModel.useAbility(
//                Ability("sphere", 20.0, "XD"),
//                target,
//                animationData) {
//                    updateHPBar(hpRend?.view?.textView_healthbar, target)
//                }
                // changing player target to the tapped model
                if (playerTarget != null) {
                    if (playerTarget!!.healthBar != null) {
                        playerTarget!!.healthBar!!.text = ""
                    }
                }
                playerTarget = PlayerTargetData(node, casterModel as NPC, playerTargetHpRend?.view?.textView_healthbar)
                playground_targetTxt.text = "Target: ${target.name}"
                playerTargetHpRend?.view?.textView_healthbar?.text = casterModel.getStatus().currentHealth.toString()
        }
    }

    private fun getScreenCenter(): android.graphics.Point {
        val vw = findViewById<View>(android.R.id.content)
        return android.graphics.Point(vw.width / 2, vw.height / 2)
    }
    private fun removeAnchorNode(nodeToremove: AnchorNode?) { //Remove an anchor node
        if (nodeToremove != null) {
            fragment.arSceneView.scene.removeChild(nodeToremove)
            nodeToremove.anchor!!.detach()
            nodeToremove.setParent(null)
        }
    }
}