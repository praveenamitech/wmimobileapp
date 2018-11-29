package com.example.bhanu.hdevices.dialogs;

/**
 * Created by bhanu on 24/2/18.
 */

import android.content.Context;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bhanu.hdevices.Constants;
import com.example.bhanu.hdevices.DataHolder;
import com.example.bhanu.hdevices.DatabaseHandler;
import com.example.bhanu.hdevices.EnergyMeter;
import com.example.bhanu.hdevices.R;
import com.example.bhanu.hdevices.SavedDevicesListAdapter;
import com.example.bhanu.hdevices.Singletons;
import com.example.bhanu.hdevices.UndefinedDevicesListAdapter;

import java.util.ArrayList;

/**
 * Created by bhanu on 30/9/17.
 *
 */

public class AddNewMeterDialog extends DialogFragment implements View.OnClickListener {

    Button saveBtn, cancelBtn;
    EditText serialNoEditText;
    Spinner makeSpinner;
    TextView meterDialogTitle;

    String meterType = null;

    private static final String TAG = AddNewMeterDialog.class.getSimpleName();

    // required empty constructor
    public AddNewMeterDialog() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_new_meter, null);

        saveBtn = view.findViewById(R.id.saveBtn);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        serialNoEditText = view.findViewById(R.id.serialNoEditText);
        makeSpinner = view.findViewById(R.id.makeSpinner);
        meterDialogTitle = view.findViewById(R.id.meter_dialog_title);

        String[] makeValues = getResources().getStringArray(R.array.make_values);

        if (getArguments() != null) {
            meterDialogTitle.setText("EDIT METER");

            // get the meter data required for editing from bundle
            meterType = getArguments().getString("meterType", null);
            int meterIndex = getArguments().getInt("meterIndex", -1);

            // check if editing saved
            if (meterType.equals("saved")) {
                EnergyMeter m = DataHolder.getInstance().getSavedDevicesList().get(meterIndex);
                serialNoEditText.setText(m.getMtrNo());

                for (int i = 0; i < makeValues.length; i++) {
                    if (Integer.parseInt(makeValues[i]) == m.getMake()) {
                        makeSpinner.setSelection(i);
                        break;
                    }
                }

                saveBtn.setOnClickListener(v -> updateEditedMeter(m));
            }

            // check if editing unknown
            else if (meterType.equals("undefined")) {
                EnergyMeter m = DataHolder.getInstance().getUndefinedDevicesList().get(meterIndex);
                serialNoEditText.setText(m.getMtrNo());

                for (int i = 0; i < makeValues.length; i++) {
                    if (Integer.parseInt(makeValues[i]) == m.getMake()) {
                        makeSpinner.setSelection(i);
                        break;
                    }
                }

                saveBtn.setOnClickListener(v -> {
                    updateEditedMeter(m);
                });
            }
        }

        else {
            meterDialogTitle.setText("ADD NEW METER");
            saveBtn.setOnClickListener(this);
        }

        cancelBtn.setOnClickListener(this);
        setCancelable(false);
        return view;
    }

    void updateEditedMeter(EnergyMeter m) {
        String serialNoStr = serialNoEditText.getText().toString();
        int spinner_pos = makeSpinner.getSelectedItemPosition();
        String[] size_values = getResources().getStringArray(R.array.make_values);
        int make = Integer.valueOf(size_values[spinner_pos]);

        if(serialNoStr.isEmpty()) {
            Toast.makeText(getContext(), "Serial number is required!", Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseHandler db = Singletons.getInstance().getDatabaseHandler();

        m.setMtrNo(serialNoStr);
        m.setMake(make);
        db.updateMeter(m);

        if (meterType.equals("saved")) {
            SavedDevicesListAdapter savedDevicesListAdapter = SavedDevicesListAdapter.getInstance();
            savedDevicesListAdapter.updateList();
        }

        else if (meterType.equals("undefined")) {
            UndefinedDevicesListAdapter undefinedDevicesListAdapter = UndefinedDevicesListAdapter.getInstance();
            undefinedDevicesListAdapter.updateList();
        }

        Toast.makeText(getContext(), "Meter updated", Toast.LENGTH_LONG).show();
        dismiss();
    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "button clicked");
        if(view.getId() == R.id.saveBtn) {
            String serialNoStr = serialNoEditText.getText().toString();

            int spinner_pos = makeSpinner.getSelectedItemPosition();
            String[] size_values = getResources().getStringArray(R.array.make_values);
            int make = Integer.valueOf(size_values[spinner_pos]);

            if(serialNoStr.isEmpty()) {
                Toast.makeText(getContext(), "Serial number is required!", Toast.LENGTH_LONG).show();
                return;
            }

            SavedDevicesListAdapter savedDevicesListAdapter = SavedDevicesListAdapter.getInstance();
            UndefinedDevicesListAdapter undefinedDevicesListAdapter = UndefinedDevicesListAdapter.getInstance();

            ArrayList<EnergyMeter> energyMeters;
            energyMeters = DataHolder.getInstance().getSavedDevicesList();

            DatabaseHandler db = Singletons.getInstance().getDatabaseHandler();
            db.addMeter(serialNoStr, make);

            EnergyMeter m = new EnergyMeter();
            m.setMtrNo(serialNoStr);
            m.setMake(make);
            m.setReadAgain(false);
            m.setSavedMeter(true);
            m.setReadStatus(Constants.NA);
            energyMeters.add(m);
            m.setIndex(energyMeters.size());

            savedDevicesListAdapter.notifyItemInserted(energyMeters.size() - 1);

            Toast.makeText(getContext(), "New meter added", Toast.LENGTH_LONG).show();
            dismiss();
        }

        else if(view.getId() == R.id.cancelBtn) {
            dismiss();
        }
    }
}