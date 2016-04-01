package com.example.ashton.smarttrak;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandPendingResult;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.SampleRate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LandingPage extends AppCompatActivity {


    boolean bandConnected = false;
    private BandClient client = null;
    String placeholderData = "";
    List<Double> accelX = new ArrayList<Double>();
    List<Double> accelY = new ArrayList<Double>();
    List<Double> accelZ = new ArrayList<Double>();
    List<Double> skinTemp = new ArrayList<Double>();
    List<Integer> heartRate = new ArrayList<Integer>();
    int accIndex = 0;
    int hrIndex = 0;
    int stIndex = 0;
    String panicMsg = "Holy fucking shit help!!!!";
    String phoneNumber = "7789947262";
    boolean emergency = false;
    boolean quitEmergencyTask = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);
        setTitle("SmartTrak Landing Page");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);

        // TEST
        emergency = true;

        new EmergencyTask().execute();


        Button buttonActivate = (Button) findViewById(R.id.button_activate);
        buttonActivate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // activate band sensors here or onCreate of next activity

                if (bandConnected) {
                    try {
                        client.getSensorManager().requestHeartRateConsent(LandingPage.this, new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven) {
                                // don't need anything here apparently
                            }
                        });

                        // MS16 gives 62 Hz (every 16 ms) sampling
                        client.getSensorManager().registerAccelerometerEventListener(accel_el, SampleRate.MS16);
                        client.getSensorManager().registerSkinTemperatureEventListener(skintemp_el);

                        if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED){
                            client.getSensorManager().registerHeartRateEventListener(heartrate_el);
                        }
                    } catch (BandException e) {
                        // probably don't need to include exception handling here
                    }


                    Intent intent = new Intent(LandingPage.this, MainPage.class);
                    //intent.putExtra() // will need to import sensor objects through here probably
                    String teststr = LandingPage.this.teststring();
                    intent.putExtra("key", teststr);
//                    LandingPage.this.startActivity(intent);
                } else {
                    Context context = getApplicationContext();
                    String toast_text = "Please connect Band first.";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, toast_text, duration);
                    toast.show();
                }
            }
        });

        final Button buttonConnectBand = (Button) findViewById(R.id.button_connect_band);
        buttonConnectBand.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                // get band connection here, runs asynctask on background thread
                new BandConnectionTask().execute();
            }
        });

        Button buttonSample = (Button) findViewById(R.id.button_sample);
        buttonSample.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                TextView sampleText = (TextView) findViewById(R.id.tv_sample);
                String size = String.format(" Size: %d \n\n", accIndex);
                String accel_sample = "";
                String st_sample = "";
                String hr_sample = "";

                if (!(accelX.size() == 0)){
                    accel_sample = String.format(" X: %.3f \n Y: %.3f \n Z: %.3f \n\n",
                            accelX.get(accIndex -1), accelY.get(accIndex -1), accelZ.get(accIndex -1));
                }

                if (!(skinTemp.size() == 0)){
                    st_sample = String.format(" Temp: %.3f \n\n", skinTemp.get(stIndex -1));
                } else {
                    st_sample = " Temp: N/A \n\n";
                }

                if (!(heartRate.size() == 0)){
                    hr_sample = String.format(" HR: %d \n\n", heartRate.get(hrIndex -1));
                } else {
                    hr_sample = " HR: N/A \n\n";
                }
                if (client.getSensorManager().getCurrentHeartRateConsent() != UserConsent.GRANTED){
                    hr_sample = " HR: Permission needed";
                }

                String tempsize = String.format(" tSize: %d\n\n", stIndex);
                String hrsize = String.format(" hrSize: %d\n\n", hrIndex);
                String sample = size + accel_sample + st_sample + tempsize + hr_sample + hrsize;
                sampleText.setText(sample);


                //file saving stuff
                String fileOutput = "";
                String filename = "bandoutput.txt";
                File file = getStorageDir(filename);

                File storage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File directory = new File(storage.getAbsolutePath()+"/testdir");
                directory.mkdirs();

                File testfile = new File(directory, filename);
//                FileOutputStream fOut = new FileOutputStream(testfile);
//                OutputStreamWriter osw = new OutputStreamWriter(fOut);


//                for (int i = 0; i < (accIndex -1); )

            }
        });

        Button buttonPanic = (Button) findViewById(R.id.button_panic);
        buttonPanic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSMS(phoneNumber, panicMsg);
            }
        });
    }

    private class EmergencyTask extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... params){
            

            while (!quitEmergencyTask){
                // implement emergency detection here

                if (emergency){
                   emergency = false;
                   quitEmergencyTask = true;
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            String emergencyMsg = "";

            // TEST VALUE
            emergencyMsg = "hello i am dying";

            sendSMS(phoneNumber, emergencyMsg);
            quitEmergencyTask = false;
            new EmergencyTask().execute();
        }

    }

    // async runs on background thread
    private class BandConnectionTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params){


            BandInfo[] pairedBands = BandClientManager.getInstance().getPairedBands();
            if (pairedBands.length == 0){
                return "UNPAIRED";
            }
            client = BandClientManager.getInstance().create(LandingPage.this, pairedBands[0]);

            BandPendingResult<ConnectionState> pendingResult = client.connect();
            try {
                ConnectionState state = pendingResult.await();
                if (state == ConnectionState.CONNECTED){
                    return "CONNECTED";
                } else {
                    return "DISCONNECTED";
                }
            } catch (InterruptedException e){
                return "INTERRUPT ERROR";
            } catch (BandException e){
                return "BAND ERROR";
            }
        }

        // runs on UI thread
        @Override
        protected void onPostExecute(String result){
            String state = result;

            if (state == "CONNECTED"){
                bandConnected = true;
            }

            Button buttonConnectionState = (Button) findViewById(R.id.button_connect_band);
                buttonConnectionState.setText(state);
        }
    }


    public String teststring(){
        String test;
        return test = "placeholdertest";
    }


    private BandAccelerometerEventListener accel_el = new BandAccelerometerEventListener() {
        @Override
        public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
            if (event != null){
                double x = event.getAccelerationX();
                double y = event.getAccelerationY();
                double z = event.getAccelerationZ();

                // ArrayList can only hold 2^31 - 1 elements and will fail after storing that many samples
                // Probably won't be a deal but is technically a "bug"
                accelX.add(accIndex, x);
                accelY.add(accIndex, y);
                accelZ.add(accIndex, z);
                accIndex++;
            }
        }
    };

    private BandSkinTemperatureEventListener skintemp_el = new BandSkinTemperatureEventListener() {
        @Override
        public void onBandSkinTemperatureChanged(final BandSkinTemperatureEvent event) {
            if (event != null){
                double temperature = event.getTemperature();
                skinTemp.add(stIndex, temperature);
                stIndex++;
            }
        }
    };

    private BandHeartRateEventListener heartrate_el = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null){
                int hRate = event.getHeartRate();
                heartRate.add(hrIndex, hRate);
                hrIndex++;
            }
        }
    };

    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_landing_page, menu);
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

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public File getStorageDir(String name){
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name);
        file.mkdirs();
        return file;
    }

    private void sendSMS(String phoneNumber, String message){
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
        if (permission == PackageManager.PERMISSION_GRANTED){
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNumber, null, message, null, null);

            Context context = getApplicationContext();
            String toast_text = "Emergency message sent.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, toast_text, duration);
            toast.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
            sendSMS(phoneNumber, message);
        }

    }


}
