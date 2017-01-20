package com.mahmoodms.bluetooth.bluetoothemgv2;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.androidplot.Plot;
import com.androidplot.util.PlotStatistics;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
import com.beele.BluetoothLe;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by mahmoodms on 5/31/2016.
 */

public class DeviceControlActivity extends Activity implements BluetoothLe.BluetoothLeListener {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    //LocalVars
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mConnected;
    //Class instance variable
    private BluetoothLe mBluetoothLe;
    private BluetoothManager mBluetoothManager = null;
    private BluetoothGatt mBluetoothGatt = null;
    private BluetoothDevice mBluetoothDevice;
    //Layout - TextViews and Buttons
    private TextView mDeviceNameView;
    private TextView mAddressView;
//    private TextView mBatteryLevel;
    private TextView mConnectionState;
    private TextView mRssi;
    private TextView mRawData;
    private TextView mHexData;
    private Button mExportButton;

    private Menu menu;
    //RSSI Stuff:
    private static final int RSSI_UPDATE_TIME_INTERVAL = 2000;
    private Handler mTimerHandler = new Handler();
    private boolean mTimerEnabled = false;
    /**
     * Initialize Plot:
     *
     */
    private XYPlot plot;
    private Redrawer redrawer;
    private SimpleXYSeries dataSeries;
    private static final int HISTORY_SIZE = 400;
    //Data Variables:
    private float dataVoltage;
    private int batteryWarning = 20;//%
    private static ArrayList<Float> source_na_sensor_data = new ArrayList<>();
    //Intents:
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        //Set orientation of device based on screen type/size:
        if(getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        //Recieve Intents:
        Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(AppConstant.EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(AppConstant.EXTRAS_DEVICE_ADDRESS);
        //Set up action bar:
        if(getActionBar()!=null){
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        ActionBar actionBar = getActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#6078ef")));

        //Flag to keep screen on (stay-awake):
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //Set up TextViews
        mDeviceNameView = (TextView) findViewById(R.id.textViewDeviceName);
        mAddressView = (TextView) findViewById(R.id.textViewDeviceAddress);
        mRssi = (TextView) findViewById(R.id.textViewRssi);
        mConnectionState = (TextView) findViewById(R.id.textViewStatus);
        mRawData = (TextView) findViewById(R.id.rawData);
        mHexData = (TextView) findViewById(R.id.signedHex);
//        mBatteryLevel = (TextView) findViewById(R.id.textViewBatteryLevel);
//        mExportButton = (Button) findViewById(R.id.button_export);
//        mRawData = (TextView) findViewById(R.id.textViewEmgSignal);
//        mRawData.setTextColor(Color.parseColor("#FFFFFF"));
        //Initialize Bluetooth
        mDeviceNameView.setText(mDeviceName);
        mAddressView.setText(mDeviceAddress);
        ActionBar ab = getActionBar();
        ab.setTitle(mDeviceName);
        ab.setSubtitle(mDeviceAddress);
        initialize();
        //set Address and Name:
        // Initialize our XYPlot reference:
        dataSeries = new SimpleXYSeries("Sodium Sensor Data");
        //Todo: temporarily uses - find alternate (seconds)
        dataSeries.useImplicitXVals();
        plot = (XYPlot) findViewById(R.id.plot);
        plot.setRangeBoundaries(-0.6, 0.6, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
        plot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        plot.setDomainStepValue(HISTORY_SIZE/10);
        plot.setRangeStepMode(XYStepMode.INCREMENT_BY_VAL);
        plot.setRangeStepValue(0.3);
//        plot.setTicksPerRangeLabel(2);
        plot.setDomainLabel("Sample History");
        plot.getDomainLabelWidget().pack();
        plot.setRangeLabel("Voltage (V)");
        plot.getRangeLabelWidget().pack();
        plot.setRangeValueFormat(new DecimalFormat("#.#"));
        plot.setDomainValueFormat(new DecimalFormat("#"));
        plot.getDomainLabelWidget().getLabelPaint().setColor(Color.BLACK);
        plot.getDomainLabelWidget().getLabelPaint().setTextSize(20);
        plot.getRangeLabelWidget().getLabelPaint().setColor(Color.BLACK);
        plot.getRangeLabelWidget().getLabelPaint().setTextSize(20);
        plot.getGraphWidget().getDomainTickLabelPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getRangeTickLabelPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getDomainTickLabelPaint().setTextSize(36);
        plot.getGraphWidget().getRangeTickLabelPaint().setTextSize(36);
        plot.getGraphWidget().getDomainGridLinePaint().setColor(Color.WHITE);
        plot.getGraphWidget().getRangeGridLinePaint().setColor(Color.WHITE);
        plot.getLegendWidget().getTextPaint().setColor(Color.BLACK);
        plot.getLegendWidget().getTextPaint().setTextSize(20);
        plot.getTitleWidget().getLabelPaint().setTextSize(20);
        plot.getTitleWidget().getLabelPaint().setColor(Color.BLACK);
        LineAndPointFormatter lineAndPointFormatter = new LineAndPointFormatter(Color.BLACK,null,null,null);
        lineAndPointFormatter.getLinePaint().setStrokeWidth(8);
        plot.addSeries(dataSeries, lineAndPointFormatter/*new LineAndPointFormatter(Color.rgb(200, 0, 0), null, null, null)*/);
        final PlotStatistics histStats = new PlotStatistics(1000, false);
        plot.addListener(histStats);

        redrawer = new Redrawer(
                Arrays.asList(new Plot[]{plot}),
                100, false);
        /*mExportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(source_na_sensor_data!=null && source_na_sensor_data.size()!=0) {
                    final Intent intent_export = new Intent(v.getContext(), ExportActivity.class);
                    intent_export.putExtra("EXTRA_EMG_DATA", source_na_sensor_data);
                    v.getContext().startActivity(intent_export);
                } else {
                    Toast.makeText(v.getContext(),"Not enough data to export!",Toast.LENGTH_SHORT).show();
                }*//**//*


            }
        });*/
    }

    @Override
    public void onResume() {
        redrawer.start();
        super.onResume();

    }

    @Override
    protected void onPause() {
        redrawer.pause();
        stopMonitoringRssiValue();
        mBluetoothLe.disconnect(mBluetoothGatt);
        super.onPause();
    }

    private void initialize() {
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothDevice = mBluetoothManager.getAdapter().getRemoteDevice(mDeviceAddress);
        mBluetoothLe = new BluetoothLe(this, mBluetoothManager, this);
        mBluetoothGatt = mBluetoothLe.connect(mBluetoothDevice, false);
    }

    private void setNameAddress(String name_action, String address_action) {
        MenuItem name = menu.findItem(R.id.action_title);
        MenuItem address = menu.findItem(R.id.action_address);
        name.setTitle(name_action);
        address.setTitle(address_action);
        invalidateOptionsMenu();
    }

    @Override
    protected void onDestroy() {
        redrawer.finish();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_control, menu);
        getMenuInflater().inflate(R.menu.actionbar_item, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        this.menu = menu;
        setNameAddress(mDeviceName, mDeviceAddress);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                if (mBluetoothLe != null)
                    mBluetoothLe.connect(mBluetoothDevice, false);
                    connect();
                return true;
            case R.id.menu_disconnect:
                if (mBluetoothLe != null){
                    if(mBluetoothGatt != null) {
                        mBluetoothGatt.disconnect();
                    }
                }
                return true;
            case android.R.id.home:
                if (mBluetoothLe != null)
                    mBluetoothLe.disconnect(mBluetoothGatt);
                    NavUtils.navigateUpFromSameTask(this);
                    onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void connect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MenuItem menuItem = menu.findItem(R.id.action_status);
                mConnectionState.setText("Connecting...");
                menuItem.setTitle("Connecting...");
//                invalidateOptionsMenu();
            }
        });
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.i(TAG, "onServicesDiscovered");
        if(status == BluetoothGatt.GATT_SUCCESS) {
            for (BluetoothGattService service : gatt.getServices()) {
                if ((service == null) || (service.getUuid()==null)) {
                    continue;
                }
                if (AppConstant.SERVICE_DEVICE_INFO.equals(service.getUuid())) {
                    //Read the device serial number
                    mBluetoothLe.readCharacteristic(gatt, service.getCharacteristic(AppConstant.CHAR_SERIAL_NUMBER));
                    //Read the device software version
                    mBluetoothLe.readCharacteristic(gatt, service.getCharacteristic(AppConstant.CHAR_SOFTWARE_REV));
                }
                if (AppConstant.SERVICE_BATTERY_LEVEL.equals(service.getUuid())) {
                    //Read the device battery percentage
                    mBluetoothLe.readCharacteristic(gatt, service.getCharacteristic(AppConstant.CHAR_BATTERY_LEVEL));
                    mBluetoothLe.setCharacteristicNotification(gatt, service.getCharacteristic(AppConstant.CHAR_BATTERY_LEVEL), true);
                }

                if(AppConstant.SERVICE_EMG_SIGNAL.equals(service.getUuid())) {
                    //Set notification for EMG signal:
                    mBluetoothLe.setCharacteristicNotification(gatt, service.getCharacteristic(AppConstant.CHAR_EMG_SIGNAL), true);
                }

                if(AppConstant.SERVICE_ION_NA_SIGNAL.equals(service.getUuid())) {
                    mBluetoothLe.setCharacteristicNotification(gatt, service.getCharacteristic(AppConstant.CHAR_ION_NA_SIGNAL), true);
                    batteryNotAvailable();
                }
            }
        }
    }

    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
            'B', 'C', 'D', 'E', 'F' };

    public static String toHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_CHARS[v >>> 4];
            hexChars[j * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if(AppConstant.CHAR_ION_NA_SIGNAL.equals(characteristic.getUuid())) {
            int dataIonSensor = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            updateIonSensorState(dataIonSensor);
        }
        if(AppConstant.CHAR_EMG_SIGNAL.equals(characteristic.getUuid())) {
            byte[] dataEmgBytes = characteristic.getValue();
            String hexEmgValue = toHexString(dataEmgBytes);
            int dataEmg = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0);
            /*TODO: SHOULD THIS BE SIGNED INT*/
            updateEMGState(dataEmg);
            updateEMGState2(hexEmgValue);
        }

        if(AppConstant.CHAR_BATTERY_LEVEL.equals(characteristic.getUuid())) {
            int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            updateBatteryStatus(batteryLevel, batteryLevel+ " %");
            Log.i(TAG, "Battery Level :: "+batteryLevel);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.i(TAG, "onCharacteristicRead");
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (AppConstant.CHAR_BATTERY_LEVEL.equals(characteristic.getUuid())) {
                int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                updateBatteryStatus(batteryLevel,batteryLevel+" %");
                Log.i(TAG, "Battery Level :: "+batteryLevel);
            }
        } else {
            Log.e(TAG, "onCharacteristic Read Error"+status);
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        uiRssiUpdate(rssi);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        switch (newState) {
            case BluetoothProfile.STATE_CONNECTED:
                mConnected = true;
                Log.i(TAG, "Connected");
                updateConnectionState(getString(R.string.connected));
                invalidateOptionsMenu();
                //Start the service discovery:
                gatt.discoverServices();
                startMonitoringRssiValue();
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                mConnected = false;
                Log.i(TAG, "Disconnected");
                updateConnectionState(getString(R.string.disconnected));
                stopMonitoringRssiValue();
                invalidateOptionsMenu();
                break;
            default:
                break;
        }
    }

    public void startMonitoringRssiValue() {
        readPeriodicallyRssiValue(true);
    }

    public void stopMonitoringRssiValue() {
        readPeriodicallyRssiValue(false);
    }

    public void readPeriodicallyRssiValue(final boolean repeat) {
        mTimerEnabled = repeat;
        // check if we should stop checking RSSI value
        if(!mConnected || mBluetoothGatt == null || !mTimerEnabled) {
            mTimerEnabled = false;
            return;
        }

        mTimerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mBluetoothGatt == null /*||
                        mBluetoothAdapter == null*/ ||
                        !mConnected)
                {
                    mTimerEnabled = false;
                    return;
                }

                // request RSSI value
                mBluetoothGatt.readRemoteRssi();
                // add call it once more in the future
                readPeriodicallyRssiValue(mTimerEnabled);
            }
        }, RSSI_UPDATE_TIME_INTERVAL);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic
            characteristic, int status) {
        Log.i(TAG, "onCharacteristicWrite :: Status:: " + status);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        Log.i(TAG, "onDescriptorRead :: Status:: " + status);
    }

    @Override
    public void onError(String errorMessage) {
        Log.e(TAG, "Error:: " + errorMessage);
    }

    private void updateConnectionState(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(status);
                if(status.equals(getString(R.string.connected))) {
                    Toast.makeText(getApplicationContext(), "Device Connected!", Toast.LENGTH_SHORT).show();
                } else if (status.equals(getString(R.string.disconnected))) {
                    Toast.makeText(getApplicationContext(), "Device Disconnected!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateEMGState(final int value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(dataSeries.size() > HISTORY_SIZE) {
                    dataSeries.removeFirst();
                }
                float temp = (float)value/65535;
                dataVoltage = (float)(temp*1.2);
                source_na_sensor_data.add(dataVoltage);
                dataSeries.addLast(null, dataVoltage);
                mRawData.setText(String.valueOf(value));
            }
        });
    }
    private void updateEMGState2(final String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mHexData.setText(value);
            }
        });
    }

    private void updateIonSensorState(final int value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(dataSeries.size() > HISTORY_SIZE) {
                    dataSeries.removeFirst();
                }
                float temp = (float)value/255;
                dataVoltage = (float)(temp*1.2*2);
                source_na_sensor_data.add(dataVoltage);
                dataSeries.addLast(null, dataVoltage);
            }
        });
    }

    private void batteryNotAvailable() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                mBatteryLevel.setTextColor(Color.RED);
//                mBatteryLevel.setTypeface(null, Typeface.BOLD);
//                mBatteryLevel.setText("Not Available");
                //Toast.makeText(getApplicationContext(), "Device Does Not Have Battery", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateBatteryStatus(final int percent, final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(percent <= batteryWarning){
                    Toast.makeText(getApplicationContext(), "Charge Battery, Battery Low", Toast.LENGTH_SHORT).show();
                } else {
                }
            }
        });
    }

    private void uiRssiUpdate(final int rssi) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MenuItem menuItem = menu.findItem(R.id.action_rssi);
                MenuItem status_action_item = menu.findItem(R.id.action_status);
                final String valueOfRSSI = String.valueOf(rssi)+" dB";
                mRssi.setText(valueOfRSSI);
                menuItem.setTitle(valueOfRSSI);
                if(mConnected) {
                    String newStatus = "Status: " + getString(R.string.connected);
                    status_action_item.setTitle(newStatus);
                } else {
                    String newStatus = "Status: " + getString(R.string.disconnected);
                    status_action_item.setTitle(newStatus);
                }
            }
        });
    }
}
