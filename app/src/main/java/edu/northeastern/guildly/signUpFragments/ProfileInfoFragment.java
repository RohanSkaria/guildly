package edu.northeastern.guildly.signUpFragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
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

        // Validate username
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            return false;
        }

        // Validate email with proper pattern
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return false;
        }

        // Check email format using Android's Patterns.EMAIL_ADDRESS
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            return false;
        }

        // Additional email validation for common requirements
        if (!isValidEmail(email)) {
            etEmail.setError("Invalid email format");
            return false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return false;
        }

        // Add password strength requirements
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return false;
        }

        // Confirm passwords match
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

    /**
     * Additional detailed email validation beyond Android's basic pattern
     */
    private boolean isValidEmail(String email) {
        if (email == null) return false;

        // Basic structure check
        if (!email.contains("@")) return false;

        // Split into local and domain parts
        String[] parts = email.split("@");
        if (parts.length != 2) return false;

        String local = parts[0];
        String domain = parts[1];

        // Check local part
        if (local.isEmpty() || local.length() > 64) return false;

        // Check domain part
        if (domain.isEmpty() || domain.length() > 255) return false;
        if (!domain.contains(".")) return false;

        // Domain should have at least one dot and valid characters
        String[] domainParts = domain.split("\\.");
        if (domainParts.length < 2) return false;

        // Check last domain part (TLD)
        String tld = domainParts[domainParts.length - 1];
        if (tld.length() < 2) return false;

        return true;
    }
}