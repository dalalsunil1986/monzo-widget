package com.emmaguy.monzo.widget.login

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.emmaguy.monzo.widget.MonzoWidgetApp
import com.emmaguy.monzo.widget.R
import com.emmaguy.monzo.widget.balance.RefreshBalanceJobService
import com.emmaguy.monzo.widget.common.gone
import com.emmaguy.monzo.widget.common.visible
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_login.*
import java.util.concurrent.TimeUnit


class LoginActivity : AppCompatActivity(), LoginPresenter.View {
    private val JOB_ID = 1
    private val authCodeChangedRelay = PublishRelay.create<Pair<String, String>>()
    private val presenter by lazy { MonzoWidgetApp.get(this).loginModule.provideLoginPresenter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        presenter.attachView(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val uri = intent?.data
        if (uri != null && uri.toString().startsWith(getString(R.string.callback_url_scheme))) {
            authCodeChangedRelay.accept(Pair(uri.getQueryParameter("code"), uri.getQueryParameter("state")))
        }
    }

    override fun onDestroy() {
        presenter.detachView()

        super.onDestroy()
    }

    override fun loginClicks(): Observable<Unit> {
        return loginButton.clicks()
    }

    override fun authCodeChanges(): Observable<Pair<String, String>> {
        return authCodeChangedRelay
    }

    override fun showLogIn() {
        loginButton.visible()
    }

    override fun hideLoginButton() {
        loginButton.gone()
    }

    override fun showRedirecting() {
        instructionsTextView.text = getString(R.string.login_redirecting_body)
    }

    override fun startLogin(uri: String) {
        CustomTabsIntent.Builder()
                .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .build()
                .launchUrl(this, Uri.parse(uri))
    }

    override fun showLoggingIn() {
        instructionsTextView.text = getString(R.string.login_logging_in_body)
    }

    override fun showLoggedIn() {
        loginButton.gone()
        instructionsTextView.text = getString(R.string.login_logged_in_body)
    }

    override fun startBackgroundRefresh() {
        val component = ComponentName(this, RefreshBalanceJobService::class.java)

        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(JobInfo.Builder(JOB_ID, component)
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(TimeUnit.HOURS.toMillis(1))
                .build())
    }

    override fun showLoading() {
        loginProgressBar.visible()
    }

    override fun hideLoading() {
        loginProgressBar.gone()
    }
}

