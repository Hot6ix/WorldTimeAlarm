package com.simples.j.worldtimealarm

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import kotlinx.android.synthetic.main.activity_license.*
import java.io.IOException
import java.io.InputStream

class LicenseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        try {
            var inputStream: InputStream = assets.open("apache_license")
            val byteArray = ByteArray(1024)
            val text = StringBuilder()
            while(inputStream.read(byteArray) > 0) {
                text.append(String(byteArray))
            }
            text.append("\n\n\n\n\n\n")
            inputStream = assets.open("mit_license")
            while(inputStream.read(byteArray) > 0) {
                text.append(String(byteArray))
            }

            license_text.text = text
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when(item!!.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
