package com.naver.demo.identity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.NavUtils
import android.view.MenuItem
import com.naver.demo.identity.databinding.ActivityCredentialListBinding
import com.naver.demo.identity.databinding.CredentialListBinding
import com.naver.demo.identity.databinding.CredentialListContentBinding
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.json.JSONArray
import org.json.JSONObject

/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [credentialDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class credentialListActivity : AppCompatActivity() {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false
    private lateinit var binding: ActivityCredentialListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCredentialListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.title = title

        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView(binding.credentialList.credentialList)
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            android.R.id.home -> {
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                NavUtils.navigateUpFromSameTask(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        val app = application as WalletApp
        val credentials = Anoncreds.proverGetCredentials(app.indyWallet, "{}").get()
        recyclerView.adapter = SimpleItemRecyclerViewAdapter(this, JSONArray(credentials), twoPane)
    }

    class SimpleItemRecyclerViewAdapter(
        private val parentActivity: credentialListActivity,
        private val values: JSONArray,
        private val twoPane: Boolean
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as JSONObject
                if (twoPane) {
                    val fragment = credentialDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString(credentialDetailFragment.ARG_CREDENTIAL, item.toString())
                        }
                    }
                    parentActivity.supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.credential_detail_container, fragment)
                        .commit()
                } else {
                    val intent = Intent(v.context, credentialDetailActivity::class.java).apply {
                        putExtra(credentialDetailFragment.ARG_CREDENTIAL, item.toString())
                    }
                    v.context.startActivity(intent)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.credential_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position] as JSONObject
            holder.contentView.text = item.getJSONObject("attrs").getString("vaccine") + " " +
                    item.getJSONObject("attrs").getString("doses")

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.length()

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var contentBinding = CredentialListContentBinding.bind(view)
            val contentView: TextView = contentBinding.content
        }
    }
}
