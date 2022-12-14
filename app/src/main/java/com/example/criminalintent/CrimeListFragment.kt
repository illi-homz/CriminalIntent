package com.example.criminalintent

import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.criminalintent.databinding.FragmentCrimeListBinding
import java.util.*
import javax.security.auth.callback.Callback

class CrimeListFragment : Fragment() {
    private lateinit var binding: FragmentCrimeListBinding
    private val crimeListViewModel: CrimeListViewModel by lazy {
        ViewModelProvider(this)[CrimeListViewModel::class.java]
    }

    //    private var adapter: CrimeAdapter? = null
    private var adapter: CrimeAdapter = CrimeAdapter(emptyList())
    private var callbacks: Callbacks? = null
    private lateinit var appCompatActivity: AppCompatActivity

    interface Callbacks {
        fun onCrimeSelected(crimeId: UUID)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        appCompatActivity = activity as AppCompatActivity
//        Log.d(TAG, "Total crimes: ${crimeListViewModel.crimes.size}")
    }

    override fun onResume() {
        super.onResume()
        appCompatActivity.supportActionBar?.setTitle("Список чего то там")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCrimeListBinding.inflate(inflater, container, false)
        binding.crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.crimeRecyclerView.adapter = adapter

//        updateUI()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeListViewModel.crimeListLiveData.observe(
            viewLifecycleOwner, // привязывает наблюдатель к жизненному циклу фрагмента
            Observer { crimes ->
                crimes?.let {
                    Log.i(TAG, "Got crimes ${crimes.size}")
                    updateUI(crimes)
                }
            }
        )
    }

    private inner class CrimeHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        val titleTextView: TextView = itemView.findViewById(R.id.crime_title)
        val dateTextView: TextView = itemView.findViewById(R.id.crime_date)
        val solvedImageView: ImageView = itemView.findViewById(R.id.crime_solved)
        private lateinit var crime: Crime

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(crime: Crime) {
            this.crime = crime
            titleTextView.setText(crime.title)
            val date = DateFormat.format("MMM dd, yyyy", crime.date)
            dateTextView.setText(date)
            solvedImageView.visibility = when {
                crime.isSolved -> View.VISIBLE
                else -> View.GONE
            }
        }

        override fun onClick(v: View) {
            callbacks?.onCrimeSelected(crime.id)
        }
    }

    private inner class DiffCallback : DiffUtil.ItemCallback<Crime>() {
        override fun areItemsTheSame(oldItem: Crime, newItem: Crime) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Crime, newItem: Crime) = when {
            oldItem.title != newItem.title -> false
            oldItem.isSolved != newItem.isSolved -> false
            oldItem.date != newItem.date -> false
            else -> true
        }
    }

    //    private inner class CrimeAdapter(var crimes: List<Crime>) : RecyclerView.Adapter<CrimeHolder>() {
    private inner class CrimeAdapter(var crimes: List<Crime>) :
        ListAdapter<Crime, CrimeHolder>(DiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            val view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
            return CrimeHolder(view)
        }

        override fun getItemCount(): Int = crimes.size

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = crimes[position]
            holder.bind(crime)
        }

        override fun onCurrentListChanged(
            previousList: MutableList<Crime>,
            currentList: MutableList<Crime>
        ) {
            super.onCurrentListChanged(previousList, currentList)
            crimes = currentList
        }
    }

    private fun updateUI(crimes: List<Crime>) {
//        val adapter = CrimeAdapter(crimes)
//        binding.crimeRecyclerView.adapter = adapter
        adapter.submitList(crimes as MutableList<Crime>)

        binding.emptyView.visibility = if (crimes.size == 0) View.VISIBLE else View.GONE
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.new_crime -> {
                val crime = Crime()
                crimeListViewModel.addCrime(crime)
                callbacks?.onCrimeSelected(crime.id)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        @JvmStatic
        val TAG = "CrimeListFragment"

        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }
}