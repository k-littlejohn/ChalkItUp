package com.example.chalkitup.ui.viewmodel.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chalkitup.ui.components.TutorSubject
import com.example.chalkitup.ui.viewmodel.Email
import com.example.chalkitup.ui.viewmodel.EmailMessage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalTime

class AdminHomeViewModel : ViewModel() {

    private val _unapprovedTutors = MutableStateFlow<List<User>>(emptyList())
    val unapprovedTutors: StateFlow<List<User>> get() = _unapprovedTutors

    private val _approvedTutors = MutableStateFlow<List<User>>(emptyList())
    val approvedTutors: StateFlow<List<User>> get() = _approvedTutors

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports

    private val _usersWithReports = MutableStateFlow<List<User>>(emptyList())
    val usersWithReports: StateFlow<List<User>> = _usersWithReports

    private val db = FirebaseFirestore.getInstance()

    init {
        fetchUnapprovedTutors()
        fetchApprovedTutors()
        fetchReports()
        fetchReportsAndUsers()
    }

    fun resolveReport(report: Report){
        viewModelScope.launch {
            try {
                val reportRef = db.collection("reports").document(report.id)
                reportRef.delete().await()

                refreshReports()

            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error deleting report: ${e.message}")
            }

        }
    }

    private fun refreshReports() {
        viewModelScope.launch {
            // Re-fetch or update reports
            fetchReportsAndUsers()
            fetchReports()
        }
    }

    fun fetchReportsAndUsers() {
        viewModelScope.launch {
            try {
                // Step 1: Fetch all reports
                val snapshot = db.collection("reports").get().await()
                val reportUserIds = snapshot.documents.mapNotNull { doc ->
                    doc.getString("userId")
                }.distinct()

                // Step 2: Fetch users by userId
                val users = fetchUsersByIds(reportUserIds)
                _usersWithReports.value = users

                fetchProfilePicturesReported(users)

            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error fetching reports or users: ${e.message}")
            }
        }
    }

    private suspend fun fetchUsersByIds(userIds: List<String>): List<User> {
        val users = mutableListOf<User>()
        for (userId in userIds) {
            val userSnapshot = db.collection("users").document(userId).get().await()
            val user = userSnapshot.toObject(User::class.java)
            user?.apply { id = userId }
            user?.let { users.add(it) }
        }
        return users
    }


    private fun fetchReports() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("reports").get().await()
                val reportsList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Report::class.java)?.apply { id = doc.id }
                }
                _reports.value = reportsList
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error fetching reports: ${e.message}")
            }
        }
    }

    fun fetchUnapprovedTutors() {
        viewModelScope.launch {
            try {
                val snapshot: QuerySnapshot = db.collection("users")
                    .whereEqualTo("userType", "Tutor")
                    .whereEqualTo("adminApproved", false)
                    .whereEqualTo("active", true)
                    .get()
                    .await()

                val updatedTutors = snapshot.documents.mapNotNull { doc ->
                    val tutor = doc.toObject(User::class.java)
                    tutor?.apply { id = doc.id }
                }.sortedBy { tutor -> tutor.firstName } // Sort alphabetically by name

                _unapprovedTutors.value = updatedTutors

            } catch (e: Exception) {
                println("Error fetching unapproved tutors: ${e.message}")
            }
        }
    }

    fun fetchApprovedTutors() {
        viewModelScope.launch {
            try {
                val snapshot: QuerySnapshot = db.collection("users")
                    .whereEqualTo("userType", "Tutor")
                    .whereEqualTo("adminApproved", true)
                    .whereEqualTo("active", true)
                    .get()
                    .await()

                val activeTutors = snapshot.documents.mapNotNull { doc ->
                    val tutor = doc.toObject(User::class.java)
                    tutor?.apply { id = doc.id }
                }.sortedBy { tutor -> tutor.firstName } // Sort alphabetically by name

                // Update the list with sorted, active tutors
                _approvedTutors.value = activeTutors

                fetchProfilePictures(activeTutors)

            } catch (e: Exception) {
                println("Error fetching approved tutors: ${e.message}")
            }
        }
    }

    fun approveTutor(tutorId: String) {
        viewModelScope.launch {
            try {

                // Update Firestore
                db.collection("users").document(tutorId)
                    .update("adminApproved", true)
                    .await()
                db.collection("users").document(tutorId)
                    .update("active", true)
                    .await()


                // Remove from unapproved list
                _unapprovedTutors.value = _unapprovedTutors.value.filterNot { it.id == tutorId }

                // Fetch the updated tutor from Firestore
                val tutorDoc = db.collection("users").document(tutorId).get().await()
                val approvedTutor = tutorDoc.toObject(User::class.java)?.apply { id = tutorDoc.id }

                // Add to the approved list if not null
                approvedTutor?.let {
                    _approvedTutors.value = (_approvedTutors.value + it).distinctBy { it.id }
                }

            } catch (e: Exception) {
                println("Error approving tutor: ${e.message}")
            }
        }
    }

    // LiveData to hold and observe the user's profile picture URL
    private val _profilePictureUrls = MutableStateFlow<Map<String, String?>>(emptyMap())
    val profilePictureUrls: StateFlow<Map<String, String?>> get() = _profilePictureUrls

    val storage = Firebase.storage

    // Function to load the profile picture from storage
    private fun fetchProfilePictures(tutors: List<User>) {
        viewModelScope.launch {
            val profileUrls = mutableMapOf<String, String?>()

            tutors.forEach { tutor ->
                val storageRef = storage.reference.child("${tutor.id}/profilePicture.jpg")
                try {
                    val uri = storageRef.downloadUrl.await()
                    profileUrls[tutor.id] = uri.toString()
                } catch (e: Exception) {
                    profileUrls[tutor.id] = null
                }
            }

            _profilePictureUrls.value = profileUrls
        }
    }

    //TODO Have function to fetch pfp and variable to store them for reported users in fetchreportusers func
    // LiveData to hold and observe the user's profile picture URL
    private val _profilePictureUrlsReported = MutableStateFlow<Map<String, String?>>(emptyMap())
    val profilePictureUrlsReported: StateFlow<Map<String, String?>> get() = _profilePictureUrlsReported
    // Function to load the profile picture from storage
    private fun fetchProfilePicturesReported(users: List<User>) {
        viewModelScope.launch {
            val profileUrls = mutableMapOf<String, String?>()

            users.forEach { user ->
                val storageRef = storage.reference.child("${user.id}/profilePicture.jpg")
                try {
                    val uri = storageRef.downloadUrl.await()
                    profileUrls[user.id] = uri.toString()
                } catch (e: Exception) {
                    profileUrls[user.id] = null
                }
            }

            _profilePictureUrlsReported.value = profileUrls
        }
    }


    fun signout() {
        FirebaseAuth.getInstance().signOut()
    }

    private fun addNotification(
        notifUserID: String,
        notifUserName: String, // Name of the person in the notification
        notifTime: String,
        notifDate: String,
        comments: String,
        notifType: String
    ) {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()

            val notifData = hashMapOf(
                "notifID" to "",
                "notifType" to notifType,
                "notifUserID" to notifUserID,
                "notifUserName" to notifUserName,
                "notifTime" to notifTime,
                "notifDate" to notifDate,
                "comments" to comments,
                "sessType" to "",
                "sessDate" to "",
                "sessTime" to "",
                "otherID" to "",
                "otherName" to "",
                "subject" to "",
                "grade" to "",
                "spec" to "",
                "mode" to "",
                "price" to ""
            )

            db.collection("notifications")
                .add(notifData)
                .await()
        }
    }

    fun denyTutor(tutor: User, reason: String, type: String) {
        Log.d("TutorDeactivation", "Attempting to deactivate tutor ${tutor.id}")
        val tutorRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(tutor.id)

        _unapprovedTutors.value = _unapprovedTutors.value.filterNot { it.id == tutor.id }
        _approvedTutors.value = _approvedTutors.value.filterNot { it.id == tutor.id }
        _usersWithReports.value = _usersWithReports.value.filterNot { it.id == tutor.id }

        deleteUserReports(tutor.id)

        tutorRef.update("adminApproved", true)
        tutorRef.update("active", false)
            .addOnSuccessListener {
                Log.d("TutorDeactivation", "Tutor ${tutor.id} successfully deactivated")
                sendDeactivationEmail(tutor, reason, type)
            }
            .addOnFailureListener { e ->
                Log.e("TutorDeactivation", "Error deactivating tutor ${tutor.id}", e)
            }
    }

    private fun deleteUserReports(userId: String) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()

                val reportsSnapshot = db.collection("reports")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                for (report in reportsSnapshot.documents) {
                    db.collection("reports").document(report.id)
                        .delete()
                        .await()
                }

                Log.d("AdminViewModel", "Tutor denied and reports deleted successfully")

            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error denying tutor and deleting reports: ${e.message}")
            }
        }
    }

    private fun sendDeactivationEmail(tutor: User, reason: String, type: String) {
        val dType: String = if (type == "deny") {
            "denied"
        } else {
            "deactivated"
        }
        val emailSubj = "Your account for ChalkItUp has been $dType"

        val emailHTML =
            "<p> Hi ${tutor.firstName},<br><br> Your account for <b>ChalkItUp</b> has been $dType by an Admin.<br><br></p>" +
                    "<p> <b> Admin Reason: </b> <p>" +
                    "<p> $reason </p>" +
                    "<p> You will have access to ChalkItUp with this account but will " +
                    "<b>NOT</b> be matched to any sessions. </p>" +
                    "<p> Delete your account in app settings and sign up again to be reviewed again by an Admin. </p>" +
                    "<p> -ChalkItUp Tutors </p>"

        val email = Email(
            to = tutor.email,
            message = EmailMessage(emailSubj, emailHTML)
        )

        db.collection("mail").add(email)
            .addOnSuccessListener {
                println("Deactivation email sent successfully")
            }
            .addOnFailureListener { e ->
                println("Deactivation email failed: ${e.message}")
            }

        addNotification(
            notifUserID = tutor.id,
            notifUserName = tutor.firstName,
            notifTime = LocalTime.now().toString(),
            notifDate = LocalDate.now().toString(),
            comments = "Your account has been $dType by an Admin. You have access to ChalkItUp with this account but will not be matched to any sessions." +
                    " Delete your account in app settings and sign up again to be reviewed again by an Admin. Admin Reason: $reason",
            notifType = "Deactivated"
        )
    }

    fun sendApprovalEmail(tutor: User, reason: String) {
        val emailSubj = "Your Tutor account for ChalkItUp has been approved!"

        val emailHTML =
            "<p> Hi ${tutor.firstName},<br><br> Your Tutor account for <b>ChalkItUp</b> has been approved by an Admin!<br><br></p>" +
                    "<p> <b> Admin Comments: </b> <p>" +
                    "<p> Welcome to ChalkItUp! Login to ChalkItUp to get started and have sessions! </p>" +
                    "<p> $reason </p>" +
                    "<p> -ChalkItUp Tutors </p>"

        val email = Email(
            to = tutor.email,
            message = EmailMessage(emailSubj, emailHTML)
        )

        db.collection("mail").add(email)
            .addOnSuccessListener {
                println("Approval email sent successfully")
            }
            .addOnFailureListener { e ->
                println("Approval email failed: ${e.message}")
            }

        addNotification(
            notifUserID = tutor.id,
            notifUserName = tutor.firstName,
            notifTime = LocalTime.now().toString(),
            notifDate = LocalDate.now().toString(),
            comments = "Your Tutor account has been approved by an Admin! Admin Comments: " +
                    "Enter availability to start getting matched with Students! $reason",
            notifType = "Approved"
        )
    }


}

data class Report(
    var id: String = "",
    val userId: String = "",
    val reportMessage: String = "",
    val timestamp: Timestamp? = null
)


@IgnoreExtraProperties
data class User(
    var id: String = "",
    val userType: String = "",  // Type of user ("Tutor" or "Student")
    val firstName: String = "", // First name of the user
    val lastName: String = "",  // Last name of the user
    val email: String = "",     // Email address of the user
    val subjects: List<TutorSubject> = emptyList(), // List of subjects the user is associated with (for tutors)
    val adminApproved: Boolean
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", "", "", "", emptyList(), false)
}