package com.example.argame.Activities

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.preference.PreferenceManager
import com.example.argame.Fragments.Menu.MenuFragmentController
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.Model.Ability.Ability
import com.example.argame.Model.Ability.AbilityConverter
import com.example.argame.Model.Persistence.AppDatabase
import com.example.argame.Model.Persistence.Entities
import com.example.argame.R
import kotlinx.android.synthetic.main.menu_main.*
import org.jetbrains.anko.doAsync

class MainActivity : AppCompatActivity(), FragmentCallbackListener {

    private val menuFragController =
        MenuFragmentController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initMenuContainer()
        addTestStuffRoom()
    }



    private fun initMenuContainer() {
        supportFragmentManager.beginTransaction()
            .add(R.id.main_menu_container, menuFragController)
            .addToBackStack(null)
            .commit()
    }

    private fun addTestStuffRoom() {
        val test = AbilityConverter.fromAbility(Ability.TEST)
        val beam = AbilityConverter.fromAbility(Ability.BEAM)
        val teleport = AbilityConverter.fromAbility(Ability.TELEPORT)
        val shield = AbilityConverter.fromAbility(Ability.SHIELD)
        val context: Context = this
        val db = AppDatabase.get(context)
        doAsync {
            //db.userDao().insert(User(1, "mikael"))
            db.abilitiesDao().insertAbility(Entities.SelectableAbility(test))
            db.abilitiesDao().insertAbility(Entities.SelectableAbility(beam))
            db.abilitiesDao().insertAbility(Entities.SelectableAbility(teleport))
            db.abilitiesDao().insertAbility(Entities.SelectableAbility(shield))
            //db.abilitiesDao().selectAbility(test)
        }
    }

    // This is a callback fired from every menu's buttons
    override fun onButtonPressed(btn: Button) {
        // The callback's are forwarded to MenuFragmentController,
        // that handles all the logic for these events
        menuFragController.onButtonPressed(btn)
    }
}
