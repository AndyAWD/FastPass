package tw.com.andyawd.fastpass

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telephony.SmsManager
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.zxing.integration.android.IntentIntegrator
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), PermissionCallbacks {

    private var smsTimerDisposable: Disposable? = null
    private var smsSendNumber: String = BaseConstants.STRING_EMPTY
    private var smsSendText: String = BaseConstants.STRING_EMPTY
    private var smsSettingTimer: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initComponent()
        initClickListener()

        startScanner()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(sendSmsReceiver)
        } catch (e: Exception) {

        }
    }

    private fun initComponent() {
        val sharedPreferences =
            getSharedPreferences(BaseConstants.FAST_PASS, Context.MODE_PRIVATE)
        scAmAutoSendSms.isChecked = sharedPreferences.getBoolean(BaseConstants.IS_AUTO_SEND, false)

        setSendSmsText(scAmAutoSendSms.isChecked)

        smsSettingTimer = sharedPreferences.getInt(BaseConstants.SMS_SEND_TIMER, 0)
        acsbAmSendTimer.progress = smsSettingTimer
        atvAmSendTimer.text = resources.getString(R.string.setting_sms_timer, smsSettingTimer)
    }

    private fun initClickListener() {

        mbAmStartScanner.clicks()
            .throttleFirst(BaseConstants.CLICK_CLOCK_TIMER, TimeUnit.MILLISECONDS)
            .subscribe(mbAmStartScannerClick)
        mbAmOpenSms.clicks().throttleFirst(BaseConstants.CLICK_CLOCK_TIMER, TimeUnit.MILLISECONDS)
            .subscribe(mbAmOpenSmsClick)
        mbAmSendSmsInformation.clicks()
            .throttleFirst(BaseConstants.CLICK_CLOCK_TIMER, TimeUnit.MILLISECONDS)
            .subscribe(mbAmSendSmsInformationClick)

        scAmAutoSendSms.setOnCheckedChangeListener { _, b ->

            val sharedPreferences =
                getSharedPreferences(BaseConstants.FAST_PASS, Context.MODE_PRIVATE)
            sharedPreferences.edit().putBoolean(BaseConstants.IS_AUTO_SEND, b).apply()

            setSendSmsText(b)

            firebase(BaseConstants.AUTO_SEND_SMS)
        }

        acsbAmSendTimer.setOnSeekBarChangeListener(acsbAmSendTimerSeekBarChange)

    }

    private fun checkSmsPermission() {
        val permission = arrayOf(Manifest.permission.SEND_SMS)
        if (!EasyPermissions.hasPermissions(this, *permission)) {
            EasyPermissions.requestPermissions(
                this,
                resources.getString(R.string.please_sms_permission),
                BaseConstants.SMS_PERMISSIONS_REQUEST_CODE,
                *permission
            )
            return
        }

        autoSendSms()
    }

    private fun startSendSms(isAutoSend: Boolean) {
        if (isAutoSend) {
            checkSmsPermission()
        } else {
            manualSendSms(smsSendNumber, smsSendText)
        }
    }

    private fun manualSendSms(smsSendNumber: String, smsSendText: String) {
        mbAmSendSmsInformation.text = resources.getString(R.string.ready_open_sms_1922_app)
        mbAmSendSmsInformation.icon =
            ActivityCompat.getDrawable(this, R.drawable.check_circle_24_svg)

        val intent = Intent()
        intent.action = Intent.ACTION_SENDTO
        intent.data = Uri.parse("${BaseConstants.SMS_TO_SMALL_CAPS}:$smsSendNumber")
        intent.putExtra(BaseConstants.SMS_BODY, smsSendText)
        startActivity(intent)
    }

    private fun autoSendSms() {
        mbAmSendSmsInformation.text = resources.getString(R.string.sms_start_send)
        mbAmSendSmsInformation.icon =
            ActivityCompat.getDrawable(this, R.drawable.autorenew_24_svg)

        val smsManager = SmsManager.getDefault()

        val sendSmsActionIntent = Intent(BaseConstants.SEND_SMS_ACTION)
        val sendSmsActionBroadcast =
            PendingIntent.getBroadcast(this, 102, sendSmsActionIntent, 0)

        registerReceiver(sendSmsReceiver, IntentFilter(BaseConstants.SEND_SMS_ACTION))

        smsManager.sendTextMessage(
            smsSendNumber,
            null,
            smsSendText,
            sendSmsActionBroadcast,
            null
        )
    }

    private fun setSendSmsText(isChecked: Boolean) {
        if (isChecked) {
            scAmAutoSendSms.text = resources.getString(R.string.auto_send_sms)
        } else {
            scAmAutoSendSms.text = resources.getString(R.string.manual_send_sms)
        }
    }

    private fun startScanner() {
        val intentIntegrator = IntentIntegrator(this)
        intentIntegrator.captureActivity = ScannerCodeActivity::class.java
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        intentIntegrator.setCameraId(0)
        intentIntegrator.setBeepEnabled(false)
        intentIntegrator.setBarcodeImageEnabled(true)
        intentIntegrator.initiateScan()
    }

    private fun firebase(type: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, type)
        FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    private val mbAmStartScannerClick = object : Observer<Unit> {
        override fun onComplete() {

        }

        override fun onSubscribe(d: Disposable) {

        }

        override fun onNext(t: Unit) {
            startScanner()
            firebase(BaseConstants.START_SCANNER_CLICK)
        }

        override fun onError(e: Throwable) {

        }
    }

    private val mbAmOpenSmsClick = object : Observer<Unit> {
        override fun onComplete() {

        }

        override fun onSubscribe(d: Disposable) {

        }

        override fun onNext(t: Unit) {
            manualSendSms(BaseConstants.CDC_SMS_NUMBER, BaseConstants.STRING_EMPTY)
            firebase(BaseConstants.OPEN_SMS_CLICK)
        }

        override fun onError(e: Throwable) {

        }
    }

    private val mbAmSendSmsInformationClick = object : Observer<Unit> {
        override fun onComplete() {

        }

        override fun onSubscribe(d: Disposable) {

        }

        override fun onNext(t: Unit) {
            smsTimerDisposable?.dispose()
            mbAmSendSmsInformation.text = resources.getString(R.string.sms_cancel)
            mbAmSendSmsInformation.isEnabled = false
            scAmAutoSendSms.isEnabled = true
            gAmTimer.visibility = View.VISIBLE

            firebase(BaseConstants.SEND_SMS_INFORMATION_CLICK)
        }

        override fun onError(e: Throwable) {

        }
    }

    private val sendSmsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (resultCode) {
                RESULT_OK -> {
                    mbAmSendSmsInformation.text = resources.getString(R.string.sms_send_success)
                    mbAmSendSmsInformation.icon =
                        context?.let {
                            ActivityCompat.getDrawable(
                                it,
                                R.drawable.check_circle_24_svg
                            )
                        }
                    manualSendSms(BaseConstants.CDC_SMS_NUMBER, BaseConstants.STRING_EMPTY)
                }
                SmsManager.RESULT_NO_DEFAULT_SMS_APP -> {
                    mbAmSendSmsInformation.text = resources.getString(R.string.phone_no_sim)
                }
                else -> {
                    mbAmSendSmsInformation.text =
                        resources.getString(R.string.auto_send_problem_change_manual)
                    scAmAutoSendSms.isChecked = false
                    setSendSmsText(false)
                    manualSendSms(smsSendNumber, smsSendText)
                }
            }
        }
    }

    private val smsTimerSubscribe = object : Observer<Long> {
        override fun onComplete() {

            mbAmSendSmsInformation.text = resources.getString(R.string.sms_send)
            gAmTimer.visibility = View.VISIBLE

            mbAmSendSmsInformation.isEnabled = false
            scAmAutoSendSms.isEnabled = true

            val sharedPreferences =
                getSharedPreferences(BaseConstants.FAST_PASS, Context.MODE_PRIVATE)
            startSendSms(sharedPreferences.getBoolean(BaseConstants.IS_AUTO_SEND, false))
        }

        override fun onSubscribe(d: Disposable) {
            smsTimerDisposable = d
            mbAmSendSmsInformation.visibility = View.VISIBLE
            gAmTimer.visibility = View.GONE
            scAmAutoSendSms.isEnabled = false
        }

        override fun onNext(t: Long) {

            val smsSecond = smsSettingTimer - t - 1L
            mbAmSendSmsInformation.text = resources.getString(R.string.sms_timer, smsSecond)
        }

        override fun onError(e: Throwable) {

        }
    }

    private val acsbAmSendTimerSeekBarChange = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            smsSettingTimer = p1
            atvAmSendTimer.text = resources.getString(R.string.setting_sms_timer, smsSettingTimer)
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {
            atvAmSendTimer.text = resources.getString(R.string.setting_sms_timer, smsSettingTimer)
        }

        override fun onStopTrackingTouch(p0: SeekBar?) {
            val sharedPreferences =
                getSharedPreferences(BaseConstants.FAST_PASS, Context.MODE_PRIVATE)
            sharedPreferences.edit().putInt(BaseConstants.SMS_SEND_TIMER, smsSettingTimer).apply()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result == null) {
            mbAmSendSmsInformation.visibility = View.GONE
            avtAmScannerText.text = resources.getString(R.string.qr_code_scanner_error)
            return
        }

        val scannerText = result.contents

        if (scannerText != null) {

            try {
                mbAmSendSmsInformation.isEnabled = true

                val checkScannerArray: Array<String> = scannerText.split(":").toTypedArray()
                smsSendNumber = checkScannerArray[1]
                smsSendText = checkScannerArray[2]

                if (BaseConstants.CDC_SMS_NUMBER != smsSendNumber) {
                    mbAmSendSmsInformation.visibility = View.GONE
                    avtAmScannerText.text = resources.getString(R.string.sms_not_1922)
                    return
                }

                if (smsSendText.isEmpty()) {
                    mbAmSendSmsInformation.visibility = View.GONE
                    avtAmScannerText.text = resources.getString(R.string.qr_code_empty)
                    return
                }

                if (!BaseConstants.SMS_TO.equals(checkScannerArray[0], false)) {
                    mbAmSendSmsInformation.visibility = View.GONE
                    avtAmScannerText.text = resources.getString(R.string.qr_code_format_error)
                    return
                }

                avtAmScannerText.text =
                    resources.getString(R.string.sms_information, smsSendNumber, smsSendText)

                mbAmSendSmsInformation.icon =
                    ActivityCompat.getDrawable(this, R.drawable.cancel_24_svg)

                Observable
                    .interval(0, 1, TimeUnit.SECONDS)
                    .take(smsSettingTimer.toLong())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(smsTimerSubscribe)

            } catch (e: Exception) {
                mbAmSendSmsInformation.visibility = View.GONE
                avtAmScannerText.text = resources.getString(R.string.qr_code_cannot_send)
            }
        } else {
            mbAmSendSmsInformation.visibility = View.GONE
            avtAmScannerText.text = resources.getString(R.string.qr_code_no_data)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if (BaseConstants.SMS_PERMISSIONS_REQUEST_CODE == requestCode) {
            autoSendSms()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AlertDialog.Builder(this)
                .setTitle(resources.getString(R.string.need_permission))
                .setCancelable(false)
                .setMessage(resources.getString(R.string.open_sms_permission))
                .setPositiveButton(resources.getString(R.string.confirm)) { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts(BaseConstants.PACKAGE, packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
                .show()
        }
    }
}
