package edu.northeastern.guildly.signUpFragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.Fragment;

import edu.northeastern.guildly.R;

public class ProfileInfoFragment extends Fragment {
    private EditText etUsername, etEmail, etPassword, etConfirmPassword, etAboutMe;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_info, container, false);

        etUsername = view.findViewById(R.id.etUsername);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        etAboutMe = view.findViewById(R.id.etAboutMe);

        // Restore data if available
        Bundle args = getArguments();
        if (args != null) {
            etUsername.setText(args.getString("username", ""));
            etEmail.setText(args.getString("email", ""));
            etPassword.setText(args.getString("password", ""));
            etConfirmPassword.setText(args.getString("password", ""));
            etAboutMe.setText(args.getString("aboutMe", ""));
        }

        return view;
    }

    public boolean validateAndSaveData(Bundle data) {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String aboutMe = etAboutMe.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords don't match");
            return false;
        }

        // Save data
        data.putString("username", username);
        data.putString("email", email);
        data.putString("password", password);
        data.putString("aboutMe", aboutMe);

        // IMPORTANT: Also store the sanitized email key so HabitSelection can write to DB
        String sanitizedEmailKey = email.replace(".", ",");
        data.putString("userId", sanitizedEmailKey);

        return true;
    }
}
