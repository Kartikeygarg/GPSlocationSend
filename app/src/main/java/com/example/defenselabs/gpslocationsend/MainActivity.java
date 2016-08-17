package com.example.defenselabs.gpslocationsend;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    public EditText txt_latitude, txt_longitude , txt_height;
    EditText editTextAddress, editTextPort;
    private static final int TWO_MINUTES = 1000 * 30 * 1;
    public LocationManager locationManager;
    public MyLocationListener listener;
    Button btn_send , btn_land;
    public Location previousBestLocation = null;
    boolean STOP = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txt_latitude = (EditText) findViewById(R.id.txt_latitude);
        txt_longitude = (EditText) findViewById(R.id.txt_longitude);
        txt_height = (EditText) findViewById(R.id.txt_height);
        btn_send = (Button) findViewById(R.id.btn_send);
        btn_land = (Button) findViewById(R.id.btn_land);
        editTextAddress = (EditText)findViewById(R.id.address);
        editTextAddress.setText("10.0.1.33");
        editTextPort = (EditText)findViewById(R.id.port);
        editTextPort.setText("4448");

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String latitude = txt_latitude.getText().toString();
                String longitude = txt_longitude.getText().toString();
                String height = txt_height.getText().toString();

                MyClientTask myClientTask = new MyClientTask();
                       /* editTextAddress.getText().toString(),
                        Integer.parseInt(editTextPort.getText().toString()), "Send_coordinates",
                        latitude,longitude,height);*/
                myClientTask.execute();
            }
        });
        btn_land.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyClientTask myClientTask = new MyClientTask(
                        editTextAddress.getText().toString(),
                        Integer.parseInt(editTextPort.getText().toString()), "Land_device");
                myClientTask.execute();
            }
        });


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(),"Location Permissions required.",Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    12);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, listener);

    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            Log.i("TAG", "LAST known Location NULL");
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            Log.i("TAG", "New Location isSignificantlyNewer SETTING NEW LOCATION");
            txt_latitude.setText("" + location.getLatitude());
            txt_longitude.setText("" + location.getLongitude());
            previousBestLocation = location;
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            Log.i("TAG", "New Location isSignificantlyOLDER ");
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
           // Log.i("TAG", "New Location isMoreAccurate ");
            return true;
        } else if (isNewer && !isLessAccurate) {
          /*  Log.i("TAG", "DElta Accuracy "+accuracyDelta);
            Log.i("TAG", "New Location isNewer && !isLessAccurate ");*/
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            //Log.i("TAG", "New Location isNewer && !isSignificantlyLessAccurate && isFromSameProvider ");
            return true;
        }
        return false;
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    public double measure(Location loc1, Location loc2){  // generally used geo measurement function
        double R = 6378.137; // Radius of earth in KM
        double dLat = (loc2.getLatitude() - loc1.getLatitude()) * Math.PI / 180;
        double dLon = (loc2.getLongitude() - loc1.getLongitude()) * Math.PI / 180;
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(loc1.getLatitude() * Math.PI / 180) * Math.cos(loc2.getLatitude()* Math.PI / 180) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;
        return d * 1000; // meters
    }

    public class MyLocationListener implements LocationListener
    {

        public void onLocationChanged(final Location loc)
        {
          //  Log.i("TAG", "Location Listner");
            if( isBetterLocation(loc, previousBestLocation)  ) {


                    Log.i("TAG", "Setting new Location Location Diff = " + measure(previousBestLocation, loc));
                    previousBestLocation = loc;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txt_latitude.setText("" + loc.getLatitude());
                            txt_longitude.setText("" + loc.getLongitude());
                        }
                    });
                /*intent.putExtra("Latitude", loc.getLatitude());
                intent.putExtra("Longitude", loc.getLongitude());
                intent.putExtra("Provider", loc.getProvider());
                sendBroadcast(intent);*/

            }
        }

        public void onProviderDisabled(String provider)
        {
            Toast.makeText( getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT ).show();
        }


        public void onProviderEnabled(String provider)
        {
            Toast.makeText( getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }


        public void onStatusChanged(String provider, int status, Bundle extras)
        {

        }

    }

    public class MyClientTask extends AsyncTask<Void, Void, Void> {

        String dstAddress;
        int dstPort;
        String response = "";
        String cmd ,latitude, longitude , height;

        MyClientTask(String addr, int port, String cmd,  String latitude,  String longitude, String height) {
            dstAddress = addr;
            dstPort = port;
            this.cmd = cmd;
            this.latitude =latitude;
            this.longitude =longitude;
            this.height = height;
        }

        MyClientTask(String addr, int port, String cmd) {
            dstAddress = addr;
            dstPort = port;
            this.cmd = cmd;
        }

        MyClientTask()
        {
            cmd = "Send_coordinates";
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            Socket socket = null;

            try {
               socket = new Socket(dstAddress, dstPort);
                PrintStream PS = new PrintStream(socket.getOutputStream());
                if(cmd.equals("Send_coordinates"))
                {
                    int lastSentTime=0; Location lastsentLocation = null;
                    while(!STOP) {
                        if ((System.currentTimeMillis() - lastSentTime > 10) && (previousBestLocation != lastsentLocation) ) {
                             lastsentLocation = previousBestLocation;
                            //PS.println("Latitude:" + lastsentLocation.getLatitude() + "Longitude:" + longitude + "Height:" + height + ";\n");
                           Log.i("SENDING", "Latitude:" + lastsentLocation.getLatitude() + "Longitude:" + lastsentLocation.getLongitude() + "Height:" + (lastsentLocation.getAltitude()+height) + ";\n");

                        }
                    }

                }

                else if(cmd.equals("Land_device"))
                {// PS.println("LAND;\n");
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally{
                if(socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }

    public class ContinousClientTask extends AsyncTask<Void, Void, Void> {

        String dstAddress;
        int dstPort;
        String response = "";
        String cmd ,latitude, longitude , height;

        ContinousClientTask(String addr, int port) {
            dstAddress = addr;
            dstPort = port;
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            Socket socket = null;

            try {
                socket = new Socket(dstAddress, dstPort);
                PrintStream PS = new PrintStream(socket.getOutputStream());
                long lastSent = System.currentTimeMillis();
                do {



                }while(lastSent-System.currentTimeMillis() > 500);

                if(cmd.equals("Send_coordinates"))
                    PS.println("Latitude:"+latitude+"Longitude:"+longitude+ "Height:"+height+";\n");
                else if(cmd.equals("Land_device"))
                    PS.println("LAND;\n");

            }catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "UnknownHostException: " + e.toString();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "IOException: " + e.toString();
            }  finally{
                if(socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }



}
