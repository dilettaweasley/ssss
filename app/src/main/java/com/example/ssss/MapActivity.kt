package com.example.ssss
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// interfaccia OnMapReadyCallBack cnsente di ottenere il riferimento alla mappa google
class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private val databaseUrl = "https://ssssiga-default-rtdb.firebaseio.com/"
    private lateinit var database: FirebaseDatabase
    private var dataJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // inizializza la MapView
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        // caricamento asincrono della mappa
        mapView.getMapAsync(this)
        database = FirebaseDatabase.getInstance(databaseUrl)
        val backButton = findViewById<ImageButton>(R.id.backButton)

        // chiude l'Activity corrente e torna all'Activity precedente
        backButton.setOnClickListener {
            val intent = Intent(this@MapActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    companion object {
        // per identificare la richiesta di autorizzazione per accedere alla posizione dell utente
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        // coroutine viene interrotta quando l'activity viene chiusa
        dataJob?.cancel()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // verifico se l'app ha i permessi necessari per accedere alla posizione
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            // blocco di codice asincrono
            dataJob = GlobalScope.launch {
                val lastKnownLocation = getLastKnownLocation()
                // questo codice viene eseguito sul thread principale (modifica interfaccia utente)
                withContext(Dispatchers.Main) {
                    lastKnownLocation?.let {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
                    }
                }
            }



        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }

        // leggo i dati dal database Firebase e aggiungo i marker alla mappa
        readDataAndAddMarkersToMap()

    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun readDataAndAddMarkersToMap() {
        val usersRef = database.reference.child("users")
        // listener attivato ogni volta che i dati nel nodo vengon modificti
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // rimuove i marker esistenti dalla mappa prima di aggiornarli
                 googleMap.clear()
                // database viene aggiornato in tempo reale
                // blocco di codice asincrono, lettura dati db puo richiedere tempo
                dataJob = GlobalScope.launch {
                    for (userSnapshot in snapshot.children) {
                        for (macchinettaSnapshot in userSnapshot.children) {
                            val machineId = macchinettaSnapshot.key.toString()
                            val position = macchinettaSnapshot.child("position").getValue(String::class.java).toString()

                            if (machineId.isNotEmpty()) {
                                // posizione della macchinetta
                                val latLng = LatLng(getLatitude(position), getLongitude(position))
                                withContext(Dispatchers.Main) {
                                    val marker = googleMap.addMarker(MarkerOptions().position(latLng).title(machineId))
                                    marker?.let {
                                        it.tag = machineId // assegna il nome del luogo come tag del marker
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MapActivity", "DatabaseError: ${error.message}")
            }
        })

        // imposta il listener per il click sul marker
        googleMap.setOnMarkerClickListener { marker ->
            val luogo = marker.tag as? String
            luogo?.let {
                val intent = Intent(this@MapActivity, InfoNoAuthActivity::class.java)
                intent.putExtra("luogo", luogo)
                startActivity(intent)
            }
            true
        }
    }

    // funzione per estrarre la latitudine dalla posizione
    private fun getLatitude(position: String): Double {
        return position.split(",")[0].toDoubleOrNull() ?: 0.0
    }

    // funzione per estrarre la longitudine dalla posizione
    private fun getLongitude(position: String): Double {
        val coordinates = position.split(",")
        if (coordinates.size >= 2) {
            return coordinates[1].toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }


    // funzione per ottenere l'ultima posizione nota dell'utente in modo asincrono
    private suspend fun getLastKnownLocation(): LatLng? = withContext(Dispatchers.IO) {
        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@MapActivity)

        if (ActivityCompat.checkSelfPermission(this@MapActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            runCatching {
                // ottengo l'ultima posizione nota dell'utente in modo asincrono
                val locationTask: Task<Location> = fusedLocationClient.lastLocation
                val location: Location = locationTask.await()
                LatLng(location.latitude, location.longitude)
            }.onSuccess { location ->
                return@withContext location
            }.onFailure { exception ->
                exception.printStackTrace()
            }
        } else {
            // se i permessi di accesso alla posizione non sono stati concessi, vengono richiesti all'utente
            ActivityCompat.requestPermissions(this@MapActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
        }

        return@withContext null
    }

}



