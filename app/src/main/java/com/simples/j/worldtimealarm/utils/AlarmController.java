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
        if(type == TYPE_ALARM) {
            try {
                start.setTimeInMillis(Long.parseLong(item.getStartDate()));
            } catch(NumberFormatException e) {
                e.printStackTrace();
            }

            calendar.setTime(new Date(Long.valueOf(item.getTimeSet())));
            if(start.after(today)) {
                calendar.set(Calendar.YEAR, start.get(Calendar.YEAR));
                calendar.set(Calendar.MONTH, start.get(Calendar.MONTH));
                calendar.set(Calendar.DAY_OF_YEAR, start.get(Calendar.DAY_OF_YEAR));
            }
            else {
                calendar.set(Calendar.YEAR, today.get(Calendar.YEAR));
                calendar.set(Calendar.MONTH, today.get(Calendar.MONTH));
                calendar.set(Calendar.DAY_OF_YEAR, today.get(Calendar.DAY_OF_YEAR));
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
                // One time alarm
                // Add a day until calendar time is after from current time

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
                // Repeating alarm
                // Get repeat information
                int[] repeatValues = context.getResources().getIntArray(R.array.day_of_week_values);
                ArrayList<Integer> repeat = new ArrayList<>();
                for(int i=0; i<item.getRepeat().length; i++) {
                    if(item.getRepeat()[i] > 0) repeat.add(repeatValues[i]);
                }

                // Check if today's date contained in repeat information
                int todayIndex = today.get(Calendar.DAY_OF_WEEK);
                int currentDayIndex = 0;
                boolean isContained = false;
                for(int i=0; i<repeat.size(); i++) {
                    if(repeat.get(i) == todayIndex) {
                        currentDayIndex = i;
                        isContained = true;
                        break;
                    }
                }

                if(isContained) {
                    // currentDay is contained in repeat
                    if(today.getTimeInMillis() >= calendar.getTimeInMillis()) {
                        // If today is last repeat day, return to first repeat day
                        if(currentDayIndex == repeat.size() - 1) {
                            calendar.add(Calendar.WEEK_OF_MONTH, 1);
                            calendar.set(Calendar.DAY_OF_WEEK, repeat.get(0));
                        }
                        else {
                            calendar.set(Calendar.WEEK_OF_MONTH, today.get(Calendar.WEEK_OF_MONTH));
                            calendar.set(Calendar.DAY_OF_WEEK, repeat.get(currentDayIndex + 1));
                        }
                    }
                }
                else {
                    // currentDay is not contained in repeat
                    // Add a week
                    // If today is not after from repeated day back to current week
                    calendar.set(Calendar.WEEK_OF_YEAR, start.get(Calendar.WEEK_OF_YEAR));

                    if(repeat.get(repeat.size()-1) < todayIndex) {
                        calendar.set(Calendar.WEEK_OF_YEAR, start.get(Calendar.WEEK_OF_YEAR) + 1);
                    }

                    int expectedDay = start.get(Calendar.DAY_OF_WEEK);
                    if (expectedDay == Calendar.SATURDAY) expectedDay = 1;
                    while(!repeat.contains(expectedDay)) {
                        expectedDay++;
                        if(expectedDay > 7) expectedDay = 1;
                    }
                    calendar.set(Calendar.DAY_OF_WEEK, expectedDay);
                }
            }

            calendar.set(Calendar.SECOND, 0);
        }
        else calendar.add(Calendar.MILLISECOND, (int) item.getSnooze());

        try {
            Calendar end = Calendar.getInstance();
            end.setTimeInMillis(Long.valueOf(item.getEndDate()));
            if(calendar.after(end)) {
                // This alarm had been expired
                Log.d(C.TAG, "Alarm had been expired : ID(${item.notiId+1})");

                item.setOn_off(0);
                new DatabaseCursor(context).updateAlarm(item);

                Intent requestIntent = new Intent(MainActivity.ACTION_UPDATE_SINGLE);
                Bundle bundle = new Bundle();
                bundle.putParcelable(AlarmReceiver.ITEM, item);
                requestIntent.putExtra(AlarmReceiver.OPTIONS, bundle);
                context.sendBroadcast(requestIntent);
                return;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
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

}
