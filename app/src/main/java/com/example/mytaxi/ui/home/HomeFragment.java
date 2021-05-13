package com.example.mytaxi.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.mytaxi.FetchURL;
import com.example.mytaxi.MainActivity;
import com.example.mytaxi.R;
import com.example.mytaxi.TaskLoadedCallback;
import com.example.mytaxi.ui.login.LoginActivity;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import kotlin.jvm.internal.ShortSpreadBuilder;

public class HomeFragment extends Fragment implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        TaskLoadedCallback {
    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    public String loginType,customerID="";
    private FirebaseAuth mAuth;
    private MarkerOptions place1, place2;
    private Polyline currentPolyline;
    private FirebaseUser currentUser;

    AutoCompleteTextView TargetLoc;
    Switch DriverEnabled;
    EditText currLoc;
    private boolean DriverIsEnabled=false,currentLogoutDriverStatus=false;

    // обновление текстового поля
    public void setSelectedItem(String postedID) {
        customerID=postedID;
    }
    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = mMap.addPolyline((PolylineOptions) values[0]);
    }

    public interface OnFragmentSendDataListener {
        void onSendData(Location location);
    }
    public interface OnFragmentSendMapListener {
        void onSendData(GoogleMap map);
    }

    public OnFragmentSendDataListener fragmentSendDataListener;
    public OnFragmentSendMapListener fragmentSendMapListener;

    private OnMapReadyCallback callback = new OnMapReadyCallback() {


        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;
        fragmentSendMapListener.onSendData(mMap);
            buildGoogleApiClient();
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);

        }
    };

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        try {
            fragmentSendDataListener = (OnFragmentSendDataListener) context;
            fragmentSendMapListener = (OnFragmentSendMapListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " должен реализовывать интерфейс OnFragmentInteractionListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapView);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
        SharedPreferences sPref = getActivity().getPreferences( Context.MODE_PRIVATE);
        loginType = sPref.getString("login_type", "");

        getActivity().findViewById(R.id.bottom_sheet).setVisibility(View.VISIBLE);
        currLoc=(EditText) getActivity().findViewById(R.id.curr_loc);
        DriverEnabled=getActivity().findViewById(R.id.driver_enabled);

        DriverEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(DriverEnabled.isChecked()){
                    DriverIsEnabled=true;
                }
                else
                    DriverIsEnabled=false;

            }
        });

        mAuth=FirebaseAuth.getInstance();
        currentUser=mAuth.getCurrentUser();

    }

    @Override
    public void onConnected(@Nullable @org.jetbrains.annotations.Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(100000);
        locationRequest.setFastestInterval(100000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull @NotNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

            lastLocation = location;
// Посылаем данные Activity
        fragmentSendDataListener.onSendData(lastLocation);

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

            //текущий адрес
        currLoc.setText(""+getCompleteAddressString(lastLocation.getLatitude(),lastLocation.getLongitude()));

        TargetLoc=getActivity().findViewById(R.id.target_loc);

        List<String> namesList = Arrays.asList(new String[]{"1","2","3"});
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_dropdown_item_1line, namesList);
        TargetLoc.setAdapter(adapter);
        TargetLoc.setText("1");
        TargetLoc.setThreshold(1);
        //place2 = new MarkerOptions().position(new LatLng(54.310154, 26.844183)).title("Target Trip");
       /* TargetLoc.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                ArrayAdapter<Address> adapter = null;
                try {
                    adapter = new ArrayAdapter<>(
                            getContext(), android.R.layout.simple_dropdown_item_1line, getAddressByString(v.getText().toString()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                TargetLoc.setAdapter(adapter);
                TargetLoc.setThreshold(1);
                return false;
            }
        });*/


        if(loginType.equals("customer")) {//расположение водителя, если зашли за него
            //маркер
        }
        if(loginType.equals("driver")) {//расположение водителя, если зашли за него
            //маркер

        if(getContext() != null){
            lastLocation = location;


            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference DriverAvalablityRef = FirebaseDatabase.getInstance().getReference().child("Driver Available");
            GeoFire geoFireAvailablity = new GeoFire(DriverAvalablityRef);

            DatabaseReference DriverWorkingRef = FirebaseDatabase.getInstance().getReference().child("Driver Working");
            GeoFire geoFireWorking = new GeoFire(DriverWorkingRef);


            switch (customerID)
            {
                case "":
                    geoFireWorking.removeLocation(userID);
                    geoFireAvailablity.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                default:
                    geoFireAvailablity.removeLocation(userID);
                    geoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }
        }
        }
    }
    private void DisconnectDriver()
    {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference DriverAvalablityRef = FirebaseDatabase.getInstance().getReference().child("Driver_Available");

        GeoFire geoFire = new GeoFire(DriverAvalablityRef);
        geoFire.removeLocation(userID);
    }
    private String getCompleteAddressString(double LATITUDE, double LONGITUDE) {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(getContext(), new Locale("ru", "RU"));
        try {
            List<Address> addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);
                strAdd = addresses.get(0).getAddressLine(0);
            }
     catch (Exception e) {
            e.printStackTrace();
            Log.w("My Current loction address", "Canont get Address!");
        }
        return strAdd;
    }
    private List<Address> getAddressByString(String locName) throws IOException {
        Geocoder gc = new Geocoder(getContext());
        List<Address> addressList = gc.getFromLocationName(locName, 3);
        return addressList;
    }

    // обновление текстового поля
    public void setTargetPlace(String targetPlace) {
      //  place1 = new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).title("Start Trip");
    }

    protected synchronized void buildGoogleApiClient()
    {
        googleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }
    @Override
    public void onMapReady(@NonNull @NotNull GoogleMap googleMap) {
        mMap = googleMap;

        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!currentLogoutDriverStatus) {

            DisconnectDriver();
        }
    }


}
