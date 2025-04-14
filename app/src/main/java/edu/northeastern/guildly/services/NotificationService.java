package edu.northeastern.guildly.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import edu.northeastern.guildly.MainActivity;
import edu.northeastern.guildly.R;

/**
 * Service for creating and showing notifications to users,
 */
public class NotificationService {

    private static final String CHANNEL_ID = "guildly_channel";
    private static final String CHANNEL_NAME = "Guildly Notifications";
    private static final String CHANNEL_DESC = "Notifications from Guildly";

    // Notification types
    public static final int NOTIFICATION_TYPE_HABIT_REMINDER = 1;
    public static final int NOTIFICATION_TYPE_FRIEND_REQUEST = 2;
    public static final int NOTIFICATION_TYPE_CHALLENGE = 3;
    public static final int NOTIFICATION_TYPE_STREAK = 4;

    /**
     * Initialize notification channels (required for Android 8.0+)
     */
    public static void createNotificationChannel(Context context) {
        // Create the notification channel only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESC);

            // Register the channel with the system
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Show a habit reminder notification
     */
    public static void showHabitReminderNotification(Context context, String habitName) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_fire)
                .setContentTitle("Habit Reminder")
                .setContentText("Don't forget to complete your habit: " + habitName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notifyUser(context, NOTIFICATION_TYPE_HABIT_REMINDER, builder);
    }

    /**
     * Show a friend request notification
     */
    public static void showFriendRequestNotification(Context context, String username) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("openConnections", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.unknown_profile)
                .setContentTitle("New Friend Request")
                .setContentText(username + " sent you a friend request")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notifyUser(context, NOTIFICATION_TYPE_FRIEND_REQUEST, builder);
    }

    /**
     * Show a weekly challenge notification
     */
    public static void showWeeklyChallengeNotification(Context context, String challengeName) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_fire)
                .setContentTitle("New Weekly Challenge")
                .setContentText("This week's challenge: " + challengeName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notifyUser(context, NOTIFICATION_TYPE_CHALLENGE, builder);
    }

    /**
     * Show a streak milestone notification
     */
    public static void showStreakMilestoneNotification(Context context, String habitName, int streakCount) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_fire)
                .setContentTitle("Streak Milestone!")
                .setContentText("You've maintained a " + streakCount + " day streak for " + habitName + "!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notifyUser(context, NOTIFICATION_TYPE_STREAK, builder);
    }

    /**
     * Helper method to show notifications
     */
    private static void notifyUser(Context context, int notificationType, NotificationCompat.Builder builder) {
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(notificationType, builder.build());
        } catch (SecurityException e) {
            // Handle permission issue - this happens when notification permission isn't granted
        }
    }
}