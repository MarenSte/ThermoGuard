package com.example.marensteinkamp.thermoguard;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This is the main Activity that displays the current chat session.
 */
public class MainActivity extends AppCompatActivity {
    // Debugging
    private static final String TAG = "MainActivity";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_DISCONNECTED=6;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String DEVICE_NUMBER="number";

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 2;
    private static final String THINGSPEAK_UPDATE_URL = "https://api.thingspeak.com/update?";
    private static final String THINGSPEAK_API_KEY_STRING = "api_key";

    /* Be sure to use the correct fields for your own app*/
    private static final String THINGSPEAK_FIELD1 = "field1";
    private static final String THINGSPEAK_FIELD2 = "field2";

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mThermoService = null;

    private Toolbar toolbar;

    private int deviceNumber;

    private Set<BluetoothDevice> pairedDevices;

    private String deviceAddress[]={"98:D3:21:FC:7D:67","98:D3:31:FC:54:69","98:D3:91:FD:32:F0","98:D3:91:FD:34:80","98:D3:81:FD:36:F6","98:D3:91:FD:34:97","98:D3:91:FD:32:D8"};

    private TempSensor tempSensors[]={null,null,null,null,null,null,null};

    TextView textWohn;
    TextView textEss;
    TextView textBad;
    TextView textKueche;
    TextView textArbeit;
    TextView textSchlaf;
    TextView textBalkon;

    ArrayList<TextView> textViews=new ArrayList<>();

    ArrayList<ThingSpeak> thingSpeakList=new ArrayList<>();

    Timer timer=new Timer();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.inflateMenu(R.menu.menu_main);


        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = mBluetoothAdapter.getBondedDevices();

        textWohn=findViewById(R.id.wohn);
        textEss=findViewById(R.id.ess);
        textKueche=findViewById(R.id.kueche);
        textViews.add(textWohn);
        textViews.add(textEss);
        textViews.add(textKueche);

        initializeTempSensors();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        deviceNumber=0;
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mThermoService == null) setupThermoGuard();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mThermoService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mThermoService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mThermoService.start();
                mThermoService.checkForDisconnectedBLE();

            }
        }
    }

    private void setupThermoGuard() {
        Log.d(TAG, "setupChat()");

        initializeThingSpeakChannels();
        TimerTask timerTask=new TimerTask() {
            @Override
            public void run() {
                sendDataToThingSpeak();
            }
        };
        timer.schedule(timerTask,120000,3600000);
        // Initialize the BluetoothChatService to perform bluetooth connections
        mThermoService = new BluetoothService(this, mHandler);
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mThermoService != null) mThermoService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }


    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            //mTitle.setText(R.string.title_connected_to);
                            //mTitle.append(mConnectedDeviceName);
                           // mConversationArrayAdapter.clear();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            //mTitle.setText(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            //mTitle.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
//                    mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                    int deviceNum=findAddressInArray(mConnectedDeviceName);
                    stringToTempSensor(readMessage,tempSensors[deviceNum]);
                    updateTextField(deviceNum);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
//                    Toast.makeText(getApplicationContext(), "Connected to "
//                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_DISCONNECTED:
                    int disconnected=msg.getData().getInt(DEVICE_NUMBER);
                    BluetoothDevice device = getDeviceByAddress(deviceAddress[disconnected]);
                    System.out.println("disconnected: "+disconnected);
                    if(device!=null) {
                        System.out.println("connecting to: "+device.getAddress());
                        mThermoService.connect(device, disconnected);
                    }else{
                        System.out.println(deviceAddress[disconnected]+" is null");
                    }
                    break;

            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }


    public void stringToTempSensor(String string, TempSensor sensor){
        if(string != null && string.length() > 0 && string.charAt(string.length()-1)==';'){
            string = string.substring(0, string.length() - 1);
            String tmp[]=string.split(",");
            double temp=Double.valueOf(tmp[0]);
            double hum=Double.valueOf(tmp[1]);
            sensor.setTemp(temp);
            sensor.setHum(hum);
        }
    }

    private BluetoothDevice getDeviceByAddress(String address){
        for(BluetoothDevice device:pairedDevices){
            if(device.getAddress().equals(address)){
                return device;

            }
        }
        return null;
    }

    private int findAddressInArray(String address){
        for(int i=0;i<deviceAddress.length;i++){
            if(deviceAddress[i].equals(address)){
                return i;
            }
        }
        return -1;
    }

    private void updateTextField(int number){
        TextView view=textViews.get(number);
        view.setText("T: "+tempSensors[number].getTemp()+", H: "+tempSensors[number].getHum());
    }

    private void initializeThingSpeakChannels(){
        ThingSpeak neu=new ThingSpeak(520484,"8FZRH3LVN30XE5XD","W4LCE87JEFM7RCIT","Wohnzimmer");
        thingSpeakList.add(neu);
        neu= new ThingSpeak(520489,"5O45PVMU95YOKWYA","8YAIVV4TF96VZ6JO","Esszimmer");
        thingSpeakList.add(neu);
        neu=new ThingSpeak(520490,"DK4G4AOH687GZED7","YXMLA6AKMPJ7Z1M7","Kueche");
        thingSpeakList.add(neu);
        neu=new ThingSpeak(520763,"0QH52WF61QBGOJ4G","ROUWP6QHFX98DCXG","Bad");
        thingSpeakList.add(neu);
        neu=new ThingSpeak(520764,"1SJ3K8JTKKENY66O","3RIZ0JRMHVAISREQ","Arbeitszimmer");
        thingSpeakList.add(neu);
        neu=new ThingSpeak(520766,"I2MQ9RGIDKHSN7PB","BE8EUHER3O00E7LS","Schlafzimmer");
        thingSpeakList.add(neu);
        neu=new ThingSpeak(520767,"DDLA7HAI6XPQKIHR","SNSYCY2JHF2CMYTH","Balkon");
        thingSpeakList.add(neu);
    }

    private void initializeTempSensors(){
        for(int i=0;i<tempSensors.length;i++){
            TempSensor sensor=new TempSensor(0.0,0.0);
            tempSensors[i]=sensor;
        }
    }

    private void sendDataToThingSpeak(){

        for(int i=0;i<thingSpeakList.size();i++){
            new UpdateThingspeakTask(thingSpeakList.get(i).getWrite_key(),tempSensors[i]).execute();
        }

    }

    class UpdateThingspeakTask extends AsyncTask<Void, Void, String> {

        private Exception exception;
        private String apiString;
        private TempSensor sensor;
        public UpdateThingspeakTask(String apiString,TempSensor sensor){
            this.apiString=apiString;
            this.sensor=sensor;
        }

        protected void onPreExecute() {
        }

        protected String doInBackground(Void... urls) {
            try {
                URL url = new URL(THINGSPEAK_UPDATE_URL + THINGSPEAK_API_KEY_STRING + "=" +
                        apiString + "&" + THINGSPEAK_FIELD1 + "=" + sensor.getTemp() +
                        "&" + THINGSPEAK_FIELD2 + "=" + sensor.getHum());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                }
                finally{
                    urlConnection.disconnect();
                }
            }
            catch(Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(String response) {
            // We completely ignore the response
            // Ideally we should confirm that our update was successful
        }
    }
}

//mBtAdapter = BluetoothAdapter.getDefaultAdapter();

// Get a set of currently paired devices
//        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();