package com.example.chalkitup.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.chalkitup.ui.screens.TutorAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class TutorAvailabilityViewModel : ViewModel() {
    private val _selectedDay = MutableStateFlow<String?>(null)
    val selectedDay: StateFlow<String?> = _selectedDay

    private val _selectedTimeSlots = MutableStateFlow<Set<String>>(emptySet())
    val selectedTimeSlots: StateFlow<Set<String>> = _selectedTimeSlots

    private val _tutorAvailabilityList = MutableStateFlow<List<TutorAvailability>>(emptyList())
    val tutorAvailabilityList: StateFlow<List<TutorAvailability>> = _tutorAvailabilityList

    val timeIntervals = (9..20).flatMap { hour ->
        listOf("$hour:00", "$hour:30")
    }

    init {
        fetchAvailabilityFromFirestore() // Automatically fetch on ViewModel creation
    }

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
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val availability = document.toObject(TutorAvailabilityWrapper::class.java)?.availability ?: emptyList()
                    _tutorAvailabilityList.value = availability
                } else {
                    _tutorAvailabilityList.value = emptyList() // Ensure empty state if no data
                }
            }
    }

    fun selectDay(day: String) {
        _selectedDay.value = day
        _selectedTimeSlots.value = getSavedTimeSlotsForDay(day)
    }

    fun toggleTimeSlotSelection(timeSlot: String) {
        _selectedTimeSlots.value = _selectedTimeSlots.value.toMutableSet().apply {
            if (contains(timeSlot)) remove(timeSlot) else add(timeSlot)
        }
    }

    fun saveAvailability() {
        _selectedDay.value?.let { day ->
            // Check if selectedTimeSlots is empty
            if (_selectedTimeSlots.value.isEmpty()) {
                // Remove the entry for this day if it exists
                val updatedList = _tutorAvailabilityList.value.toMutableList()
                val existingEntry = updatedList.find { it.day == day }

                if (existingEntry != null) {
                    updatedList.remove(existingEntry)
                    _tutorAvailabilityList.value = updatedList
                    saveToFirestore()
                }
                // If there's no existing entry, do nothing
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

    private fun getSavedTimeSlotsForDay(day: String): Set<String> {
        return _tutorAvailabilityList.value.find { it.day == day }?.timeSlots?.toSet() ?: emptySet()
    }

}

data class TutorAvailabilityWrapper(val availability: List<TutorAvailability> = emptyList())
