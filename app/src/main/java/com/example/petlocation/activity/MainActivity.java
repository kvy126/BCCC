package com.example.petlocation.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.petlocation.R;
import com.example.petlocation.model.UserLocation;
import com.example.petlocation.service.LocationService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener, LocationListener {
    private MapView mapView;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private DatabaseReference locationRef;
    private Button qrButton, qrScan;
    private LocationManager locationManager;
    private static final String PREF_QUET_UID = "pref_quet_uid";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        locationRef = FirebaseDatabase.getInstance().getReference("locations");
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_main);
        startService(new Intent(this, LocationService.class));
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        qrButton = findViewById(R.id.qrButton);
        qrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialog = new Dialog(MainActivity.this);
                dialog.setContentView(R.layout.qr_code);
                ImageView imageViewPopup = dialog.findViewById(R.id.imageView);
                displayQRCode(imageViewPopup);
                dialog.show();
            }
        });
        qrScan = findViewById(R.id.qrScan);
        qrScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startQRScanner();
            }
        });
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String quetUID = sharedPreferences.getString(PREF_QUET_UID, "");
        if (!quetUID.isEmpty()) {
            getLocationFromFirebase(quetUID);
        }
    }

    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan a QR code");
        integrator.setBeepEnabled(true);
        integrator.setCaptureActivity(CustomScannerActivity.class);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
            } else {
                String scannedData = result.getContents();
                getLocationFromFirebase(scannedData);
            }
        }
    }

    private void getLocationFromFirebase(String scannedUID) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_QUET_UID, scannedUID);
        editor.apply();
        DatabaseReference userLocationRef = locationRef.child(scannedUID);
        userLocationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    double otherDeviceLatitude = dataSnapshot.child("latitude").getValue(Double.class);
                    double otherDeviceLongitude = dataSnapshot.child("longitude").getValue(Double.class);
                    Location lastKnownLocation = mapboxMap.getLocationComponent().getLastKnownLocation();
                    if (lastKnownLocation != null) {
                        double yourLatitude = lastKnownLocation.getLatitude();
                        double yourLongitude = lastKnownLocation.getLongitude();
                        float[] results = new float[1];
                        Location.distanceBetween(yourLatitude, yourLongitude, otherDeviceLatitude, otherDeviceLongitude, results);
                        float distanceInMeters = results[0];
                        updateMapWithLocationsAndDistance(yourLatitude, yourLongitude, otherDeviceLatitude, otherDeviceLongitude, distanceInMeters);
                    }
                } else
                    Toast.makeText(MainActivity.this, "Không tìm thấy vị trí cho UID này", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void clearQuetUID() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PREF_QUET_UID);
        editor.apply();
    }

    private Marker otherDeviceLocationMarker;
    private Polyline distanceLine;

    private void updateMapWithLocationsAndDistance(double yourLatitude, double yourLongitude, double otherDeviceLatitude, double otherDeviceLongitude, float distanceInMeters) {
        LatLng yourLocation = new LatLng(yourLatitude, yourLongitude);
        LatLng otherDeviceLocation = new LatLng(otherDeviceLatitude, otherDeviceLongitude);
        if (otherDeviceLocationMarker != null)
            otherDeviceLocationMarker.remove();
        otherDeviceLocationMarker = mapboxMap.addMarker(new MarkerOptions().position(otherDeviceLocation).title("Other Device Location"));
        if (distanceLine != null)
            distanceLine.remove();
        List<LatLng> points = new ArrayList<>();
        points.add(otherDeviceLocation);
        points.add(yourLocation);
        distanceLine = mapboxMap.addPolyline(new PolylineOptions().addAll(points).color(Color.BLUE).width(5));
        Toast.makeText(MainActivity.this, "Khoảng cách: " + distanceInMeters + "m", Toast.LENGTH_SHORT).show();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(yourLocation);
        builder.include(otherDeviceLocation);
        LatLngBounds bounds = builder.build();
        int padding = 100;
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }

    private void displayQRCode(ImageView imageView) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userUUID = currentUser.getUid();
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        try {
            Bitmap bitmap = barcodeEncoder.encodeBitmap(userUUID, BarcodeFormat.QR_CODE, 400, 400);
            imageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        if (PermissionsManager.areLocationPermissionsGranted(this))
            mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
                enableLocationComponent();
            });
        else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressLint("MissingPermission")
    private void enableLocationComponent() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            LocationComponentOptions locationComponentOptions = LocationComponentOptions.builder(this).build();
            LocationComponentActivationOptions activationOptions = new LocationComponentActivationOptions.Builder(this, mapboxMap.getStyle()).locationComponentOptions(locationComponentOptions).build();
            mapboxMap.getLocationComponent().activateLocationComponent(activationOptions);
            mapboxMap.getLocationComponent().setLocationComponentEnabled(true);
            mapboxMap.getLocationComponent().setCameraMode(CameraMode.TRACKING);
            mapboxMap.getLocationComponent().setRenderMode(RenderMode.NORMAL);
            Location lastKnownLocation = mapboxMap.getLocationComponent().getLastKnownLocation();
            if (lastKnownLocation != null) {
                double currentLatitude = lastKnownLocation.getLatitude();
                double currentLongitude = lastKnownLocation.getLongitude();
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    String userUUID = currentUser.getUid();
                    UserLocation userLocation = new UserLocation(currentLatitude, currentLongitude);
                    locationRef.child(userUUID).setValue(userLocation);
                }
            }
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted)
            enableLocationComponent();
        else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userUUID = currentUser.getUid();
            UserLocation userLocation = new UserLocation(location.getLatitude(), location.getLongitude());
            locationRef.child(userUUID).setValue(userLocation);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}