package com.example.petlocation.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.petlocation.R;
import com.example.petlocation.model.UserLocation;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import javax.annotation.Nullable;

public class LocationService extends Service implements LocationListener {
    private DatabaseReference locationRef;
    private LocationManager locationManager;
    private static final int NOTIFICATION_ID = 123;
    private static final String CHANNEL_ID = "YourChannelId";

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        locationRef = FirebaseDatabase.getInstance().getReference("locations");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        requestLocationUpdates();
        return START_STICKY;
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(this);
    }

    private void startForegroundService() {
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
    }

    private Notification createNotification() {
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Dịch vụ Foreground")
                .setContentText("Dịch vụ đang chạy")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Kênh Dịch vụ Foreground",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}
