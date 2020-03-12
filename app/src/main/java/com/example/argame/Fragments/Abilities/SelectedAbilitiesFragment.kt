package com.example.argame.Fragments.Abilities

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.argame.Model.Ability.AbilityConverter
import com.example.argame.Model.Ability.AbilityModifier
import com.example.argame.Model.Persistence.AbilitiesLiveModel
import com.example.argame.Model.Persistence.AppDatabase
import com.example.argame.Model.Persistence.Entities
import com.example.argame.R
import com.google.android.material.button.MaterialButton
import kotlinx.android.synthetic.main.ability_item.view.*
import kotlinx.android.synthetic.main.fragment_selected_abilities.*
import androidx.preference.PreferenceManager
import org.jetbrains.anko.forEachChild

/**
*  The top fragment of the "select abilities" menu,
*  and the Adapter class for its recycler view.
*
*  Uses live data from room to update the recycler view.
*/

class SelectedAbilitiesFragment(
    private val database: AppDatabase, private val mContext: AbilityMenuFragment) : Fragment() {

    interface AbilitySelectCallback {
        fun onAbilityRemove(id: Int)
    }

    private lateinit var db: AppDatabase
    private lateinit var selectItemListener: AbilitySelectCallback
    private lateinit var con: ViewGroup

    override fun onAttach(context: Context) {
        super.onAttach(context)
        selectItemListener = mContext
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (container != null) {
            con = container
        }
        return layoutInflater.inflate(R.layout.fragment_selected_abilities, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        con = view.parent as ViewGroup
        db = database
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val abilitiesLiveModel
                = ViewModelProviders.of(this).get(AbilitiesLiveModel::class.java)
        abilitiesLiveModel.getSelectedAbilities().observe(this, Observer {
            val adapter = SelectedAbilitiesAdapter(it, selectItemListener, activity!!)
            val layoutManager = LinearLayoutManager(context)
            recycler_selected_abilities.adapter = adapter
            recycler_selected_abilities.layoutManager = layoutManager
        })
    }
}

class SelectedAbilitiesAdapter(
    private var abilities: List<Entities.SelectableAbility>?,
    private val cbListener: SelectedAbilitiesFragment.AbilitySelectCallback,
    private val activity : FragmentActivity) : RecyclerView.Adapter<SelectedViewHolder>() {

    private lateinit var mRecyclerView: RecyclerView
    private val saver = PreferenceManager.getDefaultSharedPreferences(activity)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedViewHolder {
        val item = LayoutInflater.from(parent.context)
            .inflate(R.layout.ability_item, parent, false) as View
        return SelectedViewHolder(item)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    override fun getItemCount(): Int {
        return 4
    }

    override fun onBindViewHolder(holder: SelectedViewHolder, position: Int) {
        var name = "Empty"
        val skillsToLevel = saver.getInt("skillLevel", 0)
        if (skillsToLevel > 0) {
            holder.itemView.button_pwrplus.visibility = View.VISIBLE
            holder.itemView.button_cdminus.visibility = View.VISIBLE
        }
        if (abilities != null) {
            if (abilities!!.count() >= position + 1) {
                val ability = AbilityConverter.toAbility(abilities!![position].abilityID)
                val image = ability.getImage(holder.itemView.context)
                name = ability.toString()
                holder.itemView.alpha = 1f
                if (image != null) {
                    holder.itemView.ability_image.setImageDrawable(image)
                }
                holder.itemView.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.healthbar_background)
                // create a callback that the ability should be removed from selected abilities
                holder.itemView.button_select.setOnClickListener {
                    if (holder.itemView.button_pwrplus.visibility == View.GONE) {
                        cbListener.onAbilityRemove(AbilityConverter.fromAbility(ability))
                    } else {
                        val t = Toast.makeText(activity, "Upgrade one of the abilities" , Toast.LENGTH_SHORT)
                        t.show()
                    }
                }
                holder.itemView.button_pwrplus.setOnClickListener {
                    AbilityModifier.setModifier(ability, true)
                    if (skillsToLevel > 0) {
                       saver.edit().putInt("skillLevel", (skillsToLevel-1)).apply()
                        if (skillsToLevel-1 == 0) {
                            mRecyclerView.forEach {
                                it.findViewById<MaterialButton>(R.id.button_pwrplus).visibility =
                                    View.GONE
                                it.findViewById<MaterialButton>(R.id.button_cdminus).visibility =
                                    View.GONE
                            }
                        }
                    }
                }
                holder.itemView.button_cdminus.setOnClickListener {
                    AbilityModifier.setModifier(ability, false)
                    if (skillsToLevel > 0) {
                        saver.edit().putInt("skillLevel", (skillsToLevel-1)).apply()
                        if (skillsToLevel-1 == 0) {
                            mRecyclerView.forEach {
                                it.findViewById<MaterialButton>(R.id.button_pwrplus).visibility =
                                    View.GONE
                                it.findViewById<MaterialButton>(R.id.button_cdminus).visibility =
                                    View.GONE
                            }
                        }
                    }
                }
            } else {
                name = "Slot ${position + 1}"
                holder.itemView.button_select.visibility = View.GONE
            }
        }
        holder.itemView.ability_name.text = name
    }
}

class SelectedViewHolder(v : View) : RecyclerView.ViewHolder(v)