package com.simples.j.worldtimealarm.fragments


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import com.kizitonwose.calendarview.utils.yearMonth
import com.simples.j.worldtimealarm.ContentSelectorActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.databinding.FragmentDatePickerBinding
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.models.ContentSelectorViewModel
import com.simples.j.worldtimealarm.utils.MediaCursor
import org.threeten.bp.format.TextStyle
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

// https://stackoverflow.com/a/58659241
// This fragment doesn't use view binding due to issue of tab items with view binding
class DatePickerFragment : Fragment(), View.OnClickListener {

    private lateinit var fragmentContext: Context
    private lateinit var viewModel: ContentSelectorViewModel
    private lateinit var binding: FragmentDatePickerBinding

    private var dateSet = ZonedDateTime.now()
    private var currentMonth = YearMonth.now()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        fragmentContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentDatePickerBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activity?.run {
            viewModel = ViewModelProvider(this)[ContentSelectorViewModel::class.java]
        }

        dateSet = dateSet.withZoneSameInstant(ZoneId.of(viewModel.timeZone))

        // day header
        binding.calendarView.dayBinder = object : DayBinder<DayViewContainer> {
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                if(day.owner == DayOwner.THIS_MONTH) {
                    container.textView.text = day.date.dayOfMonth.toString()
                }
                else {
                    container.textView.text = null
                }

                val startDate = viewModel.startDate
                val endDate = viewModel.endDate

                when(day.owner) {
                    DayOwner.THIS_MONTH -> {
                        if(day.date.isBefore(dateSet.toLocalDate())) {
                            container.textView.setTextColor(ContextCompat.getColor(fragmentContext, R.color.textColorDisabled))
                            container.roundView.setBackgroundResource(android.R.color.transparent)
                            container.root.setBackgroundColor(ContextCompat.getColor(fragmentContext, android.R.color.transparent))
                        }
                        else {
                            when {
                                startDate != null && day.date.isEqual(startDate) && endDate == null -> {
                                    container.textView.setTextColor(Color.WHITE)
                                    container.roundView.setBackgroundResource(R.drawable.calendar_day_view_selected)
                                }
                                startDate != null && day.date.isEqual(startDate) -> {
                                    container.textView.setTextColor(Color.WHITE)
                                    container.root.setBackgroundColor(ContextCompat.getColor(fragmentContext, R.color.blueGrayDark))
                                }
                                startDate != null && endDate != null && (day.date.isAfter(startDate) && day.date.isBefore(endDate)) -> {
                                    container.textView.setTextColor(Color.WHITE)
                                    container.root.setBackgroundColor(ContextCompat.getColor(fragmentContext, R.color.blueGray))
                                }
                                endDate != null && day.date.isEqual(endDate) -> {
                                    container.textView.setTextColor(Color.WHITE)
                                    container.root.setBackgroundColor(ContextCompat.getColor(fragmentContext, R.color.blueGrayDark))
                                }
                                day.date.isEqual(dateSet.toLocalDate()) -> {
                                    container.textView.setTextColor(Color.WHITE)
                                    container.root.setBackgroundColor(ContextCompat.getColor(fragmentContext, R.color.color11))
                                }
                                else -> {
                                    container.textView.setTextColor(ContextCompat.getColor(fragmentContext, R.color.textColorEnabled))
                                    container.roundView.setBackgroundResource(android.R.color.transparent)
                                    container.root.setBackgroundColor(ContextCompat.getColor(fragmentContext, android.R.color.transparent))
                                }
                            }
                        }
                    }
                    DayOwner.NEXT_MONTH -> {
                        container.roundView.setBackgroundResource(android.R.color.transparent)
                        if(startDate != null && endDate != null && highlightNextDate(day.date, startDate, endDate)) {
                            container.root.setBackgroundColor(ContextCompat.getColor(fragmentContext, R.color.blueGray))
                        }
                        else {
                            container.root.setBackgroundColor(ContextCompat.getColor(fragmentContext, android.R.color.transparent))
                        }
                    }
                    DayOwner.PREVIOUS_MONTH -> {
                        container.roundView.setBackgroundResource(android.R.color.transparent)
                        if(startDate != null && endDate != null && highlightPrevDate(day.date, startDate, endDate)) {
                            container.root.setBackgroundColor(ContextCompat.getColor(fragmentContext, R.color.blueGray))
                        }
                        else {
                            container.root.setBackgroundColor(ContextCompat.getColor(fragmentContext, android.R.color.transparent))
                        }
                    }
                }

                container.root.setOnClickListener {
                    if(day.date.isBefore(dateSet.toLocalDate())) {
                        Toast.makeText(fragmentContext, "error", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    when {
                        startDate == null && endDate == null -> {
                            viewModel.startDate = day.date
                        }
                        endDate == null -> {
                            if(day.date.isBefore(startDate)) {
                                viewModel.endDate = viewModel.startDate
                                viewModel.startDate = day.date
                            }
                            else {
                                viewModel.endDate = day.date
                            }
                        }
                        else -> {
                            viewModel.startDate = day.date
                            viewModel.endDate = null
                        }
                    }
                    Log.d(C.TAG, "${viewModel.startDate}, ${viewModel.endDate}")

                    binding.calendarView.notifyCalendarChanged()
                }
            }

            override fun create(view: View): DayViewContainer {
                return DayViewContainer(view)
            }

        }

        // month & day of week header
        val dayOfWeek = MediaCursor.getWeekDaysInLocale()
        binding.calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                if(month.yearMonth == YearMonth.of(dateSet.year, dateSet.monthValue)) {
                    container.textView.setTypeface(container.textView.typeface, Typeface.BOLD)
                }
                else {
                    container.textView.setTypeface(container.textView.typeface, Typeface.NORMAL)
                }

                val format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyy MMMM")
                container.textView.text = month.yearMonth.format(DateTimeFormatter.ofPattern(format))

                container.firstDayOfWeek.text = dayOfWeek[0].getDisplayName(TextStyle.NARROW, Locale.getDefault())
                container.secondDayOfWeek.text = dayOfWeek[1].getDisplayName(TextStyle.NARROW, Locale.getDefault())
                container.thirdDayOfWeek.text = dayOfWeek[2].getDisplayName(TextStyle.NARROW, Locale.getDefault())
                container.fourthDayOfWeek.text = dayOfWeek[3].getDisplayName(TextStyle.NARROW, Locale.getDefault())
                container.fifthDayOfWeek.text = dayOfWeek[4].getDisplayName(TextStyle.NARROW, Locale.getDefault())
                container.sixthDayOfWeek.text = dayOfWeek[5].getDisplayName(TextStyle.NARROW, Locale.getDefault())
                container.seventhDayOfWeek.text = dayOfWeek[6].getDisplayName(TextStyle.NARROW, Locale.getDefault())
            }

            override fun create(view: View): MonthViewContainer {
                return MonthViewContainer(view)
            }

        }

        // set up calendar
        binding.calendarView.apply {
            setup(currentMonth, currentMonth.plusMonths(12), WeekFields.of(Locale.getDefault()).firstDayOfWeek)
            scrollToMonth(currentMonth)
        }

        // endless scroll
        binding.calendarView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                when {
                    binding.calendarView.findFirstVisibleMonth()?.yearMonth == currentMonth.minusMonths(6) -> {
                        currentMonth = currentMonth.minusMonths(6)

                        when (currentMonth) {
                            YearMonth.of(dateSet.year, dateSet.monthValue) -> {
                                // do nothing
                            }
                            YearMonth.of(dateSet.year, dateSet.monthValue).plusMonths(6) -> {
                                val now = YearMonth.of(dateSet.year, dateSet.monthValue)
                                binding.calendarView.updateMonthRangeAsync(now, now.plusMonths(12))
                            }
                            else -> {
                                binding.calendarView.updateMonthRangeAsync(currentMonth.minusMonths(6), currentMonth.plusMonths(6))
                            }
                        }
                    }
                    binding.calendarView.findLastVisibleMonth()?.yearMonth == currentMonth.plusMonths(6) -> {
                        currentMonth = currentMonth.plusMonths(6)

                        binding.calendarView.updateMonthRangeAsync(currentMonth.minusMonths(6), currentMonth.plusMonths(6))
                    }
                }
            }
        })

        binding.action.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when(view?.id) {
            R.id.action -> {
                val resultIntent = Intent().apply {
                    putExtra(ContentSelectorActivity.START_DATE_KEY, viewModel.startDate?.toEpochDay())
                    putExtra(ContentSelectorActivity.END_DATE_KEY, viewModel.endDate?.toEpochDay())
                }

                activity?.run {
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            }
        }
    }

    private fun highlightPrevDate(prevDate: LocalDate, startDate: LocalDate, endDate: LocalDate): Boolean {
        if (startDate.yearMonth == endDate.yearMonth) return false
        if (prevDate.yearMonth == startDate.yearMonth) return true
        val firstDayInPrevMonth = prevDate.plusMonths(1).yearMonth.atDay(1)
        return firstDayInPrevMonth >= startDate && firstDayInPrevMonth <= endDate && startDate != firstDayInPrevMonth
    }

    private fun highlightNextDate(nextDate: LocalDate, startDate: LocalDate, endDate: LocalDate): Boolean {
        if (startDate.yearMonth == endDate.yearMonth) return false
        if (nextDate.yearMonth == endDate.yearMonth) return true
        val lastDayInNextMonth = nextDate.minusMonths(1).yearMonth.atEndOfMonth()
        return lastDayInNextMonth >= startDate && lastDayInNextMonth <= endDate && endDate != lastDayInNextMonth
    }

    inner class DayViewContainer(view: View): ViewContainer(view) {
        val root = view
        val textView: TextView = view.findViewById(R.id.calendarDayText)
        val roundView: View = view.findViewById(R.id.calendarRoundView)
    }

    inner class MonthViewContainer(view: View): ViewContainer(view) {
        val root = view
        val textView: TextView = view.findViewById(R.id.calendarMonthText)
        val firstDayOfWeek: TextView = view.findViewById(R.id.firstDayOfWeek)
        val secondDayOfWeek: TextView = view.findViewById(R.id.secondDayOfWeek)
        val thirdDayOfWeek: TextView = view.findViewById(R.id.thirdDayOfWeek)
        val fourthDayOfWeek: TextView = view.findViewById(R.id.fourthDayOfWeek)
        val fifthDayOfWeek: TextView = view.findViewById(R.id.fifthDayOfWeek)
        val sixthDayOfWeek: TextView = view.findViewById(R.id.sixthDayOfWeek)
        val seventhDayOfWeek: TextView = view.findViewById(R.id.seventhDayOfWeek)
    }

    companion object {
        const val TAG = "ContentSelectorFragment"

        @JvmStatic
        fun newInstance() = DatePickerFragment()
    }
}
