package com.sampleassignment.lifespark.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sampleassignment.lifespark.MainActivity;
import com.sampleassignment.lifespark.R;
import com.sampleassignment.lifespark.callbacklistener.IRecyclerClickListener;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DevicesNameAdapter extends RecyclerView.Adapter<DevicesNameAdapter.ViewHolder> {

    Context context;
    List<String> deviceNames;
    BluetoothDevice[] bluetoothDevices;
    IRecyclerClickListener iRecyclerClickListener;


    public DevicesNameAdapter(Context context, List<String> deviceNames, BluetoothDevice[] bluetoothDevices) {
        this.context = context;
        this.deviceNames = deviceNames;
        this.bluetoothDevices = bluetoothDevices;
        iRecyclerClickListener = (IRecyclerClickListener) context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DevicesNameAdapter.ViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_devices, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView_name.setText(deviceNames.get(position));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iRecyclerClickListener.onClick(position);
            }
        });

    }

    @Override
    public int getItemCount() {
        return deviceNames.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {


        @BindView(R.id.lay_txt_name)
        TextView textView_name;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }


    }
}
