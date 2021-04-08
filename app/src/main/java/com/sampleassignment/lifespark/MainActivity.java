package com.sampleassignment.lifespark;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BlendMode;
import android.graphics.Color;
import android.icu.text.DateFormatSymbols;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialElevationScale;
import com.google.firebase.database.FirebaseDatabase;
import com.sampleassignment.lifespark.adapter.DevicesNameAdapter;
import com.sampleassignment.lifespark.callbacklistener.IRecyclerClickListener;
import com.sampleassignment.lifespark.common.Common;

import org.w3c.dom.Text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.disposables.Disposable;

public class MainActivity extends AppCompatActivity implements IRecyclerClickListener {

    @BindView(R.id.main_relative_layout)
    RelativeLayout main_relative_layout;

    @BindView(R.id.bottom)
    LinearLayout layout_bottom;

    @BindView(R.id.main_txt_view)
    TextView logTextView;

    @BindView(R.id.main_btn_send)
    MaterialButton button_transmit;

    @BindView(R.id.main_btn_send_to_transmitter)
    MaterialButton btn_send_to_transmitter;

    @BindView(R.id.main_btn_stop)
    MaterialButton button_stop;

    @BindView(R.id.main_scroll)
    ScrollView scrollView;

    @BindView(R.id.main_txt_status)
    public TextView status_txt_view;

    BluetoothAdapter bluetoothAdapter;
    @NonNull Disposable disposable;
    BluetoothDevice[] btArray;
    SendReceive sendReceive;
    BottomSheetDialog bottomSheetDevices;

    static final int BLUETOOTH_ENABLE_REQUEST = 101;
    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;
    static final String APP_NAME = "Transmitter";
    Handler handler;
    String log = "";
    int pingSent = 0;
    int pingReceived = 0;
    File rootStorage;
    String fileName = "sampleDevelopment.txt";

    File actualFile;

    List<String> cloudLog = new ArrayList<>();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-YYY  hh:mm:ss a");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, intentFilter);

        rootStorage = new File(String.valueOf(getApplicationContext().getExternalFilesDir("storage/emulated/0")));
        actualFile = new File(rootStorage.getAbsolutePath() + "/" + fileName);


        if (Common.appMode == Common.RECEIVER_MODE) {
            getSupportActionBar().setTitle(Common.RECEIVER_BLUETOOTH_ADAPTER_NAME);
        } else {
            getSupportActionBar().setTitle(Common.TRANSMITTER_BLUETOOTH_ADAPTER_NAME);

        }


        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@androidx.annotation.NonNull Message msg) {

                switch (msg.what) {
                    case STATE_LISTENING: {
                        status_txt_view.setText("LISTENING");
                        break;
                    }
                    case STATE_CONNECTING: {
                        status_txt_view.setTextColor(Color.YELLOW);
                        status_txt_view.setText("CONNECTING.....");
                        button_transmit.setEnabled(false);
                        button_stop.setEnabled(false);
                        btn_send_to_transmitter.setEnabled(false);
                        break;
                    }
                    case STATE_CONNECTED: {
                        status_txt_view.setTextColor(Color.GREEN);
                        status_txt_view.setText("CONNECTED");
                        button_transmit.setEnabled(true);
                        button_stop.setEnabled(true);
                        btn_send_to_transmitter.setEnabled(true);
                        break;
                    }
                    case STATE_CONNECTION_FAILED: {
                        status_txt_view.setTextColor(Color.RED);
                        status_txt_view.setText("CONNECTION FAILED");
                        button_transmit.setEnabled(false);
                        button_stop.setEnabled(false);
                        btn_send_to_transmitter.setEnabled(false);
                        break;
                    }
                    case STATE_MESSAGE_RECEIVED: {
                        byte[] readBuff = (byte[]) msg.obj;
                        String tempMessage = new String(readBuff, 0, msg.arg1);
                        pingReceived++;
                        status_txt_view.setText(new StringBuilder("Ping Received = " + pingReceived + " , " + " Ping sent = " + pingSent));
                        log = log + "\n" + tempMessage;
                        logTextView.setText(log);
                        scrollView.post(new Runnable() {
                            @Override
                            public void run() {
                                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        });
                        updateTextFile(actualFile.getAbsolutePath(), tempMessage);
                        updateLogOnCloud(tempMessage);
                        sendPings();

                        break;
                    }
                }

                return true;
            }
        });
        try {
            initViews();
        } catch (IOException e) {
            e.printStackTrace();
        }

        setListeners();


    }


    private void initViews() throws IOException {
        ButterKnife.bind(this);

        if (Common.appMode == Common.RECEIVER_MODE) {
            layout_bottom.setVisibility(View.GONE);
        } else {
            btn_send_to_transmitter.setVisibility(View.GONE);
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        if (null == bluetoothAdapter) {
            Snackbar.make(main_relative_layout, "Bluetooth not supported by device", Snackbar.LENGTH_LONG).show();

        } else {
            if (!bluetoothAdapter.isEnabled()) {
                showBluetoothNotEnabled();

            } else {
                connectToReceiver();
            }
        }
    }


    private void setListeners() {

        button_transmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendPings();
                disposable = Observable.interval(4, TimeUnit.SECONDS)
                        .doOnNext(t -> sendPings())
                        .subscribe();

            }
        });


        button_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!disposable.isDisposed()) {
                    disposable.dispose();
                    logTextView.setText(new StringBuilder(log).append(" \n---------S T O P P E D---------\n "));

                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                }
            }
        });


        btn_send_to_transmitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Date date = new Date();
                long dateTime = date.getTime();


                String message = new StringBuilder().append("Ping By Receiver at " + simpleDateFormat.format(dateTime) + "\n").toString();
                log = log + "\n--Pinged Transmitter on  " + simpleDateFormat.format(dateTime) + "--\n";
                logTextView.setText(log);
                sendReceive.write(message.getBytes());
                updateTextFile(actualFile.getAbsolutePath(), message);
                updateLogOnCloud(message);
                pingSent++;
                status_txt_view.setText("Ping sent = " + pingSent + ", Ping Received = " + pingReceived);
            }
        });
    }


    private void connectToReceiver() throws IOException {

        ServerClass serverClass = new ServerClass();
        serverClass.start();

        bluetoothAdapter.startDiscovery();

        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();

        String[] deviceNames = new String[devices.size()];
        btArray = new BluetoothDevice[devices.size()];

        int index = 0;
        if (devices.size() > 0) {
            ClientClass clientClass;
            for (BluetoothDevice device : devices) {
                deviceNames[index] = device.getName();
                btArray[index] = device;
                index++;

            }

            bottomSheetDevices = new BottomSheetDialog(MainActivity.this);
            bottomSheetDevices.setContentView(R.layout.bottomsheet_devices);
            bottomSheetDevices.setCanceledOnTouchOutside(false);
            bottomSheetDevices.setCancelable(false);

            RecyclerView recyclerView = bottomSheetDevices.findViewById(R.id.bts_devices_recycler);
            DevicesNameAdapter devicesNameAdapter = new DevicesNameAdapter(MainActivity.this, Arrays.asList(deviceNames), btArray);
            recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
            recyclerView.setAdapter(devicesNameAdapter);
            if (Common.appMode == Common.RECEIVER_MODE)
                bottomSheetDevices.show();
        }


    }


    private void sendPings() {

        Date date = new Date();
        long dateTime = date.getTime();

        pingSent++;

        log = new StringBuilder(log).append("Pinged Receiver on " + simpleDateFormat.format(dateTime) + "\n").toString();
        status_txt_view.setText(new StringBuilder(" Ping sent = " + pingSent));


        String message = new StringBuilder().append("Ping by Transmitter on " + simpleDateFormat.format(dateTime) + "\n").toString();
        sendReceive.write(message.getBytes());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logTextView.setText(log);

                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            }
        });


    }


    public void updateTextFile(String fileName, String contents) {
        try {

            File textFile = new File(fileName);

            if (textFile.exists()) {
                System.out.println("File path exists = " + textFile.getAbsolutePath());

                // set to true if you want to append contents to text file
                // set to false if you want to remove preivous content of text
                // file
                FileWriter textFileWriter = new FileWriter(textFile, true);

                BufferedWriter out = new BufferedWriter(textFileWriter);

                // create the content string
                String contentString = new String(contents);

                // write the updated content
                out.write(contentString);
                out.close();
                System.out.println("Updaetd text file");

            } else {
                createTextFile(fileName);
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createTextFile(String actualFile) {
        try {

            File file = new File(actualFile);

            if (!file.exists()) {
                System.out.println("File path = " + file.getAbsolutePath());
                if (file.createNewFile()) {
                    Toast.makeText(this, "File Name is sampleDeveloper.txt", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "File not Created", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLogOnCloud(String tempMessage) {
        cloudLog.add(tempMessage);
        FirebaseDatabase
                .getInstance()
                .getReference("Log")
                .setValue(cloudLog)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this,
                                "Updated Log on Cloud",
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                });

    }


    BroadcastReceiver broadcastReceiver
            = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                System.out.println("Available devices = " + device.getName());

            }
        }
    };


    private void showBluetoothNotEnabled() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);
        bottomSheetDialog.setContentView(R.layout.bottomsheet_enable_bluetooth);
        bottomSheetDialog.setCancelable(false);
        bottomSheetDialog.setCanceledOnTouchOutside(false);

        MaterialButton btn_enable_bluetooth = bottomSheetDialog.findViewById(R.id.bts_btn_enable_bluetooth);

        assert btn_enable_bluetooth != null;
        btn_enable_bluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, BLUETOOTH_ENABLE_REQUEST);

                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog.show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case BLUETOOTH_ENABLE_REQUEST: {
                if (resultCode == RESULT_CANCELED) {
                    showBluetoothNotEnabled();
                } else if (resultCode == RESULT_OK) {

                }
            }
        }
    }

    @Override
    public void onClick(int pos) {
        System.out.println("Interface invoked pos = " + pos);
        bottomSheetDevices.dismiss();
        ClientClass clientClass = new ClientClass(btArray[pos]);
        clientClass.start();
    }


    private class ServerClass extends Thread {
        private BluetoothServerSocket bluetoothServerSocket;

        public ServerClass() throws IOException {
            bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, Common.MY_UUID);

        }

        @Override
        public void run() {
            super.run();
            BluetoothSocket socket = null;

            while (socket == null) {
                try {
                    Message message = new Message();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket = bluetoothServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();

                    Message message = new Message();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);


                }

                if (socket != null) {
                    Message message = new Message();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendReceive = new SendReceive(socket);
                    sendReceive.start();
                    break;
                }
            }


        }
    }


    private class ClientClass extends Thread {
        private BluetoothSocket bluetoothSocket;
        private BluetoothDevice bluetoothDevice;


        public ClientClass(BluetoothDevice device) {
            bluetoothDevice = device;
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(Common.MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();

            try {
                bluetoothSocket.connect();
                Message message = new Message();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);
                sendReceive = new SendReceive(bluetoothSocket);
                sendReceive.start();


            } catch (IOException e) {
                Message message = new Message();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
                e.printStackTrace();
            }
        }
    }


    private class SendReceive extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive(BluetoothSocket socket) {
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


