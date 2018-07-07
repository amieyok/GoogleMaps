package com.noq.googlemapgoogleplaces;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.print.PrintHelper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.bridge.Bridge;
import com.afollestad.bridge.BridgeException;
import com.afollestad.bridge.Callback;
import com.afollestad.bridge.Form;
import com.afollestad.bridge.Request;
import com.afollestad.bridge.Response;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.noq.googlemapgoogleplaces.models.PlaceInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener{

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

            init();
        }

    }

    private static final String TAG = "MapActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final int PLACE_PICKER_REQUEST = 1;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40, -168), new LatLng(71,136));


    //widgets
    private AutoCompleteTextView mSearchText;
    private ImageView mGps;
    private ImageView mInfo;
    private ImageView mPlacePicker;
    private Button mCalculateDistance;

    //vars
    private  Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private PlaceInfo mPlace;
    private Marker mMarker;
    private double lat1, lon1, lat2, lon2, lat3, lon3;
    private Boolean mGeoLocate = false;
    private String district;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mSearchText = (AutoCompleteTextView) findViewById(R.id.input_search);
        mGps = (ImageView) findViewById(R.id.ic_gps);
        mInfo = (ImageView) findViewById(R.id.place_info);
        mPlacePicker = (ImageView)findViewById(R.id.place_picker);
        mCalculateDistance = (Button) findViewById(R.id.calculation_btn);

        getLocationPermission();
    }

    private void init()
    {
        Log.d(TAG, "init:initialising");
        mSearchText.setText(MainActivity.district);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
        mSearchText.setOnItemClickListener(mAutocompleteClickListener);

        mPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, LAT_LNG_BOUNDS, null);
        mSearchText.setAdapter(mPlaceAutocompleteAdapter);
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || event.getAction() == event.ACTION_DOWN
                        || event.getAction() == event.KEYCODE_ENTER)
                {
                    //execute our method for searching
                    geoLocate();
                }

                return false;
            }
        });

       // mSearchText.getText(MainActivity.getSearchLocation());

        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked gps icon");
                getDeviceLocation();
            }
        });

        mInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked place info");
                try{
                    if (mMarker.isInfoWindowShown()){
                        mMarker.hideInfoWindow();
                    }else{
                        Log.d(TAG, "onClick: place info: " + mPlace.toString());
                        mMarker.showInfoWindow();
                    }

                }catch (NullPointerException e){
                    Log.e(TAG, "onClick: NullPointerException" + e.getMessage());
                }
            }
        });

        mPlacePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

                try {
                    startActivityForResult(builder.build(MapActivity.this), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    Log.e(TAG, "onClick: GooglePlayServicesRepairableException" + e.getMessage());
                } catch (GooglePlayServicesNotAvailableException e) {
                    Log.e(TAG, "onClick: GooglePlayServicesNotAvailableException" + e.getMessage());
                }
            }
        });

        mCalculateDistance.setOnClickListener(new View.OnClickListener() {
            //public String hfc_district;

            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked Calculate distance");
                

                    try{
                        if (isInRange (lat1, lon1, lat2, lon2))
                        {
                            Intent intent = new Intent (MapActivity.this, InRange.class);
                            startActivity(intent);
                        }else
                        {
                            Intent intent = new Intent (MapActivity.this, OutRange.class);
                            startActivity(intent);
                        }
                    }catch (Exception e){
                        e.getMessage();
                    }

                
            }
        });

        hideSoftKeyboard();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);

                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, place.getId());
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            }
        }
    }

    private void geoLocate()
    {
        Log.d(TAG, "geoLocate: geolocating");

        String searchString = mSearchText.getText().toString();

        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();
        try
        {
            list = geocoder.getFromLocationName(searchString, 1);
        }catch(IOException e)
        {
            Log.e(TAG, "geoLocate: IOException:" + e.getMessage() );
        }

        if(list.size()> 0)
        {
            Address address = list.get(0);
            Log.d(TAG, "geoLocate: found a location:" + address.toString() );
            //Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();

            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM , address.getAddressLine(0));

            lat2 = address.getLatitude();
            lon2 = address.getLongitude();

            mGeoLocate = true;
        }


    }

    private void getDeviceLocation()
    {
        Log.d(TAG, "getDeviceLocation: getting the devices current location ");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try
        {
            if(mLocationPermissionsGranted)
            {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task)
                    {
                        if (task.isSuccessful())
                        {
                            Log.d(TAG,"onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();

                            moveCamera(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()), DEFAULT_ZOOM, "My Location");
                            lat1 = currentLocation.getLatitude();
                            lon1 = currentLocation.getLongitude();

                        }else
                        {
                            Log.d(TAG,"onComplete: current location is null");
                            Toast.makeText(MapActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        }catch(SecurityException e)
        {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }


    }

    private void moveCamera(LatLng latlng, float zoom, String title)
    {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latlng.latitude+ ", lng:" + latlng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng,zoom));

        if(!title.equals("My Location"))
        {
            MarkerOptions options = new MarkerOptions().position(latlng).title(title);
            mMap.addMarker(options);
        }

        hideSoftKeyboard();

    }

    private void moveCamera(LatLng latlng, float zoom, PlaceInfo placeInfo)
    {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latlng.latitude+ ", lng:" + latlng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng,zoom));

        mMap.clear();

        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MapActivity.this));

       if (placeInfo != null) {
           try{

               String snippet = "Address: " + placeInfo.getAddress() + "\n" +
                       "Phone Number: " + placeInfo.getPhoneNumber() + "\n" +
                       "Website: " + placeInfo.getWebsiteUri() + "\n" +
                       "Price Rating: " + placeInfo.getRating() + "\n" ;

               MarkerOptions options = new MarkerOptions()
                       .position(latlng)
                       .title(placeInfo.getName())
                       .snippet(snippet);

               mMarker = mMap.addMarker(options);
           }catch (NullPointerException e){
               Log.e(TAG, "moveCamera: NullPointerException:" + e.getMessage());
           }
       }else{
           mMap.addMarker(new MarkerOptions().position(latlng));
       }
        hideSoftKeyboard();

    }

    private void initMap()
    {
        Log.d(TAG, "initMap: initialising map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(MapActivity.this);
    }

    private void getLocationPermission()
    {
        Log.d(TAG, "getLocationPermission: getting loction permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION)== PackageManager.PERMISSION_GRANTED)
        {
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION)== PackageManager.PERMISSION_GRANTED)
            {
                mLocationPermissionsGranted = true;
                initMap();
            }else {
                ActivityCompat.requestPermissions(this,
                        permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }

        }else
        {
            ActivityCompat.requestPermissions(this,permissions,LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false ;

        switch(requestCode)
        {
            case LOCATION_PERMISSION_REQUEST_CODE:
            {
                if(grantResults.length > 0 )
                {
                    for (int i = 0; i < grantResults.length; i++)
                    {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                        {
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }

                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    //initialise our map
                    initMap();
                }
            }
        }
    }
    private void hideSoftKeyboard()
    {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /*
        --------------------------------google places API autocomplete suggestions---------------------------
    */

    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            hideSoftKeyboard();

            final AutocompletePrediction item = mPlaceAutocompleteAdapter.getItem(i);
            final String placeId = item.getPlaceId();

            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if(!places.getStatus().isSuccess())
            {
                Log.d(TAG, "OnResult: Place query did not complete successfully" + places.getStatus().toString());
                places.release();
                return;
            }
            final Place place = places.get(0);

            try{
                mPlace = new PlaceInfo();
                mPlace.setName(place.getName().toString());
                Log.d(TAG, "onResult: name:  " + place.getName());
                mPlace.setAddress(place.getAddress().toString());
                Log.d(TAG, "onResult: address:  " + place.getAddress());
                //mPlace.setAttributions(place.getAttributions().toString());
                //Log.d(TAG, "onResult: attribution:  " + place.getAttributions());
                mPlace.setId(place.getId());
                Log.d(TAG, "onResult: id:  " + place.getId());
                mPlace.setLatlng(place.getLatLng());
                Log.d(TAG, "onResult: latlng:  " + place.getLatLng());
                mPlace.setRating(place.getRating());
                Log.d(TAG, "onResult: rating:  " + place.getRating());
                mPlace.setPhoneNumber(place.getPhoneNumber().toString());
                Log.d(TAG, "onResult: phone number:  " + place.getPhoneNumber());
                mPlace.setWebsiteUri(place.getWebsiteUri());
                Log.d(TAG, "onResult: website uri:  " + place.getWebsiteUri());

            Log.d(TAG, "onResult: place :" + mPlace.toString());

            }catch (NullPointerException e){
                 Log.e(TAG, "onResult: NullPointerException :" + e.getMessage());
            }
            moveCamera(new LatLng(place.getViewport().getCenter().latitude, place.getViewport().getCenter().longitude), DEFAULT_ZOOM, mPlace);
            lat2 = place.getViewport().getCenter().latitude;
            lon2 = place.getViewport().getCenter().longitude;
            places.release();
        }
    };


 /*
        --------------------------------distance calculation---------------------------
 */

    private static boolean isInRange (double lat1, double lon1, double lat2, double lon2)
    {

        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;

        //convert to KM
        dist = dist * 1.609344;

        if (dist<=30)
        {
            return true;
        }else
        {
            return false;
        }

    }
    //degree to radian
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    //radian to degree
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    //public MainActivity obj = new MainActivity();
    //MainActivity.district;

    private boolean calculateFromDB(String district, double lat1, double lon1)
    {
            Form form = new Form()
                    .add("hfc_name",district);
            String url = "http://247f2c7b.ngrok.io/noq/v1/Api.php?apicall=calculateDistance";
            Request req = Bridge
                    .post(url)
                    .body(form)
                    .request(new Callback() {
                        @Override
                        public void response(Request request, Response response, BridgeException e) {
                            if (e != null) {
                                Log.d(TAG, "searchLocation: health facility not in database");
                                Toast.makeText(MapActivity.this, "Health Facility is not in our database", Toast.LENGTH_SHORT).show();
                            } else {
                                // Use the Response object
                                //String poo = e.toString();
                                //Log.d(TAG,poo);
                                String responseContent = response.asString();
                                System.out.println(responseContent);
                                try {
                                    //JSONArray jsnArr = response.asJsonArray();
                                    JSONObject obj = new JSONObject(responseContent);
                                    //JSONObject obj = new JSONObject(poo);
                                    Log.d(TAG, obj.toString());
                                    JSONObject userJson = obj.getJSONObject("user");
                                    String check = (String) obj.getString("error");
                                    if (check == "false") {
                                        //JSONObject userJson = obj.getJSONObject("district");
                                        Log.d(TAG, check);
                                        Double latitude = new Double(userJson.getString("latitude"));
                                        Double longitude = new Double(userJson.getString("longitude"));
                                        //Log.d(TAG,district2);
                                        Log.d(TAG, "getSearchLocation: district: " + latitude + longitude);

                                        MapActivity.this.lat3 = latitude;
                                        MapActivity.this.lon3 = longitude;
                                        //Intent intent = new Intent(MainActivity.this, MapActivity.class);
                                        //startActivity(intent);

                                        //System.out.println(userPatient.getFullname() + " : " + userPatient.getIcnumber() + " : " + userPatient.getGender());
                                        //SharedPrefManager.getInstance(getApplicationContext()).patientSign(userPatient);
                                        //JSONObject jsnObj = (JSONObject) jsnArr.get(0);
                                        //String strFullName = (String) jsnObj.get("FULLNAME");
                                        //nameRegister.setText(strFullName);
                                        //nameRegister.setText(userPatient.getFullname());


                                    } else {
                                        Log.d(TAG, "searchLocation: health facility not in database babi hanat");
                                        Toast.makeText(MapActivity.this, "Health Facility is not in our database babi hanat", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (JSONException e1) {
                                    e1.printStackTrace();
                                    System.out.println("JSON Err");
                                }

                            }
                        }
                    });

        double theta = lon1 - this.lon3;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(this.lat3)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(this.lat3)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;

        //convert to KM
        dist = dist * 1.609344;

        if (dist<=30)
        {
            return true;
        }else
        {
            return false;
        }

        }

}



