package com.simples.j.worldtimealarm.fragments


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.models.ContentSelectorViewModel
import com.simples.j.worldtimealarm.utils.MediaCursor.Companion.formatDate
import kotlinx.android.synthetic.main.fragment_date_picker.*
import java.util.*

class DatePickerFragment : Fragment(), TabLayout.OnTabSelectedListener, CalendarView.OnDateChangeListener {

    private lateinit var fragmentContext: Context
    private lateinit var viewModel: ContentSelectorViewModel

    private var startDateTab: TabLayout.Tab? = null
    private var endDateTab: TabLayout.Tab? = null

    private val sharedCalendar = Calendar.getInstance()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        fragmentContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_date_picker, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activity?.run {
            viewModel = ViewModelProvider(this)[ContentSelectorViewModel::class.java]
        }

        calendar.minDate = System.currentTimeMillis()
        calendar.setOnDateChangeListener(this)

        when(viewModel.currentTab) {
            0 -> {
                sharedCalendar.timeInMillis = viewModel.startDate.value ?: System.currentTimeMillis()
                calendar.date = sharedCalendar.timeInMillis
            }
            1 -> {
                sharedCalendar.timeInMillis = viewModel.endDate.value ?: System.currentTimeMillis()
                calendar.date = sharedCalendar.timeInMillis
            }
        }

        startDateTab = tab_layout.getTabAt(0)?.apply {
            view.findViewById<TextView>(R.id.title)?.text = getString(R.string.range_start)
        }
        endDateTab = tab_layout.getTabAt(1)?.apply {
            view.findViewById<TextView>(R.id.title)?.text = getString(R.string.range_end)
        }

        tab_layout.getTabAt(viewModel.currentTab)?.select()
        tab_layout.addOnTabSelectedListener(this)

        // Observe
        viewModel.startDate.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            it?.let {
                if(it > 0) setTabSummary(startDateTab, formatDate(fragmentContext, it))
                else setTabSummary(startDateTab, getString(R.string.range_not_set))
            }
        })
        viewModel.endDate.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            it?.let {
                if(it > 0) setTabSummary(endDateTab, formatDate(fragmentContext, it))
                else setTabSummary(endDateTab, getString(R.string.range_not_set))
            }
        })
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        viewModel.currentTab = tab?.position ?: 0

        when(tab?.position) {
            0 -> {
                calendar.date = viewModel.startDate.value ?: System.currentTimeMillis()
            }
            1 -> {
                calendar.date = viewModel.endDate.value ?: System.currentTimeMillis()
            }
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {}

    override fun onTabReselected(tab: TabLayout.Tab?) {
        when(viewModel.currentTab) {
            0 -> {
                viewModel.startDate.value = null
                setTabSummary(startDateTab, getString(R.string.range_not_set))
            }
            1 -> {
                viewModel.endDate.value = null
                setTabSummary(endDateTab, getString(R.string.range_not_set))
            }
        }
    }

    override fun onSelectedDayChange(view: CalendarView, year: Int, month: Int, dayOfMonth: Int) {
        sharedCalendar.set(year, month, dayOfMonth)

        when(viewModel.currentTab) {
            0 -> {
                viewModel.startDate.value = sharedCalendar.timeInMillis
            }
            1 -> {
                viewModel.endDate.value = sharedCalendar.timeInMillis
            }
        }
    }

    private fun setTabSummary(tab: TabLayout.Tab?, content: String?) {
        tab?.view?.findViewById<TextView>(R.id.summary)?.text = content
    }

    companion object {
        const val TAG = "ContentSelectorFragment"

        @JvmStatic
        fun newInstance() = DatePickerFragment()
    }
}
