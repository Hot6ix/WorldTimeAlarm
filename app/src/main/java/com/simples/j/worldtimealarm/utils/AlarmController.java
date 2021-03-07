package com.simples.j.worldtimealarm.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;

import com.simples.j.worldtimealarm.AlarmReceiver;
import com.simples.j.worldtimealarm.MainActivity;
import com.simples.j.worldtimealarm.R;
import com.simples.j.worldtimealarm.etc.AlarmItem;
import com.simples.j.worldtimealarm.etc.C;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;
import org.threeten.bp.temporal.TemporalAdjusters;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by j on 28/02/2018.
 *
 */

public class AlarmController {

    private AlarmManager alarmManager;
    private static AlarmController ourInstance;
    public static final int TYPE_ALARM = 0;
    public static final int TYPE_SNOOZE = 1;

    public static AlarmController getInstance() {
        if(ourInstance == null) ourInstance = new AlarmController();
        return ourInstance;
    }

    public ZonedDateTime calculateDateTime(@NonNull AlarmItem item, int type) {
        Instant timeSet = Instant.ofEpochMilli(item.getPickerTime());
        ZoneId targetZoneId = ZoneId.of(item.getTimeZone());
        ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(targetZoneId);
        ZonedDateTime target = ZonedDateTime.ofInstant(timeSet, targetZoneId)
                .withYear(now.getYear())
                .withMonth(now.getMonthValue())
                .withDayOfMonth(now.getDayOfMonth())
                .withSecond(0)
                .withNano(0);
        ZonedDateTime start = now;
        ZonedDateTime end;

        if(type == TYPE_ALARM) {
            if(item.getEndDate() != null && item.getEndDate() > 0) {
                Instant endInstant = Instant.ofEpochMilli(item.getEndDate());
                end = ZonedDateTime.ofInstant(endInstant, targetZoneId).withSecond(0).withNano(0);

                if(end.isBefore(now)) {
                    target = end;
                    return target.withSecond(0).withNano(0);
                }
            }

            if(item.getStartDate() != null && item.getStartDate() > 0) {
                Instant startInstant = Instant.ofEpochMilli(item.getStartDate());
                start = ZonedDateTime.ofInstant(startInstant, targetZoneId).withSecond(0).withNano(0);

                if(start.isAfter(now)) {
                    target = start;
                }
            }

            boolean isRepeating = false;
            for(int i : item.getRepeat()) {
                if(i > 0) {
                    isRepeating = true;
                    break;
                }
            }

            if(isRepeating) {
                int[] repeatValues = {7,1,2,3,4,5,6};
                ArrayList<DayOfWeek> repeat = new ArrayList<>();

                for(int i=0; i<item.getRepeat().length; i++) {
                    if(item.getRepeat()[i] > 0) {
                        repeat.add(DayOfWeek.of(repeatValues[i]));
                    }
                }
                Collections.sort(repeat);

                DayOfWeek nextDayOfWeek;

                if(start.isAfter(now)) {
                    nextDayOfWeek = start.getDayOfWeek();
                }
                else {
                    nextDayOfWeek = now.getDayOfWeek();
                }

                boolean isContained = repeat.contains(nextDayOfWeek);

                if(isContained) {
                    // currentDay is contained in repeat
                    if(now.isAfter(target) || now.isEqual(target)) {
                        if(nextDayOfWeek == repeat.get(repeat.size() - 1))
                            nextDayOfWeek = repeat.get(0);
                        else
                            nextDayOfWeek = repeat.get(repeat.indexOf(nextDayOfWeek) + 1);

                        target = target.with(TemporalAdjusters.next(nextDayOfWeek));
                    }
                    else {
                        target = target.with(TemporalAdjusters.nextOrSame(nextDayOfWeek));
                    }
                }
                else {
                    // currentDay is not contained in repeat
                    // If today is not after from repeated day back to current week
                    int nextIndex = nextDayOfWeek.getValue();
                    while(!repeat.contains(nextDayOfWeek)) {
                        nextIndex++;
                        if(nextIndex > 7) nextIndex = 1;

                        nextDayOfWeek = DayOfWeek.of(nextIndex);
                    }

                    target = target.with(TemporalAdjusters.next(nextDayOfWeek));
                }
            }
            else {
                while(target.isBefore(ZonedDateTime.now())) {
                    target = target.plusDays(1);
                }
            }
        }
        else {
            long seconds = item.getSnooze() / 1000;
            target = ZonedDateTime.now().plusSeconds(seconds);
        }

        return target.withSecond(0).withNano(0);
    }

    @Deprecated
    public Calendar calculateDate(AlarmItem item, int type, boolean applyDayRepetition) {
        Calendar calendar = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        Calendar start = (Calendar) today.clone();
        Calendar end = Calendar.getInstance();

        if(item == null) throw new IllegalStateException("AlarmItem is null");

        if(type == TYPE_ALARM) {
            if(item.getEndDate() != null && item.getEndDate() > 0) {
                end.setTimeInMillis(item.getEndDate());

                if(end.before(today)) {
                    String msg = "Alarm had been expired : ID("+item.getNotiId()+1+")";
                    throw new IllegalStateException(msg);
                }
            }
            else end = null;

            if(item.getStartDate() != null && item.getStartDate() > 0)
                start.setTimeInMillis(item.getStartDate());

            calendar.setTimeInMillis(Long.parseLong(item.getTimeSet()));
            if(start.after(today)) {
                calendar.setTimeInMillis(start.getTimeInMillis());
            }
            else {
                calendar.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
                calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
                calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));
            }
            calendar.set(Calendar.SECOND, 0);

            // Check if alarm repeating
            boolean isRepeating = false;
            for(int i : item.getRepeat()) {
                if(i > 0) {
                    isRepeating = true;
                    break;
                }
            }

            if(!isRepeating) {
                // one time alarm
                // add a day until calendar time is after from current time
                while(calendar.getTimeInMillis() < System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }
            }
            else {
                // repeating alarm
                // get repeat information
                int[] repeatValues = {1,2,3,4,5,6,7};
                ArrayList<Integer> repeat = new ArrayList<>();

                long difference = TimeZone.getTimeZone(item.getTimeZone()).getOffset(System.currentTimeMillis()) - TimeZone.getDefault().getOffset(System.currentTimeMillis());
                Calendar tmp = (Calendar) calendar.clone();
                tmp.add(Calendar.MILLISECOND, (int) difference);

                long dayDiff = Math.abs(MediaCursor.Companion.getDayDifference(tmp, calendar, true));

                for(int i=0; i<item.getRepeat().length; i++) {
                    if(item.getRepeat()[i] > 0) {
                        int dayOfWeek = i;

                        if(difference != 0 && !MediaCursor.Companion.isSameDay(tmp, calendar) && applyDayRepetition) {
                            if (tmp.getTimeInMillis() > calendar.getTimeInMillis())
                                dayOfWeek -= dayDiff;
                            else if (tmp.getTimeInMillis() < calendar.getTimeInMillis())
                                dayOfWeek += dayDiff;
                        }

                        if(dayOfWeek > 6) {
                            dayOfWeek = dayOfWeek - 7;
                        }
                        if(dayOfWeek < 0) {
                            dayOfWeek = dayOfWeek + 7;
                        }

                        repeat.add(repeatValues[dayOfWeek]);
                    }
                }
                Collections.sort(repeat);

                // Check if today contained in repeats
                int startIndex;

                if(start.after(today)) {
                    startIndex = start.get(Calendar.DAY_OF_WEEK);
                }
                else {
                    startIndex = today.get(Calendar.DAY_OF_WEEK);
                }

                boolean isContained = repeat.contains(startIndex);

                if(isContained) {
                    // currentDay is contained in repeat
                    if(today.getTimeInMillis() >= calendar.getTimeInMillis()) {
                        // If today is last repeat day, return to first repeat day
                        if(startIndex == repeat.get(repeat.size()-1)) {
                            calendar.add(Calendar.WEEK_OF_MONTH, 1);
                            calendar.set(Calendar.DAY_OF_WEEK, repeat.get(0));
                        }
                        else {
                            calendar.set(Calendar.WEEK_OF_MONTH, today.get(Calendar.WEEK_OF_MONTH));
                            calendar.set(Calendar.DAY_OF_WEEK, repeat.get(repeat.indexOf(startIndex)+1));
                        }
                    }
                }
                else {
                    // currentDay is not contained in repeat
                    // If today is not after from repeated day back to current week
                    if(repeat.get(repeat.size()-1) < startIndex) {
                        calendar.add(Calendar.WEEK_OF_MONTH, 1);
                    }

                    int expectedDay = startIndex;
                    while(!repeat.contains(expectedDay)) {
                        expectedDay++;
                        if(expectedDay > 7) expectedDay = 1;
                    }
                    calendar.set(Calendar.DAY_OF_WEEK, expectedDay);

                    while(calendar.before(today)) {
                        calendar.add(Calendar.WEEK_OF_MONTH, 1);
                    }
                }
            }

        }
        else {
            end = null;
            calendar.add(Calendar.MILLISECOND, (int) item.getSnooze());
        }

        if(end != null && calendar.after(end)) {
            // This alarm had been expired
            String msg = "Alarm has been expired : ID("+item.getNotiId()+1+")";
            throw new IllegalStateException(msg);
        }

        return calendar;
    }

    public Long scheduleLocalAlarm(Context context, AlarmItem item, int type) {
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        ZonedDateTime alarmDateTime = null;
        try {
            alarmDateTime = calculateDateTime(item, type);
            item.setTimeSet(String.valueOf(alarmDateTime.toInstant().toEpochMilli()));
        } catch (IllegalStateException | NullPointerException e) {
            e.printStackTrace();
        }

        if(alarmDateTime == null || alarmDateTime.isBefore(ZonedDateTime.now())) {
            disableAlarm(context, item);
            return -1L;
        }

        int notiId = item.getNotiId();

        Intent intent = new Intent(context, AlarmReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(AlarmReceiver.ITEM, item);
        intent.putExtra(AlarmReceiver.OPTIONS, bundle);
        if(type == TYPE_ALARM) {
            intent.setAction(AlarmReceiver.ACTION_ALARM);
        }
        else {
            intent.setAction(AlarmReceiver.ACTION_SNOOZE);
        }

        ZonedDateTime resultDateTime = alarmDateTime.withZoneSameInstant(ZoneId.systemDefault());
        int alarmNotificationId = notiId + 1;
        PendingIntent mainIntent = PendingIntent.getActivity(context, notiId, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, alarmNotificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(resultDateTime.toInstant().toEpochMilli(), mainIntent), alarmIntent);
        Log.d(C.TAG, "Alarm will fire on " + resultDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.FULL)) + ", AlarmItem(" + item + "), " + type);
        Log.d(C.TAG, "Alarm(notiId=" + alarmNotificationId + ", type=" + type + ") scheduled");

        return resultDateTime.toInstant().toEpochMilli();
    }

    @Deprecated
    public Long scheduleAlarm(Context context, AlarmItem item, int type) {
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);

        boolean applyDayRepetition = prefManager.getBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), false);

        Calendar alarmCalendar = null;
        try {
            alarmCalendar = calculateDate(item, type, applyDayRepetition);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        if(alarmCalendar == null) {
            disableAlarm(context, item);
            return -1L;
        }

        int notiId = item.getNotiId();

        Intent intent = new Intent(context, AlarmReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(AlarmReceiver.ITEM, item);
        intent.putExtra(AlarmReceiver.OPTIONS, bundle);
        if(type == TYPE_ALARM) {
            intent.setAction(AlarmReceiver.ACTION_ALARM);
        }
        else {
            intent.setAction(AlarmReceiver.ACTION_SNOOZE);
        }

        int alarmNotificationId = notiId + 1;
        PendingIntent mainIntent = PendingIntent.getActivity(context, notiId, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, alarmNotificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(alarmCalendar.getTimeInMillis(), mainIntent), alarmIntent);
        Log.d(C.TAG, "Alarm will fire on " + SimpleDateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.getDefault()).format(alarmCalendar.getTime()) + ", AlarmItem(" + item + "), " + type);
        Log.d(C.TAG, "Alarm(notiId=" + alarmNotificationId + ", type=" + type + ") scheduled");

        return alarmCalendar.getTimeInMillis();
    }

    public void cancelAlarm(Context context, int notiId) {
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        int alarmNotificationId = notiId + 1;
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(AlarmReceiver.ACTION_ALARM);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, alarmNotificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(alarmIntent);

        intent.setAction(AlarmReceiver.ACTION_SNOOZE);
        alarmIntent = PendingIntent.getBroadcast(context, alarmNotificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(alarmIntent);

        Log.d(C.TAG, "Alarm cancelled : ID(" + alarmNotificationId + ")");
    }

    public void disableAlarm(Context context, AlarmItem item) {
        item.setOn_off(0);

        AppDatabase db = Room.databaseBuilder(context, AppDatabase.class, DatabaseManager.DB_NAME)
                .allowMainThreadQueries()
                .addMigrations(AppDatabase.Companion.getMIGRATION_7_8())
                .build();
        db.alarmItemDao().updateItem(item);

        Intent requestIntent = new Intent(MainActivity.ACTION_UPDATE_SINGLE);
        Bundle bundle = new Bundle();
        bundle.putParcelable(AlarmReceiver.ITEM, item);
        requestIntent.putExtra(AlarmReceiver.OPTIONS, bundle);
        context.sendBroadcast(requestIntent);
    }

    public static final String EXTRA_TIME_IN_MILLIS = "EXTRA_TIME_IN_MILLIS";

}
