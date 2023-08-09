package com.naver.demo.identity

import java.text.SimpleDateFormat
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.naver.demo.identity.databinding.ActivityCredentialDetailBinding
import com.naver.demo.identity.databinding.CredentialDetailBinding
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
    private lateinit var detailBinding: ActivityCredentialDetailBinding
    private lateinit var binding: CredentialDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            if (it.containsKey(ARG_CREDENTIAL)) {
                item = JSONObject(it.getString(ARG_CREDENTIAL))
                detailBinding = ActivityCredentialDetailBinding.inflate(layoutInflater)
                detailBinding.toolbarLayout?.title = getString(R.string.title_credential_detail)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = CredentialDetailBinding.inflate(inflater, container, false)
        val rootView = binding.root
        item?.let {
            val attrs = it.getJSONObject("attrs")
            val date = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).parse(attrs.getString("date")).toLocaleString()
            binding.credentialDetail.text =
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
