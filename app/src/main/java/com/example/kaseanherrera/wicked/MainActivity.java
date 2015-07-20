package com.example.kaseanherrera.wicked;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothAdapter;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.logging.LogRecord;

public class MainActivity extends ActionBarActivity {

    private static final int MESSAGE_READ = 1;
    ArrayList<BluetoothDevice> addedDevices = new ArrayList<>();
    ArrayAdapter<String> mArrayAdapter;
    ListView listview;
    public final UUID MY_UUID =UUID.fromString("00001101-0000-1000-8000-00805F9834FB");

    private BroadcastReceiver mReceiver;
    IntentFilter filter;
    private static final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> pairedDevices;
    private static final int CONNECTED_SUCCESS = 0;

    public Handler mHandler = new Handler(){



        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);

            switch (message.what) {
                case CONNECTED_SUCCESS:
                    Toast.makeText(getApplicationContext(),"We connection",Toast.LENGTH_LONG).show();
                    mArrayAdapter.add("Bitch we connected!");
                    break;

            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);

        }



        //getPairedDevices();
        startDiscovery();



    }

    private void startDiscovery() {
        mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

            if(resultCode == RESULT_CANCELED){
                Toast.makeText(getApplicationContext(), "This application requires bluetooth", Toast.LENGTH_LONG).show();
                finish();
            }


    }


    private void getPairedDevices() {

        pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }


    private void init() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        listview = (ListView)findViewById(R.id.listView);
        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,0);
        listview.setAdapter(mArrayAdapter);



        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)){

                    ;
                    //Toast.makeText(getApplicationContext(), "This application requires bluetooth", Toast.LENGTH_LONG).show();
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);


                    String deviceName = device.getName();
                    if(deviceName == null)
                        System.out.println("nullname");
                    else{
                        if ("WICED Sense Kit".equals(deviceName)){
                            // Add the name and address to an array adapter to show in a ListView

                          //  if (!pairedDevices.contains(device) & !addedDevices.contains(device)) {
                                if (!addedDevices.contains(device)) {


                                    addedDevices.add(device);
                                    mArrayAdapter.add(device.getName() + "\n" + device.getAddress());

                                //create a thread to make a connection

                                ConnectThread connection = new ConnectThread(device);
                                //attempt to make a connection
                                connection.run();
                                System.out.println("Made it past the run!!");



                            }
                        }
                    }



                }
                else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){


                }
                else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

                   // startDescovery();
                }
            }


        };

        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;


        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            System.out.println("Made it to run");
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
            mHandler.obtainMessage(CONNECTED_SUCCESS);
            System.out.println("Made it past obtain Message in the run to the handler");
        }

        private void manageConnectedSocket(BluetoothSocket mmSocket) {



        }


        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }



    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
