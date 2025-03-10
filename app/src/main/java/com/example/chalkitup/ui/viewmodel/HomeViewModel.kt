package com.example.chalkitup.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chalkitup.ui.screens.TutorAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
                    addTimeSlotsToAvailability(tutorId, date, timeSlots)

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
    }

    /**
     * Adds the given time slots to the tutor's availability for the specified date.
     */
    private fun addTimeSlotsToAvailability(tutorId: String, date: String, timeSlots: List<String>) {
        val db = FirebaseFirestore.getInstance()
        val monthYear = date.substring(0, 7) // Extract "yyyy-MM" from the date

        // Fetch the current availability for the tutor
        db.collection("availability")
            .document(monthYear)
            .collection(tutorId)
            .document("availabilityData")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val availabilityWrapper = document.toObject(TutorAvailabilityWrapper::class.java)
                    val availabilityList = availabilityWrapper?.availability ?: emptyList()

                    // Find the availability entry for the specified date
                    val existingEntry = availabilityList.find { it.day == date }

                    // Update the availability list
                    val updatedList = availabilityList.toMutableList()
                    if (existingEntry != null) {
                        // Add the new time slots to the existing entry
                        val updatedTimeSlots = existingEntry.timeSlots.toMutableList().apply {
                            addAll(timeSlots)
                        }
                        updatedList[updatedList.indexOf(existingEntry)] = existingEntry.copy(timeSlots = updatedTimeSlots)
                    } else {
                        // Create a new availability entry for the date
                        updatedList.add(TutorAvailability(day = date, timeSlots = timeSlots))
                    }

                    // Save the updated availability list to Firestore
                    db.collection("availability")
                        .document(monthYear)
                        .collection(tutorId)
                        .document("availabilityData")
                        .set(TutorAvailabilityWrapper(updatedList))
                        .addOnSuccessListener {
                            Log.d("Availability", "Time slots added back to availability")
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
                times.add(currentTime.format(DateTimeFormatter.ofPattern("H:mm"))) // Format as "H:mm"
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