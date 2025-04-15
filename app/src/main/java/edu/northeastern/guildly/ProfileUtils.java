package edu.northeastern.guildly;

public class ProfileUtils {
    public static int getProfileImageRes(String profilePicUrl) {
        if (profilePicUrl == null) {
            return R.drawable.unknown_profile;
        }
        switch (profilePicUrl) {
            case "gamer": return R.drawable.gamer;
            case "man":   return R.drawable.man;
            case "girl":  return R.drawable.girl;
            default:      return R.drawable.unknown_profile;
        }
    }
}
