package com.simples.j.worldtimealarm

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.util.Log
import android.view.MenuItem
import com.simples.j.worldtimealarm.etc.C
import kotlinx.android.synthetic.main.activity_license.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.StringBuilder

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
