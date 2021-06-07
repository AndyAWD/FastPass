package tw.com.andyawd.fastpass

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.journeyapps.barcodescanner.CaptureManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_scanner_code.*

class ScannerCodeActivity : AppCompatActivity() {

    private var captureManager: CaptureManager? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner_code)

        initComponent(savedInstanceState)
        initClickListener()
    }

    private fun initComponent(savedInstanceState: Bundle?) {
        vibrator = getSystemService(Service.VIBRATOR_SERVICE) as (Vibrator)

        val sharedPreferences =
            getSharedPreferences(BaseConstants.FAST_PASS, Context.MODE_PRIVATE)
        scAscVibratorSwitch.isChecked =
            sharedPreferences.getBoolean(BaseConstants.VIBRATOR, true)

        captureManager = CaptureManager(this, dbvAscScanner)
        captureManager?.initializeFromIntent(intent, savedInstanceState)
        captureManager?.decode()

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            scAscFlashlightSwitch.visibility = View.VISIBLE
        } else {
            scAscFlashlightSwitch.visibility = View.GONE
        }

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

                vibratorStart()

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

    private fun initClickListener() {
        scAscFlashlightSwitch.setOnCheckedChangeListener { _, b ->
            if (b) {
                dbvAscScanner.setTorchOn()
            } else {
                dbvAscScanner.setTorchOff()
            }

            firebase(BaseConstants.FLASHLIGHT_SWITCH)
        }

        scAscVibratorSwitch.setOnCheckedChangeListener { _, b ->
            if (b) {
                vibratorStart()
            }

            val sharedPreferences =
                getSharedPreferences(BaseConstants.FAST_PASS, Context.MODE_PRIVATE)
            sharedPreferences.edit().putBoolean(BaseConstants.VIBRATOR, b).apply()

            firebase(BaseConstants.VIBRATOR)
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

    private fun vibratorStart() {
        if (vibrator?.hasVibrator() != true) {
            return
        }

        if (!scAscVibratorSwitch.isChecked) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(500, 255))
        } else {
            vibrator?.vibrate(500)
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