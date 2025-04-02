package edu.northeastern.guildly.signUpFragments;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import edu.northeastern.guildly.R;

public class AvatarSelectionFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1001;
    private ImageView imageViewProfilePic;
    private String selectedImageUriString = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_avatar_selection, container, false);

        imageViewProfilePic = view.findViewById(R.id.imageViewProfilePic);
        Button btnSelectPic = view.findViewById(R.id.btnSelectPic);

        // Restore previously selected image
        if (getArguments() != null) {
            String imageUri = getArguments().getString("profileImageUri");
            if (imageUri != null && !imageUri.isEmpty()) {
                selectedImageUriString = imageUri;
                imageViewProfilePic.setImageURI(Uri.parse(imageUri));
            }
        }

        btnSelectPic.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(
                    Intent.createChooser(intent, "Select Profile Picture"),
                    PICK_IMAGE_REQUEST
            );
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            Uri imageUri = data.getData();
            selectedImageUriString = imageUri.toString();
            imageViewProfilePic.setImageURI(imageUri);
        }
    }

    public boolean validateAndSaveData(Bundle data) {
        // Avatar is optional
        if (selectedImageUriString != null) {
            data.putString("profileImageUri", selectedImageUriString);
        }
        return true;
    }
}
