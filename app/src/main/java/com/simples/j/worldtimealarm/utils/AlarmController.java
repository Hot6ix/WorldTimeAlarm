package com.simples.j.worldtimealarm.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.simples.j.worldtimealarm.AlarmReceiver;
import com.simples.j.worldtimealarm.MainActivity;
import com.simples.j.worldtimealarm.R;
import com.simples.j.worldtimealarm.etc.AlarmItem;
import com.simples.j.worldtimealarm.etc.C;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import kotlin.jvm.Throws;

/**
 * Created by j on 28/02/2018.
 *
 */

public class AlarmController {

    private AlarmManager alarmManager;
    private static AlarmController ourInstance;
    public static final int TYPE_ALARM = 0;
    public static final int TYPE_SNOOZE = 1;

    public static AlarmController getInstance(Context context) {
        if(ourInstance == null) ourInstance = new AlarmController(context);
        return ourInstance;
    }

    private AlarmController(Context context) {
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void scheduleAlarm(Context context, AlarmItem item, int type) {
        Calendar calendar = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        if(type == TYPE_ALARM) {
            if(item.getEndDate() != null) {
                end.setTimeInMillis(item.getEndDate());

                if(end.before(today)) {
                    Log.d(C.TAG, "Alarm had been expired : ID("+item.getNotiId()+1+")");
                    disableAlarm(context, item);
                    return;
                }
            }
            else end = null;

            if(item.getStartDate() != null)
                start.setTimeInMillis(item.getStartDate());

            calendar.setTime(new Date(Long.valueOf(item.getTimeSet())));
            if(start.after(today)) {
                calendar.set(start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH));
            }
            else {
                calendar.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
            }

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
                // If expected date is less than 24 hour from today
                // Set to date of today
                if(calendar.getTimeInMillis() - System.currentTimeMillis() > C.ONE_DAY) {
                    calendar.set(Calendar.DAY_OF_YEAR, today.get(Calendar.DAY_OF_YEAR));
                }
            }
            else {
                // repeating alarm
                // get repeat information
                int[] repeatValues = context.getResources().getIntArray(R.array.day_of_week_values);
                ArrayList<Integer> repeat = new ArrayList<>();
                for(int i=0; i<item.getRepeat().length; i++) {
                    if(item.getRepeat()[i] > 0) repeat.add(repeatValues[i]);
                }

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
                            calendar.add(Calendar.WEEK_OF_YEAR, 1);
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
                        calendar.set(Calendar.WEEK_OF_YEAR, start.get(Calendar.WEEK_OF_YEAR) + 1);
                    }

                    int expectedDay = start.get(Calendar.DAY_OF_WEEK);
                    if (expectedDay == Calendar.SATURDAY) expectedDay = 1;
                    while(!repeat.contains(expectedDay)) {
                        expectedDay++;
                        if(expectedDay > 7) expectedDay = 1;
                    }
                    calendar.set(Calendar.DAY_OF_WEEK, expectedDay);
                    if(calendar.before(today)) calendar.add(Calendar.WEEK_OF_MONTH, 1);
                }
            }

            calendar.set(Calendar.SECOND, 0);
        }
        else calendar.add(Calendar.MILLISECOND, (int) item.getSnooze());

        if(calendar.after(end)) {
            // This alarm had been expired
            Log.d(C.TAG, "Alarm has been expired : ID("+item.getNotiId()+1+")");
            disableAlarm(context, item);
            return;
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
            notiId = notiId+2;
        }

        PendingIntent mainIntent = PendingIntent.getActivity(context, notiId, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, notiId+1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), mainIntent), alarmIntent);
        Log.d(C.TAG, "Alarm will fire on " + SimpleDateFormat.getDateTimeInstance().format(calendar.getTime()) + ", Info(" + item + "), " + type);
        Log.d(C.TAG, "Alarm scheduled : ID(" + notiId+1 + ")");
    }

    public void cancelAlarm(Context context, int notiId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(AlarmReceiver.ACTION_ALARM);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, notiId+1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(alarmIntent);
        Log.d(C.TAG, "Alarm cancelled : ID(" + notiId+1 + ")");
    }

    private void disableAlarm(Context context, AlarmItem item) {
        item.setOn_off(0);
        new DatabaseCursor(context).updateAlarm(item);

        Intent requestIntent = new Intent(MainActivity.ACTION_UPDATE_SINGLE);
        Bundle bundle = new Bundle();
        bundle.putParcelable(AlarmReceiver.ITEM, item);
        requestIntent.putExtra(AlarmReceiver.OPTIONS, bundle);
        context.sendBroadcast(requestIntent);
    }

}
