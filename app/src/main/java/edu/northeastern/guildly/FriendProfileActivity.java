package edu.northeastern.guildly;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.*;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.guildly.data.User;
import edu.northeastern.guildly.R;

public class FriendProfileActivity extends AppCompatActivity {

    private static final String EXTRA_FRIEND_KEY = "FRIEND_KEY";

    private CircleImageView friendProfileImage;
    private TextView textFriendUsername, textFriendAboutMe;

    private String friendKey;
    private DatabaseReference friendRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_profile);

        Toolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // enable back arrow
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Friend Profile");
        }

        friendProfileImage = findViewById(R.id.friendProfileImage);
        textFriendUsername = findViewById(R.id.textFriendUsername);
        textFriendAboutMe  = findViewById(R.id.textFriendAboutMe);

        friendKey = getIntent().getStringExtra(EXTRA_FRIEND_KEY);
        if (TextUtils.isEmpty(friendKey)) {
            Toast.makeText(this, "No friend key provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        friendRef = FirebaseDatabase.getInstance().getReference("users").child(friendKey);

        loadFriendProfile();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // handle back arrow
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadFriendProfile() {
        friendRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User friendUser = snapshot.getValue(User.class);
                if (friendUser == null) {
                    Toast.makeText(FriendProfileActivity.this,
                            "Friend not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                if (!TextUtils.isEmpty(friendUser.username)) {
                    textFriendUsername.setText(friendUser.username);
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(friendUser.username);
                    }
                }

                if (!TextUtils.isEmpty(friendUser.aboutMe)) {
                    textFriendAboutMe.setText(friendUser.aboutMe);
                }

                // set avatar
                int resourceId;
                if ("gamer".equals(friendUser.profilePicUrl)) {
                    resourceId = R.drawable.gamer;
                } else if ("man".equals(friendUser.profilePicUrl)) {
                    resourceId = R.drawable.man;
                } else if ("girl".equals(friendUser.profilePicUrl)) {
                    resourceId = R.drawable.girl;
                } else {
                    resourceId = R.drawable.unknown_profile;
                }
                friendProfileImage.setImageResource(resourceId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FriendProfileActivity.this,
                        "Error loading friend data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void openProfile(Context context, String friendKey) {
        Intent intent = new Intent(context, FriendProfileActivity.class);
        intent.putExtra(EXTRA_FRIEND_KEY, friendKey);
        context.startActivity(intent);
    }
}
