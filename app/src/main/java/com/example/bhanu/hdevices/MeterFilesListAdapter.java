package com.example.bhanu.hdevices;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by bhanu on 22/2/18.
 */

public class MeterFilesListAdapter extends BaseAdapter {

    private static final String TAG = SavedDevicesListAdapter.class.getSimpleName();
    private LayoutInflater inflater;
    private Handler handler = null;
    private ArrayList<File> files;

    MeterFilesListAdapter(Context context, ArrayList<File> files) {
        this.files = files;
        inflater = (LayoutInflater.from(context));
        handler = new Handler();
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public Object getItem(int position) {
        return files.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MyViewHolder mViewHolder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.file_list_item, parent, false);
            mViewHolder = new MyViewHolder(convertView);
            convertView.setTag(mViewHolder);
        } else {
            mViewHolder = (MyViewHolder) convertView.getTag();
        }

        File file = files.get(position);

        mViewHolder.uploadBtn.setOnClickListener(view -> {
            Log.d(TAG, "Upload clicked");
            String fileName = file.getName();
            mViewHolder.uploadProgressBar.setVisibility(View.VISIBLE);
            mViewHolder.uploadBtn.setEnabled(false);
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
                CustomUtils.uploadFile(file.getName(), isUploaded -> {
                    handler.post(() -> {
                        if (isUploaded) {
                            Toast.makeText(parent.getContext(), "File " + fileName + "  uploaded successfully", Toast.LENGTH_SHORT).show();

                            FileManager.getInstance().moveFile(file.getParent() + "/", file.getName(), file.getParent() + "/uploaded/");
                            Log.d(TAG, "moved file to \'uploaded\' folder: " + file.getName());

                            files.remove(position);

                            mViewHolder.uploadProgressBar.setVisibility(View.GONE);
                            mViewHolder.uploadBtn.setEnabled(true);
                            updateList();
                        } else {
                            mViewHolder.uploadProgressBar.setVisibility(View.GONE);
                            mViewHolder.uploadBtn.setEnabled(true);
                            Toast.makeText(parent.getContext(), "Cannot upload file! Please try again later.", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }).start();
        });

        mViewHolder.fileNameTextView.setText(file.getName());
        mViewHolder.sizeTextView.setText(CustomUtils.friendlyFileSize(file.length()));

        Date date = new Date(file.lastModified());
        // format of the date
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        jdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        String java_date = jdf.format(date);
        mViewHolder.lastModifiedTextView.setText(java_date);

        return convertView;
    }

    private class MyViewHolder {
        TextView fileNameTextView, sizeTextView, lastModifiedTextView;
        Button uploadBtn;
        ProgressBar uploadProgressBar;

        MyViewHolder(View item) {
            fileNameTextView = item.findViewById(R.id.fileNameTextView);
            sizeTextView = item.findViewById(R.id.sizeTextView);
            lastModifiedTextView = item.findViewById(R.id.lastModifiedTextView);
            uploadBtn = item.findViewById(R.id.uploadBtn);
            uploadProgressBar = item.findViewById(R.id.uploadProgressBar);
        }
    }

    public void updateList() {
        handler.post(this::notifyDataSetChanged);
    }
}
