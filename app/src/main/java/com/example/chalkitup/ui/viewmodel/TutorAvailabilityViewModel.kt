package com.example.chalkitup.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.chalkitup.ui.screens.TutorAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
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

    // Holds the set of selected time slots for the selected day
    private val _selectedTimeSlots = MutableStateFlow<Set<String>>(emptySet())
    val selectedTimeSlots: StateFlow<Set<String>> = _selectedTimeSlots

    // Holds the list of all availability entries for the tutor
    private val _tutorAvailabilityList = MutableStateFlow<List<TutorAvailability>>(emptyList())
    val tutorAvailabilityList: StateFlow<List<TutorAvailability>> = _tutorAvailabilityList

    // State to manage edit mode
    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    // Generates time intervals from <9:00 AM to 9:30 PM> in 30-minute increments
    val timeIntervals = (9..20).flatMap { hour ->
        listOf("$hour:00", "$hour:30")
    }

    init {
        fetchAvailabilityFromFirestore() // Automatically fetch on ViewModel creation
    }

    // Fetches the tutor's availability data from Firestore and updates the LiveData list
    private fun fetchAvailabilityFromFirestore() {
        // Get the currently logged-in tutor's ID
        val tutorId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Format the current month and year as "yyyy-MM" to structure Firestore documents
        val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(System.currentTimeMillis())
        val db = FirebaseFirestore.getInstance()

        // Listen for changes in the tutor's availability data for the current month
        db.collection("availability")
            .document(monthYear)
            .collection(tutorId)
            .document("availabilityData")
            .addSnapshotListener { document, error ->
                if (error != null) {
                    return@addSnapshotListener // Error Handling needs to be added
                }

                if (document != null && document.exists()) {
                    // Convert Firestore document into a TutorAvailabilityWrapper object
                    val availability = document.toObject(TutorAvailabilityWrapper::class.java)?.availability ?: emptyList()
                    // Update LiveData with the fetched availability list
                    _tutorAvailabilityList.value = availability
                } else {
                    // If no availability data exists, set an empty list to prevent null issues
                    _tutorAvailabilityList.value = emptyList()
                }
            }
    }

    // Sets the selected day and loads its corresponding saved time slots
    fun selectDay(day: String) {
        _selectedDay.value = day
        _selectedTimeSlots.value = getSavedTimeSlotsForDay(day)
    }

    // Toggles the selection of a time slot for the selected day
    fun toggleTimeSlotSelection(timeSlot: String) {
        _selectedTimeSlots.value = _selectedTimeSlots.value.toMutableSet().apply {
            // If the time slot is already selected, remove it; otherwise, add it
            if (contains(timeSlot)) remove(timeSlot) else add(timeSlot)
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
        val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(System.currentTimeMillis())
        val db = FirebaseFirestore.getInstance()

        // Filter out empty availability entries before saving
        val nonEmptyAvailabilityList = _tutorAvailabilityList.value.filter { it.timeSlots.isNotEmpty() }

        // Store the updated availability data in Firestore
        db.collection("availability")
            .document(monthYear)
            .collection(tutorId)
            .document("availabilityData")
            .set(TutorAvailabilityWrapper(nonEmptyAvailabilityList))
    }

    // Retrieves the saved time slots for a specific day from the availability lis
    private fun getSavedTimeSlotsForDay(day: String): Set<String> {
        return _tutorAvailabilityList.value.find { it.day == day }?.timeSlots?.toSet() ?: emptySet()
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
}

/**
 * Wrapper class for storing a list of TutorAvailability objects in Firestore.
 *
 * @property availability A list of TutorAvailability entries.
 */
data class TutorAvailabilityWrapper(val availability: List<TutorAvailability> = emptyList())
