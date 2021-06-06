package tw.com.andyawd.fastpass

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.journeyapps.barcodescanner.CaptureManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_scanner_code.*

class ScannerCodeActivity : AppCompatActivity() {

    private var captureManager: CaptureManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner_code)

        initComponent(savedInstanceState)
        initClickListener()
    }

    private fun initClickListener() {
        scAscFlashlightSwitch.setOnCheckedChangeListener { _, b ->
            if (b) {
                dbvAscScanner.setTorchOn()
            } else {
                dbvAscScanner.setTorchOff()
            }

            firebase(BaseConstants.FLASHLIGHT_SWITCH)
        }
    }

    override fun onResume() {
        super.onResume()
        captureManager?.onResume()
    }

    override fun onPause() {
        super.onPause()
        captureManager?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        captureManager?.onDestroy()
    }

    private fun initComponent(savedInstanceState: Bundle?) {
        captureManager = CaptureManager(this, dbvAscScanner)
        captureManager?.initializeFromIntent(intent, savedInstanceState)
        captureManager?.decode()

        dbvAscScanner.decodeContinuous {
            try {
                val checkScannerArray: Array<String> =
                    it.result.toString().split(":").toTypedArray()
                val smsSendNumber = checkScannerArray[1]
                val smsSendText = checkScannerArray[2]

                if (BaseConstants.CDC_SMS_NUMBER != smsSendNumber) {
                    return@decodeContinuous
                }

                if (smsSendText.isEmpty()) {
                    return@decodeContinuous
                }

                if (!BaseConstants.SMS_TO.equals(checkScannerArray[0], false)) {
                    return@decodeContinuous
                }

                dbvAscScanner.pause()
                dbvAscScanner.barcodeView.isCameraClosed

                val bundle = Bundle()
                bundle.putString(BaseConstants.SMS_SEND_TEXT, smsSendText)

                val intent = Intent()
                intent.putExtras(bundle)

                setResult(RESULT_OK, intent)
                finish()

            } catch (e: Exception) {

            }

        }
    }

    private fun firebase(type: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, type)
        FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        captureManager?.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        captureManager?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}