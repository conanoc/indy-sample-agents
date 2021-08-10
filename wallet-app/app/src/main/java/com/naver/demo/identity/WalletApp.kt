package com.naver.demo.identity

import android.app.Application
import android.content.SharedPreferences
import android.content.res.Configuration
import android.preference.PreferenceManager
import android.system.Os
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.hyperledger.indy.sdk.LibIndy
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.pool.Pool
import org.hyperledger.indy.sdk.pool.PoolJSONParameters.CreatePoolLedgerConfigJSONParameter
import org.hyperledger.indy.sdk.pool.PoolLedgerConfigExistsException
import org.hyperledger.indy.sdk.wallet.Wallet
import org.hyperledger.indy.sdk.wallet.WalletExistsException
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ExecutionException


class WalletApp : Application() {
    var indyWallet : Wallet? = null
    var masterSecretId : String? = null
    var walletOpened : Boolean = false

    private fun openWallet() {
        val walletConfig = JSONObject().put("id", "demoWallet").toString()
        val walletCredentials = JSONObject().put("key", "1234").toString()
        try {
            Wallet.createWallet(walletConfig, walletCredentials).get()
        } catch (e : ExecutionException) {
            if (e.cause !is WalletExistsException)
                throw e.cause!!
        }

        indyWallet = Wallet.openWallet(walletConfig, walletCredentials).get()
        walletOpened = true
        val pref = PreferenceManager.getDefaultSharedPreferences(this);
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

        Os.setenv("EXTERNAL_STORAGE", applicationContext.filesDir.toString(), true);

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