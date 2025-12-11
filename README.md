# StudyXStudy - Task Management App

StudyXStudy is a simple and effective Android application designed to help users, especially students and learners, manage their tasks and stay organized. Built with Kotlin and leveraging the power of Firebase, it provides a seamless and real-time task tracking experience.

## Features

*   **User Authentication:** Secure sign-in and registration powered by Firebase Authentication. Each user's data is private and linked to their account.
*   **Create, Read, Update, Delete (CRUD) Tasks:**
    *   **Add:** Quickly add new tasks with a title and an optional description.
    *   **View:** See all your tasks in a clean, scrollable list.
    *   **Edit:** Modify the title and description of existing tasks.
    *   **Delete:** Remove tasks you no longer need with a confirmation dialog.
    *   **Toggle Completion:** Mark tasks as complete or incomplete with a simple checkbox.
*   **Real-time Database:** Utilizes Firebase Firestore to instantly sync tasks across sessions. Changes made are reflected immediately.
*   **Personalized Experience:** The app robustly filters tasks to ensure you only see the ones you've created, using Firestore's security rules.
*   **Task Statistics:** An at-a-glance dashboard shows you the total number of tasks, as well as how many are completed and pending.
*   **User-Friendly Interface:**
    *   A clean, intuitive layout built with modern Android components.
    *   An "empty state" message encourages users to add their first task.
    *   Dialogs for adding, editing, and confirming deletion make for a smooth workflow.

## Contribution to Sustainable Development Goals (SDG)

This project supports the **United Nations Sustainable Development Goal 4: Quality Education**.

*   **SDG Target 4.7:** By providing a tool that helps learners organize their studies, manage deadlines, and track academic progress, StudyX promotes the development of effective study habits and self-management skills. These are essential for lifelong learning and ensuring that all learners acquire the knowledge and skills needed to promote sustainable development. An organized student is a more effective learner, better equipped to succeed in their educational journey.

## Tech Stack & Dependencies

*   **Language:** [Kotlin](https://kotlinlang.org/)
*   **Backend & Database:** [Firebase](https://firebase.google.com/)
    *   **Firebase Authentication:** For handling user accounts.
    *   **Firebase Firestore:** As a real-time, NoSQL database for storing tasks.
*   **UI Components:**
    *   **AndroidX Libraries:** `appcompat`, `fragment`, `constraintlayout`.
    *   **Material Design:** `com.google.android.material:material` for modern UI elements like `FloatingActionButton` and `AlertDialog`.
    *   **RecyclerView:** For efficiently displaying the list of tasks.
*   **Architecture:**
    *   The application logic is structured within a `Fragment` (`TasksFragment.kt`).
    *   A `RecyclerView.Adapter` (`SimpleTaskAdapter`) manages the task data and its presentation in the UI.
    *   A simple data class (`Task.kt`) models the task object.

## How It Works

The `TasksFragment` is the core of the task management feature.

1.  **Initialization:** When the view is created, it initializes Firebase Authentication and Firestore.
2.  **Loading Tasks:** It attaches a `SnapshotListener` to the `tasks` collection in Firestore. This listener filters documents by the current user's `userId`, ensuring data privacy.
3.  **Real-time Updates:** The listener automatically receives updates whenever a task is added, modified, or deleted in the database. It then refreshes the local `RecyclerView`, statistics, and empty state visibility.
4.  **User Actions:**
    *   The `FloatingActionButton` triggers a dialog to add a new task.
    *   Clicking on a task opens an edit dialog.
    *   Toggling a task's checkbox directly updates its `isCompleted` status in Firestore.
    *   A long-press or delete icon shows a confirmation dialog before removing the task from Firestore.
5.  **Data Persistence:** All tasks are saved with a `userId` field, which is critical for fetching the correct data for each logged-in user.

## Future Improvements

*   **Due Dates & Priority:** Implement the UI to allow users to set due dates and priority levels for tasks.
*   **Sorting & Filtering:** Add options to sort tasks by due date, creation date, or priority.
*   **Notifications:** Send reminders for tasks that are approaching their due date.
*   **Improved UI/UX:** Enhance the user interface with more advanced components, animations, and a dedicated task detail screen.
*   **Profile Management:** Create a screen where users can manage their account details.

## Development Team

| Name                                  | Sr - Code  | Email                          |
| ------------------------------------- | ---------- | ------------------------------ |
| **Balmes, Genrique Sean Arkin D.**    | 23-06630   | 23-06630@g.batstate-u.edu.ph   |
| **Carranza, John Timothy S.**         | 23-05494   | 23-05494@g.batstate-u.edu.ph   | 
| **Ramirez, Kent Ian V.**              | 23-00686   | 23-00686@g.batstate-u.edu.ph   |

## Acknowledgements

* **Mr. Joshua Fronda** (*Project Advisor*)
