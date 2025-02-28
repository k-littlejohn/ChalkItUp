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

    // Generates time intervals from <9:00 AM to 9:30 PM> in 30-minute increments
    val timeIntervals = (9..20).flatMap { hour ->
        listOf("$hour:00", "$hour:30")
    }

    init {
        fetchAvailabilityFromFirestore() // Automatically fetch on ViewModel creation
    }

    /**
     * Fetches the tutor's availability data from Firestore.
     *
     * Retrieves the tutor's availability for the current month and listens
     * for updates in real-time. If data exists, it is loaded into the
     * `_tutorAvailabilityList` state. If no data is found, an empty list is used.
     */
    private fun fetchAvailabilityFromFirestore() {
        val tutorId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(System.currentTimeMillis())
        val db = FirebaseFirestore.getInstance()

        db.collection("availability")
            .document(monthYear)
            .collection(tutorId)
            .document("availabilityData")
            .addSnapshotListener { document, error ->
                if (error != null) {
                    return@addSnapshotListener // Error Handling needs to be added
                }

                if (document != null && document.exists()) {
                    // Parse Firestore data into a list of availability entries
                    val availability = document.toObject(TutorAvailabilityWrapper::class.java)?.availability ?: emptyList()
                    _tutorAvailabilityList.value = availability
                } else {
                    _tutorAvailabilityList.value = emptyList() // Ensure empty state if no data
                }
            }
    }

    /**
     * Selects a specific day for modifying availability.
     *
     * Updates the selected day and loads any previously saved time slots
     * for that day.
     *
     * @param day The selected date in "yyyy-MM-dd" format.
     */
    fun selectDay(day: String) {
        _selectedDay.value = day
        _selectedTimeSlots.value = getSavedTimeSlotsForDay(day)
    }

    /**
     * Toggles the selection of a specific time slot.
     *
     * If the time slot is already selected, it is removed. If not, it is added.
     *
     * @param timeSlot The time slot in "HH:mm" format.
     */
    fun toggleTimeSlotSelection(timeSlot: String) {
        _selectedTimeSlots.value = _selectedTimeSlots.value.toMutableSet().apply {
            if (contains(timeSlot)) remove(timeSlot) else add(timeSlot)
        }
    }

    /**
     * Saves the selected availability to the ViewModel and Firestore.
     *
     * If time slots exist for the selected day, the availability is saved or
     * updated. If no time slots are selected, the existing availability entry
     * for that day is removed.
     */
    fun saveAvailability() {
        _selectedDay.value?.let { day ->
            // Check if selectedTimeSlots is empty
            if (_selectedTimeSlots.value.isEmpty()) {
                // Remove the entry for this day if it exists
                val updatedList = _tutorAvailabilityList.value.toMutableList()
                val existingEntry = updatedList.find { it.day == day }

                // Remove the entry if no time slots are selected
                if (existingEntry != null) {
                    updatedList.remove(existingEntry)
                    _tutorAvailabilityList.value = updatedList
                    saveToFirestore()
                }
            } else {
                // Save or update the entry
                val updatedList = _tutorAvailabilityList.value.toMutableList()
                val existingEntry = updatedList.find { it.day == day }

                if (existingEntry != null) {
                    updatedList[updatedList.indexOf(existingEntry)] =
                        existingEntry.copy(timeSlots = _selectedTimeSlots.value.toList())
                } else {
                    updatedList.add(TutorAvailability(day, _selectedTimeSlots.value.toList()))
                }

                _tutorAvailabilityList.value = updatedList
                saveToFirestore()
            }
        }
    }

    /**
     * Saves the tutor's availability data to Firestore.
     *
     * Filters out empty entries and updates Firestore with the current
     * availability data.
     */
    private fun saveToFirestore() {
        val tutorId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(System.currentTimeMillis())
        val db = FirebaseFirestore.getInstance()

        // Filter out entries with empty time slots
        val nonEmptyAvailabilityList = _tutorAvailabilityList.value.filter { it.timeSlots.isNotEmpty() }

        db.collection("availability")
            .document(monthYear)
            .collection(tutorId)
            .document("availabilityData")
            .set(TutorAvailabilityWrapper(nonEmptyAvailabilityList))
    }

    /**
     * Retrieves saved time slots for a given day.
     *
     * @param day The date in "yyyy-MM-dd" format.
     * @return A set of time slots selected for the given day.
     */
    private fun getSavedTimeSlotsForDay(day: String): Set<String> {
        return _tutorAvailabilityList.value.find { it.day == day }?.timeSlots?.toSet() ?: emptySet()
    }

}

/**
 * Wrapper class for storing a list of TutorAvailability objects in Firestore.
 *
 * @property availability A list of TutorAvailability entries.
 */
data class TutorAvailabilityWrapper(val availability: List<TutorAvailability> = emptyList())
