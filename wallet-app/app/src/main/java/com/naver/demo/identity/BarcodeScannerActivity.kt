package com.naver.demo.identity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import kotlinx.android.synthetic.main.activity_qrcode.*
import org.json.JSONObject

class BarcodeScannerActivity : BaseCameraActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraView.addFrameProcessor {
            val metadata = FirebaseVisionImageMetadata.Builder()
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setHeight(it.size.height)
                .setWidth(it.size.width)
                .build()
            runBarcodeScanner(FirebaseVisionImage.fromByteArray(it.data, metadata))
        }
    }

    private fun runBarcodeScanner(image: FirebaseVisionImage) {
        //Optional : Define what kind of barcodes you want to scan
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(
                            FirebaseVisionBarcode.FORMAT_QR_CODE
                )
                .build()

        //Get access to an instance of FirebaseBarcodeDetector
        val detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)

        //Use the detector to detect the labels inside the image
        var task = detector.detectInImage(image)
                .addOnSuccessListener {
                    // Task completed successfully
                    for (firebaseBarcode in it) {
                        when (firebaseBarcode.valueType) {
                            //Handle the URL here
                            FirebaseVisionBarcode.TYPE_URL -> {
                                baseContext.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(firebaseBarcode.displayValue)
                                    ).setPackage("com.android.chrome")
                                )
                            }

                            FirebaseVisionBarcode.TYPE_TEXT -> {
                                val data = Intent()
                                data.putExtra("qrcode", firebaseBarcode.rawValue)
                                setResult(RESULT_OK, data)
                                finish()
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    // Task failed with an exception
                     Toast.makeText(baseContext, "Sorry, something went wrong!", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                }
        Tasks.await(task)
    }
}