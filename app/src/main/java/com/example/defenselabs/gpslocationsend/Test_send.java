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

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Test_send extends AppCompatActivity {

    int height =18, sleeptime=3 , deltaLatitude=0, deltaLongitude=0;
    Button btn_send, btn_leftUp,btn_leftDown, btn_rightUp, btn_rightDown;
    boolean stop = true;
    public Location previousBestLocation = null;
    public LocationManager locationManager;
    public MyLocListener listener;
    EditText editTextAddress, editTextPort ,txt_height, txt_sleepTime, txt_latitude, txt_longitude , editText_square_side;
    TextView text_send;
    SendAsyncTask myTask;
    String cmd;
    Socket socket = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_send);
        btn_send = (Button) findViewById(R.id.btn_send);

        btn_leftDown = (Button) findViewById(R.id.btn_leftDown);
        btn_leftUp = (Button) findViewById(R.id.btn_leftUp);
        btn_rightUp = (Button) findViewById(R.id.btn_rightUp);
        btn_rightDown = (Button) findViewById(R.id.btn_rightDown);

        // btn_new_send = (Button) findViewById(R.id.btn_new_send);
        text_send = (TextView) findViewById(R.id.text_send);

        txt_height = (EditText) findViewById(R.id.txt_height);       txt_latitude = (EditText) findViewById(R.id.txt_latitude);
        txt_longitude = (EditText) findViewById(R.id.txt_longitude);
        txt_sleepTime = (EditText) findViewById(R.id.txt_sleepTime);
        editText_square_side = (EditText) findViewById(R.id.editText_square_side);

        editTextAddress = (EditText)findViewById(R.id.address);
        editTextAddress.setText("192.168.1.5");
        editTextPort = (EditText)findViewById(R.id.port);
        editTextPort.setText("4448");

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                height = Integer.parseInt(txt_height.getText().toString());
                if(stop) {
                    myTask = new SendAsyncTask(editTextAddress.getText().toString(),
                            Integer.parseInt(editTextPort.getText().toString()), "Send_coordinates");
                    myTask.execute();
                    btn_send.setText("STOP SENDING..");
                    stop=false;
                }
                else
                {
                    stop=true;
                    btn_send.setText("SEND Repeated Co-ordinates");
                    text_send.setText("STOPPED Sending.");
                }
            }
        });

        btn_leftDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!stop) {
                    stop = true;
                    myTask.cancel(true);
                }
                int offset  = Integer.parseInt(editText_square_side.getText().toString());
                MyNewTask myNewTask = new MyNewTask(editTextAddress.getText().toString(),
                        Integer.parseInt(editTextPort.getText().toString()),  getLatitudeOffset(previousBestLocation.getLatitude(),(-1*offset)/2)   ,
                        getLongitudeOffset(previousBestLocation.getLongitude(),(-1*offset)/2) , height );
                myNewTask.execute();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text_send.setText("Going to LEFT DOWN... ");
                    }
                });


            }
        });
        btn_leftUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!stop) {
                    stop = true;
                    myTask.cancel(true);
                }
                int offset  = Integer.parseInt(editText_square_side.getText().toString());
                MyNewTask myNewTask = new MyNewTask(editTextAddress.getText().toString(),
                        Integer.parseInt(editTextPort.getText().toString()),  getLatitudeOffset(previousBestLocation.getLatitude(),offset/2)   ,
                        getLongitudeOffset(previousBestLocation.getLongitude(),(-1*offset)/2) , height );
                myNewTask.execute();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text_send.setText("Going to LEFT UP... ");
                    }
                });

            }
        });

        btn_rightDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!stop) {
                    stop = true;
                    myTask.cancel(true);
                }
                int offset  = Integer.parseInt(editText_square_side.getText().toString());
                MyNewTask myNewTask = new MyNewTask(editTextAddress.getText().toString(),
                        Integer.parseInt(editTextPort.getText().toString()),  getLatitudeOffset(previousBestLocation.getLatitude(),(-1*offset)/2)   ,
                        getLongitudeOffset(previousBestLocation.getLongitude(),+offset/2) , height );
                myNewTask.execute();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text_send.setText("Going to RIGHT DOWN... ");
                    }
                });
            }
        });

        btn_rightUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!stop) {
                    stop = true;
                    myTask.cancel(true);
                }
                int offset  = Integer.parseInt(editText_square_side.getText().toString());
                MyNewTask myNewTask = new MyNewTask(editTextAddress.getText().toString(),
                        Integer.parseInt(editTextPort.getText().toString()),  getLatitudeOffset(previousBestLocation.getLatitude(),offset/2)   ,
                        getLongitudeOffset(previousBestLocation.getLongitude(),offset/2) , height );
                myNewTask.execute();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text_send.setText("Going to RIGHT UP... ");
                    }
                });

            }
        });


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(),"Location Permissions required.",Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(Test_send.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    12);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, listener);




    }

    public double measure(Location loc1, Location loc2){  // generally used geo measurement function
       if(loc1 == null || loc2==null)
       {
           return 100;
       }

        double R = 6378.137; // Radius of earth in KM
        double dLat = (loc2.getLatitude() - loc1.getLatitude()) * Math.PI / 180;
        double dLon = (loc2.getLongitude() - loc1.getLongitude()) * Math.PI / 180;
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(loc1.getLatitude() * Math.PI / 180) * Math.cos(loc2.getLatitude()* Math.PI / 180) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;
        Log.i("TAG","Distance is "+ d*1000 );
        return d * 1000;

    }

    public double getLatitudeOffset(double latitude)
    {
        double R = 6378137;
        double new_latitude = latitude + ((deltaLatitude/R) * (180 / Math.PI ));
        return new_latitude;
    }

    public double getLongitudeOffset(double longitude)
    {
        double R = 6378137;
        double new_longitude = longitude + (deltaLongitude/R) * ( 180 /Math.PI) / Math.cos(longitude * Math.PI/180);
        return new_longitude;
    }

    public double getLatitudeOffset(double latitude, double offset)
    {
        double R = 6378137;
        double new_latitude = latitude + ((offset/R) * (180 / Math.PI ));
        return new_latitude;
    }

    public double getLongitudeOffset(double longitude, double offset)
    {
        double R = 6378137;
        double new_longitude = longitude + (offset/R) * ( 180 /Math.PI) / Math.cos(longitude * Math.PI/180);
        return new_longitude;
    }

    public class MyLocListener implements LocationListener
    {


        @Override
        public void onLocationChanged(Location loc) {
            previousBestLocation = loc;
            height = Integer.parseInt(txt_height.getText().toString());
            sleeptime = Integer.parseInt(txt_sleepTime.getText().toString());
            if(txt_latitude.getText().toString()!="")
                deltaLatitude = Integer.parseInt(txt_latitude.getText().toString());
            if(txt_longitude.getText().toString()!="")
                deltaLongitude = Integer.parseInt(txt_longitude.getText().toString());
            Log.i("TAG on LocationChanged","deltaLatitude : " + deltaLatitude+" deltaLongitude : "+ deltaLongitude);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            Toast.makeText( getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT ).show();
        }

        @Override
        public void onProviderEnabled(String s) {
            Toast.makeText( getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(String s) {

        }
    }

     public class SendAsyncTask extends AsyncTask<Void, Void, Void> {

        String dstAddress, cmd;
        int dstPort;

        SendAsyncTask(String addr, int port, String cmd) {
            dstAddress = addr;
            dstPort = port;
            this.cmd = cmd;

        }




        @Override
        protected Void doInBackground(Void... voids) {




            long lastSentTime=0; Location lastsentLocation = null;
            byte[] input = new byte[1024];
            try {
                socket = new Socket(dstAddress, dstPort);
              //  PrintStream PS = new PrintStream(socket.getOutputStream());
                InputStream in = socket.getInputStream();

                BufferedReader is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String message = is.readLine();

                while(true) {
                    Log.i("TAG","D");

                    if (previousBestLocation!=null && (System.currentTimeMillis() - lastSentTime > 10) && (measure(lastsentLocation,previousBestLocation)>0) ) {
                        lastsentLocation = previousBestLocation;
                        lastSentTime = System.currentTimeMillis();
                  //      PS.println("Latitude:" + getLatitudeOffset(lastsentLocation.getLatitude()) + ":Longitude:" + getLongitudeOffset(lastsentLocation.getLongitude()) + ":Height:" + height + ":\n");
                       // Log.i("SENDING", "Latitude:" + lastsentLocation.getLatitude() + "Longitude:" + lastsentLocation.getLongitude() + "Height:" +height + ";\n");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                text_send.setText("Sending ... "+"Latitude:" + getLatitudeOffset(previousBestLocation.getLatitude()) + " Longitude:" + getLongitudeOffset(previousBestLocation.getLongitude()) + "  Height:" + +height + ";");
                            }
                        });

                    }


                    try {
                        Thread.sleep(sleeptime*700);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                text_send.setText("Complete Send...");
                            }
                        });
                        Thread.sleep(sleeptime*300);
                    } catch (InterruptedException e) {
                        e.printStackTrace(); }
                }


            }catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }  /*finally{
                if(socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }*/





            return null;
        }
    }

    public class MyNewTask extends AsyncTask<Void, Void, Void> {
        String dstAddress;
        int dstPort;
        double latitude, longitude , height;

            MyNewTask(String addr, int port, double latitude,  double longitude, int height  )
            {
                dstAddress = addr;
                dstPort = port;
                this.latitude =latitude;
                this.longitude =longitude;
                this.height = height;
            }
        @Override
        protected Void doInBackground(Void... voids) {



            try {
                PrintStream PS = new PrintStream(socket.getOutputStream());
                PS.println("Latitude:" + latitude + ":Longitude:" + longitude + ":Height:" + height + ":\n");


            } catch (IOException e) {
                e.printStackTrace();
            }/*finally{
                if(socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }*/


            return null;
        }
    }
}
