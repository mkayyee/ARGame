package com.example.argame.Fragments.Abilities

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.Model.Ability.AbilityConverter
import com.example.argame.Model.Persistence.AppDatabase
import com.example.argame.R
import kotlinx.android.synthetic.main.ability_item.view.*
import kotlinx.android.synthetic.main.activity_level_intermission.*
import kotlinx.android.synthetic.main.fragment_unselected_abilities.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.onComplete
import org.jetbrains.anko.uiThread

class AbilityMenuFragment(private val mContext: Context) : Fragment(),
    SelectedAbilitiesFragment.AbilitySelectCallback,
    UnselectedAbilitiesFragment.AbilitySelectCallback {

    private lateinit var fragmentCallbackListener: FragmentCallbackListener
    private lateinit var db: AppDatabase

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentCallbackListener = context as FragmentCallbackListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.activity_level_intermission, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button_close_ability_menu.setOnClickListener {
            //fragmentCallbackListener.onButtonPressed(it as Button)
            fragmentManager!!.popBackStack()
        }
        initFragments()
    }

    private fun initFragments() {
        db = AppDatabase.get(mContext)
        doAsync {
            val abilities = db.abilitiesDao().getAllAbilities()
            for (ability in abilities) {
                Log.d("ABILITY", AbilityConverter.toAbility(ability.abilityID).toString())
            }
            val unselected = db.abilitiesDao().checkUnselectedAbilities()
            for (ab in unselected) {
                Log.d("ABILITYUNSELECTED", AbilityConverter.toAbility(ab.abilityID).toString())
            }

        }
        val userID = 1 //TODO: Get from sharedprefs?
        val fragSelected = SelectedAbilitiesFragment(db, userID, this)
        val fragUnselected = UnselectedAbilitiesFragment(db, userID, this)
        fragmentManager!!.beginTransaction()
            .add(R.id.selected_abilities_container, fragSelected)
            .add(R.id.unselected_abilities_container, fragUnselected)
            .commit()
    }

    override fun onAbilityRemove(id: Int) {
        Log.d("ABILITY", "ability: ${AbilityConverter.toAbility(id).toString()} removed")
        doAsync {
            db.abilitiesDao().deselectAbility(id)
        }
    }

    override fun onAbilityAdd(id: Int) {
        Log.d("ABILITY", "ability: ${AbilityConverter.toAbility(id).toString()} added")
        disableButtons()
        doAsyncResult {
            db.abilitiesDao().selectAbility(id)
            onComplete {
                enableButtons()
                uiThread {
                    // TODO: if max amount of abilities, keep buttons disabled
                    enableButtons()
                }
            }
        }
    }

    private fun disableButtons() {
        if (recycler_unselected_abilities != null) {
            recycler_unselected_abilities.forEach {
                it.button_select?.isEnabled = false
            }
        }
    }

    private fun enableButtons() {
        if (recycler_unselected_abilities != null) {
            recycler_unselected_abilities.forEach {
                it.button_select?.isEnabled = true
            }
        }
    }
}