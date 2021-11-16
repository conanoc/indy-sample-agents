package com.naver.demo.identity

import java.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_credential_detail.*
import kotlinx.android.synthetic.main.credential_detail.view.*
import org.json.JSONObject
import java.util.*

/**
 * A fragment representing a single credential detail screen.
 * This fragment is either contained in a [credentialListActivity]
 * in two-pane mode (on tablets) or a [credentialDetailActivity]
 * on handsets.
 */
class credentialDetailFragment : Fragment() {

    private var item: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            if (it.containsKey(ARG_CREDENTIAL)) {
                item = JSONObject(it.getString(ARG_CREDENTIAL))
                activity?.toolbar_layout?.title = getString(R.string.title_credential_detail)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.credential_detail, container, false)

        item?.let {
            val attrs = it.getJSONObject("attrs")
            val date = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).parse(attrs.getString("date")).toLocaleString()
            rootView.credential_detail.text =
                """
                    Issuer organization: ${attrs.getString("organization")}
                    Vaccine name: ${attrs.getString("vaccine")}
                    Total doses: ${attrs.getString("doses")}
                    Recipient name: ${attrs.getString("target")}
                    Date of issueance: $date
                    
                    Revocation ID: ${it.getString("cred_rev_id")}
                """.trimIndent()

        }

        return rootView
    }

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        const val ARG_CREDENTIAL = "item_credential"
    }
}
