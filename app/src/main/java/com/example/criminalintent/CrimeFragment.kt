package com.example.criminalintent

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.criminalintent.databinding.FragmentCrimeBinding
import java.util.*

class CrimeFragment : Fragment(), DatePickerFragment.Callbacks {
    private lateinit var binding: FragmentCrimeBinding
    private lateinit var crime: Crime
    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }
    private lateinit var appCompatActivity: AppCompatActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val crimeId = arguments?.getSerializable(ARG_CRIME_ID) as UUID
//        val crimeRepository = CrimeRepository.get()
//        crime = crimeRepository.getCrime(crimeId)
        crime = Crime()
        crimeDetailViewModel.loadCrime(crimeId)
        appCompatActivity = activity as AppCompatActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCrimeBinding.inflate(inflater, container, false)
        val date = DateFormat.format("MMM dd, yyyy", crime.date)
        binding.crimeDate.text = date

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer { crime ->
                crime?.let {
                    this.crime = crime
                    updateUi()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        setupUi()
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    private fun setupUi() {
        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//                TODO("Not yet implemented")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {
//                TODO("Not yet implemented")
            }
        }

        binding.crimeTitle.addTextChangedListener(titleWatcher)
        binding.crimeSolved.setOnCheckedChangeListener { _, isChecked ->
            crime.isSolved = isChecked
        }
        binding.crimeDate.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.requireFragmentManager(), DIALOG_DATE)
            }
        }
        binding.crimeReport.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plane"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also {
//                startActivity(it)
                val chooserIntent = Intent.createChooser(it, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }
        binding.crimeSuspect.apply {
            val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }
        }
    }

    private fun updateUi() {
        binding.crimeTitle.setText(crime.title)
        binding.crimeDate.text = DateFormat.format("MMM dd, yyyy", crime.date)
        binding.crimeSolved.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        appCompatActivity.supportActionBar?.setTitle(crime.title)
        if (crime.suspect.isNotEmpty()) {
            binding.crimeSuspect.text = crime.suspect
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri? = data.data
                // Указать, для каких полей ваш запрос должен возвращать значения.
                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                // Выполняемый здесь запрос — contactUri похож на предложение "where"
                val cursor = contactUri?.let { requireActivity().contentResolver.query(it, queryFields, null, null, null) }
                cursor?.use {
                    // Verify cursor contains at least one result
                    if (it.count == 0) {
                        return
                    }

                    // Первый столбец первой строки данных —
                    // это имя вашего подозреваемого.
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.suspect = suspect
                    crimeDetailViewModel.saveCrime(crime)
                    binding.crimeSuspect.text = suspect
                }
            }
        }
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUi()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.new_crime -> {
//                val crime = Crime()
//                crimeDetailViewModel.addCrime(crime)
//                callbacks?.onCrimeSelected(crime.id)
                crimeDetailViewModel.removeCrime(crime.id)
                requireActivity().onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getCrimeReport(): String {
        val solvedString = when {
            crime.isSolved -> getString(R.string.crime_report_solved)
            else -> getString(R.string.crime_report_unsolved)
        }
        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        var suspect = when {
            crime.suspect.isBlank() -> getString(R.string.crime_report_no_suspect)
            else -> getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }


    companion object {
        private const val ARG_UUID = "ARG_UUID"
        private const val ARG_CRIME_ID = "crime_id"
        private const val DIALOG_DATE = "DialogDate"
        private const val REQUEST_DATE = 0
        private const val REQUEST_CONTACT = 1
        private const val TAG = "CrimeFragment"
        private const val DATE_FORMAT = "EEE, MMM, dd"

        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}