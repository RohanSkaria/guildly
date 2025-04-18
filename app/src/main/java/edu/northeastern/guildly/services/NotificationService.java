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
import edu.northeastern.guildly.ChatDetailActivity;

public class NotificationService {

    private static final String CHANNEL_ID = "guildly_channel";
    private static final String CHANNEL_NAME = "Guildly Notifications";
    private static final String CHANNEL_DESC = "Notifications from Guildly";

    public static final int NOTIFICATION_TYPE_HABIT_REMINDER = 1;
    public static final int NOTIFICATION_TYPE_FRIEND_REQUEST = 2;
    public static final int NOTIFICATION_TYPE_CHALLENGE = 3;
    public static final int NOTIFICATION_TYPE_STREAK = 4;
    public static final int NOTIFICATION_TYPE_MESSAGE = 5;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESC);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

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

    public static void showNewMessageNotification(Context context, String senderUsername, String messageContent) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        String title = senderUsername + " sent you a new message";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_fire)
                .setContentTitle(title)
                .setContentText(messageContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notifyUser(context, NOTIFICATION_TYPE_MESSAGE, builder);
    }


    private static void notifyUser(Context context, int notificationType, NotificationCompat.Builder builder) {
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(notificationType, builder.build());
        } catch (SecurityException e) {
        }
    }
}