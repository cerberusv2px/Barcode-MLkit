package com.rosia.barcodetest

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.rosia.barcodetest.R.id.btn_camera
import com.rosia.barcodetest.R.id.imageView
import com.rosia.barcodetest.R.id.textView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private var mImageView: ImageView? = null
    private lateinit var pictureManager: PictureManager
    private var mSelectedImage: Bitmap? = null

    private var mGraphicOverlay: GraphicOverlay? = null
    // Max width (portrait mode)
    private var mImageMaxWidth: Int? = null
    // Max height (portrait mode)
    private var mImageMaxHeight: Int? = null

    private var imageUri = ""
    private var imagePathList = mutableListOf<String>()

    // Functions for loading images from app assets.

    // Returns max image width, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    // Calculate the max width in portrait mode. This is done lazily since we need to
    // wait for
    // a UI layout pass to get the right values. So delay it to first time image
    // rendering time.
    private val imageMaxWidth: Int?
        get() {
            if (mImageMaxWidth == null) {
                mImageMaxWidth = mImageView!!.width
            }

            return mImageMaxWidth
        }

    // Returns max image height, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    // Calculate the max width in portrait mode. This is done lazily since we need to
    // wait for
    // a UI layout pass to get the right values. So delay it to first time image
    // rendering time.
    private val imageMaxHeight: Int?
        get() {
            if (mImageMaxHeight == null) {
                mImageMaxHeight = mImageView!!.height
            }

            return mImageMaxHeight
        }

    // Gets the targeted width / height.
    private val targetedWidthHeight: Pair<Int, Int>
        get() {
            val targetWidth: Int
            val targetHeight: Int
            val maxWidthForPortraitMode = imageMaxWidth!!
            val maxHeightForPortraitMode = imageMaxHeight!!
            targetWidth = maxWidthForPortraitMode
            targetHeight = maxHeightForPortraitMode
            return Pair(targetWidth, targetHeight)
        }

    companion object {
        private val TAG = "MainActivity"

        private const val PICK_FROM_CAMERA = 1
        fun getBitmapFromAsset(context: Context, filePath: String): Bitmap? {
            val assetManager = context.assets

            val `is`: InputStream
            var bitmap: Bitmap? = null
            try {
                `is` = assetManager.open(filePath)
                bitmap = BitmapFactory.decodeStream(`is`)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return bitmap
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pictureManager = PictureManager(this)
        mImageView = findViewById(R.id.imageView)
        btn_camera.setOnClickListener {
            checkCameraPermissionAndProceed()
        }
    }

    private fun checkCameraPermissionAndProceed() {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity as Activity,
                arrayOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PICK_FROM_CAMERA
            )
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PICK_FROM_CAMERA) {
            if (!grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            }
        }
    }

    private fun openCamera() {
        imagePathList.clear()
        pictureManager.startCameraIntent(this@MainActivity) { currentImagePath ->
            if (!currentImagePath.isEmpty()) {
                this.imageUri = currentImagePath
                this.imagePathList.add(this.imageUri)
                if (this.imagePathList.isNotEmpty()) {
                    showCameraThumb(imagePathList)
                }
            }
        }
    }

    private fun showCameraThumb(imagePathList: MutableList<String>) {
        //println("lis size: ${imagePathList.size}")

        imagePathList.forEach {

            //val sd = Environment.getExternalStorageDirectory()
            val image = File(it)

            val bmOptions = BitmapFactory.Options()
            val bitmap = BitmapFactory.decodeFile(image.absolutePath, bmOptions)
            mSelectedImage = bitmap

            //Glide.with(this@MainActivity).load(bitmap).into(imageView)

            if (mSelectedImage != null) {
                // Get the dimensions of the View
                val (targetWidth, maxHeight) = targetedWidthHeight

                // Determine how much to scale down the image
                val scaleFactor = Math.max(
                    mSelectedImage!!.width.toFloat() / targetWidth.toFloat(),
                    mSelectedImage!!.height.toFloat() / maxHeight.toFloat()
                )

                val width = (mSelectedImage!!.width.toFloat() / scaleFactor).toInt()
                val height = (mSelectedImage!!.height.toFloat() / scaleFactor).toInt()
                val resizedBitmap = Bitmap.createScaledBitmap(
                    mSelectedImage!!, width, height, true)

                Glide.with(this@MainActivity).load(resizedBitmap).into(imageView)

                mSelectedImage = resizedBitmap
                getQRCodeDetails(resizedBitmap)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        pictureManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun getQRCodeDetails(bitmap: Bitmap) {
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(
                FirebaseVisionBarcode.FORMAT_ALL_FORMATS
            )
            .build()
        val detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        detector.detectInImage(image)
            .addOnSuccessListener {
                button.visibility  = View.VISIBLE

                for (firebaseBarcode in it) {


                    println("Firebase ${firebaseBarcode.valueType}")
                    textView.text = firebaseBarcode.displayValue //Display contents inside the barcode
                    button.setOnClickListener {
                        startActivity(BarCodeGeneratorActivity.getIntent(this@MainActivity, firebaseBarcode.displayValue!!))
                    }

                    println("Raw value: ${firebaseBarcode.rawValue}")

                    when (firebaseBarcode.valueType) {
                        //Handle the URL here
                        FirebaseVisionBarcode.TYPE_URL -> {
                            println("URL ${firebaseBarcode.url}")
                        }
                        // Handle the contact info here, i.e. address, name, phone, etc.
                        FirebaseVisionBarcode.TYPE_CONTACT_INFO -> {
                            println(" Contact info ${firebaseBarcode.contactInfo}")
                        }
                        // Handle the wifi here, i.e. firebaseBarcode.wifi.ssid, etc.
                        FirebaseVisionBarcode.TYPE_WIFI -> {
                            println("WIFI ${firebaseBarcode.wifi}")
                        }

                        FirebaseVisionBarcode.TYPE_PRODUCT -> {
                            println("Product ${firebaseBarcode.rawValue}")

                        }

                        FirebaseVisionBarcode.TYPE_TEXT -> {
                            println("Text ${firebaseBarcode.valueType}")
                        }
                        //Handle more type of Barcodes
                    }
                }
            }
            .addOnFailureListener {
                it.printStackTrace()
                Toast.makeText(baseContext, "Sorry, something went wrong!", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnCompleteListener {
                //fabProgressCircle.hide()
                //sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                println("Completed")
            }
    }
}
