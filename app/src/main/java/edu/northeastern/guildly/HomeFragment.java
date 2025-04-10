package edu.northeastern.guildly;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.northeastern.guildly.adapters.HabitAdapter;
import edu.northeastern.guildly.data.Habit;
import edu.northeastern.guildly.data.User;

public class HomeFragment extends Fragment {

    private TextView tvUserName, tvStreak,tvWeeklyChallenge;
    private ImageView weeeklyChallengeIcon;
    private RecyclerView habitRecyclerView;
    private Button btnAddHabit;

    private HabitAdapter habitAdapter,weeklyHabitAdapter;
    private final List<Habit> habitList = new ArrayList<>();

    private DatabaseReference userRef;       // /users/<myUserKey>
    private DatabaseReference userHabitsRef; // /users/<myUserKey>/habits
    private String myUserKey;

    private final List<Habit> predefinedHabits = Arrays.asList(
            new Habit("Drink 64oz of water", R.drawable.ic_water),
            new Habit("Workout for 30 mins", R.drawable.ic_workout),
            new Habit("Do homework", R.drawable.ic_homework),
            new Habit("Read a book", R.drawable.ic_book),
            new Habit("Meditate for 10 minutes", R.drawable.ic_meditation),
            new Habit("Save money today", R.drawable.ic_savemoney),
            new Habit("Eat vegetables", R.drawable.ic_vegetable),
            new Habit("No phone after 10PM", R.drawable.ic_phonebanned)
    );

    private final List<Habit> weeklyChallengeOptions = Arrays.asList(
            // TODO: add to the backend
            new Habit("Take a walk outside", R.drawable.ic_walk_icon),
            new Habit("Drink tea instead of coffee", R.drawable.ic_tea),
            new Habit("Compliment someone", R.drawable.ic_compliment),
            new Habit("Journal for 5 minutes", R.drawable.ic_journal),
            new Habit("No social media today", R.drawable.ic_nosocial),
            new Habit("Stretch for 10 minutes", R.drawable.ic_stretch),
            new Habit("Sleep 8+ hours", R.drawable.ic_sleep)
    );

    public HomeFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

//        tvUserName = view.findViewById(R.id.user_name);
        tvStreak   = view.findViewById(R.id.textViewStreak);
        habitRecyclerView = view.findViewById(R.id.habit_list);
        btnAddHabit = view.findViewById(R.id.btn_add_habit);
        tvWeeklyChallenge = view.findViewById(R.id.weekly_challenge_text);
        weeeklyChallengeIcon = view.findViewById(R.id.weekly_challenge_icon);

        // weekly challenge
        Habit challenge = getWeeklyChallenge();
        tvWeeklyChallenge.setText(challenge.getHabitName());
        weeeklyChallengeIcon.setImageResource(challenge.getIconResId());


        String myEmail = MainActivity.currentUserEmail;
        if (!TextUtils.isEmpty(myEmail)) {
            myUserKey = myEmail.replace(".", ",");
            userRef       = FirebaseDatabase.getInstance().getReference("users").child(myUserKey);
            userHabitsRef = userRef.child("habits");
            loadUserInfo();
        } else {
            tvUserName.setText("Welcome, Guest!");
        }

        // HOME MODE => isSelectionMode=false
        habitAdapter = new HabitAdapter(habitList, userHabitsRef, /* isSelectionMode= */ false);
        habitRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        habitRecyclerView.setAdapter(habitAdapter);

        if (userHabitsRef != null) {
            loadHabitsFromFirebase();
        }

        btnAddHabit.setOnClickListener(v -> showPredefinedHabitsDialog());
        return view;
    }

    private void loadUserInfo() {
        if (userRef == null) return;
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
//                if (user != null && user.username != null) {
//                    tvUserName.setText("Welcome, " + user.username + "!");
//                } else {
//                    tvUserName.setText("Welcome!");
//                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void loadHabitsFromFirebase() {
        userHabitsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                habitList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Habit h = ds.getValue(Habit.class);
                    // Only show habits with isTracked = true
                    if (h != null && h.isTracked()) {
                        habitList.add(h);
                    }
                }
                habitAdapter.notifyDataSetChanged();
                updateStreakText();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    /**
     * The user can re-check which habits they're tracking by a popup with the single adapter in selection mode.
     */
    private void showPredefinedHabitsDialog() {

        List<Habit> cloneList = new ArrayList<>();
        for (Habit ph : predefinedHabits) {
            boolean alreadyTracked = false;
            Habit existingHabit = null;


            for (Habit current : habitList) {
                if (current.getHabitName().equals(ph.getHabitName())) {
                    alreadyTracked = true;
                    existingHabit = current;
                    break;
                }
            }


            Habit newHabit = new Habit(ph.getHabitName(), ph.getIconResId());
            newHabit.setTracked(alreadyTracked);


            if (alreadyTracked && existingHabit != null) {
                newHabit.setStreakCount(existingHabit.getStreakCount());
                newHabit.setLastCompletedTime(existingHabit.getLastCompletedTime());
                newHabit.setCompletedToday(existingHabit.isCompletedToday());
                newHabit.setNextAvailableTime(existingHabit.getNextAvailableTime());
            }

            cloneList.add(newHabit);
        }


        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_predefined_habits, null);
        RecyclerView rv = dialogView.findViewById(R.id.predefinedHabitsRecycler);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));


        HabitAdapter tempAdapter = new HabitAdapter(cloneList, userHabitsRef, /* isSelectionMode= */ true);
        rv.setAdapter(tempAdapter);

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Habits to Track")
                .setView(dialogView)
                .setPositiveButton("Done", (dialog, which) -> {


                    for (Habit h : cloneList) {
                        DatabaseReference habitRef = userHabitsRef.child(h.getHabitName());

                        if (h.isTracked()) {

                            boolean existsInCurrent = false;
                            Habit existingHabit = null;

                            for (Habit current : habitList) {
                                if (current.getHabitName().equals(h.getHabitName())) {
                                    existsInCurrent = true;
                                    existingHabit = current;
                                    break;
                                }
                            }

                            if (existsInCurrent && existingHabit != null) {

                                habitRef.child("tracked").setValue(true);
                            } else {

                                habitRef.setValue(h);
                            }
                        } else {

                            habitRef.removeValue();
                        }
                    }


                    loadHabitsFromFirebase();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                })
                .create()
                .show();
    }

    @SuppressLint("SetTextI18n")
    private void updateStreakText() {
        if (habitList.isEmpty()) {
            tvStreak.setText("Start a streak today!");
            return;
        }
        int bestStreak = 0;
        String bestHabitName = null;
        for (Habit h : habitList) {
            if (h.getStreakCount() > bestStreak) {
                bestStreak = h.getStreakCount();
                bestHabitName = h.getHabitName();
            }
        }
        if (bestStreak > 0 && bestHabitName != null) {
//            tvStreak.setText("You have " + bestStreak + " days of " + bestHabitName + "!");
            if(bestStreak == 1) {
                tvStreak.setText(bestStreak + " day streak!");
            }
            tvStreak.setText(bestStreak + " day streak!");
        } else {
            tvStreak.setText("Start a streak today!");
        }
    }

    private Habit getWeeklyChallenge() {
        int randomIndex = (int) (Math.random() * weeklyChallengeOptions.size());
        return weeklyChallengeOptions.get(randomIndex);
    }
}
