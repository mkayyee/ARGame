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
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.Model.Persistence.AppDatabase
import com.example.argame.Model.Persistence.User
import com.example.argame.Model.Persistence.UserDao
import com.example.argame.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_profile_nouser.*
import kotlinx.android.synthetic.main.profile.*
import kotlinx.android.synthetic.main.profile.profile_button_back
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.onComplete
import org.jetbrains.anko.uiThread

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
        return if (userId != -1 && !changeUser) {
            userExists = true
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
                profile_textView_games_played.text = it.numberOfGames.toString()
                if (it.avatar != null) {
                    profile_imageView_avatar.setImageURI(Uri.parse(it.avatar))
                }
                if (it.highScore != 0) {
                    profile_textView_highest_score.text = it.highScore.toString()
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
            doAsyncResult {
                newUser()
                onComplete {
                    uiThread {
                        // create user, reload fragment
                        val ft = fragmentManager!!.beginTransaction()
                        if (changeUser) changeUser = false
                        fragmentManager!!.popBackStack()
                        ft.detach(this@ProfileFragment)
                            .attach(this@ProfileFragment).commit() } } } }
    }

    private fun textInputListenerSetup() {
        input_username.addTextChangedListener {
            profile_button_create_user.isEnabled = !input_username.text.isNullOrEmpty()
        }
    }

    private fun newUser() {
        userDao.insert(User(1, input_username.text.toString()))
        prefs.edit().putInt("USER", 1).apply()
    }
}