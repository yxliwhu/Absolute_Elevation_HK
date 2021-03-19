package com.example.absolute_elevation_hk;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.FragmentActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import Jama.Matrix;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    private GoogleMap mMap;
    private Timer timer;
    private Timer timerCalibration;
    private SensorManager sensorManager = null;
    private Sensor pressureSensor;
    private int updateInterval = 1000*10*60;
    private int updateIntervalofcalibration = 1000*5;
    private double calibrationNum = 0.0;
    private SimpleRate simpleRate = new SimpleRate();
    String timestamp;
    private boolean first = true;
    TextView pressureText ;
    TextView elevationText ;
    TextView pressureCalText;
    TextView CalibrationNumText;
    TextView GPSElevationText;
    TextView GPSPrecisionText;
    TextView TrueElevationText;
    TextView RawPressureText;
    Button showDT;
    Button holdDT;
    Button calibManual;
    Transform mytransform = new Transform();

    protected LocationManager locationManager;
    Location myCurrentLocaiton;
    List<Double> webPressureList = new ArrayList<>();
    double calcuatedPressure = 0.0;
    double sensorPressure = 0.0;
    double elevationResult = 0.0;
    double pressureValue = 0.0;
    double GPSPrecision = 0.0;
    double GPSElevation = 0.0;
    double TrueElevation = 0.0;
    double GPSEleAccuracy = 0.0;

    private boolean doCalib = true;


    final DelaunayTriangulator[] delaunayTriangulator = new DelaunayTriangulator[1];


    private SensorEventListener sensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float[] values = sensorEvent.values;
            Log.i("Pressure_Output:" , String.valueOf(values[0]));
            functions.writeValueToFile(values[0],"Sensor_Output_Raw_Pressure.txt");
            DecimalFormat df = new DecimalFormat("#.00");
            pressureValue = values[0];
            pressureText.setText(String.valueOf(df.format(pressureValue+calibrationNum)));
            sensorPressure = pressureValue+calibrationNum;
            RawPressureText.setText(String.valueOf(df.format(GPSEleAccuracy)));
            CalibrationNumText.setText(String.valueOf(df.format(calibrationNum)));
            GPSElevationText.setText(String.valueOf(df.format(GPSElevation)));
            GPSPrecisionText.setText(String.valueOf(df.format(GPSPrecision)));
            TrueElevationText.setText(String.valueOf(df.format(TrueElevation)));

            //sensorManager.unregisterListener(this, pressureSensor);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //Location
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        updateCurrentLocation();

        doCalib = !functions.fileisExist("Sensor_Output_Calibration_Manual.txt");
        if (!doCalib){
            calibrationNum = functions.readManualCalibResult("Sensor_Output_Calibration_Manual.txt");
        }
        final List<Vector2D> pointSet = new ArrayList<>();
        delaunayTriangulator[0] = new DelaunayTriangulator(pointSet);
        pressureText = (TextView) findViewById(R.id.pressure);
        elevationText = (TextView) findViewById(R.id.elevation);
        pressureCalText = (TextView) findViewById(R.id.pressureCal);
        CalibrationNumText = (TextView) findViewById(R.id.CalibrationNum);
        GPSElevationText = (TextView) findViewById(R.id.GPSElevation);
        GPSPrecisionText = (TextView) findViewById(R.id.GPSPrecision);
        TrueElevationText = (TextView) findViewById(R.id.TrueElevation);
        RawPressureText = (TextView) findViewById(R.id.rawPressure);

        showDT = (Button) findViewById(R.id.Show);
        holdDT = (Button) findViewById(R.id.hold);
        calibManual = (Button) findViewById(R.id.calibration);

        showDT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearDelaunayTriangulatorInMap();
                updateCurrentLocation();
                if (delaunayTriangulator[0].getPointSet().size()!=0){
                    showDelaunayTriangulatorInMap(delaunayTriangulator);
                    if (myCurrentLocaiton != null){
                        Triangle2D triangle = functions.PointInWhichTriangle2D(delaunayTriangulator,myCurrentLocaiton);
                        if (triangle != null){
                            showCurrentLocationAndTriangularInMap(triangle);
                            calcuatedPressure = functions.CalculatePressureBasedOnIDW(delaunayTriangulator,myCurrentLocaiton,webPressureList);
                        }else{
                            Edge2D nearestEdge = functions.PointNearstEdge(delaunayTriangulator,myCurrentLocaiton);
                            calcuatedPressure = functions.CalculatePressureBasedOnIDW(delaunayTriangulator,myCurrentLocaiton,webPressureList);
                        }
                    }
                }
                DecimalFormat df = new DecimalFormat("#.00");
                pressureCalText.setText(String.valueOf(df.format(calcuatedPressure)));

                elevationResult = functions.calculateElevation(sensorPressure,calcuatedPressure);
                elevationText.setText(String.valueOf(df.format(elevationResult)));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom
                        (new LatLng(myCurrentLocaiton.getLatitude(),myCurrentLocaiton.getLongitude()),12.0f));
            }
        });
        holdDT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (delaunayTriangulator[0].getPointSet().size()!=0){
                    clearDelaunayTriangulatorInMap();
                }
            }
        });

        calibManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((myCurrentLocaiton!=null)&&(delaunayTriangulator[0].getPointSet().size()!= 0)&&(pressureValue!=0)&&(webPressureList.size()!=0)){
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MapsActivity.this);
                    builder1.setMessage("If you want to do manually calibration, you should mark your " +
                            "location in the map. Please make sure you are on the ground!!");
                    builder1.setCancelable(true);

                    builder1.setPositiveButton(
                            "Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                                        @Override
                                        public void onMapClick(LatLng point) {
                                            mMap.addMarker(new MarkerOptions().position(point));
                                            Location testLocation = myCurrentLocaiton;
                                            testLocation.setLatitude(point.latitude);
                                            testLocation.setLongitude(point.longitude);
                                            double tempCalibrationParam = CalculateCalbirationParameterManual(delaunayTriangulator,
                                                    testLocation,pressureValue,webPressureList);
                                            if (tempCalibrationParam!=0){
                                                calibrationNum = tempCalibrationParam;
                                                functions.rewriteValueToFile(calibrationNum,"Sensor_Output_Calibration_Manual.txt");
                                            }

                                            mMap.setOnMapClickListener(null);
                                        }
                                    });

                                }
                            });

                    builder1.setNegativeButton(
                            "No",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

                    AlertDialog alert11 = builder1.create();
                    alert11.show();
                }


            }
        });



        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (first){
            timestamp = String.valueOf(System.currentTimeMillis());
            first = false;
            Log.i("TestFistIndicator1", String.valueOf(first));
        }
        Log.i("TestFistIndicator2", String.valueOf(first));

        if(pressureSensor == null)
        {
            Log.i("Warning","No Barometer found!!");
            return;
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                processesForElaevation();
                getPressureFormSensor();
                webPressureList.clear();
                delaunayTriangulator[0] = getDelaunayTriangulation(pointSet);
            }
        },0,updateInterval);
        if (doCalib){
            timerCalibration = new Timer();
            timerCalibration.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (myCurrentLocaiton != null){
                        Location testLocation = myCurrentLocaiton;
                        double tempcalibration = CalculateCalbirationParameterAuto(delaunayTriangulator,
                                testLocation,pressureValue,webPressureList);
                        if (calibrationNum==0){
                            if (tempcalibration!=0){
                                calibrationNum = tempcalibration;
                            }
                        }else{
                            if (tempcalibration!=0){
                                calibrationNum = calibrationNum*0.9+tempcalibration*0.1;
                            }
                        }
                    }
                    functions.writeValueToFile(calibrationNum,"Sensor_Output_Calibration_Number.txt");

                }
            },0,updateIntervalofcalibration);
        }
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(22.3019444, 114.174167);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    public void updateCurrentLocation(){
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                return;
            }
            //Get GPS location from Android system and set frequency
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.i("Location Info:",location.toString());
                    myCurrentLocaiton = location;
                    DecimalFormat df = new DecimalFormat("#.00");
                    Log.i("GPS Elevation:",String.valueOf(location.getAltitude()));
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onProviderEnabled(String s) {

                }

                @Override
                public void onProviderDisabled(String s) {

                }
            });
        }
    }

    /**
     * Show special triangle in Google map View
     * @param triangle
     */
    public void showCurrentLocationAndTriangularInMap(Triangle2D triangle){
        mMap.addMarker(new MarkerOptions().position(new LatLng(myCurrentLocaiton.getLatitude(),myCurrentLocaiton.getLongitude()))
                .title("My Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        Vector2D a = triangle.a;
        Vector2D b = triangle.b;
        Vector2D c = triangle.c;
        GeoData mywgs = mytransform.HKGEO(2,a.y,a.x);
        List<LatLng> hole = new ArrayList<>();
        hole.add(new LatLng(mywgs.getPhi(),mywgs.getFlam()));
        mywgs = mytransform.HKGEO(2,b.y,b.x);
        hole.add(new LatLng(mywgs.getPhi(),mywgs.getFlam()));
        mywgs = mytransform.HKGEO(2,c.y,c.x);
        hole.add(new LatLng(mywgs.getPhi(),mywgs.getFlam()));
        PolygonOptions polyOptions = new PolygonOptions()
                .fillColor(0x44000000)
                .addAll(functions.createBoundsOfEntireMap())
                .strokeColor(0xFf000000)
                .strokeWidth(5);
        polyOptions.addHole(hole);
        mMap.addPolygon(polyOptions);
    }

    /**
     * Show the Triangular mesh in Google map view
     * @param delaunayTriangulator: Triangular Mesh
     */
    public void showDelaunayTriangulatorInMap(DelaunayTriangulator[] delaunayTriangulator){
        List<Vector2D> pointSet = delaunayTriangulator[0].getPointSet();
        for (int i = 0; i < pointSet.size();i++){
            GeoData mywgs = mytransform.HKGEO(2,pointSet.get(i).y,pointSet.get(i).x);
            LatLng tempLocation = new LatLng(mywgs.getPhi(),mywgs.getFlam());
            mMap.addMarker(new MarkerOptions().position(tempLocation).title("Station Location"));
        }
        List<List<LatLng>> holes = new ArrayList<>();
        for (int i = 0; i < delaunayTriangulator[0].getTriangles().size();i++){
            List<LatLng> hole = new ArrayList<>();
            Triangle2D triangle = delaunayTriangulator[0].getTriangles().get(i);
            Vector2D a = triangle.a;
            Vector2D b = triangle.b;
            Vector2D c = triangle.c;
            GeoData mywgs = mytransform.HKGEO(2,a.y,a.x);
            hole.add(new LatLng(mywgs.getPhi(),mywgs.getFlam()));
            mywgs = mytransform.HKGEO(2,b.y,b.x);
            hole.add(new LatLng(mywgs.getPhi(),mywgs.getFlam()));
            mywgs = mytransform.HKGEO(2,c.y,c.x);
            hole.add(new LatLng(mywgs.getPhi(),mywgs.getFlam()));
            holes.add(hole);
        }
        mMap.addPolygon(functions.createPolygonWithHoles(holes));
    }

    /**
     * Remove all the elements from Google map view
     */
    public void clearDelaunayTriangulatorInMap() {
        mMap.clear();
    }


    /**
     * Establish triangular mesh based on the points
     * @param pointSet:points used
     * @return Triangular mesh
     */
    public DelaunayTriangulator getDelaunayTriangulation(List<Vector2D> pointSet){
        List<PointType> locationinfor = CalculateElevation.getStartionLocationList();
        DelaunayTriangulator delaunayTriangulator;
        delaunayTriangulator = new DelaunayTriangulator(pointSet);
        pointSet.clear();
        for (int i = 0; i < locationinfor.size();i++){
            if (locationinfor.get(i).getP()!= 0.0){
                pointSet.add(new Vector2D(locationinfor.get(i).getX(),locationinfor.get(i).getY()));
                webPressureList.add(locationinfor.get(i).getP());
            }else{

            }

        }
        delaunayTriangulator = CalculateElevation.estibish2DDT(pointSet);
        return delaunayTriangulator;
    }

    /**
     * Get MSL pressure data from website "http://www.weather.gov.hk/wxinfo/ts/text_readings_e.htm"
     */
    public void processesForElaevation(){
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            //your codes here
            CalculateElevation.getInfoFromWeb("http://www.weather.gov.hk/wxinfo/ts/text_readings_e.htm",timestamp);
        }
    }

    /**
     * Get pressure output from built-in barometer
     */
    public void getPressureFormSensor(){
        sensorManager.registerListener(sensorEventListener, pressureSensor, simpleRate.get_SENSOR_RATE_NORMAL());
    }

    /**
     * Method1: (Automatically) Calculate the system error (calibration parameter) of the built-in barometer
     * @param delaunayTriangulator
     * @param myLocation :user location in WGS84
     * @param mysensorPressure : raw pressure form built-in barometer
     * @return calibration parameter
     */
    public double CalculateCalbirationParameterAuto(DelaunayTriangulator[] delaunayTriangulator,
                                                Location myLocation,
                                                Double mysensorPressure,
                                                List<Double> webPressureList){
        double CalibrationParameter = 0.0;
        double myCalcuatedPressure = 0.0;
        double myAltitude = myLocation.getAltitude();
        double myGPSAccuracy = myLocation.getAccuracy();
        double myGPSEleAccuracy = myLocation.getVerticalAccuracyMeters();
        double trueElevation = 0.0;
        // Make sure the location information is correct
        if (myAltitude!=0.0&&mysensorPressure!=0.0&&myGPSAccuracy<=5&&myGPSEleAccuracy<=5){
            if (delaunayTriangulator[0].getPointSet().size()!=0){
                Triangle2D triangle = functions.PointInWhichTriangle2D(delaunayTriangulator,myLocation);
                if (triangle != null){
                    myCalcuatedPressure = functions.CalculatePressureBasedOnIDW(delaunayTriangulator, myLocation,webPressureList);
                }else{
                    Edge2D nearestEdge = functions.PointNearstEdge(delaunayTriangulator,myLocation);
                    myCalcuatedPressure = functions.CalculatePressureBasedOnIDW(delaunayTriangulator, myLocation,webPressureList);
                }
                // make sure the user is in the ground
                GridData myHK80 = mytransform.GEOHK(1,myLocation.getLatitude(),myLocation.getLongitude());
                int index_N1 =  (int) (Math.ceil((myHK80.getN()-799997.5)/5000));
                int index_E1 =  (int) (Math.ceil((myHK80.getE()-799997.5)/5000));
                int index_N2 =  (int) (Math.ceil((myHK80.getN()-799997.5)/5)-(index_N1-1)*1000);
                int index_E2 =  (int) (Math.ceil((myHK80.getE()-799997.5)/5)-(index_E1-1)*1000);
                String filename = "N" + String.valueOf(index_N1) +"E" + String.valueOf(index_E1) + ".txt";
                Matrix elevationMatrix = functions.loadfile(filename);
                // MSL = HKPD-1.3m
                double[] result = functions.CalcualteSTDofTrueElevation(elevationMatrix,index_N2-1,index_E2-1);
                double std = result[0];
                double meanDif = result[1];
                if (std <=2 && meanDif <=2){
                    trueElevation  = elevationMatrix.get(index_N2-1,index_E2-1) - 1.3;
                    if (Math.abs(trueElevation-myAltitude) < 10){
                        CalibrationParameter = Math.pow((1-trueElevation/44330), 5.255)*myCalcuatedPressure - mysensorPressure;
                        Log.i("Calibration Paramter",String.valueOf(CalibrationParameter));
                    }
                }

            }else{ }

        }else{ }
        GPSElevation = myAltitude;
        GPSPrecision = myGPSAccuracy;
        TrueElevation = trueElevation;
        GPSEleAccuracy = myGPSEleAccuracy;
        return CalibrationParameter;
    }


    /**
     * Method2: (Manually) Calculate the system error (calibration parameter) of the built-in barometer
     * @param delaunayTriangulator
     * @param myLocation :user location in WGS84
     * @param mysensorPressure : raw pressure form built-in barometer
     * @return calibration parameter
     */
    public double CalculateCalbirationParameterManual(DelaunayTriangulator[] delaunayTriangulator,
                                                    Location myLocation,
                                                    Double mysensorPressure,
                                                    List<Double> webPressureList){
        double CalibrationParameter = 0.0;
        double myCalcuatedPressure = 0.0;
        double trueElevation = 0.0;
        // Make sure the location information is correct
        if (mysensorPressure!=0.0){
            if (delaunayTriangulator[0].getPointSet().size()!=0){
                Triangle2D triangle = functions.PointInWhichTriangle2D(delaunayTriangulator,myLocation);
                if (triangle != null){
                    myCalcuatedPressure = functions.CalculatePressureBasedOnIDW(delaunayTriangulator, myLocation,webPressureList);
                }else{
                    Edge2D nearestEdge = functions.PointNearstEdge(delaunayTriangulator,myLocation);
                    myCalcuatedPressure = functions.CalculatePressureBasedOnIDW(delaunayTriangulator, myLocation,webPressureList);
                }
                // make sure the user is in the ground
                GridData myHK80 = mytransform.GEOHK(2,myLocation.getLatitude(),myLocation.getLongitude());
                int index_N1 =  (int) (Math.ceil((myHK80.getN()-799997.5)/5000));
                int index_E1 =  (int) (Math.ceil((myHK80.getE()-799997.5)/5000));
                int index_N2 =  (int) (Math.ceil((myHK80.getN()-799997.5)/5)-(index_N1-1)*1000);
                int index_E2 =  (int) (Math.ceil((myHK80.getE()-799997.5)/5)-(index_E1-1)*1000);
                String filename = "N" + String.valueOf(index_N1) +"E" + String.valueOf(index_E1) + ".txt";
                Matrix elevationMatrix = functions.loadfile(filename);
                // MSL = HKPD-1.3m
                trueElevation  = elevationMatrix.get(index_N2-1,index_E2-1) - 1.3;
                CalibrationParameter = Math.pow((1-trueElevation/44330), 5.255)*myCalcuatedPressure - mysensorPressure;
                Log.i("Calibration Paramter",String.valueOf(CalibrationParameter));


            }else{ }

        }else{ }
        TrueElevation = trueElevation;
        return CalibrationParameter;
    }
}
