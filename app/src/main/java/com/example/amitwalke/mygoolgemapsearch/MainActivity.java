package com.example.amitwalke.mygoolgemapsearch;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.amitwalke.mygoolgemapsearch.homeinfo.model.AddressAutoCompleteAdapter;
import com.example.amitwalke.mygoolgemapsearch.model.DropLocationModel;
import com.example.amitwalke.mygoolgemapsearch.model.PickLocationModel;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.clustering.ClusterManager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import butterknife.BindView;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,View.OnClickListener{

    ImageView imgViewBtnClearPickLocation;
    ImageView imgViewBtnClearDropLocation;
    ImageView imgViewBtnPickMyCurrentLocation;

    AutoCompleteTextView autoCompleteTextViewPickLocation;
    AutoCompleteTextView autoCompleteTextViewDropLocation;

    private GoogleApiClient mGoogleApiClient;
    private AddressAutoCompleteAdapter addressAutoCompleteAdapter;

    private double pickLatitude;
    private double pickLongitude;

    ImageView imgViewBtnCenterPin;

    GoogleMap googleMap;

    PickLocationModel pickLocationModel ;
    DropLocationModel dropLocationModel;

    private CameraIdleListener cameraIdleListener;

    int PLACE_AUTOCOMPLETE_PICK_REQUEST_CODE = 1;
    int PLACE_AUTOCOMPLETE_DROP_REQUEST_CODE = 2;
    String selectedPlace="";
    boolean isPickLocationFoculable;
    boolean isdropLocationFoculable;

    TextView txtViewPickLocationMoc;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map_passenger_home);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        imgViewBtnClearPickLocation=findViewById(R.id.ibtn_clear_text_field);
        imgViewBtnClearDropLocation=findViewById(R.id.ibtn_clear_text_field_drop_location);
        imgViewBtnPickMyCurrentLocation=findViewById(R.id.ibtn_goto_current_location);
        autoCompleteTextViewPickLocation=findViewById(R.id.txt_book_a_car_pick_location);
        autoCompleteTextViewDropLocation=findViewById(R.id.txt_book_a_car_drop_location);
        imgViewBtnCenterPin=findViewById(R.id.img_address_pin);
        txtViewPickLocationMoc=findViewById(R.id.txtView_pick);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .build();
        imgViewBtnCenterPin.setTag(PLACE_AUTOCOMPLETE_PICK_REQUEST_CODE);
        setListener();
    }

    private void setListener() {
        imgViewBtnPickMyCurrentLocation.setOnClickListener(this);
        imgViewBtnClearDropLocation.setOnClickListener(this);
        imgViewBtnClearPickLocation.setOnClickListener(this);
        //autoCompleteTextViewPickLocation.setOnClickListener(this);
        autoCompleteTextViewDropLocation.setOnClickListener(this);
        txtViewPickLocationMoc.setOnClickListener(this);

        autoCompleteTextViewPickLocation.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    autoCompleteTextViewPickLocation.setText("");
                }else{
                    autoCompleteTextViewPickLocation.setText(selectedPlace);
                    txtViewPickLocationMoc.setVisibility(View.VISIBLE);
                }
            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap=googleMap;
        cameraIdleListener = new CameraIdleListener(this, googleMap);
        loadMap();
        loadPlacesAutoCompleteData();
    }

    private void loadMap() {
        try {
            UiSettings settings = googleMap.getUiSettings();
            settings.setMapToolbarEnabled(false);
            settings.setCompassEnabled(false);
            settings.setMyLocationButtonEnabled(true);
            settings.setRotateGesturesEnabled(false);
            if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(this), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            googleMap.setMyLocationEnabled(true);
            googleMap.setOnCameraIdleListener(cameraIdleListener);
            googleMap.setOnMarkerClickListener(cameraIdleListener);

        } catch (Exception e) {
        }
    }

    private class CameraIdleListener extends ClusterManager implements GoogleMap.OnCameraIdleListener {
        CameraIdleListener(Context context, GoogleMap map) {
            super(context, map);
        }
        @Override
        public void onCameraIdle() {
            super.onCameraIdle();
            LatLng latLng = googleMap.getCameraPosition().target;
            Geocoder geocoder;
            List<Address> addresses = null;
            geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
            try {
                addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(addresses!=null && addresses.size()!=0){
                selectedPlace = addresses.get(0).getAddressLine(0);
            }
            autoCompleteTextViewPickLocation.setText(selectedPlace);
            autoCompleteTextViewPickLocation.clearFocus();


            Integer tag = Integer.valueOf(imgViewBtnCenterPin.getTag().toString());
            if (tag == PLACE_AUTOCOMPLETE_PICK_REQUEST_CODE) {
                if(pickLocationModel==null){
                    pickLocationModel=new PickLocationModel(latLng.latitude,latLng.longitude);
                }

                pickLocationModel.setLatitude(latLng.latitude);
                pickLocationModel.setLongitude(latLng.longitude);
                pickLocationModel.setLocationName(selectedPlace);
                autoCompleteTextViewPickLocation.setText(selectedPlace);
            }else if (tag == PLACE_AUTOCOMPLETE_DROP_REQUEST_CODE) {
                autoCompleteTextViewDropLocation.setText(selectedPlace);
            }

//            Integer tag = Integer.valueOf(imgCenterPin.getTag().toString());

        }
    }

    private void loadPlacesAutoCompleteData() {

        autoCompleteTextViewPickLocation.setOnItemClickListener(new AutoCompleteTextCLickListener());
        autoCompleteTextViewPickLocation.setOnEditorActionListener(new AddressEditorActionListener());

            addressAutoCompleteAdapter = new AddressAutoCompleteAdapter(this, mGoogleApiClient, null,
                    null);
            autoCompleteTextViewPickLocation.setAdapter(addressAutoCompleteAdapter);
        autoCompleteTextViewPickLocation.setThreshold(0);
    }

    private class AutoCompleteTextCLickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            final AutocompletePrediction item = addressAutoCompleteAdapter.getItem(position);
            final String placeId = item.getPlaceId();

            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(new UpdatedPlaceDetailsCallBack());

        }
    }


    private class UpdatedPlaceDetailsCallBack implements ResultCallback<PlaceBuffer> {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                places.release();
                return;
            }


            pickLatitude= places.get(0).getLatLng().latitude;
            pickLongitude= places.get(0).getLatLng().longitude;

            pickLocationModel=new PickLocationModel(pickLatitude,pickLongitude);
            setPickDropPlace(pickLocationModel,null);

            places.release();

        }
    }

    public void setPickDropPlace(PickLocationModel pickupPlaceLocationModel, DropLocationModel deliveryPlaceLocationModel) {

        CameraPosition cameraPosition = null;

        if (pickupPlaceLocationModel != null) {
            cameraPosition = new CameraPosition.Builder().target(new LatLng(pickupPlaceLocationModel.getLatitude(), pickupPlaceLocationModel.getLongitude())).zoom(15).build();
            imgViewBtnCenterPin.setImageResource(R.drawable.ic_pic_location_center);
        } else if (deliveryPlaceLocationModel != null) {
            cameraPosition = new CameraPosition.Builder().target(new LatLng(deliveryPlaceLocationModel.getLatitude(), deliveryPlaceLocationModel.getLongitude())).zoom(15).build();
            imgViewBtnCenterPin.setImageResource(R.drawable.user_pin);
        }
        if (cameraPosition == null) {
            return;
        }
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    /**
     * Editor listener to detect done button click
     */
    private class AddressEditorActionListener implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) { ;
                autoCompleteTextViewPickLocation.clearFocus();
                return true;
            }
            return false;
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();

    }

    @Override
    public void onStop() {
        if (mGoogleApiClient != null)
            mGoogleApiClient.disconnect();
        super.onStop();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ibtn_clear_text_field:
                autoCompleteTextViewPickLocation.setText("");
                autoCompleteTextViewPickLocation.requestFocus();
                autoCompleteTextViewPickLocation.setCursorVisible(true);
                break;
            case R.id.ibtn_clear_text_field_drop_location:
                autoCompleteTextViewDropLocation.setText("");
                autoCompleteTextViewDropLocation.requestFocus();
                autoCompleteTextViewDropLocation.setCursorVisible(true);
                break;
            case R.id.ibtn_goto_current_location:
                navigateToMyCurrentLocation();
                break;
            case R.id.txt_book_a_car_pick_location:
                autoCompleteTextViewPickLocation.setText("");
                imgViewBtnCenterPin.setTag(PLACE_AUTOCOMPLETE_PICK_REQUEST_CODE);
                break;
            case R.id.txt_book_a_car_drop_location:
                autoCompleteTextViewDropLocation.setText("");
                imgViewBtnCenterPin.setTag(PLACE_AUTOCOMPLETE_DROP_REQUEST_CODE);
                break;
            case R.id.txtView_pick:
                txtViewPickLocationMoc.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        InputMethodManager imm = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm.isAcceptingText()) {
          hideKeyboard(this);
          autoCompleteTextViewPickLocation.clearFocus();
        } else {
            super.onBackPressed();
        }


    }

    public static void hideKeyboard(Activity activity) {
        View view = activity.findViewById(android.R.id.content);
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    public void navigateToMyCurrentLocation() {
        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(Objects.requireNonNull(this));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng latLang = new LatLng(location.getLatitude(), location .getLongitude());
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLang, 17);
                            googleMap.animateCamera(cameraUpdate);
                        }
                    }
                });
    }
}
