package com.example.chalkitup.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.tasks.await

// Left these functions here for now in case they might be useful to
// reference in the future -Jeremelle

/**
 * Suspends to fetch location predictions from the Google Places API.
 *
 * This function takes a PlacesClient, a query string, and a session token to
 * request autocomplete predictions restricted to cities. It returns a list of
 * city and province/state combinations.
 *
 * CURRENTLY NOT IN USE
 *
 * @param placesClient The Google Places API client.
 * @param query The user's input text for location search.
 * @param sessionToken The session token for autocomplete requests.
 * @return A list of formatted city and province/state strings.
 */
suspend fun fetchPredictions(
    placesClient: PlacesClient,
    query: String,
    sessionToken: AutocompleteSessionToken
): List<String> {
    val request = FindAutocompletePredictionsRequest.builder()
        .setQuery(query)
        .setTypeFilter(TypeFilter.CITIES)  // Restrict to cities only
        .setSessionToken(sessionToken)
        .build()

    return try {
        val response = placesClient.findAutocompletePredictions(request).await()
        response.autocompletePredictions.mapNotNull { prediction ->
            val city = prediction.getPrimaryText(null).toString()
            val provinceOrState = prediction.getSecondaryText(null).toString()
            "$city, $provinceOrState"
        }
    } catch (e: Exception) {
        Log.e("Places", "Error fetching predictions", e)
        emptyList() // Return empty list in case of error
    }
}

/**
 * Composable function for a location autocomplete text field.
 *
 * This UI component provides location suggestions as the user types. It uses
 * Google Places API to fetch city predictions and displays them in a dropdown list.
 * When a user selects a location, the text field updates, and the selection is passed
 * to the parent composable through a callback.
 *
 * CURRENTLY NOT IN USE
 *
 * @param placesClient The Google Places API client.
 * @param sessionToken The session token for autocomplete requests.
 * @param location The currently selected location.
 * @param onLocationSelected Callback invoked when a location is selected.
 * @param locationError Indicates if there's a validation error for the location field.
 */
@Composable
fun LocationAutocompleteTextField(
    placesClient: PlacesClient,
    sessionToken: AutocompleteSessionToken,
    location: String,
    onLocationSelected: (String) -> Unit,
    locationError: Boolean
) {
    // State variables to manage search query, predictions, dropdown visibility, and selection status
    var searchQuery by remember { mutableStateOf(location) }
    var predictions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isDropdownVisible by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(true) }
    var localLocationError = locationError

    // Syncs the search query with the initial location value
    LaunchedEffect(location) {
        searchQuery = location
    }

    // Fetch predictions whenever the search query changes
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty() && !selected) {
            // Call the function that fetches predictions
            predictions = fetchPredictions(placesClient, searchQuery, sessionToken)
            isDropdownVisible = predictions.isNotEmpty()
        } else {
            isDropdownVisible = false
        }
    }

    Column {
        // Outlined text field for location input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                selected = false
            },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search query",
                        modifier = Modifier.clickable {
                            searchQuery = ""
                            onLocationSelected("")
                        }
                    )
                }
            },
            isError = if (searchQuery.isNotEmpty()) false else localLocationError,
            supportingText = { if (localLocationError) Text("Please enter a location") }
        )

        // Dropdown to show location predictions
        if (isDropdownVisible) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(predictions) { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLocationSelected(suggestion)
                                    searchQuery = suggestion
                                    isDropdownVisible = false
                                    selected = true
                                    localLocationError = false
                                }
                                .padding(16.dp)
                        ) {
                            Text(text = suggestion)
                        }
                    }
                }
            }
        }
    }
}

//// Google Places API client for location autocomplete.
//val placesClient = remember { Places.createClient(context) }
//val sessionToken = remember { AutocompleteSessionToken.newInstance() }