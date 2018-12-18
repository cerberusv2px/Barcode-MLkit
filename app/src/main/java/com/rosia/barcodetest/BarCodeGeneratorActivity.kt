package com.rosia.barcodetest

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.android.synthetic.main.activity_bar_code_generator.*

class BarCodeGeneratorActivity : AppCompatActivity() {

    companion object {
        const val BARCODE_TEXT = "barcode"
        fun getIntent(context: Context, barcode: String) = Intent(context
            , BarCodeGeneratorActivity::class.java).apply {
            putExtra(BARCODE_TEXT, barcode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(true)
        }
        setContentView(R.layout.activity_bar_code_generator)
        val barText = intent?.extras?.getString(BARCODE_TEXT)
        txt_barcode.text = barText

        val multiFormatWriter = MultiFormatWriter()
        try {
            val bitMatrix = multiFormatWriter.encode(barText, BarcodeFormat.EAN_13, 200, 200)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)
            image_barcode.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
        }

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
