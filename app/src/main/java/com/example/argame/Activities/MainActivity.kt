package com.example.argame.Activities

import android.accounts.NetworkErrorException
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.preference.PreferenceManager
import com.example.argame.Fragments.Menu.MenuFragmentController
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.Model.Ability.Ability
import com.example.argame.Model.Ability.AbilityConverter
import com.example.argame.Model.Persistence.AppDatabase
import com.example.argame.Model.Persistence.Entities
import com.example.argame.Networking.HighscoreService
import com.example.argame.Networking.NetworkAPI
import com.example.argame.Networking.RetrofitClientInstance
import com.example.argame.Networking.UserService
import com.example.argame.R
import okhttp3.ResponseBody
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.onComplete
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.Error

class MainActivity : AppCompatActivity(), FragmentCallbackListener {

    private lateinit var prefs: SharedPreferences
    private lateinit var connManager: ConnectivityManager
    private val menuFragController = MenuFragmentController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        initMenuContainer()
        addTestStuffRoom()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        getUserData()
    }

    private fun initMenuContainer() {
        supportFragmentManager.beginTransaction()
            .add(R.id.main_menu_container, menuFragController)
            .addToBackStack(null)
            .commit()
    }

    private fun addTestStuffRoom() {
        val test = AbilityConverter.fromAbility(Ability.FBALL)
        val beam = AbilityConverter.fromAbility(Ability.BEAM)
        val teleport = AbilityConverter.fromAbility(Ability.TELEPORT)
        val shield = AbilityConverter.fromAbility(Ability.SHIELD)
        val dot = AbilityConverter.fromAbility(Ability.DOT)
        val atk = AbilityConverter.fromAbility(Ability.ATK)
        val context: Context = this
        val db = AppDatabase.get(context)
        // TODO: only do this if user does not exist
        doAsync {
            //db.userDao().insert(User(1, "mikael"))
            db.abilitiesDao().insertAbility(Entities.SelectableAbility(test))
            db.abilitiesDao().insertAbility(Entities.SelectableAbility(beam))
            db.abilitiesDao().insertAbility(Entities.SelectableAbility(teleport))
            db.abilitiesDao().insertAbility(Entities.SelectableAbility(shield))
            db.abilitiesDao().insertAbility(Entities.SelectableAbility(dot))
            //db.abilitiesDao().insertAbility(Entities.SelectableAbility(atk))
            //db.abilitiesDao().selectAbility(test)
        }
    }

    // This is a callback fired from every menu's buttons
    override fun onButtonPressed(btn: Button) {
        // The callback's are forwarded to MenuFragmentController,
        // that handles all the logic for these events
        menuFragController.onButtonPressed(btn)
    }

    // Get user data from the back end if user id in preferences is not null.
    // Updates a high score if local high score is higher than data from the server,
    // and also updates the user object in sharedPreferences to match the retrieved data.
    private fun getUserData() {
        val id = prefs.getInt("USER", -1)
        if (id != -1) {
            val db = AppDatabase.get(this).userDao()
            doAsyncResult {
                val user = db.getUser(id)
                onComplete {
                    NetworkAPI.executeIfConnected(this@MainActivity) {
                        try {
                            val service =
                                RetrofitClientInstance.retrofitInstance?.create(UserService::class.java)
                            val call = service?.getUser(id)
                            call?.enqueue(object : Callback<NetworkAPI.UserModel.GetUserResponse> {
                                override fun onFailure(
                                    call: Call<NetworkAPI.UserModel.GetUserResponse>,
                                    t: Throwable
                                ) {
                                    Log.d(
                                        "RETROFIT",
                                        "Couldn't get userdata. Reason: ${t.localizedMessage}"
                                    )
                                }

                                override fun onResponse(
                                    call: Call<NetworkAPI.UserModel.GetUserResponse>,
                                    response: Response<NetworkAPI.UserModel.GetUserResponse>
                                ) {
                                    val res = response.body()
                                    if (res != null) {
                                        if (res.score != null) {
                                            Log.d(
                                                "RETROFIT",
                                                "user.highScore: ${user.highScore} res.Score: ${res.score}"
                                            )
                                            if (user.highScore > res.score) {
                                                postNewHighScore(id, user.highScore)
                                            }
                                        } else {
                                            if (user.highScore > 0) {
                                                postNewHighScore(id, user.highScore)
                                            }
                                        }
                                    }
                                }
                            })
                        } catch (error: Error) {
                            "Couldn't get userdata. Reason: ${error.localizedMessage}"
                        }
                    }
                }
            }
        }
    }

    private fun postNewHighScore(uid: Int, score: Int) {
        try {
            val service =
                RetrofitClientInstance.retrofitInstance?.create(HighscoreService::class.java)
            val body = NetworkAPI.HighScoreModel.PostHSBody(uid, score)
            val call = service?.newHighScore(body)
            call?.enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.d("RETROFIT", "Error updating highscore: ${t.localizedMessage}")
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    Log.d("RETROFIT", "New high score saved to back end")
                }
            })
        } catch (e: Error) {
            Log.d("RETROFIT", "Error updating highscore: ${e.localizedMessage}")
        }
    }

}
