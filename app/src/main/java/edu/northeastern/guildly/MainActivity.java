package edu.northeastern.guildly;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {
    private TextView UserName;
    private Button btnAddHabit;
    private RecyclerView HabitList, Leaderboard;

    // add testing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UserName = findViewById(R.id.user_name);
        btnAddHabit = findViewById(R.id.btn_add_habit);
        HabitList = findViewById(R.id.habit_list);
        Leaderboard = findViewById(R.id.leaderboard);

        // TODO: need to add username set text here

    }
}
