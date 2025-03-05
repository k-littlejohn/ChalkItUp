package com.example.chalkitup.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chalkitup.ui.components.TutorSubject
import com.example.chalkitup.ui.screens.TutorAvailability
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class BookingViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // ------------------- EMAIL NOTIFICATIONS FOR BOOKING ---------------------------

    // Values are set in addSessionToFirestore() function

    // may be better to put these in a data class or something
    // feel free to make changes to this, variables, functions, whatever u like
    // just made this quick -Jeremelle
    private val _userSubject = MutableStateFlow<TutorSubject?>(null)
    private val userSubject: StateFlow<TutorSubject?> get() = _userSubject

    private val _userType = MutableStateFlow<String?>("Unknown")
    val userType: StateFlow<String?> get() = _userType

    private val _fName = MutableStateFlow<String?>("Unknown")
    private val fName: StateFlow<String?> get() = _fName

    private val _userEmail = MutableStateFlow<String?>("Unknown")
    private val userEmail: StateFlow<String?> get() = _userEmail

    private val _tutorName = MutableStateFlow<String?>("Unknown")
    private val tutorName: StateFlow<String?> get() = _tutorName

    private val _price = MutableStateFlow<String?>("Unknown")
    private val price: StateFlow<String?> get() = _price

    private val _timeSlot = MutableStateFlow<String?>("Unknown")
    private val timeSlot: StateFlow<String?> get() = _timeSlot

    private val _date = MutableStateFlow<String?>("Unknown")
    private val date: StateFlow<String?> get() = _date

    init {
        getUserInfoFromUsers()
    }

    private fun getUserInfoFromUsers() {
        // Necessary user information
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: ""
        if (userId.isBlank()) {
            Log.e("MessagesScreen", "User ID is empty. Cannot fetch user info.")
            return
        }
        viewModelScope.launch {
            try {
                val userDocument =
                    Firebase.firestore.collection("users").document(userId).get().await()
                // Check if the document exists
                if (userDocument.exists()) {
                    Log.d("MessagesScreen", "User document found: ${userDocument.data}")

                    val userTypeValue = userDocument.getString("userType") ?: "Unknown"
                    val firstNameValue = userDocument.getString("firstName") ?: "Unknown"
                    val emailValue = userDocument.getString("email") ?: "Unknown"

                    Log.d("MessagesScreen", "userType: $userTypeValue, fName: $firstNameValue, email: $emailValue")

                    // Update LiveData or StateFlow properties
                    _userType.value = userTypeValue
                    _fName.value = firstNameValue
                    _userEmail.value = emailValue
                    Log.d("MessagesScreen", "userType: $userTypeValue, fName: ${fName.value}, email: ${userEmail.value}")

                } else {
                    Log.e("MessagesScreen", "User document does not exist for userId: $userId")
                }
            } catch (exception: Exception) {
                Log.e("MessagesScreen", "Error fetching user type: ${exception.message}")
            }
        }
    }

    private fun sendEmail(onSuccess: () -> Unit) {

        Log.d("MessagesScreen", "fName: ${fName.value}, email: ${userEmail.value}")

        val formattedSubject =
            "${userSubject.value!!.subject} ${userSubject.value!!.grade} ${userSubject.value!!.specialization}"

        val emailSubj = "Your appointment for $formattedSubject has been booked"
        val emailHTML =
            "<p> Hi ${fName.value},<br><br> Your appointment for <b>$formattedSubject</b>" +
                    " with ${tutorName.value} has been booked at ${date.value}: ${timeSlot.value}. </p>" +
                    "<p> The rate of the appointment is: ${price.value} <p>" +
                    "<p> The appointment has been added to your calendar. </p>" +
                    "<p> Have a good day! </p>" +
                    "<p> -ChalkItUp Tutors </p>"

        val email = Email(
            to = userEmail.value!!,
            message = EmailMessage(emailSubj, emailHTML)
        )

        db.collection("mail").add(email)
            .addOnSuccessListener {
                println("Appointment booked successfully!")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error booking appointment: ${e.message}")
            }
    }

    // ------------------- EMAIL NOTIFICATIONS FOR BOOKING END---------------------------


    // State for tutors who can teach the selected subject
    private val _tutors = MutableStateFlow<List<String>>(emptyList())
    val tutors: StateFlow<List<String>> get() = _tutors

    // State for availability data
    private val _availability = MutableStateFlow<Map<LocalDate, List<LocalTime>>>(emptyMap())
    val availability: StateFlow<Map<LocalDate, List<LocalTime>>> get() = _availability

    private val _tutorAvailabilityMap = MutableStateFlow<Map<String, Map<LocalDate, List<LocalTime>>>>(emptyMap())
    val tutorAvailabilityMap: StateFlow<Map<String, Map<LocalDate, List<LocalTime>>>> get() = _tutorAvailabilityMap

    // State for selected day and times
    private val _selectedDay = MutableStateFlow<LocalDate?>(null)
    val selectedDay: StateFlow<LocalDate?> get() = _selectedDay

    private val _selectedStartTime = MutableStateFlow<LocalTime?>(null)
    val selectedStartTime: StateFlow<LocalTime?> get() = _selectedStartTime

    private val _selectedEndTime = MutableStateFlow<LocalTime?>(null)
    val selectedEndTime: StateFlow<LocalTime?> get() = _selectedEndTime

    // State for loading and errors
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error


    // TODO: ALERT DIALOG FOR ERRORS -for after submit: "your session has been booked" or "your session could not be booked bc..."

    // Function to reset all state variables
    fun resetState() {
        _selectedDay.value = null
        _selectedStartTime.value = null
        _selectedEndTime.value = null
        _availability.value = emptyMap()
        _isLoading.value = false
        _error.value = null

        // EMAIL VARIABLES RESET
        _userSubject.value = null
        _tutorName.value = "Unknown"
        _userType.value = "Unknown"
        _fName.value = "Unknown"
        _userEmail.value = "Unknown"
        _timeSlot.value = "Unknown"
        _date.value = "Unknown"
    }

    fun getFirstDayOfMonth(currentDate: LocalDate): LocalDate {
        return currentDate.withDayOfMonth(1) // First day of the month
    }

    fun getLastDayOfMonth(currentDate: LocalDate): LocalDate {
        return currentDate.withDayOfMonth(currentDate.lengthOfMonth()) // Last day of the month
    }

    // Set the selected subject and fetch tutors
    fun setSubject(subject: TutorSubject, priceRange: ClosedFloatingPointRange<Float>) {
        fetchTutors(subject, priceRange)
    }

    // Function to fetch tutors who can teach the selected subject
    private fun fetchTutors(selectedSubject: TutorSubject, priceRange: ClosedFloatingPointRange<Float>) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                db.collection("users")
                    .whereEqualTo("userType", "Tutor") // Filter for tutors only
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val matchedTutorIds = mutableListOf<String>()

                        for (tutorDoc in querySnapshot.documents) {
                            val tutorId = tutorDoc.id
                            println("Fetched tutor: $tutorId")

                            // Fetch the subjects list
                            val subjects = tutorDoc.get("subjects") as? List<Map<String, String>>
                            println("Subjects for tutor $tutorId: $subjects")

                            if (subjects == null) {
                                println("Subjects field is null or not a List for tutor $tutorId")
                            } else {
                                subjects.forEach { subjectMap ->
                                    val subject = subjectMap["subject"] ?: ""
                                    val grade = subjectMap["grade"] ?: ""
                                    val specialization = subjectMap["specialization"] ?: ""
                                    val priceString = subjectMap["price"] ?: ""

                                    println("Checking subject: $subject, grade: $grade, specialization: $specialization, price: $priceString")

                                    // Extract the numeric price from the price string
                                    val price = extractPriceFromString(priceString)

                                    // Check if the subject matches the selected subject
                                    if (subject == selectedSubject.subject &&
                                        grade == selectedSubject.grade &&
                                        specialization == selectedSubject.specialization &&
                                        price != null && price in priceRange // Check if price is within the range
                                    ) {
                                        println("Match found for tutor: $tutorId")
                                        matchedTutorIds.add(tutorId) // Add tutor ID to the list
                                    }
                                }
                            }
                        }

                        println("Matched tutor IDs: $matchedTutorIds")
                        _tutors.value = matchedTutorIds
                        fetchAvailabilityForTutors(matchedTutorIds) // Fetch availability for matched tutors
                    }
                    .addOnFailureListener { e ->
                        println("Failed to fetch tutors: ${e.message}")
                        _error.value = "Failed to fetch tutors: ${e.message}"
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Helper function to extract the numeric price from a string like "$25/hr"
    private fun extractPriceFromString(priceString: String): Float? {
        // Use a regular expression to extract the numeric part of the string
        val regex = Regex("""\d+(\.\d+)?""")
        val matchResult = regex.find(priceString)
        return matchResult?.value?.toFloatOrNull() // Convert the matched string to a Float
    }

    // Fetch availability data for the selected tutors
    private fun fetchAvailabilityForTutors(tutorIds: List<String>) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val availabilityMap = mutableMapOf<LocalDate, MutableList<LocalTime>>()
                val tutorAvailabilityMap = mutableMapOf<String, MutableMap<LocalDate, MutableList<LocalTime>>>()

                // Use a list of Deferred to track all Firestore queries
                val deferredList = tutorIds.map { tutorId ->
                    async {
                        db.collection("availability")
                            .document(currentMonth)
                            .collection(tutorId)
                            .document("availabilityData")
                            .get()
                            .await()
                    }
                }

                // Wait for all Firestore queries to complete
                val documents = deferredList.awaitAll()

                documents.forEachIndexed { index, document ->
                    val tutorId = tutorIds[index]
                    if (document.exists()) {
                        println("Fetched availability data for tutor $tutorId: ${document.data}")
                        val availabilityList = document.toObject(TutorAvailabilityWrapper::class.java)?.availability
                        println("Parsed availability list for tutor $tutorId: $availabilityList")

                        availabilityList?.forEach { tutorAvailability ->
                            val date = LocalDate.parse(tutorAvailability.day)
                            val timeSlots = tutorAvailability.timeSlots.map { timeString ->
                                // Pad single-digit hours with a leading zero (e.g., "9:00" → "09:00")
                                val formattedTimeString = if (timeString.length == 4) "0$timeString" else timeString
                                LocalTime.parse(formattedTimeString, DateTimeFormatter.ofPattern("HH:mm"))
                            }.sorted()
                            println("Time slots for $date (tutor $tutorId): $timeSlots")

                            // Add time slots to the tutor-specific availability map
                            tutorAvailabilityMap.getOrPut(tutorId) { mutableMapOf() }.getOrPut(date) { mutableListOf() }.addAll(timeSlots)

                            // Add time slots to the global availability map
                            availabilityMap.getOrPut(date) { mutableListOf() }.addAll(timeSlots)
                        }
                    } else {
                        println("No availability data found for tutor $tutorId")
                    }
                }

                println("Final availability map: $availabilityMap")
                println("Final tutor availability map: $tutorAvailabilityMap")
                _availability.value = availabilityMap
                _tutorAvailabilityMap.value = tutorAvailabilityMap
            } catch (e: Exception) {
                println("Error fetching availability: ${e.message}")
                _error.value = "Failed to fetch availability: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Wrapper class to parse the availability array from Firestore
    data class TutorAvailabilityWrapper(
        val availability: List<TutorAvailability> = emptyList()
    )

    // Set the selected day
    fun selectDay(day: LocalDate) {
        _selectedDay.value = day
        _selectedStartTime.value = null
        _selectedEndTime.value = null
    }

    // Set the selected start time
    fun selectStartTime(time: LocalTime) {
        _selectedStartTime.value = time
        _selectedEndTime.value = null
    }

    // Set the selected end time
    fun selectEndTime(time: LocalTime) {
        _selectedEndTime.value = time
    }

    // Function to get valid end times for a selected start time
    fun getValidEndTimes(startTime: LocalTime, availability: List<LocalTime>): List<LocalTime> {
        val validEndTimes = mutableListOf<LocalTime>()
        var currentTime = startTime.plusMinutes(30) // Start with the next 30-minute increment

        while (currentTime in availability) {
            validEndTimes.add(currentTime)
            currentTime = currentTime.plusMinutes(30) // time slots are 30 minutes apart
        }

        validEndTimes.add(currentTime) // supplement the next 30 min slot for end times

        return validEndTimes
    }

    fun matchTutorForTimeRange(selectedDay: LocalDate, startTime: LocalTime, endTime: LocalTime, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            println("matchTutorForTimeRange called for $selectedDay, $startTime - $endTime") // Logging

            val tutorAvailabilityMap = _tutorAvailabilityMap.value
            val weekNumber = getWeekNumber(selectedDay) // Helper function to get the week number
            val yearMonth = selectedDay.format(DateTimeFormatter.ofPattern("yyyy-MM")) // Format as yyyy-MM

            val matchingTutors = mutableListOf<Pair<String, Int>>() // Pair of tutorId and session count

            for ((tutorId, availability) in tutorAvailabilityMap) {
                val dayAvailability = availability[selectedDay] ?: continue

                // Log the tutor's availability for the selected day
                println("Tutor $tutorId availability for $selectedDay: $dayAvailability")


                // Check if the selected time range is fully within the tutor's availability
                if (dayAvailability.contains(startTime) && dayAvailability.contains(endTime.minusMinutes(30))) {
                    // Ensure there are no gaps in the selected time range
                    var currentTime = startTime
                    while (currentTime < endTime) {
                        if (currentTime !in dayAvailability) {
                            break
                        }
                        currentTime = currentTime.plusMinutes(30) // Assuming time slots are 30 minutes apart
                    }

                    if (currentTime >= endTime) {
                        // Fetch the session count for the selected week
                        val sessionCount = getSessionCountForWeek(tutorId, yearMonth, weekNumber)
                        matchingTutors.add(tutorId to sessionCount)
                        println("Tutor $tutorId is available for the selected time range") // Logging
                    }
                }
            }

            // If no tutors match, return null
            val result = if (matchingTutors.isEmpty()) {
                println("No matching tutors found") // Logging
                null
            } else {
                // Find the tutor with the lowest session count
                println("Selected tutor: ${matchingTutors.minByOrNull { it.second }?.first}") // Logging
                matchingTutors.minByOrNull { it.second }?.first

            }

            // Return the result via callback
            onResult(result)
        }
    }

    // Helper function to get the week number of the month
    private fun getWeekNumber(date: LocalDate): Int {
        val firstDayOfMonth = date.withDayOfMonth(1)
        val dayOfWeek = firstDayOfMonth.dayOfWeek.value
        val weekNumber = (date.dayOfMonth + dayOfWeek - 1) / 7 + 1
        return weekNumber
    }

    // Function to fetch the session count for a specific week
    private suspend fun getSessionCountForWeek(tutorId: String, yearMonth: String, weekNumber: Int): Int {
        // Fetch the session count from Firestore
        val sessionCountDoc = db.collection("availability")
            .document(yearMonth)
            .collection(tutorId)
            .document("sessionCount")
            .get()
            .await()

        // Get the session count for the specific week
        return sessionCountDoc.getLong("week$weekNumber")?.toInt() ?: 0
    }

    fun submitBooking(
        tutorId: String,
        comments: String,
        sessionType: String,
        subject: TutorSubject, // Keep subject as a parameter
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val day = _selectedDay.value
            val startTime = _selectedStartTime.value
            val endTime = _selectedEndTime.value

            if (day == null || startTime == null || endTime == null) {
                // Handle error: Selected day, start time, or end time is missing
                // should be non-null
                return@launch
            }

            // Get the current user's ID
            // should also be non null
            val studentId = FirebaseAuth.getInstance().currentUser?.uid
            if (studentId == null) {
                // Handle error: No user is logged in
                _error.value = "No user is logged in."
                return@launch
            }

            val weekNumber = getWeekNumber(day)
            val yearMonth = day.format(DateTimeFormatter.ofPattern("yyyy-MM"))

            // Increment the session count for the selected week
            db.collection("availability")
                .document(yearMonth)
                .collection(tutorId)
                .document("sessionCount")
                .update("week$weekNumber", FieldValue.increment(1))
                .await()

            // Remove the booked time slots from the tutor's availability
            removeBookedTimesFromAvailability(tutorId, yearMonth, day, startTime, endTime)

            // Fetch tutor's full name
            val tutorFullName = fetchUserFullName(tutorId)

            // Fetch student's full name
            val studentFullName = fetchUserFullName(studentId)

            // Fetch the tutor's price for the subject
            val price = fetchTutorPriceForSubject(tutorId, subject)
            if (price == null) {
                // Handle error: Price not found for the subject
                _error.value = "Price not found for the subject."
                return@launch
            }
            subject.price = price

            // Add the session to Firestore
            addSessionToFirestore(
                tutorId = tutorId,
                comments = comments,
                sessionType = sessionType,
                day = day,
                startTime = startTime,
                endTime = endTime,
                subject = subject,
                studentId = studentId,
                tutorFullName = tutorFullName,
                studentFullName = studentFullName
            )

            sendEmail(
                onSuccess = { onSuccess() }
            )
        }
    }


    private suspend fun fetchTutorPriceForSubject(tutorId: String, selectedSubject: TutorSubject): String? {
        return try {
            val tutorDoc = db.collection("users")
                .document(tutorId)
                .get()
                .await()

            // Fetch the subjects list
            val subjects = tutorDoc.get("subjects") as? List<Map<String, String>>
            if (subjects == null) {
                println("Subjects field is null or not a List for tutor $tutorId")
                return null
            }

            var price: String? = null

            // Iterate through the subjects list
            subjects.forEach { subjectMap ->
                val subject = subjectMap["subject"] ?: ""
                val grade = subjectMap["grade"] ?: ""
                val specialization = subjectMap["specialization"] ?: ""
                val priceString = subjectMap["price"] ?: ""

                // Check if the subject matches the selected subject
                if (subject == selectedSubject.subject &&
                    grade == selectedSubject.grade &&
                    specialization == selectedSubject.specialization
                ) {
                    price = priceString
                    return@forEach // Exit the loop once a match is found
                }
            }

            // Return the price for the matching subject
            price
        } catch (e: Exception) {
            // Handle errors (e.g., Firestore network issues)
            println("Error fetching tutor price for subject: ${e.message}")
            null
        }
    }

    private suspend fun removeBookedTimesFromAvailability(
        tutorId: String,
        yearMonth: String,
        day: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ) {
        val availabilityRef = db.collection("availability")
            .document(yearMonth)
            .collection(tutorId)
            .document("availabilityData")

        // Fetch the current availability for the selected day
        val availabilityDoc = availabilityRef.get().await()
        val availabilityList = availabilityDoc.get("availability") as? List<Map<String, Any>> ?: return

        // Find the entry where the "day" matches the selected date
        val dayEntry = availabilityList.find { it["day"] == day.toString() } ?: return

        // Get the timeSlots array
        val dayAvailability = dayEntry["timeSlots"] as? MutableList<String> ?: return

        // Log the current time slots for the day
        println("Current time slots for $day: $dayAvailability")

        // Generate the list of time slots to remove (formatted to match Firestore)
        val timeSlotsToRemove = mutableListOf<String>()
        var currentTime = startTime
        while (currentTime < endTime) {
            timeSlotsToRemove.add(currentTime.format(DateTimeFormatter.ofPattern("H:mm")))
            currentTime = currentTime.plusMinutes(30)
        }

        // Log the time slots to remove
        println("Time slots to remove: $timeSlotsToRemove")

        // Remove booked slots
        val updatedAvailability = dayAvailability.filter { it !in timeSlotsToRemove }

        // Update Firestore with the modified availability
        val updatedAvailabilityList = availabilityList.toMutableList().apply {
            // Find the index of the day entry
            val dayIndex = indexOfFirst { it["day"] == day.toString() }
            if (dayIndex != -1) {
                if (updatedAvailability.isEmpty()) {
                    // If no time slots are left, remove the entire day entry
                    removeAt(dayIndex)
                } else {
                    // Otherwise, update the time slots for the day
                    this[dayIndex] = this[dayIndex].toMutableMap().apply {
                        this["timeSlots"] = updatedAvailability
                    }
                }
            }
        }

        // Push the updated availability back to Firestore
        availabilityRef.update("availability", updatedAvailabilityList).await()
    }

    private suspend fun addSessionToFirestore(
        tutorId: String,
        comments: String,
        sessionType: String,
        day: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        subject: TutorSubject,
        studentId: String,
        tutorFullName: String,
        studentFullName: String,
    ) {
        val formattedSubject = "${subject.subject} ${subject.grade} ${subject.specialization}"

        val formattedTimeRange = formatTimeRange(startTime.toString(), endTime.toString())

        _tutorName.value = tutorFullName
        _userSubject.value = subject
        _price.value = subject.price
        _timeSlot.value = formattedTimeRange
        _date.value = day.toString()

        val sessionData = hashMapOf(
            "tutorID" to tutorId,
            "comments" to comments,
            "mode" to sessionType,
            "date" to day.toString(),
            "time" to formattedTimeRange,
            "status" to "booked",
            "subject" to formattedSubject,
            "studentID" to studentId,
            "tutorName" to tutorFullName,
            "studentName" to studentFullName
        )

        db.collection("appointments")
            .add(sessionData)
            .await()
    }

    private fun formatTimeRange(startTime: String, endTime: String): String {
        val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault()) // 24-hour format
        val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault()) // 12-hour format with AM/PM

        // Parse the 24-hour format times to Date objects
        val startDate = inputFormat.parse(startTime)
        val endDate = inputFormat.parse(endTime)

        // Format the Date objects to 12-hour format strings
        val startFormatted = outputFormat.format(startDate!!)
        val endFormatted = outputFormat.format(endDate!!)

        // Return the formatted time range
        return "$startFormatted - $endFormatted"
    }

    private suspend fun fetchUserFullName(userId: String): String {
        return try {
            val userDoc = db.collection("users")
                .document(userId)
                .get()
                .await()

            val firstName = userDoc.getString("firstName") ?: ""
            val lastName = userDoc.getString("lastName") ?: ""
            "$firstName $lastName" // Concatenate first and last name
        } catch (e: Exception) {
            // Handle errors (e.g., Firestore network issues)
            ""
        }
    }
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