package com.example.argame.Fragments.Menu

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.Model.Persistence.AppDatabase
import com.example.argame.Model.Persistence.User
import com.example.argame.Model.Persistence.UserDao
import com.example.argame.Networking.NetworkAPI
import com.example.argame.Networking.RetrofitClientInstance
import com.example.argame.Networking.UserService
import com.example.argame.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_profile_nouser.*
import kotlinx.android.synthetic.main.profile.*
import kotlinx.android.synthetic.main.profile.profile_button_back
import okhttp3.ResponseBody
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.onComplete
import org.jetbrains.anko.uiThread
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/***
 *  Fragment for the User profile.
 *
 *  Instantiated from MenuFragmentController
 */

class ProfileFragment(private var changeUser: Boolean = false) : Fragment() {

    private var buttonCallbackListener: FragmentCallbackListener? = null
    private var userExists = false
    private lateinit var prefs: SharedPreferences
    private lateinit var userDao: UserDao

    override fun onAttach(context: Context) {
        super.onAttach(context)
        buttonCallbackListener = context as FragmentCallbackListener
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        userDao = AppDatabase.get(context).userDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val userId = prefs.getInt("USER", -1)
        // if user id is valid and not changing user -> return Profile
        if (userId != -1) userExists = true
        return if (userId != -1 && !changeUser) {
            inflater.inflate(R.layout.profile, container, false)
        } else {
            inflater.inflate(R.layout.fragment_profile_nouser, container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtonListeners()
        val userId = prefs.getInt("USER", -1)
        if (userId != -1 && !changeUser) {
            getUser(userId) {
                Log.d("USER", "games played: ${it.numberOfGames}")
                profile_textView_username.text = it.username
                val gpText = "Games played: ${it.numberOfGames}"
                profile_textView_games_played.text = gpText
                if (it.avatar != null) {
                    profile_imageView_avatar.setImageURI(Uri.parse(it.avatar))
                }
                if (it.highScore != 0) {
                    val score = "High score: ${it.highScore}"
                    profile_textView_highest_score.text = score
                }
            }
        }
    }

    private fun getUser(id: Int, cb: (User) -> Unit) {
        doAsyncResult {
            val user = userDao.getUser(id)
            onComplete {
                cb(user)
            }
        }
    }

    private fun setupButtonListeners() {
        profile_button_back.setOnClickListener {
            //buttonCallbackListener!!.onButtonPressed(it as Button)
            fragmentManager!!.popBackStack()
        }
        if (!userExists || changeUser) {
            textInputListenerSetup()
            createUserBtnListener()
            if (changeUser) {
                textView_newUser.text = getString(R.string.change_user)
            }
        } else {
            btn_change_username.setOnClickListener {
                changeUsernameBtnListener()
            }
        }
    }

    private fun changeUsernameBtnListener() {
        val frag = ProfileFragment(true)
        fragmentManager!!.beginTransaction()
            .replace(R.id.main_menu_container, frag)
            .addToBackStack(null)
            .commit()
    }

    private fun createUserBtnListener() {
        profile_button_create_user.setOnClickListener {
            doAsync{
                // create user, reload fragment
                postUser {
                    if (it) {
                        uiThread {
                            val ft = fragmentManager!!.beginTransaction()
                            fragmentManager!!.popBackStack()
                            if (changeUser) {
                                changeUser = false}
                            if (userExists) {
                                ft.replace(R.id.main_menu_container, this@ProfileFragment)
                                    .addToBackStack(null)
                                    .commit()
                            } else {
                                val frag = ProfileFragment()
                                ft.replace(R.id.main_menu_container, frag)
                                    .addToBackStack(null)
                                    .commit()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun textInputListenerSetup() {
        input_username.addTextChangedListener {
            profile_button_create_user.isEnabled = !input_username.text.isNullOrEmpty()
        }
    }

    private fun newUser(id: Int, username: String, cb: () -> Unit) {
        doAsyncResult {
            if (userExists) {
                userDao.changeUsername(id, username)
            } else {
                userDao.insert(User(id, username))
            }
            prefs.edit().putInt("USER", id).apply()
            onComplete {
                cb()
            }
        }
    }

    private fun postUser(succeeded: (Boolean) -> Unit) {
        val id = prefs.getInt("USER", -1)
        if (id == -1) {
            postNewUser(succeeded)
        } else {
            postNewUsername(id, succeeded)
        }
    }

    private fun postNewUser(cb: (Boolean) -> Unit) {
        val service = RetrofitClientInstance.retrofitInstance?.create(UserService::class.java)
        val username = input_username.text.toString()
        val body = NetworkAPI.UserModel.NewUserBody(username)
        val call = service?.postNewUser(body)
        call?.enqueue(object: Callback<NetworkAPI.UserModel.ParseUserId> {
            override fun onFailure(call: Call<NetworkAPI.UserModel.ParseUserId>, t: Throwable) {
                Log.d("RETROFIT", "poserNewUser() failed. Reason: ${t.localizedMessage}")
                cb(false)
                Toast.makeText(context, "Network error", Toast.LENGTH_LONG).show()
            }
            override fun onResponse(
                call: Call<NetworkAPI.UserModel.ParseUserId>,
                response: Response<NetworkAPI.UserModel.ParseUserId>
            ) {
                val id = response.body()?.userid
                Log.d("RETROFIT", "User created. ID: ${response.body()?.userid}")
                if (id != null) {
                    newUser(id, username) {
                        cb(true)
                    }
                } else {
                    cb(false)
                    if (response.code() == 409) {
                        Toast.makeText(context, "Username taken", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Unknown error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun postNewUsername(id: Int, cb: (Boolean) -> Unit) {
        val service = RetrofitClientInstance.retrofitInstance?.create(UserService::class.java)
        val username = input_username.text.toString()
        val body = NetworkAPI.UserModel.NewUsernameBody(username, id)
        val call = service?.postNewUsername(body)
        call?.enqueue(object: Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.d("RETROFIT", "poserNewUser() failed. Reason: ${t.localizedMessage}")
                cb(false)
            }
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                val code = response.code()
                if (code == 200) {
                    Log.d("RETROFIT", "User updated. ID:")
                    newUser(id, username) {
                        cb(true)
                    }
                } else {
                    cb(false)
                    if (code == 409) {
                        Log.d("RETROFIT", "Username taken")
                        Toast.makeText(context, "Username taken", Toast.LENGTH_LONG).show()
                    } else {
                        Log.d("RETROFIT", "${response.code()}")
                        Toast.makeText(context, "Unknown error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}