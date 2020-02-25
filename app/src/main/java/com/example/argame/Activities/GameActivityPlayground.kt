package com.example.argame.Activities

import android.animation.ObjectAnimator
import android.content.Context
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
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
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
import kotlinx.android.synthetic.main.activity_game.*
import kotlinx.android.synthetic.main.activity_game_playground.*
import kotlinx.android.synthetic.main.healthbar.view.*
import org.jetbrains.anko.doAsyncResult
import java.sql.Time
import kotlin.math.atan
import kotlin.math.pow

class GameActivityPlayground : AppCompatActivity(), FragmentCallbackListener,
    NPCSpawnHandler.NPCSpawnCallback {

    private val menuFragController = MenuFragmentController()
    private lateinit var fragment: CustomArFragment
    private lateinit var duckUri: Uri
    private lateinit var tposeUri: Uri
    private lateinit var playerUri: Uri
    private var renderedDuck: ModelRenderable? = null
    private var renderedTpose: ModelRenderable? = null
    private var renderedPlayer: ModelRenderable? = null
    private var anchorList = ArrayList<AnchorNode>()
    private lateinit var playerAnchorPos: HitResult
    private lateinit var playerAnchorNode: AnchorNode
    private lateinit var playerNode: TransformableNode

    // MARK: Testing-abilities-related stuff
    private var playerTarget: PlayerTargetData? = null
    private var hpRenderableDuck: ViewRenderable? = null
    private var hpRenderablePlayer: ViewRenderable? = null
    private lateinit var player: Player
    var ducksInScene = false
    var playerInScene = false


    // GSON and SHAREDPREFERENCE

    private lateinit var saver: SharedPreferences
    private val gson = Gson()
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
        fragment =
            supportFragmentManager.findFragmentById(R.id.playground_sceneform_fragment) as CustomArFragment
        //saver = defaultPreference(this)
        saver = PreferenceManager.getDefaultSharedPreferences(this)
        curLevel = saver.getInt("levelNum", 1)
        initButtons()
        prepareModels()
        // MARK: Testing-abilities-related stuff
        initHPRenderables()
        playground_targetTxt.text = "No target"
        //saver.clearValues

        newLevel = Level(this).createLevel()
        Log.d("LEVEL", curLevel.toString())
        findViewById<Button>(R.id.playground_toggleLevel).text = "Level " + curLevel.toString()

        spawnHandler = NPCSpawnHandler(this, curLevel ?: 1, Handler())

        fragment.arSceneView.scene.addOnUpdateListener {
            initSceneUpdateListener()
        }
    }

    override fun onPause() {
        super.onPause()
        // TODO: Save level
        saver.edit().putInt("levelNum", curLevel!!).apply()
        Log.d("SAVE", "Saving level " + curLevel.toString())
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
        saver.edit().putInt("levelNum", curLevel!!).apply()
        Log.d("SAVE", "Saving level " + curLevel.toString())
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

    private fun initSceneUpdateListener() {
        val frame = fragment.arSceneView.arFrame
        val pt = getScreenCenter()
        val hits: List<HitResult>
        if (frame != null) {
            hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat() + 400)
            for (hit in hits) {
                val trackable = hit.trackable
                playground_spawnBtn.isEnabled = trackable is Plane && !playerInScene
                // TODO - figure out an efficient implementation for code below.. maybe?
//                    if (playerInScene) {
//                        if (trackable !is Plane) {
//                            spawnHandler.pause()
//                        } else {
//                            spawnHandler.resume()
//                        }
//                    }
            }
        }
    }

    private fun prepareModels() {

        duckUri = Uri.parse("duck.sfb")
        tposeUri = Uri.parse("duck.sfb")
        playerUri = Uri.parse("playermodelv8.sfb")
        val renderableFuture = ModelRenderable.builder()
            .setSource(this, duckUri)
            .build()
        renderableFuture.thenAccept { renderedDuck = it }
        val renderableFuture2 = ModelRenderable.builder()
            .setSource(this, tposeUri)
            .build()
        renderableFuture2.thenAccept { renderedTpose = it }
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

        val spawnBtn = findViewById<Button>(R.id.playground_spawnBtn)
        spawnBtn.setOnClickListener {
            if (newLevel != null) {
                spawnObjects(newLevel!!)
                spawnPlayer()
            }
        }
        // MARK: Testing-abilities-related stuff
        playground_attackDuckBtn.setOnClickListener {
            attackTarget()
        }

        val beamBtn = findViewById<Button>(R.id.playground_beamDuckBtn)
        beamBtn.setOnClickListener {
            beamTarget()
        }
        val destroyBtn = findViewById<Button>(R.id.playground_destroyBtn)
        destroyBtn.setOnClickListener {
            clearModels()
        }

        val levelButton = findViewById<Button>(R.id.playground_toggleLevel)
        levelButton.setOnClickListener {
            when (curLevel) {
                1 -> {
                    levelButton.text = "Level 2"
                    curLevel = 2
                    spawnHandler = NPCSpawnHandler(this, curLevel ?: 1, Handler())

                }
                2 -> {
                    levelButton.text = "Level 10"
                    curLevel = 10
                    spawnHandler = NPCSpawnHandler(this, curLevel ?: 1, Handler())

                }
                else -> {
                    levelButton.text = "Level 1"
                    curLevel = 1
                    spawnHandler = NPCSpawnHandler(this, curLevel ?: 1, Handler())

                }
            }
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
//        val renderableFuture = ViewRenderable.builder()
//            .setView(this, R.layout.healthbar)
//            .build()
//        renderableFuture.thenAccept { hpRenderableDuck = it }
//        hpRenderableDuck?.view?.textView_healthbar?.text = duckNPC.getStatus().currentHealth.toString()
//
//        val renderableFuture2 = ViewRenderable.builder()
//            .setView(this, R.layout.healthbar)
//            .build()
//        renderableFuture2.thenAccept { hpRenderableTpose = it }
//        hpRenderableTpose?.view?.textView_healthbar?.text = tposeNPC.getStatus().currentHealth.toString()

        val renderableFuturePlayer = ViewRenderable.builder()
            .setView(this, R.layout.healthbar)
            .build()
        renderableFuturePlayer.thenAccept {
            hpRenderablePlayer = it
            hpRenderablePlayer?.view?.textView_healthbar?.text =
                player.getStatus().currentHealth.toString()
            hpRenderablePlayer?.view?.textView_healthbar?.background =
                ContextCompat.getDrawable(this, R.drawable.gradient_player_hpbar)
        }
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

    private fun attackPlayer(npc: NPC, node: Node) {
        // disable attack button for the animation duration
            npc.dealDamage(5.0, player)
            val ability = Ability.TEST
            val animData = ProjectileAnimationData(
                // TODO make start position relative to screen position
                node.worldPosition,
                playerAnchorNode.worldPosition,
                this,
                fragment,
                ability.uri()
            )
            npc.useAbility(ability, player, animData) {
                if (hpRenderablePlayer?.view?.textView_healthbar != null) {
                    updateHPBar(hpRenderablePlayer?.view?.textView_healthbar, player)
                }

                // enable attack button after in callback
                if (!player.getStatus().isAlive) {
                    playerAnchorNode.localRotation = Quaternion(0f, 0f, 1f, 0f)
                }
            }
        }

    private fun beamTarget() {
        if (playerTarget != null) {
            playground_beamDuckBtn.isEnabled = false
            val beam = Ability.BEAM
            val data = ProjectileAnimationData(
                playerNode.worldPosition, playerTarget!!.node.worldPosition, this, fragment, beam.uri())
            player.useAbility(beam, playerTarget!!.model, data) {
                if (playerTarget!!.healthBar != null) {
                    updateHPBar(playerTarget!!.healthBar, playerTarget!!.model)
                }
                playground_beamDuckBtn.isEnabled = true
            }
            npcAnchors.forEach {
                val npcAnchorPos = it.anchorNode.worldPosition
                val targetPos = playerTarget!!.node.worldPosition
                val zDifPow = (npcAnchorPos.z - targetPos.z).pow(2)
                val xDifPow = (npcAnchorPos.x - targetPos.x).pow(2)
                val difAdded = (zDifPow+xDifPow)
                val result = Math.sqrt(difAdded.toDouble())
                Log.d("BEAM", "RESULT " + npcAnchors.indexOf(it)+ "  " + result)

                if (result < 0.8) {
                    Log.d("BEAM", "HIT NPC  " + npcAnchors.indexOf(it) )
                    it.anchorNode.localScale = Vector3(0.4f, 0.4f, 0.4f)
                }

 /*               if (npcAnchorNode.worldPosition.x - playerTarget!!.node.worldPosition.x > 0.05f || npcAnchorNode.worldPosition.z - playerTarget!!.node.worldPosition.z < 0.05) {
                    Log.d("BEAM", "Additional target found " + npcAnchorNode.toString())
                    npcAnchorNode.localScale = Vector3(1.3f, 1.3f, 1.3f)

                    Log.d(
                        "BEAM", "Target position " + playerTarget!!.node.worldPosition.toString()
                    )
                }*/
            }
        }
    }

    private fun clearPlayerTarget() {
        playerTarget = null
    }

    // MARK: Testing-abilities-related stuff
    private fun createHPBar(node: TransformableNode, renderable: ViewRenderable?) {
        val hpNode = Node()
        hpNode.setParent(node)
        hpNode.renderable = renderable
        if (node == playerNode) {
            hpNode.localScale = Vector3(0.5f, 0.5f, 0.5f)
            hpNode.localPosition = Vector3(0f, 0.5f, 0f)
        } else {
            hpNode.localScale = Vector3(5f, 5f, 5f)
            hpNode.localPosition = Vector3(0f, 5f, 0f)
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
            if (playerTarget!!.node.children.isNotEmpty()) {
                targetPos = playerTarget!!.node.children[0].worldPosition
            } else {
                targetPos = playerTarget!!.node.worldPosition
            }
            // get the angle between the two nodes
            val x = targetPos.x - playerPos.x
            val y = targetPos.z - playerPos.z
            val tan = x / y
            val arctan = atan(tan).toDouble()
            val degrees = Math.toDegrees(arctan).toFloat()
            Log.d(
                "dbg",
                "Player x: ${playerPos.x} Player y: ${playerPos.y} Player z: ${playerPos.z}" +
                        "Target x: ${targetPos.x} Target y: ${targetPos.y} Target z: ${targetPos.z}"
            )
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
                        playerInScene = true
                        playerAnchorPos = hit!!
                        val playerAnchor = hit.createAnchor()
                        playerAnchorNode = AnchorNode(playerAnchor)
                        playerAnchorNode.localRotation =
                            Quaternion.axisAngle(Vector3(1f, 0f, 0f), 180f)
                        anchorList.add(playerAnchorNode)
                        playerAnchorNode.setParent(fragment.arSceneView.scene)
                        playerNode = TransformableNode(fragment.transformationSystem)
                        val forward = fragment.arSceneView.scene.camera.forward
                        playerAnchorNode.setLookDirection(
                            Vector3(
                                -(forward.x),
                                -(forward.y),
                                -(forward.z)
                            )
                        )
                        playerNode.scaleController.isEnabled = false
                        playerNode.rotationController.isEnabled = false
                        playerNode.setParent(playerAnchorNode)
                        playerNode.renderable = renderedPlayer
                        createHPBar(playerNode, hpRenderablePlayer)

                        // update player look direction toward target if
                        // player position changes during 0.5 seconds
                        playerNode.setOnTouchListener { _, _ ->
                            val oldPosition = playerNode.worldPosition
                            Handler().postDelayed({
                                if (playerNode.worldPosition != oldPosition)
                                    updatePlayerRotation()
                            }, 500)
                        }
                    }
                }
            }
        }
    }

    private fun spawnObjects(numOfDucks: Int) {
        if (!ducksInScene) {
            // "Spawn NPC's"
            npcSpawnThread = Thread {
                spawnHandler.run()
            }
            npcSpawnThread.start()
            ducksInScene = true
            updateNPCRemainingText("NPCs spawning: ${NPCDataForLevels.LevelOne.npcs.size}")
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
        playerTargetHpRend: ViewRenderable?
    ) {

        node.setOnTouchListener { _, _ ->
            if (playerTarget != null && playerTarget?.node != node) {
                if (playerTarget!!.healthBar != null) {
                    playerTarget!!.healthBar!!.text = ""
                }
            }
            updatePlayerRotation()
            playerTarget = PlayerTargetData(
                node, casterModel as NPC,
                playerTargetHpRend?.view?.textView_healthbar
            )
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

    private fun randomMove(node: TransformableNode, npc: NPC) {
        Log.d("RMOVE", "1")
        val randomInt = (1..100).shuffled().first()
        when (randomInt) {
            in 1..10 -> NodeCreator(node, Vector3(0.9f, 0.0f, 0.0f), npc)
            in 11..30 -> NodeCreator(node, Vector3(0.8f, 0.0f, -1.0f), npc)
            in 31..60 -> NodeCreator(node, Vector3(0.6f, 0.0f, -1.0f), npc)
            in 61..100 -> NodeCreator(node, Vector3(0.4f, 0.0f, 0.0f), npc)
        }
    }

    private fun NodeCreator(initialNode: TransformableNode, newLocation: Vector3, npc: NPC) {
        Log.d("RMOVE", "2")
        val newNode = Node()
        val summedVector = Vector3.add(initialNode.worldPosition, newLocation)
        newNode.worldPosition = summedVector
        moveToTarget(initialNode, newNode, npc)
    }

    private fun moveToTarget(model: TransformableNode, targetNode: Node, npc: NPC) {
        Log.d("RMOVE", "3")
        val objectAnimation = ObjectAnimator()
        objectAnimation.setAutoCancel(true)
        objectAnimation.target = model
        model.setLookDirection(Vector3.subtract(model.worldPosition, targetNode.worldPosition))
        objectAnimation.setObjectValues(model.worldPosition, targetNode.worldPosition)
        objectAnimation.setPropertyName("worldPosition")
        objectAnimation.setEvaluator(Vector3Evaluator())
        objectAnimation.interpolator = LinearInterpolator()
        objectAnimation.duration = 3000
        objectAnimation.start()

        Handler().postDelayed({
            val objectAnimation = ObjectAnimator()
            objectAnimation.setAutoCancel(true)
            objectAnimation.target = model
            model.setLookDirection(
                Vector3.subtract(
                    model.worldPosition,
                    playerAnchorNode.worldPosition
                )
            )
            objectAnimation.setObjectValues(model.worldPosition, playerAnchorNode.worldPosition)
            objectAnimation.setPropertyName("worldPosition")
            objectAnimation.setEvaluator(Vector3Evaluator())
            objectAnimation.interpolator = LinearInterpolator()
            objectAnimation.duration = 15000
            objectAnimation.start()
            Handler().postDelayed({

                //npc.dealDamage(100.0,player)
                attackPlayer(npc, model)
            }, 15000)
        }, 3500)
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
            Log.d(
                "NPCSPAWN",
                "NPC of type: ${type.name} spawned at: $time. NPC's remaining: $remaining"
            )
            for (index in spawnedNPCs.indices) {
                Log.d("NPCSPAWN", "spawnedNPCs[$index]: ${spawnedNPCs[index]}")
            }
            if (remaining > 0) {
                updateNPCRemainingText("NPCs spawning: $remaining")
            } else {
                updateNPCRemainingText("")
            }
        }
    }

    override fun notifyAllNPCSpawned() {
        spawnHandler.stop()
        Handler().removeCallbacks(spawnHandler)
        allNPChaveSpawned = true
    }

    private fun spawnNPC(npc: NPC) {
        lateinit var hpRenderable: ViewRenderable
        val renderableFuture = ViewRenderable.builder()
            .setView(this, R.layout.healthbar)
            .build()
        renderableFuture.thenAccept {
            hpRenderable = it
            hpRenderableDuck?.view?.textView_healthbar?.text =
                npc.getStatus().currentHealth.toString()
            val frame = fragment.arSceneView.arFrame
            val pt = getScreenCenter()
            val hits: List<HitResult>
            if (frame != null) {
                hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
                for (hit in hits) {
                    val trackable = hit.trackable
                    if (trackable is Plane) {
                        val valuePool = (200..1200)
                        val randX = valuePool.shuffled().first().toFloat()
                        val randY = valuePool.shuffled().first().toFloat()
                        Log.d("XYVALUES", randX.toString() + "  " + randY.toString())
                        val anchor = (frame.hitTest(
                            (pt.x.toFloat() - randX),
                            (pt.y.toFloat() + randY)
                        ))[0].createAnchor()
                        val anchorNode = AnchorNode(anchor)
                        npcAnchors.add(NPCAnchorData(anchorNode, npc.getID()))
                        anchorNode.setParent(fragment.arSceneView.scene)
                        val node = TransformableNode(fragment.transformationSystem)
                        node.scaleController.isEnabled = false
                        node.rotationController.isEnabled = false
                        node.setParent(anchorNode)
                        node.renderable = npc.model
                        node.localScale = Vector3(0.1f, 0.1f, 0.1f)
                        if (npc.getID() == 100) {
                            node.localScale = Vector3(0.4f, 0.4f, 0.4f)
                        }
                        createHPBar(node, hpRenderable)
                        randomMove(node, npc)
                        node.setOnTouchListener { _, _ ->
                            val oldPosition = node.worldPosition
                            Handler().postDelayed({
                                if (node.worldPosition != oldPosition) {
                                    if (playerTarget != null) {
                                        Log.d(
                                            "node",
                                            "targetnode == node: ${playerTarget!!.node == node}"
                                        )
                                        val playerTargetNode = playerTarget!!.node
                                        if (playerTargetNode != node) {
                                            Log.d("node", "target not node")
                                            val playerTargetBar = playerTarget!!.healthBar!!
                                            updateOldTargetHPBar(playerTargetBar)
                                        }
                                    }
                                    // set new target
                                    val newTarget = PlayerTargetData(
                                        node,
                                        npc,
                                        hpRenderable.view.textView_healthbar
                                    )
                                    playerTarget = newTarget
                                    updateNewTargetHPBar(newTarget)
                                    updatePlayerRotation()
                                }
                            }, 250)
                        }
                    }
                }
            }
        }
    }

    private fun updateOldTargetHPBar(hpBar: TextView) {
        hpBar.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.gradient_healthbar
            )
        // clear old target hp text
        hpBar.text = ""
    }

    private fun updateNewTargetHPBar(targetData: PlayerTargetData) {
        targetData.healthBar?.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.gradient_playertarget_hpbar
            )
        // clear old target hp text
        playerTarget!!.healthBar?.text = targetData.model.getStatus().currentHealth.toString()
    }

    private fun updateNPCRemainingText(text: String) {
        playground_remaining.text = text
    }
}
