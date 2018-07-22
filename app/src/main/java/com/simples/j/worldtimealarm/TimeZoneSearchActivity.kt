package com.simples.j.worldtimealarm

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import com.simples.j.worldtimealarm.fragments.WorldClockFragment
import com.simples.j.worldtimealarm.support.TimeZoneAdapter
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import kotlinx.android.synthetic.main.activity_time_zone_search.*
import java.util.*
import kotlin.collections.ArrayList


class TimeZoneSearchActivity : AppCompatActivity(), SearchView.OnQueryTextListener, TimeZoneAdapter.OnItemClickListener {

    private lateinit var timeZoneArray: MutableList<String>
    private lateinit var timeZoneAdapter: TimeZoneAdapter
    private var query: String? = null
    private var resultArray = ArrayList<String>()
    private var code: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_zone_search)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        code = intent.getStringExtra(WorldClockFragment.TIME_ZONE_NEW_KEY)

        if(savedInstanceState != null) {
            query = savedInstanceState.getString(STATE_QUERY)
            resultArray.addAll(savedInstanceState.getStringArrayList(STATE_RESULT))
        }

        timeZoneArray = TimeZone.getAvailableIDs().map { it.replace("_", " ") }.toMutableList()

        // If list does not have system timezone, add it temporarily
        if(!timeZoneArray.contains(TimeZone.getDefault().id)) timeZoneArray.add(TimeZone.getDefault().id)

        timeZoneAdapter = TimeZoneAdapter(resultArray, applicationContext)
        timeZoneAdapter.setOnItemClickListener(this)
        time_zone_search_list.adapter = timeZoneAdapter
        time_zone_search_list.layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_timezonesearch, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu?.findItem(R.id.search_timezone)?.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.isIconified = false
        searchView.queryHint = resources.getString(R.string.hint)
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

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString(STATE_QUERY, query)
        outState?.putStringArrayList(STATE_RESULT, resultArray)

        super.onSaveInstanceState(outState)
    }

    override fun onQueryTextChange(text: String?): Boolean {
        query = text
        resultArray.clear()
        if(text != null && text.isNotEmpty()) {
            resultArray.addAll(timeZoneArray.filter { it.toLowerCase().contains(text.toLowerCase()) })
        }
        if(resultArray.isEmpty()) {
            time_zone_search_list.visibility = View.GONE
            no_result.visibility = View.VISIBLE
        }
        else {
            time_zone_search_list.visibility = View.VISIBLE
            no_result.visibility = View.GONE
        }
        timeZoneAdapter.notifyDataSetChanged()

        return false
    }

    override fun onQueryTextSubmit(text: String?): Boolean {
        query = text
        resultArray.clear()
        if(text != null && text.isNotEmpty()) {
            resultArray.addAll(timeZoneArray.filter { it.toLowerCase().contains(text.toLowerCase()) })
        }
        timeZoneAdapter.notifyDataSetChanged()
        return false
    }

    override fun onItemClick(view: View, position: Int) {
        if(code != null) {
            val array = DatabaseCursor(applicationContext).getClockList()
            var isExist = false
            array.forEach {
                if(it.timezone == resultArray[position]) {
                    isExist = true
                }
            }
            if(isExist) {
                Toast.makeText(applicationContext, resources.getString(R.string.exist_timezone), Toast.LENGTH_SHORT).show()
            }
            else {
                val intent = Intent()
                intent.putExtra(TIME_ZONE_ID, resultArray[position])
                setResult(Activity.RESULT_OK, intent)
                finish()
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
    }
}
