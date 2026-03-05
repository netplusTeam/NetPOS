package com.woleapp.netpos.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.ActivityAuthenticationBinding
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.ui.fragments.LoginFragment
import com.woleapp.netpos.util.JWTHelper
import com.woleapp.netpos.util.PREF_AUTHENTICATED
import com.woleapp.netpos.util.PREF_USER_TOKEN
import com.woleapp.netpos.util.horizonpay.K11HardwareBridge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthenticationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthenticationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)
//        NetPosTerminalConfig.init(applicationContext)

//        if (Prefs.getBoolean(PREF_AUTHENTICATED, false) && tokenValid()) {
//            startActivity(
//                Intent(this, MainActivity::class.java).apply {
//                    flags =
//                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                }
//            )
//             MqttHelper.init<Nothing>(applicationContext)
//            NetPosTerminalConfig.init(applicationContext)
//            finish()
//        }
//        showFragment(LoginFragment())

        // Start the bridge connection
        K11HardwareBridge.connectService(this) { isSuccess ->
            if (isSuccess) {
                // Now it's safe to move to the main screen
                if (Prefs.getBoolean(PREF_AUTHENTICATED, false) && tokenValid()) {
                    startActivity(
                        Intent(this, MainActivity::class.java).apply {
                            flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
//                    MqttHelper.init<Nothing>(applicationContext)
//                    NetPosTerminalConfig.init(applicationContext)
                    finish()
                } else {
                    showFragment(LoginFragment())
                }
            } else {
                // Show an error: The hardware service couldn't be reached
                Toast.makeText(this, "Hardware initialization failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun tokenValid(): Boolean {
        val token = Prefs.getString(PREF_USER_TOKEN, null)
        return !(token.isNullOrEmpty() || JWTHelper.isExpired(token))
    }

    private fun showFragment(targetFragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .apply {
                    replace(
                        R.id.auth_container,
                        targetFragment,
                        targetFragment.javaClass.simpleName
                    )
                    setCustomAnimations(R.anim.right_to_left, android.R.anim.fade_out)
                    commit()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
