package com.simples.j.worldtimealarm.fragments

import android.content.Context
import android.icu.text.Collator
import android.icu.util.ULocale
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.SearchView
import android.view.*
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.TimeZonePickerActivity
import com.simples.j.worldtimealarm.support.BaseTimeZonePickerAdapter
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.fragment_time_zone_picker.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

// This fragment covers country and time zone list
@RequiresApi(Build.VERSION_CODES.N)
class TimeZonePickerFragment : Fragment(), CoroutineScope, SearchView.OnQueryTextListener {

    private var mListener: OnTimeZoneChangeListener? = null
    private var mRequestType: Int = -1
    private var mCountry: String? = null
    private var mQuery: String = ""
    private var mAdapter: BaseTimeZonePickerAdapter<BaseTimeZonePickerAdapter.AdapterItem>? = null
    private var mType: Int = -1
    private var mList: List<PickerItem> = emptyList()

    private var mSearchMenu: MenuItem? = null
    private var mSearchView: SearchView? = null

    private var mJob = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + mJob

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_time_zone_picker, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        arguments?.let {
            mRequestType = it.getInt(TimeZonePickerActivity.REQUEST_TYPE)
            mType = it.getInt(TimeZonePickerActivity.TYPE)
            when(mRequestType) {
                TimeZonePickerActivity.REQUEST_COUNTRY -> {
                    setHasOptionsMenu(true)
                }
                TimeZonePickerActivity.REQUEST_TIME_ZONE -> {
                    if(mCountry == null) {
                        mCountry = it.getString(TimeZonePickerActivity.GIVEN_COUNTRY)
                    }
                    setHasOptionsMenu(false)
                }
            }
        }

        savedInstanceState?.let {
            mQuery = it.getString(TIME_ZONE_PICKER_QUERY_KEY, "")
        }

        updateActionBarTitleByType(mRequestType)

        launch(coroutineContext) {
            if(mList.isEmpty()) {
                mList = withContext(Dispatchers.IO) {
                    if(mRequestType == TimeZonePickerActivity.REQUEST_COUNTRY) createCountryListAdapterItem()
                    else createTimeZoneListAdapterItem()
                }
            }

            if(mAdapter == null) {
                mAdapter = when(mRequestType) {
                    TimeZonePickerActivity.REQUEST_COUNTRY -> {
                        BaseTimeZonePickerAdapter(context, mList, showItemSummary = false, showItemDifference = false, headerText = null, listener = mLocaleChangeListener)
                    }
                    TimeZonePickerActivity.REQUEST_TIME_ZONE -> {
                        val showItemDifference = when(mType) {
                            TimeZonePickerActivity.TYPE_ALARM_CLOCK -> {
                                true
                            }
                            TimeZonePickerActivity.TYPE_WORLD_CLOCK -> {
                                false
                            }
                            else -> false
                        }
                        BaseTimeZonePickerAdapter(context, mList, true, showItemDifference, mCountry, mTimeZoneInfoChangeListener)
                    }
                    else -> {
                        throw IllegalStateException("Unexpected request type : $mRequestType")
                    }
                }
            }

            if(view != null) {
                time_zone_base_recycler_view.adapter = mAdapter
                time_zone_base_recycler_view.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                progressBar.visibility = View.GONE
            }

            mSearchView?.apply {
                if(isIconified) isIconified = false

                val query = mQuery
                setQuery("", false)
                setQuery(query, false)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnTimeZoneChangeListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement OnTimeZoneChangeListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(TIME_ZONE_PICKER_QUERY_KEY, mQuery)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_time_zone_picker_search, menu)

        mSearchMenu = menu.findItem(R.id.search_timezone)
        mSearchView = mSearchMenu?.actionView as SearchView
        mSearchView?.queryHint = resources.getString(R.string.time_zone_search_hint)
        mSearchView?.setOnQueryTextListener(this)

        mSearchView?.apply {
            isIconified = false
            setQuery(mQuery, false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.search_timezone -> false
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        mQuery = newText
        mAdapter?.filterByText(newText)
        return false
    }

    private fun updateActionBarTitleByType(type: Int) {
        (activity as TimeZonePickerActivity).supportActionBar?.title =
        when(type) {
            TimeZonePickerActivity.REQUEST_COUNTRY -> {
                getString(R.string.timezone_fragment_country_title)
            }
            TimeZonePickerActivity.REQUEST_TIME_ZONE -> {
                getString(R.string.timezone_fragment_time_zone_title)
            }
            else -> getString(R.string.timezone_fragment_title)
        }
    }

    private var mLocaleChangeListener = object : BaseTimeZonePickerAdapter.OnListItemClickListener<BaseTimeZonePickerAdapter.AdapterItem> {
        override fun onListItemClick(item: BaseTimeZonePickerAdapter.AdapterItem) {
            launch(coroutineContext) {
                val list = withContext(Dispatchers.IO) {
                    MediaCursor.getTimeZoneListByCountry(item.id)
                }
                with(list) {
                    if(this.size == 1) {
                        // selected country has only a single time zone
                        mListener?.onTimeZoneChanged(this@with[0].mTimeZone.id)
                        activity?.supportFragmentManager?.popBackStack()
                    }
                    else {
                        val bundle = Bundle().apply {
                            putInt(TimeZonePickerActivity.REQUEST_TYPE, TimeZonePickerActivity.REQUEST_TIME_ZONE)
                            putInt(TimeZonePickerActivity.TYPE, mType)
                            putString(TimeZonePickerActivity.GIVEN_COUNTRY, item.id)
                        }
                        mSearchView?.clearFocus()
                        (activity as TimeZonePickerActivity).startPickerFragment(bundle, TimeZonePickerActivity.TIME_ZONE_PICKER_FRAGMENT_TIME_ZONE_TAG)
                    }
                }
            }
        }
    }

    private var mTimeZoneInfoChangeListener = object : BaseTimeZonePickerAdapter.OnListItemClickListener<BaseTimeZonePickerAdapter.AdapterItem> {
        override fun onListItemClick(item: BaseTimeZonePickerAdapter.AdapterItem) {
            mListener?.onTimeZoneChanged(item.id)
            activity?.supportFragmentManager?.popBackStack(0, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    private fun createCountryListAdapterItem(): List<PickerItem> {
        val list = MediaCursor.getTimeZoneLocales()
        val defaultLocale = ULocale.getDefault()

        val collator = Collator.getInstance(defaultLocale)
        val items = TreeSet(PickerItemComparator(collator))

        list.forEachIndexed { index, uLocale ->
            val name = uLocale.displayCountry
            items.add(PickerItem(index.toLong(), uLocale.country, name))
        }
        return ArrayList(items)
    }

    private fun createTimeZoneListAdapterItem(): List<PickerItem> {
        val list = MediaCursor.getTimeZoneListByCountry(mCountry)
        val defaultLocale = ULocale.getDefault()

        val collator = Collator.getInstance(defaultLocale)
        val items = TreeSet(PickerItemComparator(collator))

        list.forEachIndexed { index, timeZoneInfo ->
            var name = timeZoneInfo.mExemplarName
            if(name == null) {
                name =
                    if(timeZoneInfo.mTimeZone.inDaylightTime(Date())) timeZoneInfo.mDaylightName
                    else timeZoneInfo.mStandardName
            }
            items.add(PickerItem(index.toLong(), timeZoneInfo.mTimeZone.id, name ?: timeZoneInfo.mTimeZone.id, timeZoneInfo.mGmtOffset))
        }
        return ArrayList(items)
    }

    class PickerItem(private val itemIdentifier: Long, private val uId: String, private val name: String, private val subTitle: String? = null) : BaseTimeZonePickerAdapter.AdapterItem {

        override val id: String
            get() = uId
        override val title: String
            get() = name
        override val summary: String?
            get() = subTitle
        override val itemId: Long
            get() = itemIdentifier
        override val searchKeys: Array<String>
            get() = arrayOf(id, name)

        override fun toString(): String {
            return "id : $id, title : $title, summary : $summary, itemId : $itemId, searchKeys : ${searchKeys.joinToString()}"
        }
    }

    interface OnTimeZoneChangeListener {
        fun onTimeZoneChanged(timeZone: String)
    }

    private class PickerItemComparator internal constructor(private val mCollator: Collator) : Comparator<PickerItem> {
        override fun compare(o1: PickerItem, o2: PickerItem): Int {
            return mCollator.compare(o1.title, o2.title)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(listener: OnTimeZoneChangeListener?) = TimeZonePickerFragment().apply {
            mListener = listener
        }

        const val TIME_ZONE_PICKER_QUERY_KEY = "TIME_ZONE_PICKER_QUERY_KEY"
    }
}
