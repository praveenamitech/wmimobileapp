package com.example.bhanu.hdevices;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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

public class SavedDevicesListAdapter extends RecyclerView.Adapter<SavedDevicesListAdapter.MyViewHolder> {

    private static SavedDevicesListAdapter instance = null;
    private static final String TAG = SavedDevicesListAdapter.class.getSimpleName();
    private ArrayList<EnergyMeter> devices;
    private Handler handler = null;
    private final DatabaseHandler db = Singletons.getInstance().getDatabaseHandler();
    private RecyclerViewClickListener mListener = null;

    private Fragment fragment;

    private SavedDevicesListAdapter(Fragment fragment, RecyclerViewClickListener listener) {
        this.devices = DataHolder.getInstance().getSavedDevicesList();
        handler = new Handler();
        mListener = listener;
        this.fragment = fragment;
    }

    public static SavedDevicesListAdapter getInstance(Fragment fragment, RecyclerViewClickListener listener) {
        if (instance == null) {
            instance = new SavedDevicesListAdapter(fragment, listener);
        }

        return instance;
    }

    public static SavedDevicesListAdapter getInstance() {
        return instance;
    }

    @Override
    public SavedDevicesListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.connected_devices_list_item, parent, false);
        Log.e(TAG, "creating list item");
        return new MyViewHolder(context, view, mListener);
    }

    @Override
    public void onBindViewHolder(SavedDevicesListAdapter.MyViewHolder holder, int position) {
        if (holder != null) {

            EnergyMeter currentDevice = devices.get(position);

            showReadStatus(holder.readStatusViews, currentDevice.getReadStatus());
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

            try {
                Date date = sdfDate.parse(currentDevice.getTimestamp());
                strTime = CustomUtils.getTime(date);
            } catch (ParseException e) {
                // Log.d(TAG, "No timestamp for meter: " + currentDevice.getMtrNo() + " (" +  currentDevice.getMakeString() + ")");
            } catch (NullPointerException e) {
                // Log.d(TAG, "Date is null");
                // e.printStackTrace();
            }

            holder.timeTextview.setText(strTime);

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

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView indexNoTextView, meterSnoTextView, makeTextView, timeTextview;
        CheckBox readAgainCheckBox;
        ImageButton uploadBtn;

        Context context;

        HashMap<String, View> readStatusViews = new HashMap<>();
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
            readStatusViews.put(Constants.PROGRESS, item.findViewById(R.id.statusProgress));
            readStatusViews.put(Constants.FAILED, item.findViewById(R.id.statusFailed));
            readStatusViews.put(Constants.DONE, item.findViewById(R.id.statusDone));
            readStatusViews.put(Constants.NA, item.findViewById(R.id.statusNA));

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
                db.updateMeter(currentDevice);
            });

            uploadBtn.setOnClickListener(view -> {
                Log.d(TAG, "upload button pressed");

                int position = getAdapterPosition();
                EnergyMeter currentDevice = devices.get(position);

                if (currentDevice.getRelatedFiles().size() == 0 && !currentDevice.getUploadStatus().equals(Constants.DONE)) {
                    currentDevice.setUploadStatus(Constants.DONE);
                    db.updateMeter(currentDevice);
                    return;
                }

                new Thread(() -> {
                    try {
                        FtpUploader ftpUploader = Singletons.getInstance().getFtpUploader();
                        ftpUploader.connect();
                        ftpUploader.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                        ApacheFtpUploader.getInstance().disconnect();
                        handler.post(() -> {
                            Toast.makeText(getContext(), "FTP Error!", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    currentDevice.setUploadStatus(Constants.PROGRESS);
                    notifyItemChanged(position);

                    MetersHelper.uploadAndDeleteFiles(currentDevice, isUploaded -> {
                        if (!isUploaded && !currentDevice.getUploadStatus().equals(Constants.FAILED)) {
                            currentDevice.setUploadStatus(Constants.FAILED);
                            notifyItemChanged(position);
                            db.updateMeter(currentDevice);
                        } else {
                            FileManager fileManager = FileManager.getInstance();
                            String fileNamePrefix = currentDevice.getMakeString() + "_" + currentDevice.getMtrNo();
                            currentDevice.setRelatedFiles(fileManager.getFilesStartingWithName(fileNamePrefix));
                            notifyItemChanged(position);
                        }
                    });
                }).start();
            });

            if (getAdapterPosition() != -1) {
                EnergyMeter currentDevice = devices.get(getAdapterPosition());
                if (currentDevice.getRelatedFiles().size() > 0) {
                    notifyItemChanged(getAdapterPosition());
                }
            }

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
                            bundle.putString("meterType", "saved");
                            addNewMeterDialog.setArguments(bundle);
                            addNewMeterDialog.show(manager, "addNewMeterDialog");
                            return true;
                        }
                    });

                    menu.add("Delete").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {

                            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which){
                                        case DialogInterface.BUTTON_POSITIVE:
                                            int position = getAdapterPosition();
                                            db.deleteMeter(devices.get(position).getMtrNo());
                                            devices.remove(position);
                                            DataHolder.getInstance().setSavedDevicesList(CustomUtils.reorderIndexNo(devices));
                                            notifyDataSetChanged();
                                            break;

                                        case DialogInterface.BUTTON_NEGATIVE:
                                            // do nothing
                                            break;
                                    }
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
                        }
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

    public void updateList() {
        Log.d(TAG, "Saved devices list updated");
        handler.post(() -> {
            notifyDataSetChanged();
        });
    }

    public void updateListItem(EnergyMeter device) {
        Log.d(TAG, "Saved devices list item updated");

        new Thread(() -> {
            Singletons.getInstance().getDatabaseHandler().updateMeter(device);

            handler.post(() -> {
                notifyItemChanged(device.getIndex() - 1);
            });
        }).start();
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

