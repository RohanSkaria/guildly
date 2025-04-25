# Guildly - Habit Tracking Social App

![Guildly Logo](app/src/main/res/drawable/app_logo.png)

## Overview

Guildly is a social habit tracking Android application that helps users build and maintain healthy habits while connecting with friends. The app combines personal habit tracking with social features to create a supportive environment for personal growth.

## Features

### Habit Tracking
- **Daily Habits**: Track your daily habits with streaks to build consistency
- **Weekly Challenges**: Participate in weekly challenges with unique habit suggestions
- **Streaks**: Maintain streaks for each habit to visualize your progress

### Social Features
- **Friends**: Connect with friends to share your habit journey
- **Chat**: Direct messaging with your friends
- **Friend Profiles**: View your friends' habits and streaks
- **Leaderboard**: See how your streaks compare with your friends

### User Experience
- **Customizable Profile**: Set a username, bio, and choose from various avatars
- **Notifications**: Get reminders for habits and notifications for social interactions
- **Real-time Updates**: All changes sync in real-time across devices

## App Architecture

### Backend
- **Firebase Realtime Database**: For real-time data synchronization
- **Firebase Authentication**: For user authentication

### Frontend
- **Android Native**: Built using Java for Android
- **Fragment-based Navigation**: Utilizes fragments for efficient UI transitions
- **RecyclerViews**: Used for displaying lists of habits, chats, and friends

## Screens & Components

### Main Screens
1. **Home**: View and complete your tracked habits
2. **Connections**: Manage friends and chat with them
3. **Profile**: View and edit your profile, see your top habits and friends

### Additional Screens
- **Chat Detail**: Communicate with friends
- **Friend Profile**: View details about your friends
- **Settings**: Update your account information
- **Leaderboard**: Compare streaks with friends
- **Sign Up**: Multi-step registration process

## Data Structure

### User
- Username, email, profile picture, about me
- Friends list and friend requests
- Tracked habits with streak information

### Habit
- Name, icon, tracker status
- Streak count and completion status
- Next available time to mark as complete

### Chat
- Participants
- Messages with timestamp and read status

## Getting Started

### Prerequisites
- Android Studio 4.1+
- Android SDK 21+
- Firebase account for backend services

### Setup
1. Clone the repository
   ```
   git clone https://github.com/yourusername/guildly.git
   ```
2. Open the project in Android Studio
3. Create a Firebase project and add the `google-services.json` file to the app directory
4. Enable Firebase Authentication and Realtime Database in your Firebase console
5. Build and run the application

### Firebase Database Structure
```
/users/
  /<user_key>/
    /username: String
    /email: String
    /profilePicUrl: String
    /aboutMe: String
    /friends: List<String>
    /friendRequests: Map<String, String>
    /habits/
      /<habit_name>/
        /habitName: String
        /iconResId: int
        /tracked: boolean
        /streakCount: int
        /lastCompletedTime: long
        /completedToday: boolean
        /nextAvailableTime: long

/chats/
  /<chat_id>/
    /chatId: String
    /participants: List<String>
    /messages/
      /<message_id>/
        /senderId: String
        /content: String
        /timestamp: long
        /status: String

/challenges/
  /currentWeeklyChallenge/
    /habitName: String
    /iconResId: int
    /startTimeMillis: long
    /endTimeMillis: long
```

## Key Components

### Managers
- **GuildlyDataManager**: Manages real-time data synchronization
- **WeeklyChallengeManager**: Handles weekly challenges

### Adapters
- **HabitAdapter**: Shows habits in both selection and tracking modes
- **ChatListAdapter**: Displays chats in the Connections screen
- **ChatDetailAdapter**: Shows messages in a chat conversation
- **SearchUserAdapter**: Displays user search results for adding friends
- **FriendsDialogAdapter**: Shows friends with action buttons

### Fragments
- **HomeFragment**: Main screen with habits and weekly challenge
- **ConnectionsFragment**: Friends and chat management
- **ProfileFragment**: User profile and top habits/friends

### Activities
- **MainActivity**: Container for the main fragments
- **ChatDetailActivity**: For messaging conversations
- **LoginActivity**: User authentication
- **MultiStepSignUpActivity**: Registration process
- **SettingsActivity**: Account management

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributors

- [Rohan Skaria](https://github.com/RohanSkaria)

## Acknowledgments

- [Firebase](https://firebase.google.com) for the backend infrastructure
- [CircleImageView](https://github.com/hdodenhof/CircleImageView) for circular profile images
