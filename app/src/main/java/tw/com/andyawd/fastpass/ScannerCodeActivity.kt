package tw.com.andyawd.fastpass

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.CaptureManager
import kotlinx.android.synthetic.main.activity_scanner_code.*
import tw.com.andyawd.andyawdlibrary.AWDLog

class ScannerCodeActivity : AppCompatActivity() {

    private var captureManager: CaptureManager? = null
    private var isFlashlightOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner_code)

        initComponent(savedInstanceState)

        atvAscFlashlight.setOnClickListener {
            AWDLog.d("atvAscFlashLight")

            isFlashlightOpen = if (isFlashlightOpen) {
                dbvAscScanner.setTorchOff()
                false
            } else {
                dbvAscScanner.setTorchOn()
                true
            }
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