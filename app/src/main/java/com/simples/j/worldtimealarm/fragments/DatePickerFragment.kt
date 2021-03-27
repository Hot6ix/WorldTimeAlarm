package com.simples.j.worldtimealarm.fragments


import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.*
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.MonthScrollListener
import com.kizitonwose.calendarview.ui.ViewContainer
import com.simples.j.worldtimealarm.ContentSelectorActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.databinding.FragmentDatePickerBinding
import com.simples.j.worldtimealarm.models.ContentSelectorViewModel
import com.simples.j.worldtimealarm.utils.AlarmStringFormatHelper
import com.simples.j.worldtimealarm.utils.MediaCursor
import org.threeten.bp.format.TextStyle
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.*

class DatePickerFragment : Fragment(), View.OnClickListener {

    private lateinit var fragmentContext: Context
    private lateinit var viewModel: ContentSelectorViewModel
    private lateinit var binding: FragmentDatePickerBinding

    private var dateSet = ZonedDateTime.now()
    private var now = YearMonth.now()

    @RequiresApi(Build.VERSION_CODES.N)
    private val icuCalendar = Calendar.getInstance()

    private fun Float.toSp(): Float {
        return this / resources.displayMetrics.scaledDensity
    }

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

        viewModel.selected.observe(viewLifecycleOwner) {
            highlightCardView(it)
        }

        dateSet = dateSet.withZoneSameInstant(ZoneId.of(viewModel.timeZone))
        setDateRangeText()

        val locale =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    MediaCursor.getULocaleByTimeZoneId(viewModel.timeZone)?.toLocale() ?: Locale.getDefault()
                else Locale.getDefault()

        // day header
        binding.calendarView.dayBinder = object : DayBinder<DayViewContainer> {
            override fun bind(container: DayViewContainer, day: CalendarDay) {

                val startDate = viewModel.startDate
                val endDate = viewModel.endDate

                container.highlightView.setBackgroundResource(android.R.color.transparent)
                container.textView.setTextColor(ContextCompat.getColor(fragmentContext, R.color.textColorEnabled))
                container.textView.setBackgroundResource(android.R.color.transparent)

                when(day.owner) {
                    DayOwner.THIS_MONTH -> {
                        container.textView.text = day.date.dayOfMonth.toString()

                        if(day.date.isBefore(dateSet.toLocalDate())) {
                            container.textView.apply {
                                setTextColor(ContextCompat.getColor(fragmentContext, R.color.textColorDisabled))
                                setBackgroundResource(android.R.color.transparent)
                            }
                        }
                        else {
                            when {
                                startDate != null && day.date.isEqual(startDate) && endDate == null -> {
                                    container.textView.setTextColor(Color.WHITE)
                                    container.highlightView.setBackgroundResource(R.drawable.calendar_day_selected)
                                }
                                startDate != null && day.date.isEqual(startDate) -> {
                                    container.textView.setTextColor(Color.WHITE)
                                    container.textView.setBackgroundResource(R.drawable.calendar_day_selected_start)
                                }
                                startDate != null && endDate != null && (day.date.isAfter(startDate) && day.date.isBefore(endDate)) -> {
                                    container.textView.setTextColor(Color.WHITE)
                                    container.textView.setBackgroundResource(R.drawable.calendar_day_selected_middle)
                                }
                                endDate != null && day.date.isEqual(endDate) && startDate == null -> {
                                    container.textView.setTextColor(Color.WHITE)
                                    container.highlightView.setBackgroundResource(R.drawable.calendar_day_selected)
                                }
                                endDate != null && day.date.isEqual(endDate) -> {
                                    container.textView.setTextColor(Color.WHITE)
                                    container.textView.setBackgroundResource(R.drawable.calendar_day_selected_end)
                                }
                                day.date.isEqual(dateSet.toLocalDate()) -> {
                                    container.textView.setTextColor(ContextCompat.getColor(fragmentContext, R.color.textColorEnabled))
                                    container.highlightView.setBackgroundResource(R.drawable.calendar_day_today)
                                }
                            }
                        }
                    }
                    DayOwner.NEXT_MONTH -> {
                        container.textView.text = null
                        container.textView.setTextColor(ContextCompat.getColor(fragmentContext, R.color.textColorDisabled))
                    }
                    DayOwner.PREVIOUS_MONTH -> {
                        container.textView.text = null
                        container.textView.setTextColor(ContextCompat.getColor(fragmentContext, R.color.textColorDisabled))
                    }
                }

                container.root.setOnClickListener {
                    if(day.date.isBefore(dateSet.toLocalDate()) || day.owner != DayOwner.THIS_MONTH) {
                        return@setOnClickListener
                    }

                    when(viewModel.selected.value) {
                        0 -> {
                            viewModel.startDate = day.date
                            if(viewModel.endDate != null) {
                                if(day.date.isEqual(viewModel.endDate) || day.date.isAfter(viewModel.endDate))
                                    viewModel.endDate = null
                            }

                            viewModel.selected.value = 1
                            binding.calendarView.notifyCalendarChanged()
                        }
                        1 -> {
                            when {
                                viewModel.startDate != null -> {
                                    when {
                                        day.date.isBefore(viewModel.startDate) -> {
                                            viewModel.startDate = day.date
                                            viewModel.endDate = null
                                        }
                                        day.date.isEqual(viewModel.startDate) -> {}
                                        else -> {
                                            viewModel.endDate = day.date
                                            viewModel.selected.value = -1
                                        }
                                    }
                                }
                                else -> {
                                    viewModel.endDate = day.date
                                    viewModel.selected.value = -1
                                }
                            }

                            binding.calendarView.notifyCalendarChanged()
                        }
                        else -> { }
                    }

                    setDateRangeText()
                }
            }

            override fun create(view: View): DayViewContainer {
                return DayViewContainer(view)
            }

        }

        // month & day of week header
        val dayOfWeek = MediaCursor.getWeekDaysInLocale(locale)
        binding.calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                dayOfWeek.forEachIndexed { index, dayOfWeek ->
                    (container.monthLayout.getChildAt(index) as TextView).apply {
                        text = dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                        tag = dayOfWeek

                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            with(icuCalendar.weekData) {
                                when(dayOfWeek.value) {
                                    // weekend finish
                                    MediaCursor.getDayOfWeekValueFromCalendarToThreeTenBp(weekendCease) -> {
                                        (this@apply).setTextColor(ContextCompat.getColor(fragmentContext, android.R.color.holo_red_light))
                                    }
                                    // weekend start
                                    MediaCursor.getDayOfWeekValueFromCalendarToThreeTenBp(weekendOnset) -> {
                                        (this@apply).setTextColor(ContextCompat.getColor(fragmentContext, android.R.color.holo_blue_light))
                                    }
                                    else -> {
                                        (this@apply).setTextColor(ContextCompat.getColor(fragmentContext, R.color.textColorEnabled))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun create(view: View): MonthViewContainer {
                return MonthViewContainer(view)
            }

        }

        // set up calendar
        binding.calendarView.apply {
            val startDate = viewModel.startDate

            if(startDate != null) {
                val startYearMonth = YearMonth.of(startDate.year, startDate.month)

                if(viewModel.currentYearMonth != startYearMonth && viewModel.currentYearMonth.isBefore(startYearMonth)) {
                    val difference = viewModel.currentYearMonth.until(startYearMonth, ChronoUnit.MONTHS)
                    if(difference >= 6)
                        setup(startYearMonth.minusMonths(6), startYearMonth.plusMonths(6), WeekFields.of(locale).firstDayOfWeek)
                    else
                        setup(startYearMonth.minusMonths(difference), startYearMonth.plusMonths(6 - difference), WeekFields.of(locale).firstDayOfWeek)

                    scrollToMonth(startYearMonth)
                }
                else {
                    setup(viewModel.currentYearMonth, viewModel.currentYearMonth.plusMonths(12), WeekFields.of(locale).firstDayOfWeek)
                    scrollToMonth(viewModel.currentYearMonth)
                }
            }
            else {
                setup(viewModel.currentYearMonth, viewModel.currentYearMonth.plusMonths(12), WeekFields.of(locale).firstDayOfWeek)
                scrollToMonth(viewModel.currentYearMonth)
            }

        }
        updateMonthText(locale, viewModel.currentYearMonth)

        binding.calendarView.monthScrollListener = object : MonthScrollListener {
            override fun invoke(calendarMonth: CalendarMonth) {
                updateMonthText(locale, calendarMonth.yearMonth)
                viewModel.currentYearMonth = calendarMonth.yearMonth

                if(calendarMonth.yearMonth == now) binding.previousMonth.visibility = View.INVISIBLE
                else binding.previousMonth.visibility = View.VISIBLE

                when (calendarMonth.yearMonth) {
                    viewModel.currentYearMonth.plusMonths(6) -> {
                        viewModel.currentYearMonth = viewModel.currentYearMonth.plusMonths(6)
                        binding.calendarView.updateMonthRangeAsync(viewModel.currentYearMonth.minusMonths(6), viewModel.currentYearMonth.plusMonths(6))
                    }
                    viewModel.currentYearMonth.minusMonths(6) -> {
                        viewModel.currentYearMonth = viewModel.currentYearMonth.minusMonths(6)

                        when (viewModel.currentYearMonth) {
                            YearMonth.of(dateSet.year, dateSet.monthValue) -> {
                                // do nothing
                            }
                            YearMonth.of(dateSet.year, dateSet.monthValue).plusMonths(6) -> {
                                val now = YearMonth.of(dateSet.year, dateSet.monthValue)
                                binding.calendarView.updateMonthRangeAsync(now, now.plusMonths(12))
                            }
                            else -> {
                                binding.calendarView.updateMonthRangeAsync(viewModel.currentYearMonth.minusMonths(6), viewModel.currentYearMonth.plusMonths(6))
                            }
                        }
                    }
                }
            }
        }

        // endless scroll
//        binding.calendarView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                super.onScrolled(recyclerView, dx, dy)
//
//                when {
//                    binding.calendarView.findFirstVisibleMonth()?.yearMonth == currentMonth.minusMonths(6) -> {
//                        currentMonth = currentMonth.minusMonths(6)
//
//                        when (currentMonth) {
//                            YearMonth.of(dateSet.year, dateSet.monthValue) -> {
//                                // do nothing
//                            }
//                            YearMonth.of(dateSet.year, dateSet.monthValue).plusMonths(6) -> {
//                                val now = YearMonth.of(dateSet.year, dateSet.monthValue)
//                                binding.calendarView.updateMonthRangeAsync(now, now.plusMonths(12))
//                            }
//                            else -> {
//                                binding.calendarView.updateMonthRangeAsync(currentMonth.minusMonths(6), currentMonth.plusMonths(6))
//                            }
//                        }
//                    }
//                    binding.calendarView.findLastVisibleMonth()?.yearMonth == currentMonth.plusMonths(6) -> {
//                        currentMonth = currentMonth.plusMonths(6)
//
//                        binding.calendarView.updateMonthRangeAsync(currentMonth.minusMonths(6), currentMonth.plusMonths(6))
//                    }
//                }
//            }
//        })

        binding.action.setOnClickListener(this)
        binding.startDateCardView.setOnClickListener(this)
        binding.endDateCardView.setOnClickListener(this)
        binding.clearStartDate.setOnClickListener(this)
        binding.clearEndDate.setOnClickListener(this)
        binding.previousMonth.setOnClickListener(this)
        binding.nextMonth.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when(view?.id) {
            R.id.action -> {
                val resultIntent = Intent().apply {
                    putExtra(ContentSelectorActivity.START_DATE_KEY, viewModel.startDate?.atStartOfDay(ZoneId.of(viewModel.timeZone))?.toInstant()?.toEpochMilli())
                    putExtra(ContentSelectorActivity.END_DATE_KEY, viewModel.endDate?.atStartOfDay(ZoneId.of(viewModel.timeZone))?.toInstant()?.toEpochMilli())
                }

                activity?.run {
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            }
            R.id.startDateCardView -> {
                viewModel.selected.value = 0
            }
            R.id.endDateCardView -> {
                viewModel.selected.value = 1
            }
            R.id.clearStartDate -> {
                viewModel.startDate = null
                setDateRangeText()
                binding.calendarView.notifyCalendarChanged()
            }
            R.id.clearEndDate -> {
                viewModel.endDate = null
                setDateRangeText()
                binding.calendarView.notifyCalendarChanged()
            }
            R.id.previousMonth -> {
                binding.calendarView.smoothScrollToMonth(viewModel.currentYearMonth.minusMonths(1))
            }
            R.id.nextMonth -> {
                binding.calendarView.smoothScrollToMonth(viewModel.currentYearMonth.plusMonths(1))
            }
        }
    }

    private fun highlightCardView(selected: Int) {
        val t = TimeInterpolator {
            it * 30 / 30f
        }
        when(selected) {
            0 -> {
                ObjectAnimator.ofFloat(
                        binding.startDateTitle,
                        "textSize",
                        binding.startDateTitle.textSize.toSp(),
                        resources.getDimension(R.dimen.text_middle).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
                ObjectAnimator.ofFloat(
                        binding.startDate,
                        "textSize",
                        binding.startDate.textSize.toSp(),
                        resources.getDimension(R.dimen.text_large).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
                ObjectAnimator.ofFloat(
                        binding.startDateCardView,
                        "cardElevation",
                        binding.startDateCardView.cardElevation.toSp(),
                        resources.getDimension(R.dimen.highlighted_card_view_elevation).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
                ObjectAnimator.ofFloat(
                        binding.endDateTitle,
                        "textSize",
                        binding.endDateTitle.textSize.toSp(),
                        resources.getDimension(R.dimen.text_small).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
                ObjectAnimator.ofFloat(
                        binding.endDate,
                        "textSize",
                        binding.endDate.textSize.toSp(),
                        resources.getDimension(R.dimen.text_middle).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
                ObjectAnimator.ofFloat(
                        binding.endDateCardView,
                        "cardElevation",
                        binding.endDateCardView.cardElevation.toSp(),
                        resources.getDimension(R.dimen.default_card_view_elevation).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
            }
            1 -> {
                ObjectAnimator.ofFloat(
                        binding.startDateTitle,
                        "textSize",
                        binding.startDateTitle.textSize.toSp(),
                        resources.getDimension(R.dimen.text_small).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
                ObjectAnimator.ofFloat(
                        binding.startDate,
                        "textSize",
                        binding.startDate.textSize.toSp(),
                        resources.getDimension(R.dimen.text_middle).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
                ObjectAnimator.ofFloat(
                        binding.startDateCardView,
                        "cardElevation",
                        binding.startDateCardView.cardElevation.toSp(),
                        resources.getDimension(R.dimen.default_card_view_elevation).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
                ObjectAnimator.ofFloat(
                        binding.endDateTitle,
                        "textSize",
                        binding.endDateTitle.textSize.toSp(),
                        resources.getDimension(R.dimen.text_middle).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
                ObjectAnimator.ofFloat(
                        binding.endDate,
                        "textSize",
                        binding.endDate.textSize.toSp(),
                        resources.getDimension(R.dimen.text_large).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
                ObjectAnimator.ofFloat(
                        binding.endDateCardView,
                        "cardElevation",
                        binding.endDateCardView.cardElevation.toSp(),
                        resources.getDimension(R.dimen.highlighted_card_view_elevation).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
            }
            else -> {
                ObjectAnimator.ofFloat(
                        binding.startDateTitle,
                        "textSize",
                        binding.startDateTitle.textSize.toSp(),
                        resources.getDimension(R.dimen.text_small).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
                ObjectAnimator.ofFloat(
                        binding.startDate,
                        "textSize",
                        binding.startDate.textSize.toSp(),
                        resources.getDimension(R.dimen.text_middle).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
                ObjectAnimator.ofFloat(
                        binding.startDateCardView,
                        "cardElevation",
                        binding.startDateCardView.cardElevation.toSp(),
                        resources.getDimension(R.dimen.default_card_view_elevation).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
                ObjectAnimator.ofFloat(
                        binding.endDateTitle,
                        "textSize",
                        binding.endDateTitle.textSize.toSp(),
                        resources.getDimension(R.dimen.text_small).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
                ObjectAnimator.ofFloat(
                        binding.endDate,
                        "textSize",
                        binding.endDate.textSize.toSp(),
                        resources.getDimension(R.dimen.text_middle).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
                ObjectAnimator.ofFloat(
                        binding.endDateCardView,
                        "cardElevation",
                        binding.endDateCardView.cardElevation.toSp(),
                        resources.getDimension(R.dimen.default_card_view_elevation).toSp()
                ).apply {
                    duration = ANIMATOR_DURATION
                    interpolator = t
                    start()
                }
            }
        }
    }

    private fun updateMonthText(locale: Locale, yearMonth: YearMonth) {
        val format = DateFormat.getBestDateTimePattern(locale, "yyyy MMMM")
        binding.month.text = yearMonth.format(DateTimeFormatter.ofPattern(format))
    }

    private fun setDateRangeText() {
        if(viewModel.startDate != null) {
            binding.startDate.text = AlarmStringFormatHelper.formatSingleDate(fragmentContext, viewModel.startDate?.atStartOfDay(ZoneId.of(viewModel.timeZone)))
            binding.clearStartDate.visibility = View.VISIBLE
        }
        else {
            binding.startDate.text = getString(R.string.range_not_set)
            binding.clearStartDate.visibility = View.INVISIBLE
        }

        if(viewModel.endDate != null) {
            binding.endDate.text = AlarmStringFormatHelper.formatSingleDate(fragmentContext, viewModel.endDate?.atStartOfDay(ZoneId.of(viewModel.timeZone)))
            binding.clearEndDate.visibility = View.VISIBLE
        }
        else {
            binding.endDate.text = getString(R.string.range_not_set)
            binding.clearEndDate.visibility = View.INVISIBLE
        }
    }

    inner class DayViewContainer(view: View): ViewContainer(view) {
        val root = view
        val textView: TextView = view.findViewById(R.id.calendarDayText)
        val highlightView: View = view.findViewById(R.id.calendarDayHighlightView)
    }

    inner class MonthViewContainer(view: View): ViewContainer(view) {
        val root = view
        val monthLayout: ConstraintLayout = view.findViewById(R.id.monthLayout)
    }

    companion object {
        const val TAG = "ContentSelectorFragment"
        const val ANIMATOR_DURATION = 150L

        @JvmStatic
        fun newInstance() = DatePickerFragment()
    }
}
