package edu.northeastern.guildly;

import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.guildly.data.Friend;

public class FriendChoiceAdapter extends ArrayAdapter<Friend> {
    public FriendChoiceAdapter(@NonNull Context context, @NonNull List<Friend> friends) {
        super(context, 0, friends);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_friend_choice, parent, false);

        Friend friend = getItem(position);

        TextView friendName = convertView.findViewById(R.id.friend_name);
        CircleImageView friendImage = convertView.findViewById(R.id.friend_image);

        friendName.setText(friend.getUsername());
        friendImage.setImageResource(friend.getProfileImageResource());


        int[] colors = {
                android.graphics.Color.parseColor("#4CAF50"),
                android.graphics.Color.parseColor("#2196F3"),
                android.graphics.Color.parseColor("#FFC107")
        };

        friendImage.setCircleBackgroundColor(colors[position % colors.length]);

        return convertView;
    }
}