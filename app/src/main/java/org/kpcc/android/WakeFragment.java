package org.kpcc.android;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WakeFragment extends Fragment {
    private FrameLayout mSetButtonWrapper;
    private Button mCancelButton;
    private TextView mCurrentAlarmHeader;
    private TextView mCurrentAlarmTime;
    private FrameLayout mTimePickerContainer;
    private TimePicker mTimePicker;

    public WakeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wake, container, false);
        mTimePickerContainer = (FrameLayout) view.findViewById(R.id.time_picker_fragment_container);
        mTimePicker = (TimePicker) view.findViewById(R.id.time_picker);
        Button setButton = (Button) view.findViewById(R.id.set_button);
        mSetButtonWrapper = (FrameLayout) view.findViewById(R.id.set_button_wrapper);
        mCancelButton = (Button) view.findViewById(R.id.cancel_button);
        mCurrentAlarmHeader = (TextView) view.findViewById(R.id.current_alarm_header);
        mCurrentAlarmTime = (TextView) view.findViewById(R.id.current_alarm_time);

        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hourOfDay = mTimePicker.getCurrentHour();
                int minute = mTimePicker.getCurrentMinute();

                BaseAlarmManager.WakeManager.getInstance().set(hourOfDay, minute);
                showCurrentAlarmData();
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseAlarmManager.WakeManager.getInstance().cancel();
                showTimePickerPrompt();
            }
        });

        if (BaseAlarmManager.WakeManager.getInstance().isRunning()) {
            showCurrentAlarmData();
        } else {
            final Calendar c = Calendar.getInstance();
            c.setTimeInMillis(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(8));
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            mTimePicker.setIs24HourView(DateFormat.is24HourFormat(getActivity()));
            mTimePicker.setCurrentHour(hour);
            mTimePicker.setCurrentMinute(minute);

            showTimePickerPrompt();
        }

        return view;
    }


    private void showCurrentAlarmData() {
        mSetButtonWrapper.setVisibility(View.GONE);
        mTimePickerContainer.setVisibility(View.GONE);

        mCancelButton.setVisibility(View.VISIBLE);
        mCurrentAlarmHeader.setVisibility(View.VISIBLE);
        mCurrentAlarmTime.setVisibility(View.VISIBLE);

        Date date = DataManager.getInstance().getAlarmDate();

        String hour;
        String ampm;

        // Support 24 hour format.
        if (DateFormat.is24HourFormat(getActivity())) {
            hour = "H";
            ampm = "";
        } else {
            hour = "h";
            ampm = " a";
        }

        SimpleDateFormat df = new SimpleDateFormat("E "+hour+":mm"+ampm, Locale.US);
        mCurrentAlarmTime.setText(df.format(date));
    }

    private void showTimePickerPrompt() {
        mSetButtonWrapper.setVisibility(View.VISIBLE);
        mTimePickerContainer.setVisibility(View.VISIBLE);

        mCancelButton.setVisibility(View.GONE);
        mCurrentAlarmHeader.setVisibility(View.GONE);
        mCurrentAlarmTime.setVisibility(View.GONE);
    }
}
