package com.example.chalkitup.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeViewModel : ViewModel() {

    private val _userName = MutableStateFlow<String?>("Unknown")
    val userName: StateFlow<String?> get() = _userName

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

    private fun fetchAppointments() {
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
    val comments: String = ""
)