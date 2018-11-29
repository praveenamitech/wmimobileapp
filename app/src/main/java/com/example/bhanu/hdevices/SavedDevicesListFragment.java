package com.example.bhanu.hdevices;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bhanu.hdevices.dialogs.AddNewMeterDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 *  This fragment shows the list of saved meters and their status
 */

public class SavedDevicesListFragment extends Fragment {

    private static final String TAG = SavedDevicesListFragment.class.getSimpleName();
    SavedDevicesListAdapter adapter;
    ArrayList<EnergyMeter> energyMeters;
    DatabaseHandler db;
    Handler handler;

    public SavedDevicesListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_devices_list, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.listView);

        handler = new Handler();
        db = Singletons.getInstance().getDatabaseHandler();

        RecyclerViewClickListener listener = (view1, position) -> {
            Log.d(TAG, "item clicked");
            EnergyMeter energyMeter = DataHolder.getInstance().getSavedDevicesList().get(position);
            Log.d(TAG, energyMeter.toString());

            if (energyMeter.getRelatedFiles().size() == 0) {
                Toast.makeText(getContext(), "There are no files related to the selected meter.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(getContext(), MeterDetailsActivity.class);
            intent.putExtra("meterIndex", position);
            intent.putExtra("meterType", "saved");
            startActivity(intent);
        };

        CheckBox readAgainCheckBox = view.findViewById(R.id.readAgainCheckBox);
        readAgainCheckBox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            Log.d(TAG, "saved devices: read all clicked");
            ArrayList<EnergyMeter> meters = DataHolder.getInstance().getSavedDevicesList();
            for(EnergyMeter m : meters) {
                m.setReadAgain(isChecked);
                Log.d(TAG, m.toString());
                db.updateMeter(m);
            }

            adapter.updateList();
        });

        new Thread(() -> {
            energyMeters = db.getMeters();
            DataHolder.getInstance().setSavedDevicesList(energyMeters);

            handler.post(() -> {
                adapter = SavedDevicesListAdapter.getInstance(this, listener);
                recyclerView.setHasFixedSize(true);
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
                recyclerView.setLayoutManager(mLayoutManager);

                // adding inbuilt divider line
                recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));

                recyclerView.setItemAnimator(new DefaultItemAnimator());

                recyclerView.setAdapter(adapter);
            });

            for(EnergyMeter currentDevice : energyMeters) {
                currentDevice.setRelatedFiles(FileManager.getInstance().getFilesStartingWithName(currentDevice.getFileNamePrefix()));

                int noOfRelatedFiles = currentDevice.getRelatedFiles().size();

                if (noOfRelatedFiles > 0) {
                    currentDevice.setUploadStatus(Constants.FAILED);
                    handler.post(() -> {
                        adapter.notifyItemChanged(currentDevice.getIndex() - 1);
                    });
                }
            }
        }).start();

        // sorting event handlers
        TextView indexNoHeaderText = view.findViewById(R.id.indexNoHeaderText);
        indexNoHeaderText.setOnClickListener(v -> {
            ArrayList<EnergyMeter> meters = DataHolder.getInstance().getSavedDevicesList();

            final Comparator<EnergyMeter> METER_ORDER =
                    (e1, e2) -> {
                        if (e1.getIndex() > e2.getIndex()) {
                            return 1;
                        }

                        return -1;
                    };

            Collections.sort(meters, METER_ORDER);

            adapter.updateList();
        });

        TextView meterSnoHeaderText = view.findViewById(R.id.meterSnoHeaderText);
        meterSnoHeaderText.setOnClickListener(v -> {
            ArrayList<EnergyMeter> meters = DataHolder.getInstance().getSavedDevicesList();

            final Comparator<EnergyMeter> METER_ORDER =
                    (e1, e2) -> {
                        if (e2.getMtrNo().compareTo(e1.getMtrNo()) < 0) {
                            return 1;
                        }

                        return -1;
                    };

            Collections.sort(meters, METER_ORDER);

            for (EnergyMeter m : meters) {
                Log.d(TAG, m.toString());
            }

            adapter.updateList();
        });

        TextView statusHeaderText = view.findViewById(R.id.statusHeaderText);
        statusHeaderText.setOnClickListener(v -> {
            ArrayList<EnergyMeter> meters = DataHolder.getInstance().getSavedDevicesList();

            final Comparator<EnergyMeter> STATUS_ORDER = (e1, e2) -> {
                if (!e1.getReadStatus().equals(Constants.FAILED) && e2.getReadStatus().equals(Constants.FAILED)) {
                    return 1;
                }

                return -1;
            };

            Collections.sort(meters, STATUS_ORDER);

            for (EnergyMeter m : meters) {
                Log.d(TAG, m.toString());
            }

            adapter.updateList();
        });

        TextView uploadStatusHeaderText = view.findViewById(R.id.uploadStatusHeaderText);
        uploadStatusHeaderText.setOnClickListener(v -> {
            ArrayList<EnergyMeter> meters = DataHolder.getInstance().getSavedDevicesList();

            final Comparator<EnergyMeter> STATUS_ORDER = (e1, e2) -> {
                if (!e1.getUploadStatus().equals(Constants.FAILED) && e2.getUploadStatus().equals(Constants.FAILED)) {
                    return 1;
                }

                return -1;
            };

            Collections.sort(meters, STATUS_ORDER);

            for (EnergyMeter m : meters) {
                Log.d(TAG, m.toString());
            }

            adapter.updateList();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        if (adapter != null) {
            adapter.updateList();
        }
    }
}
