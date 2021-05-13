package com.example.mytaxi;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mytaxi.ui.home.HomeFragment;
import com.example.mytaxi.ui.login.LoginActivity;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements HomeFragment.OnFragmentSendDataListener,
        HomeFragment.OnFragmentSendMapListener,TaskLoadedCallback  {

    private AppBarConfiguration mAppBarConfiguration;
    private LinearLayout driverButtom,customerButtom;

    private Button btnCallTaxi;

    private GoogleMap mMap;
    private LatLng CustomerPosition;
    Location lastLocation;
    private String customerID,driverFoundID;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference assignedCustomerRef, AssignedCustomerPositionRef;
    private DatabaseReference CustomerDatabaseRef;
    private DatabaseReference DriversAvailableRef;
    private DatabaseReference DriversRef;
    private DatabaseReference DriversLocationRef;
    Marker driverMarker, PickUpMarker;
    private MarkerOptions place1, place2;
    private Polyline currentPolyline;

    private ValueEventListener DriverLocationRefListener;
    private ValueEventListener AssignedCustomerPositionListener;

    private int radius = 1;
    private Boolean driverFound = false, requestType= false;

    AutoCompleteTextView TargetLoc;
    EditText edtDistance,edtCoast,edtCustFio,edtCustTel;
    private String driverID;
    private CircleImageView customerPhoto;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mAuth=FirebaseAuth.getInstance();
        currentUser=mAuth.getCurrentUser();
        customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        driverID = mAuth.getCurrentUser().getUid();

        CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customers Requests");
        DriversAvailableRef = FirebaseDatabase.getInstance().getReference().child("Driver_Available");
        DriversLocationRef = FirebaseDatabase.getInstance().getReference().child("Driver Working");

        // получение вью нижнего экрана
        LinearLayout llBottomSheet = (LinearLayout) findViewById(R.id.bottom_sheet);

        driverButtom=findViewById(R.id.driver_buttom);
        customerButtom=findViewById(R.id.customer_buttom);
        btnCallTaxi=findViewById(R.id.btn_call_taxi);
        TargetLoc=findViewById(R.id.target_loc);
        edtDistance=findViewById(R.id.distance);
        edtCoast=findViewById(R.id.cost_d);
        edtCustFio=findViewById(R.id.customer_fio);
        edtCustTel=findViewById(R.id.customer_nmb);

        customerPhoto=findViewById(R.id.customer_photo);

        HomeFragment fragment = (HomeFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_map);
        if (fragment != null)
            fragment.setSelectedItem(customerID);

        List<String> namesList = Arrays.asList(new String[]{"1","2","3"});
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, namesList);
        TargetLoc.setAdapter(adapter);
        TargetLoc.setText("1");
        TargetLoc.setThreshold(1);
        TargetLoc.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selAddress=TargetLoc.getText().toString();
                place2 = new MarkerOptions().position(
                        new LatLng(54.310154, 26.844183/*getAddressByString(selAddress).get(0).getLatitude(), getAddressByString(selAddress).get(0).getLongitude()*/)).title("Конечная точка");
                new FetchURL(MainActivity.this).execute(getUrl(place1.getPosition(), place2.getPosition(), "driving"), "driving");
                findViewById(R.id.travel_info).setVisibility(View.VISIBLE);
            }
        });


// настройка поведения нижнего экрана
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet);
// настройка колбэков при изменениях
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull @NotNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull @NotNull View bottomSheet, float slideOffset) {

            }
        });

                FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        btnCallTaxi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                GeoFire geofire = new GeoFire(CustomerDatabaseRef);
                geofire.setLocation(customerID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

                btnCallTaxi.setText("Поиск водителя...");
                CustomerPosition = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                getNearbyDrivers();
                PickUpMarker = mMap.addMarker(new MarkerOptions().position(CustomerPosition).title("Я здесь").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));
                GetDriverLocation();
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        SharedPreferences sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        Bundle arguments = getIntent().getExtras();
        ed.putString("login_type",arguments.get("login_type").toString());
        ed.commit();

        /*final ConstraintLayout content = findViewById(R.id.content);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string
                .navigation_drawer_close) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(drawerView.getWindowToken(), 0);
                float slideX = drawerView.getWidth() * slideOffset;
                content.setTranslationX(slideX);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();*/
        updUserFIO(navigationView);


        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_map, R.id.nav_settings, R.id.nav_orders,R.id.nav_compl_orders)
                .setDrawerLayout(drawer)
                .build();

        setUserType(navigationView);//установка интерфейса для разных пользователей


        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);



    }
    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + "AIzaSyDs26pCzhjI7oWIj0adoy-wLXy-KAvkHwg";
        return url;
    }
    private List<Address> getAddressByString(String locName) throws IOException {
        Geocoder gc = new Geocoder(this);
        List<Address> addressList = gc.getFromLocationName(locName, 3);
        return addressList;
    }
    @Override
    public void onSendData(Location location) {
        lastLocation=location;
        place1 = new MarkerOptions().position(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude())).title("вы сдесь");
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        int id = item.getItemId();
        switch(id){

            case R.id.action_settings:
                Bundle bundle = new Bundle();
                getNavController().navigate(R.id.nav_settings,bundle);
                return true;
            case R.id.action_exit:
                //mAuth.signOut();
                Intent start=new Intent(MainActivity.this, LoginActivity.class);
                startActivity(start);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUserType(NavigationView navigationView) {
        Bundle arguments = getIntent().getExtras();
        if(arguments!=null &&arguments.get("login_type").toString().equals("driver")){
            Menu menuNav = navigationView.getMenu();
            MenuItem Orders=menuNav.findItem(R.id.nav_orders);
            Orders.setVisible(false);
            MenuItem complOrders=menuNav.findItem(R.id.nav_compl_orders);
            complOrders.setVisible(true);
            driverButtom.setVisibility(View.VISIBLE);
            customerButtom.setVisibility(View.GONE);
            getAssignedCustomerRequest();
        }
        if(arguments!=null &&arguments.get("login_type").toString().equals("customer")&&!arguments.get("mail").toString().equals("admen@gmail.com")){
            Menu menuNav = navigationView.getMenu();
            MenuItem complOrders=menuNav.findItem(R.id.nav_compl_orders);
            complOrders.setVisible(false);
            MenuItem Orders=menuNav.findItem(R.id.nav_orders);
            Orders.setVisible(false);
            driverButtom.setVisibility(View.GONE);
            customerButtom.setVisibility(View.VISIBLE);
        }
        if(arguments!=null &&arguments.get("mail").toString().equals("admen@gmail.com")){
            Menu menuNav = navigationView.getMenu();
            MenuItem complOrders=menuNav.findItem(R.id.nav_compl_orders);
            complOrders.setVisible(false);
            MenuItem Orders=menuNav.findItem(R.id.nav_orders);
            Orders.setVisible(true);
        }
    }


    private void getAssignedCustomerRequest() {
        assignedCustomerRef = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverID).child("CustomerRideID");

        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    customerID = dataSnapshot.getValue().toString();

                    getAssignedCustomerPosition();


                    getAssignedCustomerInformation();
                }
                else {
                    customerID = "";

                    if (PickUpMarker!=null)
                    {
                        PickUpMarker.remove();
                    }

                    if(AssignedCustomerPositionListener!= null)
                    {
                        AssignedCustomerPositionRef.removeEventListener(AssignedCustomerPositionListener);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerPosition() {
        AssignedCustomerPositionRef = FirebaseDatabase.getInstance().getReference().child("Customers Requests")
                .child(customerID).child("l");

        AssignedCustomerPositionListener = AssignedCustomerPositionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    List<Object> customerPositionMap = (List<Object>) dataSnapshot.getValue();
                    double locationLat = Double.parseDouble(customerPositionMap.get(0).toString());
                    double locationLng = Double.parseDouble(customerPositionMap.get(1).toString());

                    LatLng DriverLatLng = new LatLng(locationLat, locationLng);
                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("Забрать клиента тут").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(DriverLatLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerInformation() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Customers").child(customerID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0)
                {
                    String name = dataSnapshot.child("name").getValue().toString();
                    final String phone  = dataSnapshot.child("phone").getValue().toString();



                    edtCustFio.setText(name);
                    edtCustTel.setText(phone);

                    /*callCustomer.setOnClickListener(new View.OnClickListener() {//вызов пользователя
                        @Override
                        public void onClick(View view) {
                            int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE);

                            if(permissionCheck != PackageManager.PERMISSION_GRANTED){
                                ActivityCompat.requestPermissions(
                                        MainActivity.this, new String[]{Manifest.permission.CALL_PHONE}, 123
                                );
                            }
                            else{
                                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel: " + phone));
                                startActivity(intent);
                            }
                        }
                    });*/

                    if (dataSnapshot.hasChild("image")) {
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(customerPhoto);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    public NavController getNavController(){
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return navController;
    }
    private void updUserFIO(NavigationView navigationView) {
        Bundle arguments = getIntent().getExtras();
        if(arguments!=null){
            String  s =arguments.get("mail").toString();
            //user=new LoggedInUser(mDBHelper.getUser( arguments.get("user").toString()));
            View header=navigationView.getHeaderView(0);

            TextView user_fio=(TextView) header.findViewById(R.id.user_mail);
            //TextView user_mail=(TextView) header.findViewById(R.id.user_mail);
            user_fio.setText(s);
            //user_mail.setText(user.getMail());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


    @Override
    public void onSendData(GoogleMap map) {
        mMap=map;
    }

    private void getNearbyDrivers() {
        GeoFire geoFire = new GeoFire(DriversAvailableRef);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(CustomerPosition.latitude, CustomerPosition.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestType)
                {
                    driverFound = true;
                    final String driverFoundID = key;

                    DriversRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideID", customerID);
                    DriversRef.updateChildren(driverMap);

                   /* DriverLocationRefListener = DriversLocationRef.child(driverFoundID).child("l").
                            addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.exists() && requestType)
                                    {
                                        List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();
                                        double locationLat = 0;
                                        double locationLng = 0;

                                        btnCallTaxi.setText("Водитель найден");

                                        //relativeLayout.setVisibility(View.VISIBLE);
                                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                                                .child("Users").child("Drivers").child(driverFoundID);

                                        reference.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0)
                                                {
                                                    String name = dataSnapshot.child("name").getValue().toString();
                                                    final String phone  = dataSnapshot.child("phone").getValue().toString();
                                                    String carname = dataSnapshot.child("carname").getValue().toString();



                                                    txtName.setText(name);
                                                    txtPhone.setText(phone);
                                                    txtCarName.setText(carname);

                                                    callDriver.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            int permissionCheck = ContextCompat.checkSelfPermission(CustomersMapActivity.this, Manifest.permission.CALL_PHONE);

                                                            if(permissionCheck != PackageManager.PERMISSION_GRANTED){
                                                                ActivityCompat.requestPermissions(
                                                                        CustomersMapActivity.this, new String[]{Manifest.permission.CALL_PHONE}, 123
                                                                );
                                                            }
                                                            else{
                                                                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel: " + phone));
                                                                startActivity(intent);
                                                            }
                                                        }
                                                    });

                                                    if (dataSnapshot.hasChild("image")) {
                                                        String image = dataSnapshot.child("image").getValue().toString();
                                                        Picasso.get().load(image).into(driverPhoto);
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });

                                        if (driverLocationMap.get(0) != null)
                                        {
                                            locationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                                        }
                                        if (driverLocationMap.get(1) != null)
                                        {
                                            locationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                                        }
                                        LatLng DriverLatLng = new LatLng(locationLat, locationLng);

                                        if(driverMarker !=null)
                                        {
                                            driverMarker.remove();
                                        }

                                        Location location1 = new Location("");
                                        location1.setLatitude(CustomerPosition.latitude);
                                        location1.setLongitude(CustomerPosition.longitude);

                                        Location location2 = new Location("");
                                        location2.setLatitude(DriverLatLng.latitude);
                                        location2.setLongitude(DriverLatLng.longitude);

                                        float Distance = location1.distanceTo(location2);
                                        if(Distance>100)
                                        {
                                            btn.setText("Ваше такси подъезжает");
                                        }
                                        else {
                                            btnCallTaxi.setText("Расстояние до такси" + String.valueOf(Distance));
                                        }

                                        //driverMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng)
                                         //       .title("Ваше такси тут").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });*/
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound)
                {
                    radius = radius + 1;
                    getNearbyDrivers();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
    private void GetDriverLocation(){
        if(!driverFoundID.equals(""))
        DriversLocationRef.child(driverFoundID).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            List<Object> driverLocationMap=(List<Object>) snapshot.getValue();
                            double LocationLat=0;
                            double LocationLng=0;
                       Toast.makeText(getBaseContext(),"Водитель найден!",Toast.LENGTH_LONG).show();
                       if(driverLocationMap.get(0)!=null){//получение координат назначенного водителя
                           LocationLat= Double.parseDouble(driverLocationMap.get(0).toString());
                       }
                       if(driverLocationMap.get(1)!=null){
                           LocationLng= Double.parseDouble(driverLocationMap.get(1).toString());
                       }
                       LatLng DriverLatnLng=new LatLng(LocationLat,LocationLng);

                       if(driverMarker!=null){
                           driverMarker.remove();
                       }
                       else{//ставим метку на найденного водителя и определяем расстояние между клиентом и водителем, стоимость
                           Location location1=new Location("");
                           location1.setLatitude(DriverLatnLng.latitude);
                           location1.setLongitude(DriverLatnLng.longitude);

                            float Distance=lastLocation.distanceTo(location1);
                            edtDistance.setText(Distance+" километров");
                           double Coast=Distance*3.25;
                           edtCoast.setText("К оплате:"+Coast+" р");
                           driverMarker=mMap.addMarker(new MarkerOptions().position(DriverLatnLng).title("Ваше такси"));
                       }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {

                    }
                });
    }

    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = mMap.addPolyline((PolylineOptions) values[0]);
    }
}