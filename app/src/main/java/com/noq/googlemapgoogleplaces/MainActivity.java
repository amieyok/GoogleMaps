package com.noq.googlemapgoogleplaces;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.bridge.Bridge;
import com.afollestad.bridge.BridgeException;
import com.afollestad.bridge.Callback;
import com.afollestad.bridge.Form;
import com.afollestad.bridge.Request;
import com.afollestad.bridge.Response;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int ERROR_DIALOG_REQUEST = 9001;

    //vars
    public static String district;
    public static String hfc_cd;

    //widget
    private EditText searchHfc;

    public static String getSearchLocation() {
        return district;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(isServicesOK())
        {
            init();
        }
    }

    public String district()
    {
        return district;
    }

    private void init() {
        Button btnMap = (Button) findViewById(R.id.btnMap);
        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                searchHfc = (EditText)findViewById(R.id.searchHfc);

                getSearchLocation(searchHfc.getText().toString());

            }

            public String getSearchLocation(String hfc_name1) {
                Form form = new Form()
                        .add("hfc_name",hfc_name1);
                String url = "http://247f2c7b.ngrok.io/noq/v1/Api.php?apicall=searchLocation";
                Request req = Bridge
                        .post(url)
                        .body(form)
                        .request(new Callback() {
                            @Override
                            public void response(Request request, Response response, BridgeException e) {
                                if (e != null) {

                                    Toast.makeText(MainActivity.this, "Health Facility is not in our database", Toast.LENGTH_SHORT).show();
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
                                        Log.d(TAG,obj.toString());
                                        JSONObject userJson = obj.getJSONObject("user");
                                        String check = (String) obj.getString("error");
                                        if(check == "false") {
                                            //JSONObject userJson = obj.getJSONObject("district");
                                            Log.d(TAG,check);
                                            String district2 = new String(userJson.getString("district"));
                                            String hfc_code = new String(userJson.getString("hfc_code"));
                                            Log.d(TAG,district2);
                                            Log.d(TAG, "getSearchLocation: district: " + district2);
                                            Log.d(TAG, "getSearchLocation: code: " + hfc_code);

                                            district = district2;
                                            hfc_cd = hfc_code;
                                            Intent intent = new Intent(MainActivity.this, MapActivity.class);
                                            startActivity(intent);

                                            //System.out.println(userPatient.getFullname() + " : " + userPatient.getIcnumber() + " : " + userPatient.getGender());
                                            //SharedPrefManager.getInstance(getApplicationContext()).patientSign(userPatient);
                                            //JSONObject jsnObj = (JSONObject) jsnArr.get(0);
                                            //String strFullName = (String) jsnObj.get("FULLNAME");
                                            //nameRegister.setText(strFullName);
                                            //nameRegister.setText(userPatient.getFullname());

                                        }
                                        else
                                        {
                                            Log.d(TAG,"searchLocation: health facility not in database babi hanat");
                                            Toast.makeText(MainActivity.this, "Health Facility is not in our database babi hanat", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (JSONException e1) {
                                        e1.printStackTrace();
                                        System.out.println("JSON Err");
                                    }

                                }
                            }
                        });
                return district;
            }
        });

        EditText searchHfc = (EditText) findViewById(R.id.searchHfc);
    }

    public boolean isServicesOK()
    {
        Log.d(TAG, "isServicesOK: checking google services version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if(available == ConnectionResult.SUCCESS)
        {
           //everything is fine and the user can make map request
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if (GoogleApiAvailability.getInstance().isUserResolvableError(available))
        {
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST );
            dialog.show();
        }
        else
        {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;

    }
}
