
package com.bignerbranch.android.zd4_demin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

class SecCrime: Fragment() {

    private lateinit var crime: Crime

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sec_crime,container,false)

        return view
    }

    override fun onStart() {
        super.onStart()
        if (view != null) {
            view?.let { Snackbar.make(it,"Проблема решена",Snackbar.LENGTH_LONG).show() }
        }
    }
}