package com.simples.j.worldtimealarm

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.simples.j.worldtimealarm.databinding.ActivityTimeZoneSearchBinding
import com.simples.j.worldtimealarm.fragments.WorldClockFragment
import com.simples.j.worldtimealarm.support.TimeZoneAdapter
import com.simples.j.worldtimealarm.utils.AppDatabase
import com.simples.j.worldtimealarm.utils.DatabaseManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList


class TimeZoneSearchActivity : AppCompatActivity(), SearchView.OnQueryTextListener, TimeZoneAdapter.OnItemClickListener {

    private lateinit var db: AppDatabase
    private lateinit var timeZoneArray: MutableList<String>
    private lateinit var timeZoneAdapter: TimeZoneAdapter
    private lateinit var binding: ActivityTimeZoneSearchBinding
    private var query: String? = null
    private var resultArray = ArrayList<String>()
    private var code: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimeZoneSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, DatabaseManager.DB_NAME)
                .build()
        code = intent.getStringExtra(WorldClockFragment.TIME_ZONE_NEW_KEY)

        if(savedInstanceState != null) {
            query = savedInstanceState.getString(STATE_QUERY)
            val list = savedInstanceState.getStringArrayList(STATE_RESULT) ?: ArrayList()
            resultArray.addAll(list)
        }

        timeZoneArray = TimeZone.getAvailableIDs().map { it.replace("_", " ") }.toMutableList()

        // If list does not have system timezone, add it temporarily
        with(TimeZone.getDefault().id.replace("_", " ")) {
            if(!timeZoneArray.contains(this)) timeZoneArray.add(this)
        }

        timeZoneAdapter = TimeZoneAdapter(resultArray, applicationContext)
        timeZoneAdapter.setOnItemClickListener(this)
        binding.timeZoneSearchList.adapter = timeZoneAdapter
//        time_zone_search_list.adapter = timeZoneAdapter
        binding.timeZoneSearchList.layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
//        time_zone_search_list.layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_time_zone_search, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu?.findItem(R.id.search_timezone)?.actionView as SearchView
        searchView.apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isIconified = false
            queryHint = resources.getString(R.string.hint)
        }
        searchView.setOnQueryTextListener(this)
        if(query != null && query!!.isNotEmpty()) {
            searchView.setQuery(query, true)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_QUERY, query)
        outState.putStringArrayList(STATE_RESULT, resultArray)

        super.onSaveInstanceState(outState)
    }

    override fun onQueryTextChange(text: String?): Boolean {
        query = text
        resultArray.clear()
        if(text != null && text.isNotEmpty()) {
            resultArray.addAll(timeZoneArray.filter {
                it.lowercase(Locale.ROOT).contains(text.lowercase(Locale.ROOT))
            })
        }
        if(resultArray.isEmpty()) {
            binding.timeZoneSearchList.visibility = View.GONE
//            time_zone_search_list.visibility = View.GONE
            binding.noResult.visibility = View.VISIBLE
//            no_result.visibility = View.VISIBLE
        }
        else {
            binding.timeZoneSearchList.visibility = View.VISIBLE
//            time_zone_search_list.visibility = View.VISIBLE
            binding.noResult.visibility = View.GONE
//            no_result.visibility = View.GONE
        }
        timeZoneAdapter.notifyDataSetChanged()

        return false
    }

    override fun onQueryTextSubmit(text: String?): Boolean {
        query = text
        resultArray.clear()
        if(text != null && text.isNotEmpty()) {
            resultArray.addAll(timeZoneArray.filter {
                it.lowercase(Locale.ROOT).contains(text.lowercase(Locale.ROOT))
            })
        }
        timeZoneAdapter.notifyDataSetChanged()
        return false
    }

    override fun onItemClick(view: View, position: Int) {
        if(code != null) {
            GlobalScope.launch {
                val array = db.clockItemDao().getAll()
                val sameClockItem = array.find {
                    it.timezone == resultArray[position]
                }

                if(sameClockItem != null) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, resources.getString(R.string.exist_timezone), Toast.LENGTH_SHORT).show()
                    }
                }
                else {
                    val intent = Intent()
                    intent.putExtra(TIME_ZONE_ID, resultArray[position])
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }
        }
        else {
            val intent = Intent()
            intent.putExtra(TIME_ZONE_ID, resultArray[position])
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    companion object {
        const val TIME_ZONE_ID = "TIME_ZONE_ID"
        const val STATE_QUERY = "STATE_QUERY"
        const val STATE_RESULT = "STATE_RESULT"
        const val TIME_ZONE_REQUEST_CODE = 1
        const val TIME_ZONE_NEW_CODE = 2
    }
}
