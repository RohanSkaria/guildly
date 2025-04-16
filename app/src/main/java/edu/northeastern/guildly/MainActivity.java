package edu.northeastern.guildly;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.Manifest;
import android.widget.Button;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import edu.northeastern.guildly.services.NotificationListenerService;
import edu.northeastern.guildly.services.NotificationService;

public class  MainActivity extends AppCompatActivity {

    // 1) A public static field to track the current user's email
    public static String currentUserEmail = null;

    private void testNotifications() {
        // Test various notifications
        Button testButton = findViewById(R.id.testNotificationButton);
        testButton.setOnClickListener(v -> {
            // Test habit reminder
            NotificationService.showHabitReminderNotification(this, "Workout for 30 mins");

            // Test friend request
            new Handler().postDelayed(() -> {
                NotificationService.showFriendRequestNotification(this, "TestUser");
            }, 3000);

            // Test streak milestone
            new Handler().postDelayed(() -> {
                NotificationService.showStreakMilestoneNotification(this, "Morning Run", 7);
            }, 6000);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // Sets your activity layout

        // for notifs
        NotificationService.createNotificationChannel(this);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.flFragment, new HomeFragment())
                    .commit();
        }

        if (getIntent().getBooleanExtra("openConnections", false)) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.flFragment, new ConnectionsFragment())
                    .commit();
            bottomNavigationView.setSelectedItemId(R.id.connections);
        } else if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.flFragment, new HomeFragment())
                    .commit();
        }

        Intent serviceIntent = new Intent(this, NotificationListenerService.class);
        startService(serviceIntent);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.home) {
                selectedFragment = new HomeFragment();
            } else if (id == R.id.profile) {
                selectedFragment = new ProfileFragment();
            } else if (id == R.id.connections) {
                selectedFragment = new ConnectionsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flFragment, selectedFragment)
                        .commit();
            }

            return true;
        });

        bottomNavigationView.setSelectedItemId(R.id.home);

        // 2) Write a test value to Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("testNode");

        myRef.setValue("Hello from the app!")
                .addOnSuccessListener(aVoid -> {
                    Log.d("RealtimeDB", "Data written successfully!");
                })
                .addOnFailureListener(e -> {
                    Log.e("RealtimeDB", "Failed to write data", e);
                });

        // idt this applies since were using oreo but idk just in case ?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        100);
            }
        }

        // testing
        testNotifications();
    }
}
