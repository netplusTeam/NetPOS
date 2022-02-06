package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.get
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.LayoutEnterPasswordBinding
import com.woleapp.netpos.databinding.LayoutReprintPasswordPrefTextBinding
import com.woleapp.netpos.util.PREF_REPRINT_PASSWORD
import timber.log.Timber

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var inputPasswordDialog: AlertDialog
    private lateinit var passwordDialogBinding: LayoutEnterPasswordBinding
    private lateinit var passwordSetDialog: AlertDialog
    private lateinit var passwordSetBinding: LayoutReprintPasswordPrefTextBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        passwordDialogBinding =
            LayoutEnterPasswordBinding.inflate(LayoutInflater.from(requireContext()), null, false)
                .apply {
                    lifecycleOwner = viewLifecycleOwner
                    executePendingBindings()
                }
        passwordSetBinding = LayoutReprintPasswordPrefTextBinding.inflate(
            LayoutInflater.from(requireContext()),
            null,
            false
        ).apply {
            lifecycleOwner = viewLifecycleOwner
            executePendingBindings()
        }
        passwordSetBinding.save.setOnClickListener {
            if (passwordSetBinding.reprintPasswordEdittext.text.toString() != passwordSetBinding.confirmReprintPasswordEdittext.text.toString()) {
                Timber.e("password mismatch")
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            Prefs.putString(
                PREF_REPRINT_PASSWORD,
                passwordSetBinding.reprintPasswordEdittext.text.toString()
            )
            passwordSetBinding.confirmReprintPasswordEdittext.setText("")
            passwordSetBinding.reprintPasswordEdittext.setText("")
            Toast.makeText(requireContext(), "Password Set", Toast.LENGTH_SHORT).show()
            passwordSetDialog.cancel()
        }
        passwordDialogBinding.proceed.setOnClickListener {
            if (passwordDialogBinding.passwordEdittext.text.toString() == Prefs.getString(
                    PREF_REPRINT_PASSWORD, ""
                )
            ) {
                passwordSetDialog.show()
            }
        }
        passwordSetDialog = AlertDialog.Builder(requireContext())
            .setView(passwordSetBinding.root)
            .create()
        inputPasswordDialog = AlertDialog.Builder(requireContext())
            .setView(passwordDialogBinding.root)
            .create()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (BuildConfig.FLAVOR != "wema")
            preferenceScreen[1].isVisible = false

        preferenceScreen[1].setOnPreferenceClickListener {
            if (Prefs.contains(PREF_REPRINT_PASSWORD)) {
                passwordDialogBinding.passwordEdittext.setText("")
                inputPasswordDialog.show()
            } else
                passwordSetDialog.show()
            true
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener { _, _ ->
            //Timber.e(Prefs.contains(key).toString())
            //Timber.e(Prefs.getString(key, ""))
        }
    }
}