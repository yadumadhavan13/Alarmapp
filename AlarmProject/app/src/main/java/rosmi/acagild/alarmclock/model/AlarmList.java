/*
 *
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license.
 *
 * Project Oxford: http://ProjectOxford.ai
 *
 * Project Oxford Mimicker Alarm Github:
 * https://github.com/Microsoft/ProjectOxford-Apps-MimickerAlarm
 *
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License:
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package rosmi.acagild.alarmclock.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import rosmi.acagild.alarmclock.database.AlarmCursorWrapper;
import rosmi.acagild.alarmclock.database.AlarmDatabaseHelper;
import rosmi.acagild.alarmclock.database.AlarmDbSchema;

/**
 * This class is a singleton which represents the current list of alarms.  This is the class via
 * which we interact with the database.  This class enables us to query, update and delete alarm
 * information from the database.
 */
public class AlarmList {
    private static final String ORDER_BY = AlarmDbSchema.AlarmTable.Columns.HOUR + ", " +
                                            AlarmDbSchema.AlarmTable.Columns.MINUTE;
    private static AlarmList sAlarmList;
    private Context mContext;
    private SQLiteDatabase mDatabase;

    private AlarmList(Context context) {
        mContext = context.getApplicationContext();
        mDatabase = new AlarmDatabaseHelper(mContext)
                .getWritableDatabase();
    }

    public static AlarmList get(Context context) {
        if (sAlarmList == null) {
            sAlarmList = new AlarmList(context);
        }
        return sAlarmList;
    }

    private static ContentValues populateContentValues(Alarm alarm) {
        ContentValues values = new ContentValues();
        values.put(AlarmDbSchema.AlarmTable.Columns.UUID, alarm.getId().toString());
        values.put(AlarmDbSchema.AlarmTable.Columns.TITLE, alarm.getTitle());
        values.put(AlarmDbSchema.AlarmTable.Columns.ENABLED, alarm.isEnabled() ? 1 : 0);
        values.put(AlarmDbSchema.AlarmTable.Columns.HOUR, alarm.getTimeHour());
        values.put(AlarmDbSchema.AlarmTable.Columns.MINUTE, alarm.getTimeMinute());
        values.put(AlarmDbSchema.AlarmTable.Columns.TONE, alarm.getAlarmTone() != null ? alarm.getAlarmTone().toString() : "");

        String repeatingDays = "";
        for (int i = 0; i < 7; ++i) {
            repeatingDays += alarm.getRepeatingDay(i) + ",";
        }
        values.put(AlarmDbSchema.AlarmTable.Columns.DAYS, repeatingDays);
        values.put(AlarmDbSchema.AlarmTable.Columns.VIBRATE, alarm.shouldVibrate());
        values.put(AlarmDbSchema.AlarmTable.Columns.TONGUE_TWISTER, alarm.isTongueTwisterEnabled());
        values.put(AlarmDbSchema.AlarmTable.Columns.COLOR_CAPTURE, alarm.isColorCaptureEnabled());
        values.put(AlarmDbSchema.AlarmTable.Columns.EXPRESS_YOURSELF, alarm.isExpressYourselfEnabled());
        values.put(AlarmDbSchema.AlarmTable.Columns.NEW, alarm.isNew() ? 1 : 0);
        values.put(AlarmDbSchema.AlarmTable.Columns.SNOOZED, alarm.isSnoozed() ? 1 : 0);
        values.put(AlarmDbSchema.AlarmTable.Columns.SNOOZED_HOUR, alarm.getSnoozeHour());
        values.put(AlarmDbSchema.AlarmTable.Columns.SNOOZED_MINUTE, alarm.getSnoozeMinute());
        values.put(AlarmDbSchema.AlarmTable.Columns.SNOOZED_SECONDS, alarm.getSnoozeSeconds());

        return values;
    }

    public void addAlarm(Alarm alarm) {
        ContentValues values = populateContentValues(alarm);

        mDatabase.insert(AlarmDbSchema.AlarmTable.NAME, null, values);
    }

    public List<Alarm> getAlarms() {
        List<Alarm> alarms = new ArrayList<>();

        AlarmCursorWrapper cursor = queryAlarms(null, null, ORDER_BY);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            alarms.add(cursor.getAlarm());
            cursor.moveToNext();
        }
        cursor.close();

        return alarms;
    }

    public Alarm getAlarm(UUID id) {
        AlarmCursorWrapper cursor = queryAlarms(
                AlarmDbSchema.AlarmTable.Columns.UUID + " = ?",
                new String[]{id.toString()},
                null
        );

        try {
            if (cursor.getCount() == 0) {
                return null;
            }

            cursor.moveToFirst();
            return cursor.getAlarm();
        } finally {
            cursor.close();
        }
    }

    public void updateAlarm(Alarm alarm) {
        ContentValues values = populateContentValues(alarm);

        mDatabase.update(AlarmDbSchema.AlarmTable.NAME, values,
                AlarmDbSchema.AlarmTable.Columns.UUID + " = ?",
                new String[]{alarm.getId().toString()});
    }

    public void deleteAlarm(Alarm alarm) {
        mDatabase.delete(AlarmDbSchema.AlarmTable.NAME,
                AlarmDbSchema.AlarmTable.Columns.UUID + " = ?",
                new String[] { alarm.getId().toString() });
    }

    private AlarmCursorWrapper queryAlarms(String queryClause, String[] queryArgs, String orderBy) {
        Cursor cursor = mDatabase.query(
                AlarmDbSchema.AlarmTable.NAME,
                null, // gets all columns
                queryClause,
                queryArgs,
                null,
                null,
                orderBy
        );

        return new AlarmCursorWrapper(cursor);
    }
}
