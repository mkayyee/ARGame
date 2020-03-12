package com.example.argame.Fragments.Abilities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import com.example.argame.Activities.GameActivity
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.Model.Ability.AbilityConverter
import com.example.argame.Model.Persistence.AppDatabase
import com.example.argame.R
import kotlinx.android.synthetic.main.ability_item.view.*
import kotlinx.android.synthetic.main.activity_level_intermission.*
import kotlinx.android.synthetic.main.fragment_unselected_abilities.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.uiThread

/**
 *  The "select abilities" menu.
 *
 *  Listen's to callbacks from each of its fragments: (SelectedAbilitiesFragment,
 *  UnselectedAbilitiesFragment) and updates the database accordingly.
 *
 *  Both fragments have a LiveData reference to the abilities database, so
 *  they will be automatically updated.
 */

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
            //fragmentManager!!.popBackStack()
            launchGameActivity()
        }
        initFragments()
    }

    override fun onResume() {
        super.onResume()
        enableContinueIfPossible()
    }

    private fun enableContinueIfPossible() {
        doAsync {
            val count = db.abilitiesDao().getSelectedAbilitiesCount()
            uiThread {
                button_close_ability_menu.isEnabled = count >= 4
            }
        }
    }

    private fun launchGameActivity() {
        val intent = Intent(activity, GameActivity::class.java)
        startActivity(intent)
        fragmentManager!!.popBackStack()
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
        val fragSelected = SelectedAbilitiesFragment(db, this)
        val fragUnselected = UnselectedAbilitiesFragment(db, this)
        fragmentManager!!.beginTransaction()
            .add(R.id.selected_abilities_container, fragSelected)
            .add(R.id.unselected_abilities_container, fragUnselected)
            .commit()
    }

    override fun onAbilityRemove(id: Int) {
        Log.d("ABILITY", "ability: ${AbilityConverter.toAbility(id)} removed")
        doAsync {
            db.abilitiesDao().deselectAbility(id)
            enableContinueIfPossible()
        }
    }

    override fun onAbilityAdd(id: Int) {
        Log.d("ABILITY", "ability: ${AbilityConverter.toAbility(id)} added")
        disableButtons()
        doAsyncResult {
            db.abilitiesDao().selectAbility(id)
            enableContinueIfPossible()
        }
    }

    private fun disableButtons() {
        if (recycler_unselected_abilities != null) {
            recycler_unselected_abilities.forEach {
                it.button_select?.isEnabled = false
            }
        }
    }
}