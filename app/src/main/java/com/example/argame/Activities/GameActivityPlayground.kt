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
import com.example.argame.Fragments.CustomArFragment
import com.example.argame.Fragments.MenuFragmentController
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.Interfaces.PreferenceHelper.defaultPreference
import com.example.argame.Interfaces.PreferenceHelper.setGson
import com.example.argame.Interfaces.PreferenceHelper.clearValues
import com.example.argame.Model.*
import com.example.argame.R
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
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.onComplete
import org.jetbrains.anko.uiThread
import java.sql.Time
import kotlin.math.atan
import kotlin.math.atan2

class GameActivityPlayground : AppCompatActivity(), FragmentCallbackListener, NPCSpawnHandler.NPCSpawnCallback {

    private val menuFragController = MenuFragmentController()
    private lateinit var fragment: CustomArFragment
    private lateinit var duckUri: Uri
    private lateinit var tposeUri: Uri
    private lateinit var playerUri: Uri
    private var renderedDuck: ModelRenderable? = null
    private var renderedTpose: ModelRenderable? = null
    private var renderedPlayer: ModelRenderable? = null
    private var protoTargetNode: TransformableNode? = null
    private var anchorList = ArrayList<AnchorNode>()
    private lateinit var firstAnchorPos: HitResult
    private lateinit var playerAnchorPos: HitResult
    private lateinit var playerAnchorNode: AnchorNode
    private lateinit var playerNode: TransformableNode

    // MARK: Testing-abilities-related stuff
    private var playerTarget: PlayerTargetData? = null
    private var hpRenderableDuck: ViewRenderable? = null
    private var hpRenderableTpose: ViewRenderable? = null
    private var hpRenderablePlayer: ViewRenderable? = null
    private var duckNPC = NPC(1.0, "duck", 5000.0, type = NPCType.MELEE, id = 500)
    private var tposeNPC = NPC(1.0, "duck 2", 5000.0, type = NPCType.MELEE, id = 600)
    private lateinit var player: Player
    var ducksInScene = false
    var playerInScene = false


    // GSON and SHAREDPREFERENCE

    private lateinit var saver: SharedPreferences
    private val gson = Gson()
    private var levelNum = 1
    private var curLevel: Int? = null
    private var newLevel: Int? = null

    // Spawning NPC's
    private lateinit var spawnHandler: NPCSpawnHandler
    private lateinit var npcSpawnThread: Thread
    private var spawnedNPCs = arrayListOf<NPC>()
    private var npcAnchors = arrayListOf<NPCAnchorData>()
    private var level = 1
    private var allNPChaveSpawned = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_playground)
        fragment = supportFragmentManager.
            findFragmentById(R.id.playground_sceneform_fragment) as CustomArFragment
        saver = defaultPreference(this)
        curLevel = saver.getInt("levelNum", 1)
        initButtons()
        prepareModels()
        // MARK: Testing-abilities-related stuff
        initHPRenderables()
        playground_targetTxt.text = "No target"
        saver.clearValues

        newLevel = Level(this).createLevel()
        Log.d("LEVEL", curLevel.toString())

        spawnHandler = NPCSpawnHandler(this, curLevel ?: 1, Handler())
    }

    override fun onPause() {
        super.onPause()
        // TODO: Save level
        saver.edit().putInt("levelNum", levelNum)
        saver.edit().apply()
/* // MARK: Example of saving items to class -> to gson -> to SharedPreferences
        val classified = ObjectLister(builderList)
        Log.d("Builderlist", builderList.size.toString())
        val saveGame = GameVault(classified)
        val saveToGson = gson.toJson(saveGame)
        saver.setGson = saveToGson
*/
        spawnHandler.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        saver.edit().putInt("levelNum", levelNum)
        saver.edit().apply()
    }

    override fun onResume() {
        super.onResume()
        curLevel = saver.getInt("levelNum", 1)
        spawnHandler.resume()


        // TODO: Restore level and abilities
/*
            val parsedVault = gson.fromJson(saver.setGson, GameVault::class.java)
            val parsedLister = parsedVault.classifiedBuilder
            val parsedBuilder = parsedLister.builderList
            val firstItem = parsedBuilder[0]
            //spawnObjectsMarkThree(builderList)
        }*/
    }

    override fun onButtonPressed(btn: Button) {
        // The callback's are forwarded to MenuFragmentController,
        // that handles all the logic for these events
        menuFragController.onButtonPressed(btn)
    }

    private fun prepareModels() {

        duckUri = Uri.parse("duck.sfb")
        tposeUri = Uri.parse("duck.sfb")
        playerUri = Uri.parse("playermodelv8.sfb")
        val renderableFuture = ModelRenderable.builder()
            .setSource(this, duckUri)
            .build()
        renderableFuture.thenAccept{renderedDuck = it}
        val renderableFuture2 = ModelRenderable.builder()
            .setSource(this, tposeUri)
            .build()
        renderableFuture2.thenAccept{renderedTpose = it}
        val renderableFuturePlayer = ModelRenderable.builder()
            .setSource(this, playerUri)
            .build()
        renderableFuturePlayer.thenAccept {
            renderedPlayer = it
            // lateinit Player, so it has a reference to the renderable, therefor the cast animation data
            player = Player(5.0, "player", 5000.0, it)
        }
    }

    private fun initButtons() {

        val menuBtn = findViewById<Button>(R.id.playground_menuBtn)
        menuBtn.setOnClickListener {
            callMenuFragment()
        }
        val spawnBtn = findViewById<Button>(R.id.playground_spawnBtn)
        spawnBtn.setOnClickListener {
            if (newLevel != null) {
                spawnObjects(newLevel!!)
            }
            spawnPlayer()
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
        // TODO: Move menu to *betweenlevelsActivity*
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.playground_main_menu_container, MenuFragmentController())
            .addToBackStack(null)
            .commit()
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

        val renderableFuturePlayer = ViewRenderable.builder()
            .setView(this, R.layout.healthbar)
            .build()
        renderableFuturePlayer.thenAccept { hpRenderablePlayer = it }
        hpRenderablePlayer?.view?.textView_healthbar?.text = player.getStatus().currentHealth.toString()
    }

    // MARK: Testing-abilities-related stuff
    private fun attackTarget() {
        // disable attack button for the animation duration
        if (playerTarget != null) {
            playground_attackDuckBtn.isEnabled = false
            player.dealDamage(5.0, playerTarget!!.model)
            val ability = Ability.TEST
            val animData = ProjectileAnimationData(
                // TODO make start position relative to screen position
                playerAnchorNode.worldPosition,
                playerTarget!!.node.worldPosition,
                this,
                fragment,
                ability.uri()
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
        anchorList.forEach {
            // TODO: Calculate a good offset for worldposition difference. 0.1f is just a placeholder.
            if (it.worldPosition.x - playerTarget.node.worldPosition.x < 0.1f
                && it.worldPosition.y - playerTarget.node.worldPosition.y < 0.1f) {
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

    private fun spawnObjectsMarkThree(builderList: ArrayList<ObjectBuilder>) {
        builderList.forEach {
            val hitResult = it.hitResult
            val wPos = it.wPos
            val scale = it.scale
            //val uri = it.uri
            //val hpNode = TODO: <-- hanki osat

            val anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(fragment.arSceneView.scene)
            val transfNode = TransformableNode(fragment.transformationSystem)
            transfNode.scaleController.isEnabled = false
            transfNode.localScale = scale
            transfNode.localPosition = wPos
            transfNode.setParent(anchorNode)
            transfNode.renderable = renderedDuck
            transfNode.select()
        }
    }

    private fun clearModels() {
        for (anchor in anchorList) {
            removeAnchorNode(anchor)
        }
        anchorList.clear()
        ducksInScene = false
        playerInScene = false
    }


    private fun updatePlayerRotation() {
        if (playerTarget != null) {
            val playerPos = playerNode.worldPosition
            var targetPos: Vector3
            if (playerTarget!!.node.children != null) {
                targetPos = playerTarget!!.node.children[0].worldPosition
            }
            else {
                targetPos = playerTarget!!.node.worldPosition
            }
            // get the angle between the two nodes
            val x = targetPos.x - playerPos.x
            val y = targetPos.z - playerPos.z
            val tan = x/y
            val arctan = atan(tan).toDouble()
            val degrees = Math.toDegrees(arctan).toFloat()
            Log.d("dbg", "Player x: ${playerPos.x} Player y: ${playerPos.y} Player z: ${playerPos.z}" +
                    "Target x: ${targetPos.x} Target y: ${targetPos.y} Target z: ${targetPos.z}")
            Log.d("DBG", "tan.... : $tan")
            Log.d("DBG", "arctan: $arctan")
            playerNode.localRotation = Quaternion.axisAngle(Vector3(0f, 1f, 0f), degrees)
            Log.d("DBG", "Player look direction changed.")
        }
    }

    private fun spawnPlayer() {
        if (!playerInScene) {
            val frame = fragment.arSceneView.arFrame
            val pt = getScreenCenter()
            val hits: List<HitResult>
            if (frame != null && renderedPlayer != null) {
                hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat() + 400)
                for (hit in hits) {
                    val trackable = hit.trackable
                    if (trackable is Plane) {
                        playerAnchorPos = hit!!
                        val playerAnchor = hit.createAnchor()
                        playerAnchorNode = AnchorNode(playerAnchor)
                        playerAnchorNode.localRotation = Quaternion.axisAngle(Vector3(1f, 0f, 0f), 180f)
                        anchorList.add(playerAnchorNode)
                        playerAnchorNode.setParent(fragment.arSceneView.scene)
                        playerNode = TransformableNode(fragment.transformationSystem)
                        val forward = fragment.arSceneView.scene.camera.forward
                        playerAnchorNode.setLookDirection(Vector3(-(forward.x), -(forward.y), -(forward.z)))
                        playerNode.scaleController.isEnabled = false
                        playerNode.rotationController.isEnabled = false
                        playerNode.setParent(playerAnchorNode)
                        playerNode.renderable = renderedPlayer
                        createHPBar(playerAnchorNode, hpRenderablePlayer)

                        // update player look direction toward target if
                        // player position changes during 0.5 seconds
                        playerNode.setOnTouchListener {_, _ ->
                            val oldPosition = playerNode.worldPosition
                            Handler().postDelayed({
                                if (playerNode.worldPosition != oldPosition)
                                    updatePlayerRotation()
                            }, 500)
                        }
                    }
                }
            }
            playerInScene = true
        }
    }

    private fun spawnObjects(numOfDucks: Int) {
        if (!ducksInScene) {
            // "Spawn NPC's" -------------------------------
            npcSpawnThread = Thread {
                spawnHandler.run()
            }
            npcSpawnThread.start()
            updateNPCRemainingText("NPCs spawning: ${NPCDataForLevels.LevelOne.npcs.size}")
            // -------------------------------------------------------------------------------------
            duckNPC = NPC(1.0, "duck", 5000.0, type = NPCType.MELEE, id = 500)
            tposeNPC = NPC(1.0, "duck 2", 5000.0, type = NPCType.MELEE, id = 600)
            // For prototyping only
            val frame = fragment.arSceneView.arFrame
            val pt = getScreenCenter()
            val hits: List<HitResult>
            if (frame != null && renderedDuck != null) {
                hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
                for (hit in hits) {
                    val trackable = hit.trackable
                    if (trackable is Plane) {
                        // firstAnchorPos is for setting the base for the initial anchor
                        firstAnchorPos = hit!!
                        val duckAnchor = hit.createAnchor()
                        val duckAnchorNode = AnchorNode(duckAnchor)
                        anchorList.add(duckAnchorNode)
                        duckAnchorNode.setParent(fragment.arSceneView.scene)
                        var additionalSpawns = (numOfDucks -1)

                        while (additionalSpawns > 0) {
                            additionalSpawns--
                            val valuePool = (10..1000)
                            val randX = valuePool.shuffled().first().toFloat()
                            val randY = valuePool.shuffled().first().toFloat()
                            Log.d("XYVALUES", randX.toString() + "  " + randY.toString())
                            val anchor = (frame.hitTest(
                                (pt.x.toFloat() - randX),
                                (pt.y.toFloat() + randY)
                            ))[0].createAnchor()
                            val anchorNode = AnchorNode(anchor)
                            anchorList.add(anchorNode) // TODO: Check if unnecessary
                            anchorNode.setParent(fragment.arSceneView.scene)
                            val npcNode = TransformableNode(fragment.transformationSystem)
                            npcNode.scaleController.isEnabled = false
                            npcNode.localScale = Vector3(0.1f, 0.1f, 0.1f)
                            npcNode.setParent(anchorNode)
                            npcNode.renderable = renderedDuck
                            createHPBar(anchorNode, hpRenderableTpose)
                        }

                        val duckNode = TransformableNode(fragment.transformationSystem)
                        duckNode.scaleController.isEnabled = false
                        duckNode.localScale = Vector3(0.1f, 0.1f, 0.1f)
                        duckNode.setParent(duckAnchorNode)
                        duckNode.renderable = renderedDuck
                        //duckNode.setLookDirection(tposeNode.worldPosition)
                        //tposeNode.setLookDirection(duckNode.worldPosition)
                        duckNode.select()
                        // MARK: Testing-abilities-related stuff
                        createHPBar(duckAnchorNode, hpRenderableDuck)

                        // tap listeners for ability usage
                        // TODO -> move hpRenderable out of this function. Could be stored inside every model..?
                        createOnTapListenerForAttack(
                            // TODO -> create separate functions for acquiring player target, and for setting up tap listeners
                            duckNode,
                            duckAnchorNode,
                            duckAnchorNode,
                            tposeNPC,
                            hpRenderableDuck,
                            duckNPC,
                            hpRenderableTpose
                        )
                        createOnTapListenerForAttack(
                            duckNode,
                            duckAnchorNode,
                            duckAnchorNode,
                            duckNPC,
                            hpRenderableTpose,
                            tposeNPC,
                            hpRenderableDuck
                        )
                        ducksInScene = true
                        randomMove(duckNode)
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

        node.setOnTouchListener {_, _ ->
            if (playerTarget != null && playerTarget?.node != node) {
                if (playerTarget!!.healthBar != null) {
                    playerTarget!!.healthBar!!.text = ""
                }
            }
            updatePlayerRotation()
            playerTarget = PlayerTargetData(node, casterModel as NPC,
                playerTargetHpRend?.view?.textView_healthbar)
            playground_targetTxt.text = "Target: ${target.name}"
            playerTargetHpRend?.view?.textView_healthbar?.text = casterModel.getStatus()
                .currentHealth.toString()
            true
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

    private fun randomMove(node: TransformableNode) {
        Log.d("RMOVE", "1")
        val randomInt = (1..10).shuffled().first()
        when(randomInt) {
            in 1..10 -> NodeCreator(node, Vector3(0.5f,0.0f,0.0f))
            /*in 11..30 -> //Liiku X suuntaan Y matka
            in 31..60 -> //Liiku X suuntaan Y matka
            in 61..100 -> // Liiku X suuntaan Y matka*/
        }
    }
    private fun NodeCreator(initialNode: TransformableNode, newLocation: Vector3) {
        Log.d("RMOVE", "2")
        val newNode = Node()
        val summedVector = Vector3.add(initialNode.worldPosition, newLocation)
        newNode.worldPosition = summedVector
        moveToTarget(initialNode,newNode)
    }

    private fun moveToTarget(model: TransformableNode, targetNode: Node) {
        Log.d("RMOVE", "3")
        val objectAnimation = ObjectAnimator()
        objectAnimation.setAutoCancel(true)
        objectAnimation.target = model
        model.setLookDirection(Vector3.subtract(model.worldPosition, targetNode.worldPosition))
        objectAnimation.setObjectValues(model.worldPosition, targetNode.worldPosition)
        objectAnimation.setPropertyName("worldPosition")
        objectAnimation.setEvaluator(Vector3Evaluator())
        objectAnimation.interpolator = LinearInterpolator()
        objectAnimation.duration = 3500
        objectAnimation.start()

            Handler().postDelayed({
                val objectAnimation = ObjectAnimator()
                objectAnimation.setAutoCancel(true)
                objectAnimation.target = model
                model.setLookDirection(Vector3.subtract(model.worldPosition, model.parent!!.worldPosition))
                objectAnimation.setObjectValues(model.worldPosition, model.parent?.worldPosition)
                objectAnimation.setPropertyName("worldPosition")
                objectAnimation.setEvaluator(Vector3Evaluator())
                objectAnimation.interpolator = LinearInterpolator()
                objectAnimation.duration = 3500
                objectAnimation.start()
                Handler().postDelayed({
                    randomMove(model)
                }, 4000)

            }, 4000)
    }

    override fun notifyNPCSpawned(type: NPCType, remaining: Int, npcID: Int) {
        lateinit var renderable: ModelRenderable
        // get NPC model
        val renderableFuture = ModelRenderable.builder()
            .setSource(this, type.modelUri())
            .build()
        renderableFuture.thenAccept {
            renderable = it
            // create the NPC object when we have a ModelRenderable ready
            val npcObject = type.getNPCObject(level, renderable, npcID)
            // add the NPC to spawnedNPCs
            spawnedNPCs.add(npcObject)
            // spawn NPC
            spawnNPC(npcObject)
            // random debugs
            val time = Time(System.currentTimeMillis())
            Log.d("NPCSPAWN", "NPC of type: ${type.name} spawned at: $time. NPC's remaining: $remaining")
            for (index in spawnedNPCs.indices) {
                Log.d("NPCSPAWN", "spawnedNPCs[$index]: ${spawnedNPCs[index]}")
            }
            // when the last NPC has spawned, the level should end when it dies.
            updateNPCRemainingText("NPCs spawning: $remaining")

        }
    }

    override fun notifyAllNPCSpawned() {
        spawnHandler.stop()
        Handler().removeCallbacks(spawnHandler)
        allNPChaveSpawned = true
        updateNPCRemainingText("")
    }

    private fun spawnNPC(npc: NPC) {
        lateinit var hpRenderable: ViewRenderable
        val renderableFuture = ViewRenderable.builder()
            .setView(this, R.layout.healthbar)
            .build()
        renderableFuture.thenAccept {
            hpRenderable = it
            hpRenderableDuck?.view?.textView_healthbar?.text = npc.getStatus().currentHealth.toString()
            val frame = fragment.arSceneView.arFrame
            val pt = getScreenCenter()
            val hits: List<HitResult>
            if (frame != null) {
                hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
                for (hit in hits) {
                    val trackable = hit.trackable
                    if (trackable is Plane) {
                        val anchor = hit.createAnchor()
                        val anchorNode = AnchorNode(anchor)
                        npcAnchors.add(NPCAnchorData(anchorNode, npc.getID()))
                        anchorNode.setParent(fragment.arSceneView.scene)
                        val node = TransformableNode(fragment.transformationSystem)
                        node.scaleController.isEnabled = false
                        node.rotationController.isEnabled = false
                        node.setParent(anchorNode)
                        node.renderable = npc.model
                        node.localScale = Vector3(0.1f,0.1f,0.1f)
                        createHPBar(anchorNode, hpRenderable)
                        node.setOnTouchListener { _, _ ->
                            playerTarget = PlayerTargetData(node, npc, hpRenderable.view.textView_healthbar)
                            val oldPosition = node.worldPosition
                            Handler().postDelayed({
                                if (node.worldPosition != oldPosition)
                                    updatePlayerRotation()
                            }, 500)
                        }
                    }
                }
            }
        }
    }

    private fun updateNPCRemainingText(text: String) {
        playground_remaining.text = text
    }
}
