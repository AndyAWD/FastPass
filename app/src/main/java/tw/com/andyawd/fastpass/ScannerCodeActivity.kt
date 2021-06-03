package tw.com.andyawd.fastpass

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.journeyapps.barcodescanner.CaptureManager
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