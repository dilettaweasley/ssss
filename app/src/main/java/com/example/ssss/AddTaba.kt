package com.example.ssss

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

class AddTaba : AppCompatActivity() {

    private var selectedPlace: Place? = null
    private var llPlace: String? = null
    // per gestire la ricerca e la selezione del luogo
    private lateinit var autocompleteFragment: AutocompleteSupportFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_taba)

        // viene inizializzato il fragment di autocompletamento
        setupAutocompleteFragment()

        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val backButton = findViewById<ImageButton>(R.id.backButton)

        // inizializzazione di google place con la chiave api del progetto
        Places.initialize(applicationContext, "AIzaSyBCsKHZY3wGnvpbBaiagduIj3OCzJAUUFY")

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val position = llPlace
            saveMachine(name, position)
        }

        // chiude l'Activity corrente e torna all'Activity precedente
        backButton.setOnClickListener {
            val intent = Intent(this@AddTaba, TabaActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupAutocompleteFragment() {

        // fragment di autocompletamento per ottenere solo determinate info
        autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.LAT_LNG, Place.Field.ID, Place.Field.NAME))
            .setTypeFilter(TypeFilter.ADDRESS)

        // quando viene selezionato un luogo
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                selectedPlace = place
                Log.i(TAG, "Place: ${selectedPlace?.name}, ${selectedPlace?.id}")

                // update con il posto selezionato
                autocompleteFragment.setText(selectedPlace?.name)

                // salva solo la latitudine e la longitudine del luogo
                val latitudine = place.latLng?.latitude
                val longitudine = place.latLng?.longitude

                if (latitudine != null && longitudine != null) {
                    llPlace = "$latitudine, $longitudine"
                } else {
                    Toast.makeText(this@AddTaba, "Operazione per ottenere lat e lng fallita", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(status: Status) {
                Toast.makeText(this@AddTaba, "Errore: $status", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveMachine(name: String, position: String?) {
        if (name.isNullOrEmpty()) {
            Toast.makeText(this@AddTaba, "Inserisci un nome per la macchinetta", Toast.LENGTH_SHORT).show()
            return
        }
        if (position.isNullOrEmpty()) {
            Toast.makeText(this@AddTaba, "Per favore seleziona una posizione", Toast.LENGTH_SHORT).show()
            return
        }
        // viene creato un nuovo nodo nel db con il nome della macchinetta scelto e la posizione
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child(name)
            .child("position")
            .setValue(position)
            .addOnSuccessListener {
                Toast.makeText(this@AddTaba, "Machinetta aggiunta", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@AddTaba, TabaActivity::class.java)
                startActivity(intent)
                // Chiude l'activity e torna a HomeActivity
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this@AddTaba, "Macchinetta non aggiunta correttamente", Toast.LENGTH_SHORT).show()
            }
    }
}


