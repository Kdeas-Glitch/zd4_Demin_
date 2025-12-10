package com.bignerbranch.android.zd4_demin

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import java.util.Date
import java.util.UUID

class CrimeFragment : Fragment() {
    private lateinit var crime: Crime
    private val REQUEST_CONTACT = 1
    private val PERMISSION_REQUEST_READ_CONTACTS = 100
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var sendButton: Button
    private lateinit var suspectButton: Button
    private lateinit var callButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private val DATE_FORMAT = "EEE, MMM, dd"

    private var selectedContactId: String? = null
    private var selectedPhoneNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime(UUID.randomUUID(), Date())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)

        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        sendButton = view.findViewById(R.id.send)
        suspectButton = view.findViewById(R.id.suspect)
        callButton = view.findViewById(R.id.call_button) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox

        callButton.isEnabled = false

        suspectButton.setOnClickListener {
            checkContactPermissionAndPickContact()
        }

        sendButton.setOnClickListener {
            if (crime.suspect.isNullOrBlank()) {
                suspectButton.performClick()
            } else {
                sendCrimeReport()
            }
        }

        callButton.setOnClickListener {
            if (selectedPhoneNumber != null) {
                callSelectedContact()
            }
        }

        dateButton.text = Date().toString()
        solvedCheckBox.isEnabled = false

        dateButton.setOnClickListener {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, SecCrime())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        dateButton.apply {
            isEnabled = false
        }

        return view
    }

    private fun checkContactPermissionAndPickContact() {//Проверка на разрешение
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                openContactPicker()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                android.Manifest.permission.READ_CONTACTS
            )-> {

            }
        }
    }

    private fun openContactPicker() {
        val pickContactIntent = Intent(
            Intent.ACTION_PICK,
            ContactsContract.Contacts.CONTENT_URI
        )
        startActivityForResult(pickContactIntent, REQUEST_CONTACT)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_READ_CONTACTS -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openContactPicker()
                } else {
                    Snackbar.make(
                        requireView(),
                        "Без разрешения невозможно выбрать контакт",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                solvedCheckBox.isEnabled = s.toString().isNotBlank()
                crime.title = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
                dateButton.isEnabled = isChecked
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CONTACT -> {
                if (resultCode != Activity.RESULT_OK || data?.data == null) {
                    return
                }

                val contactUri: Uri = data.data!!

                val cursor = requireActivity().contentResolver.query(
                    contactUri,
                    arrayOf(
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.Contacts.HAS_PHONE_NUMBER
                    ),
                    null,
                    null,
                    null
                )

                cursor?.use {
                    if (it.moveToFirst()) {
                        selectedContactId = it.getString(0)
                        crime.suspect = it.getString(1)
                        val hasPhone = it.getInt(2) == 1

                        suspectButton.text = crime.suspect

                        if (hasPhone && selectedContactId != null) {
                            getPhoneNumber(selectedContactId!!)
                        } else {
                            callButton.isEnabled = false
                            selectedPhoneNumber = null
                        }
                    }
                }
            }
        }
    }

    private fun getPhoneNumber(contactId: String) {
        val phoneCursor = requireActivity().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )
        val cursor = requireActivity().contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            ),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val contactName = it.getString(1)
                sendButton.text=contactName;
            }
        }

        phoneCursor?.use {
            if (it.moveToFirst()) {
                selectedPhoneNumber = it.getString(0)
                selectedPhoneNumber = selectedPhoneNumber?.replace(Regex("[^0-9+]"), "")
                callButton.isEnabled = true
            } else {
                callButton.isEnabled = false
                selectedPhoneNumber = null
            }
        }
    }

    private fun callSelectedContact() {
        if (selectedPhoneNumber.isNullOrBlank()) {
            return
        }

        val callUri = Uri.parse("tel:${selectedPhoneNumber}")
        val callIntent = Intent(Intent.ACTION_DIAL, callUri)

        try {
            startActivity(callIntent)
        } catch (e: Exception) {
            Snackbar.make(
                requireView(),
                "Не удалось открыть приложение для звонков",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun sendCrimeReport() {
        val report = getCrimeReport()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, report)
            putExtra(Intent.EXTRA_SUBJECT, "Отчет о преступлении")
        }

        val chooser = Intent.createChooser(intent, "Отправить отчет через...")
        startActivity(chooser)
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()

        val suspect = if (crime.suspect.isNullOrBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(
            R.string.crime_report,
            crime.title,
            dateString,
            solvedString,
            suspect
        )
    }
}