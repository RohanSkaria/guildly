package edu.northeastern.guildly;

import android.os.Bundle;
import android.view.LayoutInflater;

import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private EditText profileUsername;
    private TextView streakDescription;
    private ImageView profileEditButton;
    private TextView habitsViewMore;
    private TextView friendsViewMore;
    private DatabaseReference userRef;
    private String myUserKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_profile);


        String myEmail = MainActivity.currentUserEmail;
        myUserKey = (myEmail != null) ? myEmail.replace(".", ",") : "NO_USER";
        userRef = FirebaseDatabase.getInstance().getReference("users").child(myUserKey);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        initViews();
        setUserData();
        setupClickListeners();
    }

    private void initViews() {
        profileUsername = findViewById(R.id.profile_username);
        profileEditButton = findViewById(R.id.profile_edit_button);
        streakDescription = findViewById(R.id.streak_description);
        habitsViewMore = findViewById(R.id.habits_view_more);
        friendsViewMore = findViewById(R.id.friends_view_more);
    }

    private void setUserData() {

        profileUsername.setText("Yunmu57");


        streakDescription.setText("You have drank 64oz Water for 31 days straight!!!");


        updateHabitImages();
        updateFriendImages();
    }

    private void setupClickListeners() {

        habitsViewMore.setOnClickListener(v -> {
            showHabitsDialog();
        });


        friendsViewMore.setOnClickListener(v -> {
            showFriendsDialog();
        });


        profileEditButton.setOnClickListener(v -> {
            toggleUsernameEditing();
        });


        CircleImageView profileImage = findViewById(R.id.profile_image);
        profileImage.setOnClickListener(v -> {
            showSelectAvatarDialog();
        });
    }

    private void toggleUsernameEditing() {

        if (!profileUsername.isEnabled()) {
            profileUsername.setEnabled(true);
            profileUsername.setFocusableInTouchMode(true);
            profileUsername.requestFocus();
            profileUsername.setSelection(profileUsername.getText().length());


            profileEditButton.setImageResource(android.R.drawable.ic_menu_save);
        } else {

            String newUsername = profileUsername.getText().toString().trim();
            if (!newUsername.isEmpty()) {

                userRef.child("username").setValue(newUsername)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Username updated", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to update username", Toast.LENGTH_SHORT).show();
                        });
            }


            profileUsername.setEnabled(false);


            profileEditButton.setImageResource(R.drawable.ic_edit);
        }
    }

    private void showHabitsDialog() {

        List<Habit> userHabits = Arrays.asList(
                new Habit("Drink 64oz of water", R.drawable.ic_water),
                new Habit("Workout for 30 mins", R.drawable.ic_workout),
                new Habit("Do homework", R.drawable.ic_homework),
                new Habit("Read a book", R.drawable.ic_book),
                new Habit("Meditate for 10 minutes", R.drawable.ic_meditation),
                new Habit("Save money today", R.drawable.ic_savemoney),
                new Habit("Eat vegetables", R.drawable.ic_vegetable),
                new Habit("No phone after 10PM", R.drawable.ic_phonebanned)
        );

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_habit, null);
        ListView listView = dialogView.findViewById(R.id.habit_list_view);

        HabitChoiceAdapter adapter = new HabitChoiceAdapter(this, userHabits);
        listView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("My Habits")
                .setView(dialogView)
                .create();

        dialog.show();
    }

    private void showFriendsDialog() {
        List<Friend> friendsList = Arrays.asList(
                new Friend("RohanS3", 90, R.drawable.gamer),
                new Friend("ParwazS98", 70, R.drawable.man),
                new Friend("PMadisen43", 50, R.drawable.girl)
        );

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_view_friends, null);
        ListView listView = dialogView.findViewById(R.id.friends_list_view);

        FriendChoiceAdapter adapter = new FriendChoiceAdapter(this, friendsList);
        listView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("My Friends")
                .setView(dialogView)
                .create();

        dialog.show();
    }

    private void showSelectAvatarDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_avatar, null);

        CircleImageView avatarGamer = dialogView.findViewById(R.id.avatar_gamer);
        CircleImageView avatarMan = dialogView.findViewById(R.id.avatar_man);
        CircleImageView avatarGirl = dialogView.findViewById(R.id.avatar_girl);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select Avatar")
                .setView(dialogView)
                .create();

        avatarGamer.setOnClickListener(v -> {
            updateProfileAvatar("gamer");
            dialog.dismiss();
        });

        avatarMan.setOnClickListener(v -> {
            updateProfileAvatar("man");
            dialog.dismiss();
        });

        avatarGirl.setOnClickListener(v -> {
            updateProfileAvatar("girl");
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateProfileAvatar(String avatarName) {
        CircleImageView profileImage = findViewById(R.id.profile_image);


        int resourceId;
        switch (avatarName) {
            case "gamer":
                resourceId = R.drawable.gamer;
                break;
            case "man":
                resourceId = R.drawable.man;
                break;
            case "girl":
                resourceId = R.drawable.girl;
                break;
            default:
                resourceId = R.drawable.unknown_profile;
                break;
        }

        profileImage.setImageResource(resourceId);


        userRef.child("profilePicUrl").setValue(avatarName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Avatar updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update avatar", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateHabitImages() {

        ImageView habitWorkout = findViewById(R.id.habit_workout);
        ImageView habitMeditation = findViewById(R.id.habit_meditation);
        ImageView habitSwimming = findViewById(R.id.habit_swimming);

        habitWorkout.setImageResource(R.drawable.ic_workout);
        habitMeditation.setImageResource(R.drawable.ic_meditation);
        habitSwimming.setImageResource(R.drawable.ic_water);
    }

    private void updateFriendImages() {
        CircleImageView friendOne = findViewById(R.id.friend_one);
        CircleImageView friendTwo = findViewById(R.id.friend_two);
        CircleImageView friendThree = findViewById(R.id.friend_three);

        friendOne.setImageResource(R.drawable.gamer);
        friendTwo.setImageResource(R.drawable.man);
        friendThree.setImageResource(R.drawable.girl);
    }
}