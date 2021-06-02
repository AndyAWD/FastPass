package tw.com.andyawd.fastpass

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import tw.com.andyawd.andyawdlibrary.AWDConstants
import tw.com.andyawd.andyawdlibrary.AWDLog
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    companion object {
        const val SMS_SEND_TIMER = 8L
    }

    private var smsTimerDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initComponent()
        initClickListener()

        startScanner()
    }

    private fun initClickListener() {
        mbAmStartScanner.setOnClickListener {
            startScanner()
        }

        mbAmOpenSms.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_SENDTO
            intent.data = Uri.parse("smsto:1922")
            intent.putExtra("sms_body", "")
            startActivity(intent)
        }

        mbAmSendSmsInformation.setOnClickListener {
            smsTimerDisposable?.dispose()
            mbAmSendSmsInformation.text = resources.getString(R.string.smsCancel)
        }
    }

    private fun initComponent() {

        AWDLog.setLogLevel(AWDConstants.LOG_VERBOSE)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {

            val scannerText = result.contents
            AWDLog.d("scannerText: $scannerText")

            if (scannerText != null) {

                avtAmScannerText.text = scannerText

                Observable
                    .interval(0, 1, TimeUnit.SECONDS)
                    .take(SMS_SEND_TIMER)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(smsTimerSubscribe)

            } else {
                avtAmScannerText.text = "掃不到資料"
            }
        }
    }

    private val smsTimerSubscribe = object : Observer<Long> {
        override fun onComplete() {
            AWDLog.d("onComplete")

            mbAmSendSmsInformation.text = resources.getString(R.string.smsSend)
            gAmTimer.visibility = View.VISIBLE

            val intent = Intent()
            intent.action = Intent.ACTION_SENDTO
            intent.data = Uri.parse("smsto:0910543299")
            intent.putExtra("sms_body", "123456")
            startActivity(intent)
        }

        override fun onSubscribe(d: Disposable) {
            smsTimerDisposable = d
            gAmTimer.visibility = View.GONE
        }

        override fun onNext(t: Long) {

            val smsSecond = SMS_SEND_TIMER - t - 1L

            AWDLog.d("onNext: $t / smsSecond: $smsSecond")
            mbAmSendSmsInformation.text = resources.getString(R.string.smsTimer, smsSecond)
        }

        override fun onError(e: Throwable) {

        }
    }

}
