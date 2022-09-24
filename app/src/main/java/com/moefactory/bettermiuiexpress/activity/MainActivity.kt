package com.moefactory.bettermiuiexpress.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.edit
import com.highcapable.yukihookapi.YukiHookAPI
import com.moefactory.bettermiuiexpress.R
import com.moefactory.bettermiuiexpress.base.app.PREF_KEY_CUSTOMER
import com.moefactory.bettermiuiexpress.base.app.PREF_KEY_SECRET_KEY
import com.moefactory.bettermiuiexpress.base.app.PREF_NAME
import com.moefactory.bettermiuiexpress.base.ui.BaseActivity
import com.moefactory.bettermiuiexpress.databinding.ActivityMainBinding

@SuppressLint("WorldReadableFiles")
class MainActivity : BaseActivity<ActivityMainBinding>(false) {

    override val viewBinding by viewBinding(ActivityMainBinding::inflate)

    private val pref by lazy { getSharedPreferences(PREF_NAME, Context.MODE_WORLD_READABLE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(viewBinding.mtToolbar)

        viewBinding.btnSave.setOnClickListener {
            pref.edit {
                putString(PREF_KEY_SECRET_KEY, viewBinding.tietKey.text?.toString() ?: "")
                putString(PREF_KEY_CUSTOMER, viewBinding.tietCustomer.text?.toString() ?: "")
            }
            if (viewBinding.tietCustomer.text.isNullOrEmpty() || viewBinding.tietKey.text.isNullOrEmpty()) {
                Toast.makeText(this, R.string.save_successfully_cainiao, Toast.LENGTH_SHORT).show()
            } else {
                pref.edit {
                    putString(PREF_KEY_SECRET_KEY, viewBinding.tietKey.text.toString())
                    putString(PREF_KEY_CUSTOMER, viewBinding.tietCustomer.text.toString())
                }

                Toast.makeText(this, R.string.save_successfully_kuaidi_100, Toast.LENGTH_SHORT).show()
            }
        }
        viewBinding.btnGithub.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://github.com/Robotxm/BetterMiuiExpress")))
        }
        viewBinding.btnCoolapk.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://coolapk.com/apk/com.moefactory.bettermiuiexpress")))
        }
        viewBinding.btnBlog.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://moefactory.com")))
        }
        viewBinding.mcvYuki.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://github.com/fankes/YukiHookAPI")))
        }

        viewBinding.tietCustomer.setText(pref.getString(PREF_KEY_CUSTOMER, "") ?: "")
        viewBinding.tietKey.setText(pref.getString(PREF_KEY_SECRET_KEY, "") ?: "")

        viewBinding.tvYukiVersion.text =
            getString(R.string.yuki_version, YukiHookAPI.API_VERSION_NAME, YukiHookAPI.API_VERSION_CODE)

        if (YukiHookAPI.Status.isModuleActive) {
            viewBinding.tvStatus.setText(R.string.active)
            viewBinding.tvStatusDescription.text = getString(
                R.string.active_hook_framework_version,
                YukiHookAPI.Status.executorName,
                YukiHookAPI.Status.executorVersion
            )
            viewBinding.ivStatus.setImageResource(R.drawable.ic_active)
        } else {
            viewBinding.tvStatus.setText(R.string.inactive)
            viewBinding.tvStatusDescription.setText(R.string.inactive_description)
            viewBinding.ivStatus.setImageResource(R.drawable.ic_inactive)
        }
    }
}