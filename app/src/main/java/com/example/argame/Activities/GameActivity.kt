package com.example.argame.Activities

import android.animation.ObjectAnimator
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.argame.Fragments.CustomArFragment
import com.example.argame.Fragments.MenuFragmentController
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.Model.CombatControllable
import com.example.argame.Model.NPC
import com.example.argame.Model.NPCType
import com.example.argame.Model.Player
import com.example.argame.R
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_game.*
import kotlinx.android.synthetic.main.healthbar.view.*


class GameActivity : AppCompatActivity(), FragmentCallbackListener {

    private val menuFragController = MenuFragmentController()
    private lateinit var fragment: CustomArFragment
    private lateinit var duckUri: Uri
    private lateinit var tposeUri: Uri
    private var renderedDuck: ModelRenderable? = null
    private var renderedTpose: ModelRenderable? = null
    private var protoTargetNode: TransformableNode? = null
    private var anchorList = ArrayList<AnchorNode>()

    // MARK: Testing-abilities-related stuff
    private var hpRenderableDuck: ViewRenderable? = null
    private var hpRenderableTpose: ViewRenderable? = null
    private var duckNPC = NPC(1.0, "duck", 5000.0, type = NPCType.MELEE)
    private var tposeNPC = NPC(1.0, "tposer", 5000.0, type = NPCType.MELEE)
    private var player = Player(5.0, "player", 5000.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        fragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as CustomArFragment
        initButtons()
        prepareModels()
        // MARK: Testing-abilities-related stuff
        initHPRenderables()
        val healthTxt = findViewById<TextView>(R.id.healthTxt)
        healthTxt.text = "Health ${player.getStatus().currentHealth}"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

    }

    override fun onButtonPressed(btn: Button) {
        // The callback's are forwarded to MenuFragmentController,
        // that handles all the logic for these events
        menuFragController.onButtonPressed(btn)
    }

    private fun prepareModels() {

        duckUri = Uri.parse("duck.sfb")
        tposeUri = Uri.parse("Character.sfb")
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

        val menuBtn = findViewById<Button>(R.id.menuBtn)
        menuBtn.setOnClickListener {
            callMenuFragment()
        }
        val spawnBtn = findViewById<Button>(R.id.spawnBtn)
        spawnBtn.setOnClickListener {
            spawnObjects()
        }
        // MARK: Testing-abilities-related stuff
        attackDuckBtn.setOnClickListener {
            attackDuck()
        }
        val destroyBtn = findViewById<Button>(R.id.destroyBtn)
        destroyBtn.setOnClickListener {
            clearModels()
        }
    }

    private fun callMenuFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_menu_container, MenuFragmentController())
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
    private fun attackDuck() {
        // player deals damage to the fuck when tapping it
        hpRenderableDuck?.view?.textView_healthbar?.text = duckNPC.getStatus().currentHealth.toString()
        player.dealDamage(5.0, duckNPC)


    }

    // MARK: Testing-abilities-related stuff
    private fun createHPBar(child: Node, anchor: AnchorNode, damageTaker: CombatControllable, renderable: ViewRenderable?) {
        val hpNode = Node()
        hpNode.setParent(anchor)
        hpNode.renderable = renderable
        hpNode.localPosition = Vector3(0f,0.5f,0f)
        child.setOnTapListener {_, _ ->
            // player deals damage to the fuck when tapping it
            renderable?.view?.textView_healthbar?.text = damageTaker.getStatus().currentHealth.toString()
            player.dealDamage(5.0, damageTaker)
        }
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
                    val tposeAnchor = (frame.hitTest((pt.x.toFloat()-1450.0f), (pt.y.toFloat()+1300.0f)))[0].createAnchor()
                    val tposeAnchorNode = AnchorNode(tposeAnchor)
                    anchorList.add(tposeAnchorNode)
                    duckAnchorNode.setParent(fragment.arSceneView.scene)
                    tposeAnchorNode.setParent(fragment.arSceneView.scene)
                    val duckNode = TransformableNode(fragment.transformationSystem)
                    val tposeNode = TransformableNode(fragment.transformationSystem)
                    duckNode.scaleController.isEnabled = false
                    tposeNode.scaleController.isEnabled = false
                    duckNode.localScale = Vector3(0.1f,0.1f,0.1f)
                    tposeNode.localScale = Vector3(0.1f,0.1f,0.1f)
                    duckNode.setParent(duckAnchorNode)
                    tposeNode.setParent(tposeAnchorNode)

                    // MARK: Testing-abilities-related stuff
                    createHPBar(duckNode, duckAnchorNode, duckNPC, hpRenderableDuck)
                    createHPBar(tposeNode, tposeAnchorNode, tposeNPC, hpRenderableTpose)

                    duckNode.renderable = renderedDuck
                    duckNode.setLookDirection(Vector3.right())
                    duckNode.select()
                    tposeNode.renderable = renderedTpose
                    protoTargetNode = tposeNode

                    // Move Tposeman towards Duck right after spawn
                    moveToTarget(tposeNode, duckNode)
                    break
                }}}}

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
