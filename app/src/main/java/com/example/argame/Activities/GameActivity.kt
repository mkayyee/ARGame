package com.example.argame.Activities

import com.example.argame.Model.BackgroundMusic
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Log.wtf
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.marginEnd
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.example.argame.Fragments.CustomArFragment
import com.example.argame.Fragments.Menu.GameOverFragment
import com.example.argame.Fragments.Menu.MenuFragmentController
import com.example.argame.Fragments.Menu.NextLevelFragment
import com.example.argame.Interfaces.CAST_TIME
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.Model.*
import com.example.argame.Model.Ability.*
import com.example.argame.Model.CombatControllable.CombatControllable
import com.example.argame.Model.NPC.*
import com.example.argame.Model.Persistence.AppDatabase
import com.example.argame.Model.Persistence.User
import com.example.argame.Model.Persistence.UserDao
import com.example.argame.Model.Player.Player
import com.example.argame.Model.Player.PlayerTargetData
import com.example.argame.Networking.HighscoreService
import com.example.argame.Networking.NetworkAPI
import com.example.argame.Networking.RetrofitClientInstance
import com.example.argame.R
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_game_playground.*
import kotlinx.android.synthetic.main.activity_level_intermission.*
import kotlinx.android.synthetic.main.healthbar.view.*
import kotlinx.android.synthetic.main.healthbar.view.textView_barrier
import kotlinx.android.synthetic.main.ultimatebar.view.*
import okhttp3.ResponseBody
import org.jetbrains.anko.displayMetrics
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.*
import pl.droidsonroids.gif.GifImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.sql.Time

/**
 *  The activity where all the magic happens... literally.
 *
 *  While this activity should by no means be ~1500 lines of code,
 *  there was simply not enough time to abstract it out.
 *
 *  This class basically holds the entire game logic (mostly UI-wise),
 *  having references to helper classes for spawning enemies,
 *  override methods for game callbacks (informing that the UI should be updated),
 *  using and animating abilities --
 *
 *  and is responsible for:
 *      - rendering abilities,
 *      - rendering player and enemy models
 *      - and most importantly, rendering the AR scene.
 */

class GameActivity : AppCompatActivity(), FragmentCallbackListener,
    NPCSpawnHandler.NPCSpawnCallback, Ability.AbilityCallbackListener,
    CombatControllable.CombatControllableListener, UltimateHandler.UltimateHandlerListener {

    private val menuFragController =
        MenuFragmentController()
    private lateinit var fragment: CustomArFragment
    private var rootView : View? = null
    private lateinit var playerUri: Uri
    private var renderedPlayer: ModelRenderable? = null
    private var anchorList = ArrayList<AnchorNode>()
    private lateinit var playerAnchorNode: AnchorNode
    private lateinit var playerNode: TransformableNode

    // Abilities-related stuff
    private var playerTarget: PlayerTargetData? = null
    private var hpRenderableNPC: ViewRenderable? = null
    private var hpRenderablePlayer: ViewRenderable? = null
    private var ultRenderablePlayer: ViewRenderable? = null
    private lateinit var player: Player
    private lateinit var ultimateHandler: UltimateHandler
    private lateinit var beamRenderable: ModelRenderable
    private lateinit var fireBallRenderable: ViewRenderable
    private lateinit var dotRenderable: ViewRenderable
    var ducksInScene = false
    var playerInScene = false
    val cdHandler = Handler(Looper.getMainLooper())
    val threadList = mutableListOf<Thread>()
    var threadStop = false

    // SHAREDPREFERENCE

    private lateinit var saver: SharedPreferences
    private var userId: Int? = null
    private var curLevel: Int? = null
    val abilityList = listOf(
        Ability.FBALL,
        Ability.DOT,
        Ability.SHIELD,
        Ability.TELEPORT,
        Ability.BEAM
    )

    // MUSIC & SOUNDS
    private lateinit var intentm : Intent
    private lateinit var effectPlayerPlayer : SoundEffectPlayer
    private lateinit var effectPlayerNPC : SoundEffectPlayer

    // Spawning NPC's
    private lateinit var spawnHandler: NPCSpawnHandler
    private lateinit var npcSpawnThread: Thread
    private var spawnedNPCs = arrayListOf<NPC>()
    private var npcsAlive = arrayListOf<NPC>()
    private var npcAnchors = arrayListOf<NPCAnchorData>()
    private var hpBarNodes = arrayListOf<Node>()
    private var allNPChaveSpawned = false
    private var forceStop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_playground)
        intentm = Intent(this, BackgroundMusic::class.java)

        effectPlayerPlayer = SoundEffectPlayer(this)
        effectPlayerPlayer.initSoundEffectPlayer()

        effectPlayerNPC = SoundEffectPlayer(this)
        effectPlayerNPC.initSoundEffectPlayer()

        fragment =
            supportFragmentManager.findFragmentById(R.id.playground_sceneform_fragment) as CustomArFragment
        fragment.arSceneView.scene.addOnUpdateListener {
            sceneUpdateListener()
        }
        saver = PreferenceManager.getDefaultSharedPreferences(this)
        curLevel = saver.getInt("levelNum", 1)
        abilityList.forEach{
            val resPwr = saver.getFloat(it.name + "pwr", 0.0F).toDouble()
            val resCd = saver.getFloat(it.name + "cd", 0.0F).toDouble()
            if (resPwr > 0 && AbilityModifier.getPwrModifier(it) == 1.0) {
            AbilityModifier.setModifier(it, true, value = resPwr)
            }
            if (resCd < 0 && AbilityModifier.getCdModifier(it) == 1.0) {
                AbilityModifier.setModifier(it, false, value = resCd)
            }
            Log.d("MODIFIER","Current PWR modifier for " + it.toString() + " is: " + AbilityModifier.getPwrModifier(it) )
        }
        val uid = saver.getInt("USER", -1)
        if (uid != -1) {
            userId = uid
        }

        initButtons()
        prepareModels()
        initBeamRenderer()
        // MARK: Testing-abilities-related stuff
        initHPRenderables()
        playground_targetTxt.text = "Enemies alive ${npcsAlive.size}"
        spawnHandler = NPCSpawnHandler(
            this,
            curLevel ?: 1,
            Handler()
        )
        initUltimateHandler()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (teleportLayout != null && shieldLayout != null && beamLayout != null && attackLayout != null) {
            val layoutParamsTop = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            val layoutParamsBot = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            var marginTopTop = 0
            var marginTopBot = 0
            var marginRight = 0
            var marginLeft = 0
            when (newConfig.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> {
                    marginTopTop = 300 * displayMetrics.density.toInt()
                    marginTopBot = 500 * displayMetrics.density.toInt()
                    marginRight = 10 * displayMetrics.density.toInt()
                    marginLeft = 10 * displayMetrics.density.toInt()
                }
                Configuration.ORIENTATION_LANDSCAPE -> {
                    marginTopTop = 150 * displayMetrics.density.toInt()
                    marginTopBot = 320 * displayMetrics.density.toInt()
                    marginRight = 20 * displayMetrics.density.toInt()
                    marginLeft = 20 * displayMetrics.density.toInt()
                }
            }
            layoutParamsTop.setMargins(marginLeft, marginTopTop, marginRight, 0)
            layoutParamsBot.setMargins(marginLeft, marginTopBot, marginRight, 0)
            teleportLayout.layoutParams = layoutParamsTop
            shieldLayout.layoutParams = layoutParamsTop
            beamLayout.layoutParams = layoutParamsBot
            attackLayout.layoutParams = layoutParamsBot
        }
    }

    override fun onPause() {
        super.onPause()
        saver.edit().putInt("levelNum", curLevel!!).apply()
        Log.d("SAVE", "Saving level " + curLevel.toString())
        spawnHandler.pause()
        threadStop = true
        stopService(intentm)
    }

    override fun onDestroy() {
        super.onDestroy()
        saver.edit().putInt("levelNum", curLevel!!).apply()
        Log.d("SAVE", "Saving level " + curLevel.toString())
        //stopService(intentm)
        threadStop = true
        threadList.forEach {
            it.interrupt()
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("MODIFIER", "DESTROYED")
        abilityList.forEach{
            saver.edit().putFloat(it.name + "pwr", (AbilityModifier.getPwrModifier(it)-1).toFloat()).apply()
            saver.edit().putFloat(it.name + "cd", (AbilityModifier.getCdModifier(it)-1).toFloat()).apply()
            Log.d("MODIFIER", "Saved "+ it.name)
        }
    }

    override fun onResume() {
        super.onResume()
        threadStop = false
        curLevel = saver.getInt("levelNum", 1)
        spawnHandler.resume()
        if (npcsAlive.size > 0) {
            var npcNode : TransformableNode?
            npcsAlive.forEach { curNpc ->
                npcAnchors.forEach {
                    if (it.npc == curNpc) {
                        npcNode = it.anchorNode.children[0] as TransformableNode
                        if (npcNode != null) {
                            attackInitializer(curNpc.getType(), curNpc, npcNode!!)
                        }
                    }
                }
            }
        }
        if (!BackgroundMusic().isBackgroundMusicRunning()) {
            startService(intentm)
        }
        Log.d("MUSIC", "Running?: " + BackgroundMusic().isBackgroundMusicRunning().toString())
        // TODO: Restore level and abilities
    }

    override fun onButtonPressed(btn: Button) {
        if (btn.id == button_close_ability_menu.id) {
            callFragment("NextLevel")
        } else {
            // The callback's are forwarded to MenuFragmentController,
            // that handles all the logic for these events
            menuFragController.onButtonPressed(btn)
        }
    }

    private fun initBeamRenderer() {
        MaterialFactory.makeTransparentWithColor(this, Color(0f, 0f, 1f, 0.25f))
            .thenAccept { material: Material? ->
                beamRenderable = ShapeFactory.makeCube(
                    Vector3(0.5f, 0.5f, 1f),
                    Vector3.zero(), material
                )
                beamRenderable.isShadowCaster = false
            }
        val renderableFutureAbility = ViewRenderable.builder()
            .setView(this, R.layout.ability_animation)
            .build()
        renderableFutureAbility.thenAccept {
            fireBallRenderable = it
        }
        val renderableFutureDot = ViewRenderable.builder()
            .setView(this, R.layout.ability_animation_dot)
            .build()
        renderableFutureDot.thenAccept {
            dotRenderable = it
        }
    }

    private fun initUltimateHandler() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        ultimateHandler = UltimateHandler(accel, light, sensorManager, this)
    }

    private fun sceneUpdateListener() {
        updateHpBarOrientations()
        updatePlayerRotation()
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
        playerUri = Uri.parse("Alien.sfb")
        val renderableFuturePlayer = ModelRenderable.builder()
            .setSource(this, playerUri)
            .build()
        renderableFuturePlayer.thenAccept {
            val prevScore = saver.getInt("SCORE", 0)
            renderedPlayer = it
            player = Player(
                5.0,
                "player",
                1300.0,
                it,
                this,
                score = prevScore
            )
        }
    }

    private fun getAbilityButtonFunction(cdData: ButtonCooldownData) {
        when (cdData.ability) {
            Ability.FBALL -> attackTarget(cdData)
            Ability.BEAM -> beamTarget(playerTarget, cdData = cdData)
            Ability.SHIELD -> useBarrier(null, player, cdData)
            Ability.TELEPORT -> teleportPlayer(cdData)
            Ability.DOT -> gasTarget(cdData)
            Ability.ATK -> attackTarget(cdData)
        }
    }

    private fun initAbilityButtons() {
        doAsyncResult {
            val abilities = AppDatabase.get(this@GameActivity).abilitiesDao().getSelectedAbilities()
            val first = AbilityConverter.toAbility(abilities[0].abilityID)
            val second = AbilityConverter.toAbility(abilities[1].abilityID)
            val third = AbilityConverter.toAbility(abilities[2].abilityID)
            val fourth = AbilityConverter.toAbility(abilities[3].abilityID)
            onComplete {
                uiThread {
                    playground_attackDuckBtn.setImageDrawable(first.getImage(this@GameActivity))
                    playground_beamDuckBtn.setImageDrawable(second.getImage(this@GameActivity))
                    playground_shieldDuckBtn.setImageDrawable(third.getImage(this@GameActivity))
                    playground_teleportDuckBtn.setImageDrawable(fourth.getImage(this@GameActivity))

                    // TODO IF PLAYER IN SCENE!!!!!
                    playground_attackDuckBtn.setOnClickListener {
                        getAbilityButtonFunction(ButtonCooldownData(playground_attackDuckBtn_cd, playground_attackDuckBtn, first))
                        Log.d("BUTTONPRESS", "attack btn pressed")
                        Log.d("BUTTONPRESS", "attack btn pressed. Ability: ${first.name}")
                    }
                    playground_beamDuckBtn.setOnClickListener {
                        Log.d("BUTTONPRESS", "beam btn pressed")
                        Log.d("BUTTONPRESS", "beam btn pressed. Ability: ${second.name}")
                        getAbilityButtonFunction(ButtonCooldownData(playground_beamDuckBtn_cd, playground_beamDuckBtn, second))
                    }
                    playground_shieldDuckBtn.setOnClickListener {
                        Log.d("BUTTONPRESS", "shield btn pressed")
                        Log.d("BUTTONPRESS", "shield btn pressed. Ability: ${third.name}")
                        getAbilityButtonFunction(ButtonCooldownData(playground_shieldDuckBtn_cd, playground_shieldDuckBtn, third))
                    }
                    playground_teleportDuckBtn.setOnClickListener {
                        Log.d("BUTTONPRESS", "teleport btn pressed")
                        Log.d("BUTTONPRESS", "teleport btn pressed. Ability: ${fourth.name}")
                        getAbilityButtonFunction(ButtonCooldownData(playground_teleportDuckBtn_cd, playground_teleportDuckBtn, fourth))
                    }
                }
            }
        }
    }

    private fun initButtons() {
        playground_spawnBtn.setOnClickListener {
            if (curLevel != null && !playerInScene) {
                spawnObjects()
                mainSpawner(player)
            }
        }
        playground_exitBtn.setOnClickListener {
            threadStop = true
            callFragment("GameOver")
        }
        initAbilityButtons()
//        playground_destroyBtn.setOnClickListener {
//            clearModels()
//        }
        playground_killallBtn.setOnClickListener {
            cancelUltBtnAnimations()
            ultimateHandler.beginMeasuring(PlayerUltimate.KILLALL)
            // TODO: Hide ultimate bar
            Toast.makeText(this, "Move phone fast enough in any direction to trigger KILLALL", Toast.LENGTH_LONG).show()
        }
        playground_serenityBtn.setOnClickListener {
            cancelUltBtnAnimations()
            ultimateHandler.beginMeasuring(PlayerUltimate.SERENITY)
            Toast.makeText(this, "Cover light sensor to trigger Serenity", Toast.LENGTH_LONG).show()
        }

        val levelButton = findViewById<Button>(R.id.playground_toggleLevel)
        levelButton.setOnClickListener {
            if (curLevel==10) {
                curLevel=1
            }
            else {
                curLevel = curLevel!! +1
            }
            spawnHandler = NPCSpawnHandler(
                this,
                curLevel ?: 1,
                Handler()
            )
            levelButton.text = "Level " + curLevel
        }
        // Update current level view
        findViewById<Button>(R.id.playground_toggleLevel).text = "Level " + curLevel.toString()
    }

    private fun cancelUltBtnAnimations() {
        if (playground_killallBtn.animation != null && playground_serenityBtn.animation != null) {
            playground_killallBtn.animation.cancel()
            playground_serenityBtn.animation.cancel()
        }
    }

    private fun callFragment(fragmentName: String) {
        @Suppress("IMPLICIT_CAST_TO_ANY")
        val fragmentToGet = when (fragmentName) {
            "NextLevel" -> NextLevelFragment(supportFragmentManager)
            else -> GameOverFragment(true)
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.playground_main_menu_container, fragmentToGet as Fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun initHPRenderables() {
        val renderableFuturePlayer = ViewRenderable.builder()
            .setView(this, R.layout.healthbar)
            .build()
        renderableFuturePlayer.thenAccept {
            hpRenderablePlayer = it
            player.setHPRenderable(it)
            val hpBar = hpRenderablePlayer?.view?.textView_healthbar
            hpBar?.background =
                ContextCompat.getDrawable(this, R.drawable.playerhpbar)
        }
        val renderableFuture2 = ViewRenderable.builder()
            .setView(this, R.layout.ultimatebar)
            .build()
        renderableFuture2.thenAccept {
            ultRenderablePlayer = it
            player.setUltRenderable(it)
        }
    }

    private fun updateHpBarOrientations() {
        val forward = fragment.arSceneView.scene.camera.forward
        hpBarNodes.forEach {
            it.setLookDirection(Vector3(forward.x, forward.y + 0.25f, forward.z))
        }
    }

    // MARK: Testing-abilities-related stuff
    private fun attackTarget(cdData: ButtonCooldownData) {
        // disable attack button for the animation duration
        if (playerTarget != null) {
            if (!playerTarget!!.model.getStatus().isAlive) {
                clearPlayerTarget()
            } else {
                updatePlayerRotation()
                cdData.button.isEnabled = false
//                playground_attackDuckBtn.isEnabled = false
//                playground_attackDuckBtn_cd.isEnabled = true
                val ability = Ability.FBALL
                val animData =
                    ProjectileAnimationData(
                        this,
                        fragment,
                        ability.uri(),
                        gifRenderable = fireBallRenderable,
                        targetNode = playerTarget!!.node,
                        casterNode = playerNode
                    )
                doAsync {
                    doCooldown(cdData)
                }
                doAsync {
                    effectPlayerPlayer.playSound(R.raw.fireball)
                }

                cancelAnimator(player)
                animateCast(Ability.FBALL.getCastAnimationString()!!, renderedPlayer!!, player)
                player.useAbility(ability, playerTarget!!.model, animData) {
                    player.incrementAbilitiesUsed()
                    player.increaseUltProgress(ability.getDamage(player.getStatus()).toInt())
                    updateUltBar(player.getUltBar()?.view?.textView_ultbar, player)
                }
            }
        } else {
            lookForNewTarget(cdData)
        }
    }

    private fun gasTarget(cdData: ButtonCooldownData) {
        if (playerTarget != null) {
            if (!playerTarget!!.model.getStatus().isAlive) {
                clearPlayerTarget()
            } else {
                updatePlayerRotation()
                cdData.button.isEnabled = false
                val ability = Ability.DOT
                val animData =
                    ProjectileAnimationData(
                        this,
                        fragment,
                        ability.uri(),
                        gifRenderable = dotRenderable,
                        targetNode = playerTarget!!.node,
                        casterNode = playerNode
                    )
                doAsync {
                    doCooldown(cdData)
                }
                doAsync {
                    effectPlayerPlayer.playSound(R.raw.poison)
                }

                cancelAnimator(player)
                animateCast(Ability.DOT.getCastAnimationString()!!, renderedPlayer!!, player)
                player.useAbility(ability, playerTarget!!.model, animData) {
                    player.incrementAbilitiesUsed()
                    player.increaseUltProgress(ability.getDamage(player.getStatus()).toInt())
                    updateUltBar(player.getUltBar()?.view?.textView_ultbar, player)
                }
            }
        } else {
            lookForNewTarget(cdData)
        }
    }

    private fun getClosestNpc() : NPC? {
        var closest: NPC? = null
        var currentDiff: Float? = null
        npcsAlive.forEach {
            if (it.getStatus().isAlive) {
                val diff =
                    Vector3.subtract(playerNode.worldPosition, it.getNode().worldPosition).length()
                // alive check, to ignore dying NPCs
                if (closest == null) {
                    closest = it
                    currentDiff = diff
                } else {
                    if (currentDiff != null) {
                        if (diff < currentDiff!!) {
                            closest = it
                            currentDiff = diff
                        }
                    }
                }
            }
        }
        return closest
    }

    private fun lookForNewTarget(cdData: ButtonCooldownData) {
        if (npcsAlive.isNotEmpty()) {
            val closest = getClosestNpc()
            if (closest?.getHPBar() != null) {
                playerTarget = PlayerTargetData(
                    closest.getNode(),
                    closest, closest.getHPBar()!!.view.textView_healthbar)
                when (cdData.ability) {
                    Ability.BEAM -> beamTarget(playerTarget, cdData = cdData)
                    Ability.FBALL -> attackTarget(cdData)
                    Ability.ATK -> attackTarget(cdData)
                    Ability.DOT -> gasTarget(cdData)
                    else -> Log.d("NEWTAR", "New target found")
                }
            }
        } else {
            Toast.makeText(this, "You don't have a target", Toast.LENGTH_SHORT).show()
        }
    }

    private fun doCooldown(cdData: ButtonCooldownData) {
        Log.d("cooldown", "yes")
        var timePassed = 0.toLong()
        var cycle = 0.toLong()
        var update = true
        var cooldown = true
        while (cooldown) {
            if (update) {
            if (cdData.ability.getCooldown() - timePassed >= cycle) {
                    update = false
                    cdHandler.postDelayed({
                      runOnUiThread {
                          cdData.cdText.visibility = View.VISIBLE
                          cdData.cdText.text =
                                (((cdData.ability.getCooldown() - timePassed).toInt())/1000).toString()
                        }
                        if (cycle == 0.toLong()) {
                            cycle = 1000.toLong()
                        }
                        timePassed += cycle
                        update = true
                    }, cycle)
                } else {
                Log.d("cooldown", "WHILE OVER")

                cooldown = false
            }
            }
        }
        cdHandler.postDelayed({
        runOnUiThread {
            cdData.cdText.visibility = View.INVISIBLE
            cdData.cdText.text = ""
            cdData.button.isEnabled = true
        }
        }, cycle)
        }

    private fun cancelAnimator(cc: CombatControllable) {
        val animator = cc.getModelAnimator()
        if (animator != null) {
            if (animator.isRunning) {
                animator.cancel()
            }
        }
    }

    private fun attackPlayer(npc: NPC, node: Node) {
        val ability = Ability.FBALL
        val animData = ProjectileAnimationData(
            // TODO make start position relative to screen position
            this,
            fragment,
            ability.uri(),
            gifRenderable = fireBallRenderable,
            targetNode = playerNode,
            casterNode = node
        )
        //animateCast(npc.getType().attackAnimationString(), npc.model!!, npc)
        npc.useAbility(ability, player, animData) {
            updateHPBar(hpRenderablePlayer?.view?.textView_healthbar, player)
        }
    }

    private fun useBarrier(renderable: ViewRenderable? = null, cc: CombatControllable, cdData: ButtonCooldownData) {
        cancelAnimator(player)
        animateCast(Ability.SHIELD.getCastAnimationString()!!, renderedPlayer!!, player)
        player.incrementAbilitiesUsed()
        cdData.button.isEnabled = false
        doAsync { effectPlayerPlayer.playSound(R.raw.shield) }
        doAsync { doCooldown(cdData) }
        // TODO: Set renderable not nullable
        hpRenderablePlayer?.view?.textView_barrier?.visibility = View.VISIBLE
        cc.useShield()
    }

    // Beams a target npc and will call itself from that npc position if another npc nearby.
    // Caster node is player by default -- will be an npc node when called recursively.
    private fun beamTarget(npcData: PlayerTargetData?, subStartIdx: Int? = null,
        isRecursive : Boolean = false, caster: Node = playerNode, cdData: ButtonCooldownData) {
        if (npcData != null && npcData.model.getStatus().isAlive) {
            // Prevent indexOutOfBoundsException when calling recursively
            if (subStartIdx != null && subStartIdx + 1 > npcAnchors.size) return
            updatePlayerRotation()
            cdData.button.isEnabled = false
            val beam = Ability.BEAM
//            val attackAnimationData =
//                player.model?.getAnimationData(Ability.BEAM.getCastAnimationString())
            val data = ProjectileAnimationData(
                this,
                fragment,
                beam.uri(),
                abilityRenderable = beamRenderable,
                targetNode = npcData.node,
                casterNode = caster
            )
            if (!isRecursive) {
                animateCast(Ability.BEAM.getCastAnimationString()!!, renderedPlayer!!, player)
                doAsync { effectPlayerPlayer.playSound(R.raw.beam) }
                doAsync { doCooldown(cdData) }
            }
            player.useAbility(beam, npcData.model, data) {
                player.incrementAbilitiesUsed()
                player.increaseUltProgress(beam.getDamage(player.getStatus()).toInt())
                updateUltBar(player.getUltBar()?.view?.textView_ultbar, player)
            }
            // split the list if the function is called recursively, so it won't loop infinitely
            val subList = npcAnchors.subList(subStartIdx ?: 0, npcAnchors.size)
            subList.forEach {
                val difference = Vector3.subtract(
                    it.anchorNode.worldPosition, npcData.node.worldPosition).length()
                Log.d("BEAM", "RESULT " + npcAnchors.indexOf(it) + "  " + difference)

                if (difference < 20  && subList.size > 1) {
                    beamTarget(
                        PlayerTargetData(
                            it.npc.getNode(),
                            it.npc,
                            it.npc.getHPBar()?.view?.textView_healthbar),
                        npcAnchors.indexOf(it) + 1, isRecursive = true, caster = npcData.node, cdData = cdData)
                    Log.d("BEAM", "HIT NPC  " + npcAnchors.indexOf(it))
                    //it.anchorNode.localScale = Vector3(0.4f, 0.4f, 0.4f)
                }
            }
        } else {
            if (!isRecursive) {
                lookForNewTarget(cdData)
            }
        }
    }

    private fun animateCast(abilityStr: String, rend: ModelRenderable, cc: CombatControllable, repeatCount: Int = 0) { //TODO: callback to return the animator so can cancel
        val animData
                = rend.getAnimationData(abilityStr)
        val animator = ModelAnimator(animData, rend)
        Log.d("WALK", "repeat: " + repeatCount.toString() )
        animator.repeatCount = repeatCount
        val currentAnimator = cc.getModelAnimator()
        if (currentAnimator != null) {
            if (currentAnimator.isRunning) currentAnimator.end()
        }
        cc.setModelAnimator(animator)
        animator.start()
    }

    private fun teleportPlayer(cdData: ButtonCooldownData) {
        cdData.button.isEnabled = false
        player.incrementAbilitiesUsed()
        fragment.setOnTapArPlaneListener{ hitResult, _, _ ->
            player.restoreHealth(player.getMaxHealth() * AbilityModifier.getPwrModifier(Ability.TELEPORT))
            doAsync { effectPlayerPlayer.playSound(R.raw.swoosh) }
            val newAnchor = hitResult.createAnchor()
            val node = AnchorNode(newAnchor)
            val objectAnimation = ObjectAnimator()
            cancelAnimator(player)
            animateCast(Ability.TELEPORT.getCastAnimationString()!!, renderedPlayer!!, player)
            Handler().postDelayed({
                objectAnimation.setAutoCancel(true)
                objectAnimation.target = playerNode
                objectAnimation.setObjectValues(playerNode.worldPosition, node.worldPosition)
                objectAnimation.setPropertyName("worldPosition")
                objectAnimation.setEvaluator(Vector3Evaluator())
                objectAnimation.interpolator =LinearInterpolator()
                objectAnimation.duration = 0
                objectAnimation.start()
            }, CAST_TIME)
            doAsync { doCooldown(cdData) }
            forceStop = true
            fragment.setOnTapArPlaneListener(null)
            npcAnchors.forEach {
                if (it.npc.getType() == NPCType.MELEE) {
                    attackInitializer(it.npc.getType(),it.npc,
                        it.anchorNode.children[0] as TransformableNode
                    )
                }
            }
        }
    }

    private fun clearPlayerTarget() {
        playerTarget = null
    }

    // The player is currently the only one who has an ultimate
    private fun createUltBarPlayer(node: TransformableNode, renderable: ViewRenderable?) {
        val ultNode = Node()
        hpBarNodes.add(ultNode)
        ultNode.setParent(node)
        ultNode.renderable = renderable
        ultNode.localScale = Vector3(4f / 1.25f, 2.85f / 1.5f, 2.85f / 1.25f)
        ultNode.localPosition = Vector3(0f, 3.4f, 0f)
    }

    // MARK: Testing-abilities-related stuff
    private fun createHPBar(node: TransformableNode, renderable: ViewRenderable?) {
        val hpNode = Node()
        hpBarNodes.add(hpNode)
        hpNode.setParent(node)
        hpNode.renderable = renderable
        renderable?.isShadowCaster = false

        if (node == playerNode) {
            hpNode.localScale = Vector3(4f / 1.25f, 4f / 1.5f, 4f / 1.25f)
            hpNode.localPosition = Vector3(0f, 3.5f, 0f)
        } else {
            hpNode.localScale = Vector3(6f / 1.25f, 6f / 1.25f, 6f / 1.25f)
            hpNode.localPosition = Vector3(0f, 4f, 0f)
        }
    }

    private fun clearModels() {
        for (anchor in anchorList) {
            removeAnchorNode(anchor)
        }
        for (anchor in npcAnchors) {
            removeAnchorNode(anchor.anchorNode)
        }
        spawnedNPCs.clear()
        anchorList.clear()
        npcAnchors.clear()
        npcsAlive.clear()
        allNPChaveSpawned = false
        ducksInScene = false
        playerInScene = false
    }

    private fun updatePlayerRotation() {
        if (playerTarget != null) {
            val playerPos = playerNode.worldPosition
            val targetPos: Vector3
            if (playerTarget!!.node.children.isNotEmpty()) {
                targetPos = playerTarget!!.node.children[0].worldPosition
            } else {
                targetPos = playerTarget!!.node.worldPosition
            }
            playerNode.localRotation = AnimationAPI.calculateNewRotation(playerPos, targetPos)
        }
    }

    private fun updateNpcRotation(npcNode: TransformableNode) {
        if (player.getStatus().isAlive) {
            val q1 = AnimationAPI.calculateNewRotation(npcNode.worldPosition, playerNode.worldPosition)
            val q2 = Quaternion.axisAngle(Vector3(0f, 1f, 0f), -43f)
            val rotation = Quaternion.multiply(q1, q2)
            npcNode.localRotation = rotation
        }
    }

    private fun mainSpawner(spawnable: Any) {
        Log.d("mainSpawner", "CALLED")

        // Init vars
        var ableToSpawn = false
        lateinit var render: ModelRenderable
        lateinit var hpRenderable: ViewRenderable
        lateinit var anchor: Anchor
        val valuePool = (200..1200)
        val randX = valuePool.shuffled().first().toFloat()
        val randY = valuePool.shuffled().first().toFloat()

        // Spawn initializers
        if (spawnable is Player && !playerInScene && renderedPlayer != null
            && hpRenderablePlayer != null
        ) {
            playerInScene = true
            render = renderedPlayer!!
            render.isShadowCaster = false
            hpRenderable = hpRenderablePlayer!!
            ableToSpawn = true
        }
        if (spawnable is NPC) {
            val ids = spawnedNPCs.filter { it.getID() == spawnable.getID() }
            val checkAnchors = npcAnchors.filter { it.npc.getID() == spawnable.getID() }
            if (ids.isEmpty() && checkAnchors.isEmpty()) {
                render = spawnable.model!!
                render.isShadowCaster = false
                spawnedNPCs.add(spawnable)
                ableToSpawn = true
            }
        }

        // When all necessary components are ready, proceed with spawn
        if (ableToSpawn) {
            val frame = fragment.arSceneView.arFrame
            val pt = getScreenCenter()
            val hits: List<HitResult>
            if (frame != null) {
                hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat() + 400)
                var trackableWasPlane = false
                for (hit in hits) {
                    if (!trackableWasPlane) {
                        val trackable = hit.trackable
                        if (trackable is Plane) {
                            fragment.arSceneView.planeRenderer.isVisible = false
                            trackableWasPlane = true
                            //playerAnchorPos = hit!!
                            when (spawnable) {
                                is Player -> {
                                    anchor = (frame.hitTest(
                                        pt.x.toFloat(),
                                        (pt.y.toFloat() * 0.75).toFloat()
                                    ))[0].createAnchor()
                                }
                                else -> {
                                    anchor = (frame.hitTest(
                                        (pt.x.toFloat() - randX),
                                        (pt.y.toFloat() + randY)
                                    ))[0].createAnchor()
                                }
                            }
                            val anchorNode = AnchorNode(anchor)
                            anchorList.add(anchorNode)
                            anchorNode.setParent(fragment.arSceneView.scene)
                            val node = TransformableNode(fragment.transformationSystem)
                            node.localScale = Vector3(0.15f, 0.15f, 0.15f)
                            anchorNode.setLookDirection(Vector3.forward())
                            node.scaleController.isEnabled = false
                            node.rotationController.isEnabled = false
                            node.translationController.isEnabled = false
                            node.setParent(anchorNode)
                            node.renderable = render
                            if (spawnable is Player) {
                                node.localScale = Vector3(0.07f, 0.07f, 0.07f)
                                //PLAYER POST SPAWN CODE
                                val animData= render.getAnimationData("AlienArmature|Alien_Idle")
                                val animator = ModelAnimator(animData, render)
                                player.setModelAnimator(animator)
                                animator.start()
                                playerNode = node
                                playerAnchorNode = anchorNode
                                createHPBar(node, hpRenderable)
                                createUltBarPlayer(node, ultRenderablePlayer)
//                                playground_shieldDuckBtn.isEnabled = false
//                                playground_teleportDuckBtn.isEnabled = false
//                                playground_beamDuckBtn.isEnabled = false
//                                doAsync { doCooldown(playground_teleportDuckBtn_cd, Ability.TELEPORT.getCooldown(), playground_teleportDuckBtn) }
//                                doAsync { doCooldown(playground_beamDuckBtn_cd, Ability.BEAM.getCooldown(), playground_beamDuckBtn) }
//                                doAsync { doCooldown(playground_shieldDuckBtn_cd, Ability.SHIELD.getCooldown(), playground_shieldDuckBtn) }
                                node.setOnTouchListener { _, _ ->
                                    val oldPosition = playerNode.worldPosition
                                    Handler().postDelayed({
                                        if (playerNode.worldPosition != oldPosition)
                                            updatePlayerRotation()
                                    }, 500)
                                }
                            }
                            if (spawnable is NPC) {
                                // NPC POST SPAWN CODE
                                val renderableFutureNPC = ViewRenderable.builder()
                                    .setView(this, R.layout.healthbar)
                                    .build()
                                renderableFutureNPC.thenAccept {
                                    hpRenderableNPC = it
                                    hpRenderable = it
                                    spawnable.setHPRenderable(hpRenderable)
                                    createHPBar(node, hpRenderable)
                                    Log.d("mainSpawner", "BUILT")
                                }
                                npcAnchors.add(
                                    NPCAnchorData(
                                        anchorNode,
                                        spawnable
                                    )
                                )
                                spawnable.setNode(node)
                                npcsAlive.add(spawnable)
                                playground_targetTxt.text = "Enemies alive ${npcsAlive.size}"
                                node.localScale = Vector3(0.05f, 0.05f, 0.05f)
                                if (spawnable.getID() == 100) {
                                    node.localScale = Vector3(0.6f, 0.6f, 0.6f)
                                }

                                val newTargetNode = randomMove(node)
                                animateCast(spawnable.getType().walkAnimationString(), spawnable.model!!, spawnable, repeatCount = 100)
                                moveToTarget(node, newTargetNode, spawnable)
                                attackInitializer(spawnable.getType(), spawnable, node)
                                node.setOnTouchListener { _, _ ->
                                    val oldPosition = node.worldPosition
                                    Handler().postDelayed({
                                        if (node.worldPosition != oldPosition) {
                                            if (playerTarget != null) {
                                                val playerTargetNode = playerTarget!!.node
                                                if (playerTargetNode != node) {
                                                    val playerTargetBar = playerTarget!!.healthBar!!
                                                    updateOldTargetHPBar(playerTargetBar)
                                                }
                                            }
                                            // set new target
                                            val newTarget =
                                                PlayerTargetData(
                                                    node,
                                                    spawnable,
                                                    hpRenderable.view.textView_healthbar
                                                )
                                            playerTarget = newTarget
                                            player.dealDamage(5.0,spawnable)
                                            //doAsync { effectPlayerNPC.playSound(R.raw.gothit) }
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
        } else {
            Log.e(
                "mainSpawner",
                "Impossible spawn type or trying to spawn player twice. SPAWNTYPE: "
                        + spawnable.toString() + " PLAYERINSCENE: " + playerInScene.toString()
            )
        }
    }

    private fun spawnObjects() {
        if (!ducksInScene && !spawnHandler.isRunning()) {
            // "Spawn NPC's"
            npcSpawnThread = Thread {
                spawnHandler.run()
            }
            npcSpawnThread.start()
            ducksInScene = true
            val dataCount = NPCDataForLevels.getNPCForLevelCount(curLevel!!)
            updateNPCRemainingText("Enemies spawning: $dataCount")
        }
    }

    private fun updateHPBar(tv: TextView?, model: CombatControllable) {
        val parent = tv?.parent as View
        val ratio = model.getStatus().currentHealth / model.getStatus().maxHealth
        //val layOutParams = LinearLayout.LayoutParams((parent.width * ratio).toInt(), matchParent)
        val layOutParams = FrameLayout.LayoutParams((parent.width * ratio).toInt(), matchParent)
        val currentMargin = tv.marginEnd
        tv.layoutParams = layOutParams
        layOutParams.setMargins(currentMargin, currentMargin, currentMargin, currentMargin)
    }

    private fun updateUltBar(tv: TextView?, player: Player) {
        val parent = tv?.parent as View
        val ratio = (player.getUltProgress().toFloat() / player.getMaxUlt().toFloat())
        //val layOutParams = LinearLayout.LayoutParams((parent.width * ratio).toInt(), matchParent)
        val layOutParams = FrameLayout.LayoutParams((parent.width * ratio).toInt(), matchParent)
        val currentMargin = tv.marginEnd
        tv.layoutParams = layOutParams
        layOutParams.setMargins(currentMargin, currentMargin, currentMargin, currentMargin)
        if (ratio == 1f) {
            playground_ultimateBar.visibility = View.VISIBLE
            playground_killallBtn.animation = AnimationUtils.loadAnimation(this, R.anim.pulse_ultimate)
            playground_serenityBtn.animation = AnimationUtils.loadAnimation(this, R.anim.pulse_ultimate)
        }
    }

    private fun updateShieldBar(tv: GifImageView?, model: CombatControllable) {
        val parent = tv?.parent as View
        val ratio = model.getStatus().shieldAmount / model.getStatus().maxShieldAmount
        val layoutParams: FrameLayout.LayoutParams
        if (model.getStatus().shieldAmount == 0.0) {
            layoutParams = FrameLayout.LayoutParams((parent.width ), matchParent)
            tv.visibility = View.INVISIBLE
        } else {
            layoutParams = FrameLayout.LayoutParams((parent.width * ratio).toInt(), matchParent)
        }
        val currentMargin = tv.marginEnd
        tv.layoutParams = layoutParams
        layoutParams.setMargins(currentMargin, currentMargin, currentMargin, currentMargin)
        Log.d("SHIELD", "shield amount: ${model.getStatus().shieldAmount}")
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

    private fun randomMove(node: TransformableNode): Node {
        return when ((1..100).shuffled().first()) {
            in 1..10 -> NodeCreator(node, Vector3(0.9f, 0.0f, 0.0f))
            in 11..30 -> NodeCreator(node, Vector3(0.8f, 0.0f, -1.0f))
            in 31..60 -> NodeCreator(node, Vector3(0.6f, 0.0f, -1.0f))
            in 61..100 -> NodeCreator(node, Vector3(0.4f, 0.0f, 0.0f))
            else -> {
                wtf("WTF", "Impossible randomInt in randomMove")
                NodeCreator(node, Vector3(0.9f, 0.0f, 0.0f))
            }
        }
    }

    private fun NodeCreator(
        initialNode: TransformableNode,
        newLocation: Vector3
    ): Node {
        val newNode = Node()
        val summedVector = Vector3.add(initialNode.worldPosition, newLocation)
        newNode.worldPosition = summedVector
        return newNode
    }

    private fun moveToTarget(npcNode: TransformableNode, targetNode: Node, npc: NPC){
        // Move to previously randomized location
        val objectAnimation = ObjectAnimator()
        objectAnimation.setAutoCancel(true)
        objectAnimation.target = npcNode
        npcNode.setLookDirection(targetNode.worldPosition)
        objectAnimation.setObjectValues(npcNode.worldPosition, targetNode.worldPosition)
        objectAnimation.setPropertyName("worldPosition")
        objectAnimation.setEvaluator(Vector3Evaluator())
        objectAnimation.interpolator = LinearInterpolator()
        objectAnimation.duration = 5000
        objectAnimation.start()
        animateCast(npc.getType().walkAnimationString(), npc.model!!, npc, repeatCount = 100)
    }

    fun attackInitializer(type: NPCType, npc: NPC, npcNode: TransformableNode) {
        // Move to player and start attacking. Movement and attack pattern are received from NPCTYPE
        Handler().postDelayed({
            val objectAnimation = ObjectAnimator()
            objectAnimation.setAutoCancel(true)
            objectAnimation.target = npcNode
            updateNpcRotation(npcNode)
            val stopX = playerNode.worldPosition.x
            val stopZ = playerNode.worldPosition.z
            val startX = npcNode.worldPosition.x
            val startZ = npcNode.worldPosition.z
            when (type) {
                NPCType.MELEE -> {
                    val valuePool =
                        floatArrayOf(-0.14f,-0.12f, -0.10f, -0.08f, -0.06f, 0.06f, 0.08f, 0.10f, 0.12f,0.14f)
                    val randomifier = valuePool[(0..9).shuffled().first()]
                    val newX = (stopX + randomifier)
                    val stop = Vector3(
                        newX,
                        playerNode.worldPosition.y,
                        playerNode.worldPosition.z
                    )
                    objectAnimation.setObjectValues(npcNode.worldPosition, stop)
                    objectAnimation.duration = 5000
                    Handler().postDelayed({
                        // Attack until attacker or target is dead
                        attackLooperMelee(npc, npcNode)
                        animateCast(npc.getType().idleAnimationString(), npc.model!!, npc, repeatCount = 100)
                    }, 5000)
                }
                else -> {
                    val newX = ((stopX + startX) / 2)
                    val newZ = ((stopZ + startZ) / 2)
                    val stop = Vector3(newX, npcNode.worldPosition.y, newZ)
                    objectAnimation.setObjectValues(npcNode.worldPosition, stop)
                    objectAnimation.duration = 2500
                    // Attack until attacker or target is dead
                    attackLooper(npc, npcNode)
                }
            }
            objectAnimation.setPropertyName("worldPosition")
            objectAnimation.setEvaluator(Vector3Evaluator())
            objectAnimation.interpolator = LinearInterpolator()
            objectAnimation.start()
        }, 5200)
    }

    fun attackLooper(npc: NPC, model: TransformableNode) {
        var cooldown = false
        val thread = Thread {
            while (npc.getStatus().isAlive && player.getStatus().isAlive && !threadStop) {
                if (!cooldown) {
                    cooldown = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (!threadStop) {
                        updateNpcRotation(model)
                        attackPlayer(npc, model)
                        cooldown = false
                    }
                    }, 7100)
                    animateCast(npc.getType().idleAnimationString(), npc.model!!, npc, repeatCount = 100)
                }
            }
        }
        threadList.add(thread)
        thread.start()
    }

    fun attackLooperMelee(npc: NPC, model: TransformableNode) {

        // TODO: Redo properly with ability and animation
        forceStop = false
        var cooldown = false
        val thread = Thread {
            while (npc.getStatus().isAlive && player.getStatus().isAlive && !forceStop && !threadStop) {
                if (!cooldown) {
                    cooldown = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (!threadStop) {
                                val animDataStr = npc.getType().attackAnimationString()
                                cancelAnimator(npc)
                                updateNpcRotation(model)
                                animateCast(animDataStr, npc.model!!, npc)
                                npc.dealDamage(100.0, player)
                            }
                        }, 1500)
                        animateCast(npc.getType().idleAnimationString(), npc.model!!, npc, repeatCount = 100)
                        cooldown = false
                    }, 6000)
                }
            }
        }
        threadList.add(thread)
        thread.start()
    }

    override fun notifyNPCSpawned(type: NPCType, remaining: Int, npcID: Int) {
        val ids = spawnedNPCs.filter { it.getID() == npcID }
        val anchors = npcAnchors.filter { it.npc.getID() == npcID }
        if (ids.isEmpty() && anchors.isEmpty()) {
            lateinit var renderable: ModelRenderable
            // get NPC model
            val renderableFuture = ModelRenderable.builder()
                .setSource(this, type.modelUri())
                .build()
            renderableFuture.thenAccept {
                renderable = it
                // create the NPC object when we have a ModelRenderable ready
                val npcObject = type.getNPCObject(curLevel!!, renderable, npcID, this)
                // spawn NPC
                mainSpawner(npcObject)
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
    }

    override fun notifyAllNPCSpawned() {
        spawnHandler.stop()
        Handler().removeCallbacks(spawnHandler)
        allNPChaveSpawned = true
    }

    private fun updateOldTargetHPBar(hpBar: TextView) {
        val parent = hpBar.parent as View
        parent.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.healthbarbg
            )
    }

    private fun updateNewTargetHPBar(targetData: PlayerTargetData) {
        val parent = targetData.healthBar?.parent as View
        parent.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.hpbarbgselected
            )
    }

    private fun updateNPCRemainingText(text: String) {
        playground_remaining.text = text
    }

    override fun onAbilityCast(
        caster: CombatControllable,
        target: CombatControllable,
        ability: Ability
    ) {
        // create cast animation here
        // TODO: caster.model!!.getAnimationData("")
    }

    override fun onAbilityHit(
        caster: CombatControllable,
        target: CombatControllable,
        ability: Ability
    ) {
        // create being hit animation here
        // TODO: target.model!!.getAnimationData("")
    }

    override fun onCCDamaged(cc: CombatControllable) {
        Log.d("CCDMG", "isShielded: ${cc.getStatus().isShielded} amount: ${cc.getStatus().shieldAmount}")
        if (cc == player) {
            if (cc.getStatus().isShielded) {
                updateShieldBar(hpRenderablePlayer?.view?.textView_barrier, player)
            } else {
                // for clearing the view
                // TODO: come up with a smarter implementation
                if (player.getHPBar()?.view?.textView_barrier?.visibility == View.VISIBLE) {
                    updateShieldBar(hpRenderablePlayer?.view?.textView_barrier, player)
                }
                updateHPBar(hpRenderablePlayer?.view?.textView_healthbar, player)
                doAsync { effectPlayerPlayer.playSound(R.raw.gothit) }
            }
        } else {
            if (cc is NPC) {
                if (cc.getStatus().isShielded) {
                    updateShieldBar(cc.getHPBar()?.view?.textView_barrier, cc)
                } else {
                    Log.d("CCDMG", "NPC damaged. ID: ${cc.getID()}")
                    if (cc.getHPBar() != null) {
                        updateHPBar(cc.getHPBar()!!.view.textView_healthbar, cc)
                    }
                }
                animateCast(cc.getType().hitAnimationString(), cc.model!!, cc)
                Handler().postDelayed({
                    animateCast(cc.getType().idleAnimationString(), cc.model, cc)
                },900)
                doAsync { effectPlayerNPC.playSound(R.raw.gothit) }
            }
        }
    }

    // Adds 1 to numberOfGames and updates user high score if needed
    private fun newHighScore(score: Int) {
        Log.d("POINTS", "user id: $userId")
        if (userId != null) {
            val db = AppDatabase.get(this).userDao()
            getUser(db, userId!!) {
                val savedScore = saver.getInt("SCORE", 0)
                val totalScore = score + savedScore
                doAsync {
                    // only increment number of games if player is dead
                    if (!player.getStatus().isAlive) db.incrementNumOfGames(userId!!)
                    Log.d(
                        "POINTS", "totalScore ($totalScore) > it.highScore " +
                                "(${it.highScore} : ${totalScore > it.highScore}"
                    )
                    if (totalScore > it.highScore) {
                        db.updateHighScore(totalScore, it.id)
                        NetworkAPI.executeIfConnected(this@GameActivity) {
                            postNewHighScore(userId!!, totalScore)
                        }
                        uiThread {
                            Toast.makeText(
                                this@GameActivity,
                                "NEW HIGHSCORE!\nScore: $totalScore",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun getUser(db: UserDao, id: Int, cb: (User) -> Unit) {
        doAsyncResult {
            val user = db.getUser(id)
            onComplete {
                cb(user)
            }
        }
    }

    override fun onCCDeath(cc: CombatControllable) {
        val totalNpcCount = NPCDataForLevels.getNPCForLevelCount(curLevel!!)
        doAsync { effectPlayerNPC.playSound(R.raw.gothit) }
        Log.d(
            "NPCDED",
            "curLevel: $curLevel LevelOne.size: ${NPCDataForLevels.getNPCForLevelCount(curLevel!!)}"
        )
        val npcsRemaining = totalNpcCount - spawnedNPCs.size
        if (cc == player) {
            doAsyncResult {
                newHighScore(player.calculateScore())
                uiThread {
                    animateCast("AlienArmature|Alien_Death", renderedPlayer!!, player)
                }
                onComplete {
                    saver.edit().putInt("SCORE", 0).apply()
                    player.clearStatus()
                    uiThread {
                        Toast.makeText(this@GameActivity, "YOU DIED", Toast.LENGTH_LONG)
                            .show()
                    }
                    Handler().postDelayed ({
                        saver.edit().putInt("skillLevel", 0).apply()
                        curLevel = 1
                        uiThread {
                            callFragment("GameOver")
                        }
                    }, 3000)
                }
            }
        } else {
            if (cc is NPC) {
                player.addPoints(cc.getStatus().maxHealth.toInt())
                npcsAlive.forEach {
                    if (cc == it) {
                        val anchor = npcAnchors[npcsAlive.indexOf(it)]
                        // check that the correct anchor was indeed picked
                        if (anchor.npc.getID() == it.getID()) {
                            val hpBar = cc.getHPBar()
                            updateHPBar(hpBar!!.view.textView_healthbar, cc)
                            hpBar.view?.visibility = View.GONE
                            val node = anchor.anchorNode.children[0]
                            //node.localRotation = Quaternion(0f, 0f, 1f, 0f)
                            animateCast(cc.getType().deathAnimationString(), cc.model!!, cc)

                            if (node is TransformableNode) {
                                node.translationController.isEnabled = false
                            }
                        }
                        Handler().postDelayed({
                            // Remove the NPC from scene after a delay
                            // When using many abilities at the same time,
                            // the first one might already kill the target.
                            // That is why we need to check if the target
                            // still exists when receiving another callback.
                            if (npcsAlive.indexOf(it) >= 0) {
//                                if (cc.getNode() == playerTarget?.node) {
//                                    clearPlayerTarget()
//                                }
                                if (npcAnchors.size >= npcsAlive.indexOf(it)) {
                                    npcAnchors.removeAt(npcsAlive.indexOf(it))
                                    npcsAlive.removeAt(npcsAlive.indexOf(it))
                                }
                            }
                            // Level completed!
                            if (npcAnchors.size == 0 && npcsRemaining == 0) {
                                doAsyncResult {
                                    newHighScore(player.calculateScore())
                                    saver.edit().putInt("SCORE", player.calculateScore()).apply()
                                    uiThread {
                                        Toast.makeText(
                                            this@GameActivity,
                                            "Score: ${player.calculateScore()}",
                                            Toast.LENGTH_LONG
                                        )
                                            .show()
                                        Log.d("CURLEVEL", curLevel.toString())
                                    }
                                    if (curLevel==10) {
                                        curLevel=1
                                    }
                                    else {
                                        if (curLevel != null) {
                                            curLevel = curLevel!! + 1
                                        }
                                    }
                                    saver.edit().putInt("levelNum", curLevel!!).apply()
                                    val skillPoints = saver.getInt("skillLevel", 0)
                                    saver.edit().putInt("skillLevel", (skillPoints + 1)).apply()
                                    Log.d("CURLEVEL", curLevel.toString())
                                    onComplete {
                                        callFragment("NextLevel") } }
                            }
                            playground_targetTxt.text = "Enemies alive ${npcsAlive.size}"
                            removeAnchorNode(anchor.anchorNode)
                        }, 3000)
                    }
                }
            }
        }
    }

    // UltimateHandler callback to tell whether the player succeeded in casting the ultimate
    override fun onMeasured(succeeded: Boolean, ability: PlayerUltimate) {
        if (succeeded) {
            player.ultUsed()
            playground_ultimateBar.visibility = View.GONE
            updateUltBar(player.getUltBar()?.view?.textView_ultbar, player)
            if (ability == PlayerUltimate.KILLALL) {
                npcsAlive.forEach {
                    val health = it.getStatus().maxHealth
                    player.dealDamage(health * 2, it)
                }
            } else {
                // Serenity heals player to full health and gives shield?
                player.restoreFullHealth()
                cancelAnimator(player)
                animateCast(Ability.SHIELD.getCastAnimationString()!!, renderedPlayer!!, player)
                player.incrementAbilitiesUsed()
                playground_shieldDuckBtn.isEnabled = false
                doAsync { effectPlayerPlayer.playSound(R.raw.shield) }
                hpRenderablePlayer?.view?.textView_barrier?.visibility = View.VISIBLE
                player.useShield()
                updateShieldBar(hpRenderablePlayer?.view?.textView_barrier, player)
                updateHPBar(hpRenderablePlayer?.view?.textView_healthbar, player)
            }
            // TODO: MAKE ULTIMATE BUTTONS INVISIBLE
        }
    }

    private fun postNewHighScore(uid: Int, score: Int) {
        if (userId != null) {
            try {
                val service =
                    RetrofitClientInstance.retrofitInstance?.create(HighscoreService::class.java)
                val body = NetworkAPI.HighScoreModel.PostHSBody(uid, score)
                val call = service?.newHighScore(body)
                call?.enqueue(object : Callback<ResponseBody> {
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Toast.makeText(
                            this@GameActivity,
                            "Error updating highscore",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.d("RETROFIT", "Error updating highscore: ${t.localizedMessage}")
                    }
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>) {
                        if (response.code() == 200) {
                            Log.d("RETROFIT", "New high score saved to back end")
                        } else {
                            Log.d("RETROFIT", "High score not saved into back end")
                        }
                    }
                })
            } catch (e: Error) {
                Log.d("RETROFIT", "Error updating highscore: ${e.localizedMessage}")
            }
        }
    }
}