package com.example.argame.Fragments.Abilities

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
import kotlinx.android.synthetic.main.ability_item.view.*
import kotlinx.android.synthetic.main.fragment_unselected_abilities.*

/**
 *  The bottom fragment of the "select abilities" menu,
 *  and the Adapter class for its recycler view.
 *
 *  Uses live data from room to update the recycler view.
 */

class UnselectedAbilitiesFragment(
    private val database: AppDatabase, private val mContext: AbilityMenuFragment) : Fragment() {

    interface AbilitySelectCallback {
        fun onAbilityAdd(id: Int)
    }

    private lateinit var db: AppDatabase
    private lateinit var cb: AbilitySelectCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)
        cb = mContext
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_unselected_abilities, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = database
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val abilitiesLiveModel
                = ViewModelProviders.of(this).get(AbilitiesLiveModel::class.java)
        abilitiesLiveModel.getSelectableAbilities().observe(this, Observer {
            val adapter = UnselectedAbilitiesAdapter(it, cb)
            val layoutManager = LinearLayoutManager(context)
            recycler_unselected_abilities.adapter = adapter
            recycler_unselected_abilities.layoutManager = layoutManager
        })
    }
}

class UnselectedAbilitiesAdapter(
    private var abilities: List<Entities.SelectableAbility>?,
    private val cb: UnselectedAbilitiesFragment.AbilitySelectCallback) : RecyclerView.Adapter<UnselectedViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnselectedViewHolder {
        val item = LayoutInflater.from(parent.context)
            .inflate(R.layout.ability_item, parent, false) as View
        return UnselectedViewHolder(item)
    }

    override fun getItemCount(): Int {
        return abilities?.count() ?: 0
    }

    override fun onBindViewHolder(holder: UnselectedViewHolder, position: Int) {
        val ability = AbilityConverter.toAbility(abilities!![position].abilityID)
        val name = ability.toString()
        val btnText = "+"
        holder.itemView.ability_name.text = name
        holder.itemView.button_select.text = btnText
        holder.itemView.alpha = 1f
        // Callback for updating the selected abilities
        holder.itemView.button_select.setOnClickListener {
            cb.onAbilityAdd(AbilityConverter.fromAbility(ability))
        }
        if (abilities!!.size < 2) {
            holder.itemView.button_select.isEnabled = false
            holder.itemView.button_select.alpha = 0.1f
        }

        val image = ability.getImage(holder.itemView.context)
        if (image != null) {
            holder.itemView.ability_image.setImageDrawable(image)
        }
    }
}

class UnselectedViewHolder(v : View) : RecyclerView.ViewHolder(v)