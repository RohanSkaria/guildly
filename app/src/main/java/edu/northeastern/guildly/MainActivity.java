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

    public static String currentUserEmail = null;

    private void testNotifications() {
        Button testButton = findViewById(R.id.testNotificationButton);
        testButton.setOnClickListener(v -> {
            NotificationService.showHabitReminderNotification(this, "Workout for 30 mins");
            new Handler().postDelayed(() -> {
                NotificationService.showFriendRequestNotification(this, "TestUser");
            }, 3000);
            new Handler().postDelayed(() -> {
                NotificationService.showStreakMilestoneNotification(this, "Morning Run", 7);
            }, 6000);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationService.createNotificationChannel(this);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        Fragment initialFragment = new HomeFragment();
        int initialItem = R.id.home;

        if (getIntent().getBooleanExtra("openConnections", false)) {
            initialFragment = new ConnectionsFragment();
            initialItem = R.id.connections;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.flFragment, initialFragment)
                .commit();

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

        bottomNavigationView.setSelectedItemId(initialItem);

        Intent serviceIntent = new Intent(this, NotificationListenerService.class);
        startService(serviceIntent);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("testNode");

        myRef.setValue("Hello from the app!")
                .addOnSuccessListener(aVoid -> {
                    Log.d("RealtimeDB", "Data written successfully!");
                })
                .addOnFailureListener(e -> {
                    Log.e("RealtimeDB", "Failed to write data", e);
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        100);
            }
        }

        testNotifications();
    }
}
