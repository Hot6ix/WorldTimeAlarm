package com.simples.j.worldtimealarm

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_license.*
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.StringBuilder

class LicenseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        var inputStream: InputStream = assets.open("apache_license")
        val apache = inputStream.bufferedReader().readText()
        inputStream = assets.open("mit_license")
        val mit = inputStream.bufferedReader().readText()

        license_text.apply {
            append(apache)
            append("\n\n\n\n\n")
            append(mit)
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
