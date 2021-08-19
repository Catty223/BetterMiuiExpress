package com.moefactory.bettermiuiexpress.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.nukc.stateview.StateView
import com.github.vipulasri.timelineview.TimelineView
import com.moefactory.bettermiuiexpress.R
import com.moefactory.bettermiuiexpress.adapter.recyclerview.TimeLineAdapter
import com.moefactory.bettermiuiexpress.base.app.dataStore
import com.moefactory.bettermiuiexpress.base.datastore.DataStoreKeys
import com.moefactory.bettermiuiexpress.base.ui.BaseActivity
import com.moefactory.bettermiuiexpress.data.CredentialMemoryStore
import com.moefactory.bettermiuiexpress.databinding.ActivityExpressDetailsBinding
import com.moefactory.bettermiuiexpress.ktx.dp
import com.moefactory.bettermiuiexpress.model.*
import com.moefactory.bettermiuiexpress.utils.ExpressCompanyUtils
import com.moefactory.bettermiuiexpress.viewmodel.ExpressDetailsViewModel
import kotlinx.coroutines.flow.map

class ExpressDetailsActivity : BaseActivity<ActivityExpressDetailsBinding>(false) {

    companion object {
        const val ACTION_GO_TO_DETAILS = "com.moefactory.bettermiuiexpress.details"
        const val INTENT_EXPRESS_SUMMARY = "express_summary"
        const val INTENT_URL_CANDIDATES = "url_candidates"

        fun gotoDetailsActivity(
            context: Context,
            miuiExpress: MiuiExpress,
            urlList: ArrayList<String>?
        ) {
            if (context is Activity) { // Click items in details activity
                context.startActivity(
                    Intent(ACTION_GO_TO_DETAILS)
                        .putExtra(INTENT_EXPRESS_SUMMARY, miuiExpress)
                        .putExtra(INTENT_URL_CANDIDATES, urlList)
                )
            } else { // Click items in card
                context.startActivity(
                    Intent(ACTION_GO_TO_DETAILS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(INTENT_EXPRESS_SUMMARY, miuiExpress)
                        .putExtra(INTENT_URL_CANDIDATES, urlList)
                )
            }
        }
    }

    override val viewBinding by viewBinding(ActivityExpressDetailsBinding::inflate)
    private val miuiExpress by lazy { intent.getParcelableExtra<MiuiExpress>(INTENT_EXPRESS_SUMMARY) }
    private val urlCandidates by lazy { intent.getStringArrayListExtra(INTENT_URL_CANDIDATES) }
    private val viewModel by viewModels<ExpressDetailsViewModel>()
    private lateinit var timelineAdapter: TimeLineAdapter
    private val expressDetailsNodes = mutableListOf<ExpressDetails>()
    private lateinit var credential: Credential
    private val stateView by lazy { StateView.inject(viewBinding.clContent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (miuiExpress == null) {
            Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        stateView.emptyResource = R.layout.empty_layout
        stateView.retryResource = R.layout.empty_layout

        setSupportActionBar(viewBinding.mtToolbar)
        viewBinding.actionBarTitle.text = miuiExpress?.companyName
        viewBinding.up.setOnClickListener { onBackPressed() }

        viewBinding.tvMailNumber.text =
            getString(R.string.express_details_mail_number, miuiExpress!!.mailNumber)

        initTimeline()

        getSavedCredential().observe(this) {
            credential = it
            if (it.customer.isNotBlank() && it.secretKey.isNotBlank()) {
                stateView.showLoading()
                CredentialMemoryStore.secretKey = it.secretKey
                // Try to convert CaiNiao company code to KuaiDi100 company code
                val companyCode = ExpressCompanyUtils.convertCode(miuiExpress!!.companyCode)
                if (companyCode != null) {
                    viewModel.queryExpressDetails(
                        credential.customer,
                        companyCode,
                        miuiExpress!!.mailNumber
                    )
                } else {
                    viewModel.queryCompany(it.secretKey, miuiExpress!!.mailNumber)
                }
            } else {
                Toast.makeText(this, R.string.no_credentials, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        viewModel.queryCompanyResult.observe(this) {
            if (it.isSuccess) {
                viewModel.queryExpressDetails(
                    credential.customer,
                    it.getOrNull()!![0].companyCode,
                    miuiExpress!!.mailNumber
                )
            } else {
                viewBinding.tvStatus.setText(R.string.empty_status)
                stateView.showRetry()
            }
        }
        viewModel.queryExpressResult.observe(this) {
            if (it.isSuccess) {
                val response = it.getOrNull()
                if (response == null) {
                    stateView.showRetry()
                    return@observe
                }
                if (response.result != null) {
                    stateView.showRetry()
                    return@observe
                }
                val state = KuaiDi100ExpressState.statesMap.find { state ->
                    state.categoryCode == response.state
                }!!
                viewBinding.tvStatus.text = state.categoryName
                expressDetailsNodes.clear()
                expressDetailsNodes.addAll(response.data!!)
                timelineAdapter.notifyDataSetChanged()
                stateView.showContent()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_jump -> {
                startThirdAppByList(urlCandidates!!)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (!urlCandidates.isNullOrEmpty()) {
            menuInflater.inflate(R.menu.details_menu, menu)
        }
        return true
    }

    private fun startThirdAppByList(urlCandidates: ArrayList<String>) {
        for (url in urlCandidates) {
            try {
                startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .setData(Uri.parse(url))
                )
                return
            } catch (e: Exception) {
                // No need to process
            }
        }
    }

    private fun initTimeline() {
        val layoutManager = LinearLayoutManager(this)
        val lineColor = ContextCompat.getColor(this, R.color.timelineLineColor)
        val attributes = TimelineAttributes(
            markerSize = 10.dp,
            markerColor = Color.TRANSPARENT,
            markerInCenter = true,
            markerLeftPadding = 0.dp,
            markerTopPadding = 0.dp,
            markerRightPadding = 0.dp,
            markerBottomPadding = 0.dp,
            linePadding = 0.dp,
            startLineColor = lineColor,
            endLineColor = lineColor,
            lineStyle = TimelineView.LineStyle.NORMAL,
            lineWidth = 4.dp,
            lineDashWidth = 4.dp,
            lineDashGap = 2.dp
        )
        timelineAdapter = TimeLineAdapter(expressDetailsNodes, attributes)

        viewBinding.rvTimeline.apply {
            this.layoutManager = layoutManager
            this.adapter = timelineAdapter
        }
    }

    private fun getSavedCredential() = dataStore.data.map {
        Credential(
            it[DataStoreKeys.CUSTOMER] ?: "",
            it[DataStoreKeys.SECRET_KEY] ?: ""
        )
    }.asLiveData()
}