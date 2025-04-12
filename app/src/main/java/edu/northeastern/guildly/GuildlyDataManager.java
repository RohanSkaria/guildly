package edu.northeastern.guildly;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import edu.northeastern.guildly.data.Habit;

public class GuildlyDataManager {

    private static GuildlyDataManager instance;

    public static synchronized GuildlyDataManager getInstance() {
        if (instance == null) {
            instance = new GuildlyDataManager();
        }
        return instance;
    }

    private DatabaseReference userHabitsRef;
    private ValueEventListener habitsListener;
    private String currentUserKey;

    // LiveData that Fragments can observe
    private final MutableLiveData<List<Habit>> habitsLiveData = new MutableLiveData<>();

    private GuildlyDataManager() { }

    public void init(String userKey) {
        // if already initialized for this same user, do nothing
        if (userHabitsRef != null && userKey.equals(currentUserKey)) {
            return;
        }
        // remove old listener if switching users
        detachListeners();

        currentUserKey = userKey;
        userHabitsRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userKey)
                .child("habits");

        // Attach a persistent real-time listener
        habitsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Habit> newHabits = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Habit h = ds.getValue(Habit.class);
                    if (h != null) {
                        newHabits.add(h);
                    }
                }
                // Post this updated list to LiveData
                habitsLiveData.setValue(newHabits);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // handle error if needed
            }
        };

        userHabitsRef.addValueEventListener(habitsListener);
    }

    public LiveData<List<Habit>> getHabitsLiveData() {
        return habitsLiveData;
    }

    public void detachListeners() {
        if (userHabitsRef != null && habitsListener != null) {
            userHabitsRef.removeEventListener(habitsListener);
        }
        userHabitsRef = null;
        habitsListener = null;
        currentUserKey = null;
    }
}
