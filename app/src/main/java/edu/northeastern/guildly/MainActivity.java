package edu.northeastern.guildly;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

// 1) Import Firebase Realtime Database references
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // Sets your activity layout

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.flFragment, new HomeFragment())
                    .commit();
        }

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
    }
}
