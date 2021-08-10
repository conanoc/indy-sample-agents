package com.naver.demo.identity

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.naver.demo.identity.menu.MainMenu
import kotlinx.android.synthetic.main.activity_wallet_main.*
import kotlinx.android.synthetic.main.menu_item_list.*
import kotlinx.android.synthetic.main.menu_item_list_content.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.anoncreds.CredentialsSearchForProofReq
import org.hyperledger.indy.sdk.did.Did
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class WalletMainActivity : AppCompatActivity() {

    private var twoPane: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_main)

        setSupportActionBar(toolbar)
        toolbar.title = title

        if (item_detail_container != null) {
            twoPane = true
        }

        setupRecyclerView(item_list)
        waitForWallet()
    }

    override fun onStart() {
        super.onStart()
    }

    private fun showAlert(message: String) {
        val builder = AlertDialog.Builder(this@WalletMainActivity)
        builder.setMessage(message)
            .setPositiveButton(android.R.string.yes) { _, _ -> }
        builder.create().show()
    }

    private fun waitForWallet() {
        val app = application as WalletApp
        val progress = ProgressDialog(this)
        progress.setTitle("Opening a wallet")
        progress.setCancelable(false)
        progress.show()

        val timer = object: CountDownTimer(20000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (app.walletOpened) {
                    progress.dismiss()
                    cancel()
                }
            }

            override fun onFinish() {
                progress.dismiss()
                showAlert("Failed to open a wallet.")
            }
        }
        timer.start()
    }

    private suspend fun tryGetCredential(server: String, offerId: String) {
        val app = application as WalletApp
        val offerData = URL("http://$server/credential/credOffer/$offerId").readText()
        val offer = JSONObject(offerData)
        val oneTimeDid = Did.createAndStoreMyDid(app.indyWallet, "{}").get().did
        val credRequest = Anoncreds.proverCreateCredentialReq(app.indyWallet, oneTimeDid, offer.getString("cred_offer"),
            offer.getString("cred_def"), app.masterSecretId).get()
        val requestJson = "{\"cred_request\":${credRequest.credentialRequestJson}}"

        yield() // Check user cancellation
        (URL("http://$server/credential/credRequest/$offerId").openConnection() as? HttpURLConnection)?.run {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            doOutput = true
            outputStream.write(requestJson.toByteArray())

            val response = inputStream.bufferedReader().use { it.readText() }
            val credential = JSONObject(response).getString("credential")
            yield()
            Anoncreds.proverStoreCredential(app.indyWallet, null, credRequest.credentialRequestMetadataJson,
                credential, offer.getString("cred_def"), null).get();
        }
    }

    private fun getCredential(server: String, offerId: String) {
        val progress = ProgressDialog(this)
        progress.setTitle("Loading")
        progress.setCancelable(true)

        val job = lifecycleScope.launch(Dispatchers.IO) {
            try {
                tryGetCredential(server, offerId)
                lifecycleScope.launch(Dispatchers.Main) {
                    progress.dismiss()
                    showAlert("Received a certificate.")
                }
            } catch (e: Exception) {
                if(!isActive) return@launch
                lifecycleScope.launch(Dispatchers.Main) {
                    Log.d("demo", e.localizedMessage)
                    progress.dismiss()
                    showAlert("Failed to receive a certificate.")
                }
            }
        }

        progress.setOnCancelListener {
            job.cancel()
        }
        progress.show()
    }

    private suspend fun trySendProof(server: String, proofId: String, proofReq: String): String {
        val app = application as WalletApp
        val schemaJson = URL("http://$server/schema").readText()
        val credDefJson = URL("http://$server/definition").readText()

        yield()
        val credentialsSearch = CredentialsSearchForProofReq.open(app.indyWallet, proofReq, null).get()
        val credentialsForAttribute1 = JSONArray(credentialsSearch.fetchNextCredentials("attr1_referent", 10).get())
        val credential = credentialsForAttribute1.getJSONObject(0).getJSONObject("cred_info")
        val credentialIdForAttribute1 = credential.getString("referent")
        val schemaId = credential.getString("schema_id")
        val credDefId = credential.getString("cred_def_id")
        credentialsSearch.close()

        val requestedCredentialsJson = JSONObject()
            .put("self_attested_attributes", JSONObject())
            .put("requested_predicates", JSONObject())
            .put("requested_attributes", JSONObject()
                .put("attr1_referent", JSONObject()
                    .put("cred_id", credentialIdForAttribute1)
                    .put("revealed", true)
                )
            )
            .toString()

        val schemas = JSONObject().put(schemaId, JSONObject(schemaJson)).toString()
        val credentialDefs: String = JSONObject().put(credDefId, JSONObject(credDefJson)).toString()
        val revocStates = JSONObject().toString()

        val proofJson = Anoncreds.proverCreateProof(app.indyWallet, proofReq, requestedCredentialsJson,
            app.masterSecretId, schemas, credentialDefs, revocStates).get();

        yield()
        var result = "FAIL"
        (URL("http://$server/credential/proof/$proofId").openConnection() as? HttpURLConnection)?.run {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            doOutput = true
            outputStream.write(proofJson.toByteArray())

            val response = inputStream.bufferedReader().use { it.readText() }
            result = JSONObject(response).getString("result")
        }

        return result
    }

    private fun sendProof(server: String, proofId: String, proofReq: String) {
        val progress = ProgressDialog(this)
        progress.setTitle("Sending proof")
        progress.setCancelable(true)

        val job = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = trySendProof(server, proofId, proofReq)
                lifecycleScope.launch(Dispatchers.Main) {
                    progress.dismiss()
                    if (result == "OK") {
                        showAlert("Verification is complete.")
                    } else {
                        showAlert("Verification failed.")
                    }
                }
            } catch (e: Exception) {
                if (!isActive) return@launch
                lifecycleScope.launch(Dispatchers.Main) {
                    Log.d("demo", e.localizedMessage)
                    progress.dismiss()
                    showAlert("Failed to submit a proof.")
                }
            }
        }

        progress.setOnCancelListener {
            job.cancel()
        }
        progress.show()
    }

    private fun processCredentialOffer(json: JSONObject) {
        val title = json.getString("title")
        val target = json.getString("to")
        val server = json.getString("server")
        val offerId = json.getString("offer_id")
        val builder = AlertDialog.Builder(this@WalletMainActivity)
        builder.setMessage("This is a \"$title\" for $target. Do you want to take it?")
            .setPositiveButton(android.R.string.yes,
                DialogInterface.OnClickListener { dialog, id ->
                    getCredential(server, offerId)
                })
            .setNegativeButton(android.R.string.no,
                DialogInterface.OnClickListener { dialog, id ->
                })
        val dialog = builder.create()
        dialog.show()
    }

    private fun processVerify(json: JSONObject) {
        val title = json.getString("title")
        val server = json.getString("server")
        val proofReq = json.getString("proof_request")
        val proofId = json.getString("proof_id")
        val builder = AlertDialog.Builder(this@WalletMainActivity)
        builder.setMessage("This is a \"$title\". Do you want to proceed?")
            .setPositiveButton(android.R.string.yes,
                DialogInterface.OnClickListener { dialog, id ->
                    sendProof(server, proofId, proofReq)
                })
            .setNegativeButton(android.R.string.no,
                DialogInterface.OnClickListener { dialog, id ->
                })
        val dialog = builder.create()
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            try {
                val qrcodeData = data!!.getStringExtra("qrcode")
                val json = JSONObject(qrcodeData)
                when (json.getString("type")) {
                    "cred_offer" -> processCredentialOffer(json)
                    "cred_verify" -> processVerify(json)
                }
            } catch (e: Exception) {
                Log.d("demo", e.localizedMessage)
                showAlert("Unrecognized qrcode")
            }
        }
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        recyclerView.adapter = SimpleItemRecyclerViewAdapter(this, listOf(MainMenu.GET, MainMenu.LIST), twoPane)
    }

    class SimpleItemRecyclerViewAdapter(
        private val parentActivity: WalletMainActivity,
        private val values: List<MainMenu>,
        private val twoPane: Boolean
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val menu = v.tag as MainMenu
                when (menu) {
                    MainMenu.GET -> {
                        val intent = Intent(v.context, BarcodeScannerActivity::class.java)
                        (v.context as Activity).startActivityForResult(intent, 0)
                    }

                    MainMenu.LIST -> {
                        val intent = Intent(v.context, credentialListActivity::class.java)
                        v.context.startActivity(intent)

                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.menu_item_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.contentView.text = item.text

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val contentView: TextView = view.content
        }
    }
}
