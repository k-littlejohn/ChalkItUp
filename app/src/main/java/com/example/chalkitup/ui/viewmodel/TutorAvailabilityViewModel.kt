package com.example.chalkitup.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * ViewModel for managing tutor availability.
 *
 * This ViewModel handles the selection, storage, and retrieval of tutor
 * availability data. It allows tutors to select available time slots for specific
 * days, toggle selections, and save or remove availability data in Firestore.
 * Availability data is automatically fetched from Firestore upon ViewModel
 * initialization.
 */
class TutorAvailabilityViewModel : ViewModel() {

    // Holds the currently selected day for availability
    private val _selectedDay = MutableStateFlow<String?>(null)
    val selectedDay: StateFlow<String?> = _selectedDay

    // State to manage edit mode
    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    // Holds the set of selected time slots for the selected day
    private val _selectedTimeSlots = MutableStateFlow<Set<TimeSlot>>(emptySet())
    val selectedTimeSlots: StateFlow<Set<TimeSlot>> = _selectedTimeSlots

    // Holds the list of all availability entries for the tutor
    private val _tutorAvailabilityList = MutableStateFlow<List<TutorAvailability>>(emptyList())
    val tutorAvailabilityList: StateFlow<List<TutorAvailability>> = _tutorAvailabilityList

    private val _isCurrentMonth = MutableStateFlow(false)

    // Generates time intervals from 9:00 AM to 9:30 PM in 30-minute increments
    val timeIntervals = generateTimeIntervals()

    private fun generateTimeIntervals(): List<String> {
        // Use SimpleDateFormat to enforce "h:mm a" format
        val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

        // Generates time intervals from 9:00 AM to 9:30 PM in 30-minute increments
        return (9..20).flatMap { hour ->
            listOf(
                timeFormatter.format(Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, 0)
                }.time),
                timeFormatter.format(Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, 30)
                }.time)
            )
        }
    }

    init {
        fetchAvailabilityFromFirestore() // Automatically fetch on ViewModel creation
    }

    fun clearSelectedDay() {
        _selectedDay.value = null
    }

    fun selectAllOnline() {
        _selectedTimeSlots.value = _selectedTimeSlots.value.toMutableSet().apply {
            // Iterate through all time intervals
            timeIntervals.forEach { time ->
                // Skip if the time slot is booked
                if (any { it.time == time && it.booked }) return@forEach

                // Find the existing TimeSlot for this time
                val existingTimeSlot = find { it.time == time }
                if (existingTimeSlot != null) {
                    // Update the existing TimeSlot to ensure `online = true`
                    remove(existingTimeSlot)
                    add(existingTimeSlot.copy(online = true))
                } else {
                    // Add a new TimeSlot with `online = true`
                    add(TimeSlot(time = time, online = true))
                }
            }
        }
    }

    fun clearAllOnline() {
        _selectedTimeSlots.value = _selectedTimeSlots.value.toMutableSet().apply {
            // Iterate through all time intervals
            timeIntervals.forEach { time ->
                // Skip if the time slot is booked
                if (any { it.time == time && it.booked }) return@forEach

                // Find the existing TimeSlot for this time
                val existingTimeSlot = find { it.time == time }
                if (existingTimeSlot != null) {
                    // Update the existing TimeSlot to ensure `online = false`
                    val updatedTimeSlot = existingTimeSlot.copy(online = false)
                    remove(existingTimeSlot)

                    // Only add the updated TimeSlot back if `inPerson` is true
                    if (updatedTimeSlot.inPerson) {
                        add(updatedTimeSlot)
                    }
                    // If both `online` and `inPerson` are false, the entry is removed
                }
            }
        }
    }

    fun selectAllInPerson() {
        _selectedTimeSlots.value = _selectedTimeSlots.value.toMutableSet().apply {
            // Iterate through all time intervals
            timeIntervals.forEach { time ->
                // Skip if the time slot is booked
                if (any { it.time == time && it.booked }) return@forEach

                // Find the existing TimeSlot for this time
                val existingTimeSlot = find { it.time == time }
                if (existingTimeSlot != null) {
                    // Update the existing TimeSlot to ensure `inPerson = true`
                    remove(existingTimeSlot)
                    add(existingTimeSlot.copy(inPerson = true))
                } else {
                    // Add a new TimeSlot with `inPerson = true`
                    add(TimeSlot(time = time, inPerson = true))
                }
            }
        }
    }

    fun clearAllInPerson() {
        _selectedTimeSlots.value = _selectedTimeSlots.value.toMutableSet().apply {
            // Iterate through all time intervals
            timeIntervals.forEach { time ->
                // Skip if the time slot is booked
                if (any { it.time == time && it.booked }) return@forEach

                // Find the existing TimeSlot for this time
                val existingTimeSlot = find { it.time == time }
                if (existingTimeSlot != null) {
                    // Update the existing TimeSlot to ensure `inPerson = false`
                    val updatedTimeSlot = existingTimeSlot.copy(inPerson = false)
                    remove(existingTimeSlot)

                    // Only add the updated TimeSlot back if `online` is true
                    if (updatedTimeSlot.online) {
                        add(updatedTimeSlot)
                    }
                    // If both `online` and `inPerson` are false, the entry is removed
                }
            }
        }
    }


    // Fetches the tutor's availability data from Firestore and updates the LiveData list
    fun fetchAvailabilityFromFirestore(plusMonth: java.time.YearMonth? = null) {
        // Get the currently logged-in tutor's ID
        val tutorId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        println("Fetching availability from Firestore")

        val monthYear: String
        if (plusMonth != null) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, 1) // Move to the next month
            val nextMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
            monthYear = nextMonth
            _isCurrentMonth.value = false
        } else {
            val calendar = Calendar.getInstance()
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
            monthYear = currentMonth
            _isCurrentMonth.value = true
        }
        println("Month Year: $monthYear")

        // Format the current month and year as "yyyy-MM" to structure Firestore documents
        // monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(System.currentTimeMillis())


        val db = FirebaseFirestore.getInstance()

        initializeSessionCount(tutorId, monthYear)

        // Listen for changes in the tutor's availability data for the current month
        db.collection("availability")
            .document(monthYear)
            .collection(tutorId)
            .document("availabilityData")
            .addSnapshotListener { document, error ->
                if (error != null) {
                    // Handle the error (e.g., log it or show a message to the user)
                    Log.e("FirestoreError", "Error fetching availability data", error)
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    // Convert Firestore document into a TutorAvailabilityWrapper object
                    val availabilityWrapper = document.toObject(TutorAvailabilityWrapper::class.java)
                    val availabilityList = availabilityWrapper?.availability ?: emptyList() // CHANGE i think

                    // Update LiveData with the fetched availability list
                    _tutorAvailabilityList.value = availabilityList
                } else {
                    // If no availability data exists, set an empty list to prevent null issues
                    _tutorAvailabilityList.value = emptyList()
                }
            }
    }

    // Sets the selected day and loads its corresponding saved time slots
    fun selectDay(day: String) {
        println("Selected Day: $day")
        _selectedDay.value = day
        _selectedTimeSlots.value = getSavedTimeSlotsForDay(day)
    }

    // Toggles the selection of a time slot for the selected day
    fun toggleTimeSlotSelection(timeSlot: String, mode: String) {
        _selectedTimeSlots.value = _selectedTimeSlots.value.toMutableSet().apply {
            // Find the existing TimeSlot with the same time
            val existingTimeSlot = find { it.time == timeSlot }

            if (existingTimeSlot != null) {
                // Remove the existing TimeSlot
                remove(existingTimeSlot)
                // Toggle the mode for the existing TimeSlot
                val updatedTimeSlot = existingTimeSlot.copy(
                    online = if (mode == "online") !existingTimeSlot.online else existingTimeSlot.online,
                    inPerson = if (mode == "inPerson") !existingTimeSlot.inPerson else existingTimeSlot.inPerson
                )
                // Add the updated TimeSlot back to the Set only if either online or inPerson is true
                if (updatedTimeSlot.online || updatedTimeSlot.inPerson) {
                    add(updatedTimeSlot)
                }
            } else {
                // Add a new TimeSlot with the specified mode set to true
                val newTimeSlot = TimeSlot(
                    time = timeSlot,
                    online = mode == "online",
                    inPerson = mode == "inPerson",
                    booked = false
                )
                add(newTimeSlot)
            }
        }
    }

    // Saves the tutor's availability to Firestore, updating or removing entries as needed
    fun saveAvailability() {
        _selectedDay.value?.let { day ->
            if (_selectedTimeSlots.value.isEmpty()) {
                // If no time slots are selected, remove the day's availability
                val updatedList = _tutorAvailabilityList.value.toMutableList()
                val existingEntry = updatedList.find { it.day == day }

                if (existingEntry != null) {
                    updatedList.remove(existingEntry)
                    _tutorAvailabilityList.value = updatedList
                    saveToFirestore()
                }
            } else {
                // Update the availability list with selected time slots for the day
                val updatedList = _tutorAvailabilityList.value.toMutableList()
                val existingEntry = updatedList.find { it.day == day }

                if (existingEntry != null) {
                    // Modify the existing entry with the updated time slots
                    updatedList[updatedList.indexOf(existingEntry)] =
                        existingEntry.copy(timeSlots = _selectedTimeSlots.value.toList())
                } else {
                    // Add a new availability entry if it doesn't exist
                    updatedList.add(TutorAvailability(day, _selectedTimeSlots.value.toList()))
                }

                _tutorAvailabilityList.value = updatedList
                saveToFirestore()
            }
        }
        _isEditing.value = false // Exit editing mode
    }

    // Saves the updated availability list to Firestore, removing empty entries
    private fun saveToFirestore() {
        // Get the currently logged-in tutor's ID
        val tutorId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Format the current month and year for Firestore document structure
        val monthYear: String
        if (_isCurrentMonth.value) {
            monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(System.currentTimeMillis())
        } else {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, 1) // Move to the next month
            val nextMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
            monthYear = nextMonth
        }


        val db = FirebaseFirestore.getInstance()

        // Filter out empty availability entries before saving
        val nonEmptyAvailabilityList = _tutorAvailabilityList.value.filter { it.timeSlots.isNotEmpty() }

        // Store the updated availability data in Firestore
        db.collection("availability")
            .document(monthYear)
            .collection(tutorId)
            .document("availabilityData")
            .set(TutorAvailabilityWrapper(nonEmptyAvailabilityList))
            .addOnSuccessListener {
                // Handle success (e.g., show a toast or log a message)
                Log.d("Firestore", "Availability data saved successfully!")
            }
            .addOnFailureListener { e ->
                // Handle failure (e.g., log the error or show a message to the user)
                Log.e("Firestore", "Error saving availability data", e)
            }
    }

    // Retrieves the saved time slots for a specific day from the availability list
    private fun getSavedTimeSlotsForDay(day: String): Set<TimeSlot> {
        return _tutorAvailabilityList.value
            .find { it.day == day }
            ?.timeSlots
            ?.toSet()
            ?: emptySet()
    }

    // Toggles edit mode for modifying availability
    fun toggleEditMode() {
        _isEditing.value = !_isEditing.value
    }

    // Cancels the editing process, restoring previously saved time slots
    fun cancelEdit() {
        _selectedDay.value?.let { day ->
            _selectedTimeSlots.value = getSavedTimeSlotsForDay(day)
        }
        _isEditing.value = false
    }

    private fun initializeSessionCount(tutorId: String, yearMonth: String) {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()

            val sessionCountRef = db.collection("availability")
                .document(yearMonth)
                .collection(tutorId)
                .document("sessionCount")

            // Check if the document already exists
            val document = sessionCountRef.get().await()
            if (!document.exists()) {
                // Initialize the document with default values
                val defaultSessionCount = mapOf(
                    "week1" to 0,
                    "week2" to 0,
                    "week3" to 0,
                    "week4" to 0,
                    "week5" to 0 // Include week5 for months with 5 weeks
                )
                sessionCountRef.set(defaultSessionCount).await()
            }
        }
    }
}

/**
 * Wrapper class for storing a list of TutorAvailability objects in Firestore.
 *
 * @property availability A list of TutorAvailability entries.
 */
data class TutorAvailabilityWrapper(val availability: List<TutorAvailability> = emptyList())

// Data model for storing tutor availability
data class TutorAvailability(
    val day: String = "", // Selected day
    val timeSlots: List<TimeSlot> = emptyList() // List of available time slots for that day
)

data class TimeSlot (
    val time: String = "",
    val online: Boolean = false,
    val inPerson: Boolean = false,
    var booked: Boolean = false
)