package com.naver.demo.identity

import android.app.Application
import android.content.res.Configuration
import android.preference.PreferenceManager
import android.system.Os
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.wallet.Wallet
import org.json.JSONObject
import java.util.concurrent.ExecutionException


class WalletApp : Application() {
    var indyWallet : Wallet? = null
    var masterSecretId : String? = null
    var walletOpened : Boolean = false

    private fun openWallet() {
        val walletConfig = JSONObject().put("id", "demoWallet").toString()
        val walletCredentials : String?

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val walletExists = pref.getBoolean("walletExists", false)

        if (!walletExists) {
            try {
                val key = Wallet.generateWalletKey("{}").get()
                walletCredentials = JSONObject().put("key", key).put("key_derivation_method", "RAW").toString()
                Wallet.createWallet(walletConfig, walletCredentials).get()
                with (pref.edit()) {
                    putString("walletKey", key)
                    putBoolean("walletExists", true)
                    apply()
                }
            } catch (e: ExecutionException) {
                throw e.cause!!
            }
        } else {
            val key = pref.getString("walletKey", null)
            walletCredentials = JSONObject().put("key", key).put("key_derivation_method", "RAW").toString()
        }

        indyWallet = Wallet.openWallet(walletConfig, walletCredentials).get()
        walletOpened = true
        masterSecretId = pref.getString("masterSecret", null)
        if (masterSecretId == null) {
            masterSecretId = Anoncreds.proverCreateMasterSecret(indyWallet, null).get()
            with (pref.edit()) {
                putString("masterSecret", masterSecretId)
                apply()
            }
        }

        Log.d("demo", "wallet opened")
    }

    override fun onCreate() {
        super.onCreate()

        Os.setenv("EXTERNAL_STORAGE", applicationContext.filesDir.toString(), true)

        GlobalScope.launch(Dispatchers.IO) {
            openWallet()
        }
    }

    override fun onConfigurationChanged ( newConfig : Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }
}