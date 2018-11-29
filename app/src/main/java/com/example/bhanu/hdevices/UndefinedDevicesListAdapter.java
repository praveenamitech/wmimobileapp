package com.example.bhanu.hdevices;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bhanu.hdevices.dialogs.AddNewMeterDialog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by bhanu on 13/2/18.
 *
 *
 */

public class UndefinedDevicesListAdapter extends RecyclerView.Adapter<UndefinedDevicesListAdapter.MyViewHolder> {

    private static UndefinedDevicesListAdapter instance = null;
    private static final String TAG = UndefinedDevicesListAdapter.class.getSimpleName();
    private ArrayList<EnergyMeter> devices;
    private Handler handler = null;
    private RecyclerViewClickListener mListener = null;
    private final DatabaseHandler db = Singletons.getInstance().getDatabaseHandler();
    private Fragment fragment;

    private UndefinedDevicesListAdapter(Fragment fragment, RecyclerViewClickListener listener) {
        this.devices = DataHolder.getInstance().getUndefinedDevicesList();
        mListener = listener;
        handler = new Handler();
        this.fragment = fragment;
    }

    public static UndefinedDevicesListAdapter getInstance(Fragment fragment, RecyclerViewClickListener listener) {
        if (instance == null) {
            instance = new UndefinedDevicesListAdapter(fragment, listener);
        }

        return instance;
    }

    public static UndefinedDevicesListAdapter getInstance() {
        return instance;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.connected_devices_list_item, parent, false);
        Log.e(TAG, "creating list item");
        return new MyViewHolder(context, view, mListener);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        if (holder != null) {
            Log.d(TAG, "position : " + position);
            EnergyMeter currentDevice = devices.get(position);

            showReadStatus(holder.statusViews, currentDevice.getReadStatus());
            showUploadStatus(holder.uploadStatusViews, currentDevice.getUploadStatus());

            holder.meterSnoTextView.setText(currentDevice.getMtrNoDisplayText());
            holder.makeTextView.setText(currentDevice.getMakeString());
            holder.indexNoTextView.setText(String.valueOf(currentDevice.getIndex()));
            holder.readAgainCheckBox.setChecked(currentDevice.isReadAgain());

            if (currentDevice.getReadStatus().equals(Constants.PROGRESS)) {
                holder.readAgainCheckBox.setEnabled(false);
            } else {
                holder.readAgainCheckBox.setEnabled(true);
            }

            SimpleDateFormat sdfDate = new SimpleDateFormat(Constants.DATETIME_FORMAT);
            String strTime = null;
            String strDateTime = null;

            try {
                Date date = sdfDate.parse(currentDevice.getTimestamp());
                strTime = CustomUtils.getTime(date);
                strDateTime = sdfDate.format(date);
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                Log.d(TAG, "Date is null");
            }

            holder.timeTextview.setText(strDateTime);

            if (currentDevice.getTimestamp() != null) {
                holder.timeTextview.setVisibility(View.VISIBLE);
            } else {
                holder.timeTextview.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void updateList() {
        Log.d(TAG, "Undefined devices list updated");

        handler.post(this::notifyDataSetChanged);
    }

    public void updateListItem(EnergyMeter device) {
        Log.d(TAG, "Undefined devices list item updated");

        new Thread(() -> {
            Singletons.getInstance().getDatabaseHandler().updateUndefinedMeter(device);

            handler.post(() -> {
                notifyItemChanged(device.getIndex() - 1);
            });
        }).start();
    }

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView indexNoTextView, meterSnoTextView, makeTextView, timeTextview;
        CheckBox readAgainCheckBox;
        ImageButton uploadBtn;

        Context context;

        HashMap<String, View> statusViews = new HashMap<>();
        HashMap<String, View> uploadStatusViews = new HashMap<>();

        MyViewHolder(Context context, View item, RecyclerViewClickListener listener) {
            super(item);

            this.context = context;
            mListener = listener;
            item.setOnClickListener(this);

            indexNoTextView = item.findViewById(R.id.indexNoTextView);
            meterSnoTextView = item.findViewById(R.id.meterSno);
            makeTextView = item.findViewById(R.id.makeTextView);
            timeTextview = item.findViewById((R.id.timeTextView));
            readAgainCheckBox = item.findViewById(R.id.readAgainCheckBox);

            // final status
            statusViews.put(Constants.PROGRESS, item.findViewById(R.id.statusProgress));
            statusViews.put(Constants.FAILED, item.findViewById(R.id.statusFailed));
            statusViews.put(Constants.DONE, item.findViewById(R.id.statusDone));
            statusViews.put(Constants.NA, item.findViewById(R.id.statusNA));

            uploadBtn = item.findViewById(R.id.uploadBtn);
            // upload status
            uploadStatusViews.put(Constants.DONE, item.findViewById(R.id.statusUploaded));
            uploadStatusViews.put(Constants.FAILED, uploadBtn); // upload button displayed when upload failed.
            uploadStatusViews.put(Constants.PROGRESS, item.findViewById(R.id.statusUploading));
            uploadStatusViews.put(Constants.NA, item.findViewById(R.id.statusUploadNA));

            readAgainCheckBox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                // this if condition is needed because listener is executing automatically on scroll (bug in android)
                if (!readAgainCheckBox.isPressed()) {
                    return;
                }

                Log.d(TAG, "Read again clicked");

                EnergyMeter currentDevice = devices.get(getAdapterPosition());
                currentDevice.setReadAgain(isChecked);
                db.updateUndefinedMeter(currentDevice);
            });

            uploadBtn.setOnClickListener(view -> {
                Log.d(TAG, "upload button pressed");

                int position = getAdapterPosition();
                EnergyMeter currentDevice = devices.get(position);

                if (currentDevice.getRelatedFiles().size() == 0) {
                    currentDevice.setUploadStatus(Constants.DONE);
                    notifyItemChanged(position);
                    return;
                }

                new Thread(() -> {
                    try {
                        FtpUploader ftpUploader = Singletons.getInstance().getFtpUploader();
                        ftpUploader.connect();
                        ftpUploader.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                        handler.post(() -> {
                            Toast.makeText(getContext(), "FTP Error!", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    currentDevice.setUploadStatus(Constants.PROGRESS);
                    notifyItemChanged(position);

                    MetersHelper.uploadAndDeleteFiles(currentDevice, isUploaded -> {
                        if (!isUploaded) {
                            currentDevice.setUploadStatus(Constants.FAILED);
                            notifyItemChanged(position);
                        } else {
                            FileManager fileManager = FileManager.getInstance();
                            String fileNamePrefix = currentDevice.getMakeString() + "_" + currentDevice.getMtrNo();
                            currentDevice.setRelatedFiles(fileManager.getFilesStartingWithName(fileNamePrefix));
                            notifyItemChanged(position);
                        }
                    });
                }).start();
            });

            item.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                    menu.add("Edit").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            FragmentManager manager = fragment.getActivity().getSupportFragmentManager();
                            AddNewMeterDialog addNewMeterDialog = new AddNewMeterDialog();
                            Bundle bundle = new Bundle();
                            bundle.putInt("meterIndex", getAdapterPosition());
                            bundle.putString("meterType", "undefined");
                            addNewMeterDialog.setArguments(bundle);
                            addNewMeterDialog.show(manager, "addNewMeterDialog");
                            return true;
                        }
                    });

                    menu.add("Delete").setOnMenuItemClickListener(item1 -> {

                        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {

                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    int position = getAdapterPosition();
                                    db.deleteUndefinedMeter(devices.get(position).getMtrNo());
                                    devices.remove(position);
                                    DataHolder.getInstance().setUndefinedDevicesList(CustomUtils.reorderIndexNo(devices));
                                    notifyDataSetChanged();
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    // do nothing
                                    break;
                            }
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage("Are you sure that you want to delete the meter \""
                                + devices.get(getAdapterPosition()).getMtrNo() + "\"?")
                                .setPositiveButton("Yes", dialogClickListener)
                                .setNegativeButton("No", dialogClickListener);

                        final AlertDialog dialog = builder.create();

                        dialog.setOnShowListener((arg0) -> {
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(context.getResources().getColor(R.color.colorAccent));
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(context.getResources().getColor(R.color.colorAccent));
                        });

                        dialog.show();
                        return true;
                    });
                }
            });
        }

        public Context getContext() {
            return context;
        }

        @Override
        public void onClick(View view) {
            mListener.onClick(view, getAdapterPosition());
        }
    }

    private void showReadStatus(HashMap<String, View>statusViews, String status) {
        for (HashMap.Entry<String, View> entry : statusViews.entrySet()) {
            String key = entry.getKey();
            View view = entry.getValue();

            if (key.equals(status)) {
                view.setVisibility(View.VISIBLE);
            } else {
                view.setVisibility(View.GONE);
            }
        }
    }

    private void showUploadStatus(HashMap<String, View>uploadStatusViews, String status) {
        for (HashMap.Entry<String, View> entry : uploadStatusViews.entrySet()) {
            String key = entry.getKey();
            View view = entry.getValue();

            if (key.equals(status)) {
                view.setVisibility(View.VISIBLE);
            } else {
                view.setVisibility(View.GONE);
            }
        }
    }
}
