package com.example.chalkitup.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeViewModel : ViewModel() {

    private val _userName = MutableStateFlow<String?>("Unknown")
    val userName: StateFlow<String?> get() = _userName

    private val _userType = MutableStateFlow<String?>("Unknown")
    val userType: StateFlow<String?> get() = _userType

    private val _bookedDates = MutableStateFlow<List<String>>(emptyList())
    val bookedDates: StateFlow<List<String>> get() = _bookedDates

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> get() = _appointments

//    private val _showTutorialDialog = MutableStateFlow(false)
//    val showTutorialDialog: StateFlow<Boolean> = _showTutorialDialog

    fun checkFirstTimeLogin(onSuccess: () -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val creationTime = it.metadata?.creationTimestamp
            val lastSignInTime = it.metadata?.lastSignInTimestamp

            if (creationTime != null && lastSignInTime != null && creationTime == lastSignInTime) {
                // First-time login
                println("First-time login detected")
                onSuccess()
            }
        }
    }

    // LiveData to hold and observe the user's profile picture URL
    private val _profilePictureUrl = MutableLiveData<String?>()
    val profilePictureUrl: LiveData<String?> get() = _profilePictureUrl

    // Function to load the profile picture from storage
    fun loadProfilePicture(userId: String) {
        val storageRef = Firebase.storage.reference.child("$userId/profilePicture.jpg")
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            _profilePictureUrl.value = uri.toString()
        }.addOnFailureListener {
            _profilePictureUrl.value = null // Set to null if no profile picture exists
        }
    }

    init {
        getUserName()
        fetchBookedDates()
        fetchAppointments()
    }

    private fun getUserName() {
        viewModelScope.launch {
            val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
            currentUserID?.let {
                val db = FirebaseFirestore.getInstance()
                val userRef = db.collection("users").document(it)
                val snapshot = userRef.get().await()
                _userName.value = snapshot.getString("firstName") ?: "User"
                _userType.value = snapshot.getString("userType") ?: "Unknown"
            }
        }
    }

    private fun fetchBookedDates() {
        viewModelScope.launch {
            val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserID != null) {
                val db = FirebaseFirestore.getInstance()
                val appointmentsRef = db.collection("appointments")

                try {
                    val userAppointments = appointmentsRef
                        .whereEqualTo("studentID", currentUserID)
                        .get()
                        .await()

                    val tutorAppointments = appointmentsRef
                        .whereEqualTo("tutorID", currentUserID)
                        .get()
                        .await()

                    _bookedDates.value = (userAppointments.documents + tutorAppointments.documents)
                        .mapNotNull { it.getString("date")?.replace("\"", "") }

                } catch (e: Exception) {
                    println("Error fetching appointments: ${e.message}")
                }
            }
        }
    }

    fun fetchAppointments() {
        viewModelScope.launch {
            val currentUserID = FirebaseAuth.getInstance().currentUser?.uid

            if (currentUserID != null) {
                val db = FirebaseFirestore.getInstance()
                val appointmentsRef = db.collection("appointments")
                val usersRef = db.collection("users")

                try {
                    val userAppointments = appointmentsRef
                        .whereEqualTo("studentID", currentUserID)
                        .get()
                        .await()
                        .documents

                    val tutorAppointments = appointmentsRef
                        .whereEqualTo("tutorID", currentUserID)
                        .get()
                        .await()
                        .documents

                    val allAppointments = (userAppointments + tutorAppointments).mapNotNull { doc ->
                        val appointment = doc.toObject(Appointment::class.java)?.copy(appointmentID = doc.id)

                        appointment?.let {
                            val tutorSnapshot = usersRef.document(it.tutorID).get().await()
                            val tutorFirstName = tutorSnapshot.getString("firstName") ?: "Unknown"
                            val tutorLastName = tutorSnapshot.getString("lastName") ?: ""

                            val studentSnapshot = usersRef.document(it.studentID).get().await()
                            val studentFirstName = studentSnapshot.getString("firstName") ?: "Unknown"
                            val studentLastName = studentSnapshot.getString("lastName") ?: ""

                            it.copy(
                                tutorName = "$tutorFirstName $tutorLastName",
                                studentName = "$studentFirstName $studentLastName"
                            )
                        }
                    }

                    // Filter Out Past Appointments and Sort By Date (Earliest First)
                    val today = LocalDate.now()
                    _appointments.value = allAppointments
                        .filter { appointment ->
                            val appointmentDate = try {
                                LocalDate.parse(appointment.date, DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
                            } catch (e: Exception) {
                                null
                            }
                            appointmentDate != null && appointmentDate.isAfter(today.minusDays(1)) // Excludes past appointments
                        }
                        .sortedBy { appointment ->
                            LocalDate.parse(appointment.date, DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
                        }

                } catch (e: Exception) {
                    println("Error fetching appointments: ${e.message}")
                }
            }
        }
    }

    // Helper function to get the week number of the month
    private fun getWeekNumber(date: LocalDate): Int {
        val firstDayOfMonth = date.withDayOfMonth(1)
        val dayOfWeek = firstDayOfMonth.dayOfWeek.value
        val weekNumber = (date.dayOfMonth + dayOfWeek - 1) / 7 + 1
        return weekNumber
    }

    // Email Class info
    data class Email (
        var to: String = "",
        var message: EmailMessage
    )

    data class EmailMessage(
        var subject: String = "",
        var html: String = "",
        var body: String = "",
    )

    // Sends an email about a cancelled session
    private fun sendEmail(
        appointment: Appointment,
        userID: String) {

        val db = FirebaseFirestore.getInstance()
        var userEmail = ""

        viewModelScope.launch {
            try {
                val tempRef = db.collection("users").document(userID).get().await()
                userEmail = tempRef.getString("email") ?: "Unknown"

            } catch (e: Exception) {
                println("Error fetching user email: ${e.message}")
            }
        }

        val formattedSubject =
            "${appointment.subjectObject["subject"]} ${appointment.subjectObject["grade"]} ${appointment.subjectObject["specialization"]}"

        val emailSubj = "Your appointment for $formattedSubject has been cancelled"

        val emailHTML =
            "<p> Hi ${if (userID == appointment.studentID) appointment.studentName
            else appointment.tutorName},<br><br> Your appointment for <b>$formattedSubject</b>" +
                    " with ${if (userID == appointment.studentID) appointment.tutorName
                    else appointment.studentName} at ${appointment.date}: ${appointment.time} has been cancelled. </p>" +
                    "<p> The appointment has been removed from your calendar. </p>" +
                    "<p> Have a good day! </p>" +
                    "<p> -ChalkItUp Tutors </p>"

        val email = Email(
            to = userEmail,
            message = EmailMessage(emailSubj, html = emailHTML)
        )

        db.collection("mail").add(email)
            .addOnSuccessListener {
                println("Appointment cancelled successfully!")
            }
            .addOnFailureListener { e ->
                println("Error cancelling appointment: ${e.message}")
            }
    }

    fun cancelAppointment(appointment: Appointment, onComplete: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val appointmentRef = db.collection("appointments").document(appointment.appointmentID)

        val tutorId = appointment.tutorID
        val appointmentId = appointment.appointmentID

        appointmentRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val date = document.getString("date") ?: return@addOnSuccessListener
                    val timeRange = document.getString("time") ?: return@addOnSuccessListener

                    // Parse the time range into time slots (excluding the last one)
                    val timeSlots = parseTimeRangeExcludingLast(timeRange)

                    // Add the time slots back to the tutor's availability
                    markTimesAsAvailable(tutorId, date, timeSlots)

                    // Reduce Session Count by 1
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val localDate = LocalDate.parse(appointment.date, formatter)
                    val monthYear = date.substring(0, 7) // Extract "yyyy-MM" from the date
                    val weekNumber = getWeekNumber(localDate)

                    db.collection("availability")
                        .document(monthYear)
                        .collection(tutorId)
                        .document("sessionCount")
                        .update("week$weekNumber", FieldValue.increment(-1))

                    // Delete the appointment from Firestore
                    db.collection("appointments")
                        .document(appointmentId)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("Appointment", "Appointment canceled and time slots added back to availability")
                            fetchAppointments()
                            fetchBookedDates()
                            onComplete()
                        }
                        .addOnFailureListener { e ->
                            Log.e("Appointment", "Error deleting appointment", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Appointment", "Error fetching appointment details", e)
            }

        // Add the cancellation session notifications to firebase
        addNotification(
            notifUserID = appointment.tutorID,
            notifUserName = appointment.tutorName,
            notifTime = LocalTime.now().toString(),
            notifDate = LocalDate.now().toString(),
            comments = appointment.comments,
            sessDate = appointment.date,
            sessTime = appointment.time,
            otherID = appointment.studentID,
            otherName = appointment.studentName,
            subject = appointment.subjectObject["subject"].toString(),
            grade = appointment.subjectObject["grade"].toString(),
            spec = appointment.subjectObject["specialization"].toString(),
            mode = appointment.mode,
            price = appointment.subjectObject["price"].toString()
        )

        addNotification(
            notifUserID = appointment.studentID,
            notifUserName = appointment.studentName,
            notifTime = LocalTime.now().toString(),
            notifDate = LocalDate.now().toString(),
            comments = appointment.comments,
            sessDate = appointment.date,
            sessTime = appointment.time,
            otherID = appointment.tutorID,
            otherName = appointment.tutorName,
            subject = appointment.subjectObject["subject"].toString(),
            grade = appointment.subjectObject["grade"].toString(),
            spec = appointment.subjectObject["specialization"].toString(),
            mode = appointment.mode,
            price = appointment.subjectObject["price"].toString()
        )

        // Send two emails about the cancelled sessions
        sendEmail(
            appointment = appointment,
            userID = appointment.tutorID
        )

        sendEmail(
            appointment = appointment,
            userID = appointment.studentID
        )
    }

    // Firebase order: notifications/actual notification info
    private fun addNotification(
        notifUserID: String,
        notifUserName: String, // Name of the person in the notification
        notifTime: String,
        notifDate: String,
        comments: String,
        sessDate: String,
        sessTime: String,
        otherID: String, // ID of the other person in the notification
        otherName: String, // ID of the other person in the notification
        subject: String,
        grade: String,
        spec: String,
        mode: String,
        price: String
    ) {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()

            val notifData = hashMapOf(
                "notifID" to "",
                "notifType" to "Session",
                "notifUserID" to notifUserID,
                "notifUserName" to notifUserName,
                "notifTime" to notifTime,
                "notifDate" to notifDate,
                "comments" to comments,
                "sessType" to "Cancelled",
                "sessDate" to sessDate,
                "sessTime" to sessTime,
                "otherID" to otherID,
                "otherName" to otherName,
                "subject" to subject,
                "grade" to grade,
                "spec" to spec,
                "mode" to mode,
                "price" to price
            )

            db.collection("notifications")
                .add(notifData)
                .await()
        }
    }

    private fun markTimesAsAvailable(tutorId: String, date: String, timeSlots: List<String>) {
        val db = FirebaseFirestore.getInstance()
        val monthYear = date.substring(0, 7) // Extract "yyyy-MM" from the date

        val availabilityRef = db.collection("availability")
            .document(monthYear)
            .collection(tutorId)
            .document("availabilityData")

        // Fetch the current availability for the tutor
        availabilityRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val availabilityWrapper = document.toObject(TutorAvailabilityWrapper::class.java)
                    val availabilityList = availabilityWrapper?.availability ?: emptyList()

                    // Find the entry for the specified date
                    val dayIndex = availabilityList.indexOfFirst { it.day == date }
                    if (dayIndex == -1) return@addOnSuccessListener // No entry for this date, exit

                    val updatedList = availabilityList.toMutableList()
                    val dayEntry = updatedList[dayIndex]

                    // Update only the matching time slots to set booked = false
                    val updatedTimeSlots = dayEntry.timeSlots.map { timeSlot ->
                        if (timeSlot.time in timeSlots) {
                            timeSlot.copy(booked = false)
                        } else {
                            timeSlot
                        }
                    }

                    // Replace the modified day entry
                    updatedList[dayIndex] = dayEntry.copy(timeSlots = updatedTimeSlots)

                    // Save the updated availability back to Firestore
                    availabilityRef.set(TutorAvailabilityWrapper(updatedList))
                        .addOnSuccessListener {
                            Log.d("Availability", "Time slots marked as available")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Availability", "Error updating availability", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Availability", "Error fetching availability", e)
            }
    }

    /**
     * Parses a time range string (e.g., "2:00 PM - 3:00 PM") into a list of time slots,
     * excluding the last time slot.
     */
    private fun parseTimeRangeExcludingLast(timeRange: String): List<String> {
        val times = mutableListOf<String>()

        try {
            val (startTime, endTime) = timeRange.split(" - ")
            val start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
            val end = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))

            var currentTime = start
            while (currentTime.isBefore(end)) {
                times.add(currentTime.format(DateTimeFormatter.ofPattern("h:mm a")))
                currentTime = currentTime.plusMinutes(30)
            }
        } catch (e: Exception) {
            Log.e("TimeParsing", "Error parsing time range: $timeRange", e)
        }
        return times
    }

}

// Appointment Data Class
data class Appointment(
    val appointmentID: String = "",
    val studentID: String = "",
    val tutorID: String = "",
    val tutorName: String = "",
    val studentName: String = "",
    val date: String = "",
    val time: String = "",
    val subject: String = "",
    val mode: String = "",
    val comments: String = "",
    val subjectObject: Map<String, Any> = emptyMap()
)