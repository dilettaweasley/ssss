package com.example.ssss

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.*
class InfoNoAuthActivity : AppCompatActivity() {

    private lateinit var elementListView: ListView
    private val elementList: ArrayList<String> = ArrayList()
    private lateinit var titleTextView: TextView
    private val databaseUrl = "https://ssssiga-default-rtdb.firebaseio.com/"
    private lateinit var database: FirebaseDatabase
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_no_auth)

        val luogo = intent.getStringExtra("luogo")
        val backButton = findViewById<ImageButton>(R.id.backButton)
        database = FirebaseDatabase.getInstance(databaseUrl)

        checkAvailability(luogo, database)

        titleTextView = findViewById(R.id.titleTextView)
        titleTextView.text = "Cosa puoi trovare a $luogo"

        elementListView = findViewById(R.id.elementListView)

        elementListView.setOnItemClickListener { _, _, position, _ ->
            val selectedElement = elementList[position]
            database = FirebaseDatabase.getInstance(databaseUrl)
            showEditElementDialog(luogo, selectedElement, database)
        }

        backButton.setOnClickListener {
            val intent = Intent(this@InfoNoAuthActivity, MapActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun populateListView(luogo: String?) {
        if (!::adapter.isInitialized) {
            // adapter per associare elimentList alla listview
            adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, elementList)
            elementListView.adapter = adapter
            setListViewItemColors(luogo.toString())

        } else {
            adapter.notifyDataSetChanged()
        }
        setListViewItemColors(luogo.toString())
    }

    private fun checkAvailability(luogo: String?, database: FirebaseDatabase) {
        val usersRef = database.reference.child("users")

        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(usersSnapshot: DataSnapshot) {
                elementList.clear()
                for (userSnapshot in usersSnapshot.children) {
                    val luogoSnapshot = userSnapshot.child(luogo!!)
                    if (luogoSnapshot.exists()) {
                        val luogoRef = luogoSnapshot.ref
                        luogoRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (machineSnapshot in snapshot.children) {
                                    val elementName = machineSnapshot.key
                                    if (elementName != "position") {
                                        val availability = machineSnapshot.child("availability").getValue(String::class.java) ?: ""
                                        elementList.add(elementName.toString())

                                        val itemPosition = elementList.indexOf(elementName.toString())
                                        val item = elementListView.getChildAt(itemPosition)
                                        if (item != null) {
                                            if (availability == "si") {
                                                item.setBackgroundColor(Color.GREEN)
                                            } else if (availability == "no") {
                                                item.setBackgroundColor(Color.RED)
                                            } else {
                                                item.setBackgroundColor(Color.TRANSPARENT)
                                            }
                                        }
                                    }
                                }
                                populateListView(luogo) // aggiorna la ListView dopo aver aggiunto gli elementi a elementList
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@InfoNoAuthActivity, "Operazione di load dei dati fallita", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@InfoNoAuthActivity, "Operazione di load macchinetta fallita", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun setListViewItemColors(luogo: String) {
        val usersRef = database.reference.child("users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(usersSnapshot: DataSnapshot) {
                val availabilityMap: HashMap<String, String> = HashMap()

                for (userSnapshot in usersSnapshot.children) {
                    val luogoSnapshot = userSnapshot.child(luogo)
                    if (luogoSnapshot.exists()) {
                        val luogoRef = luogoSnapshot.ref
                        luogoRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (machineSnapshot in snapshot.children) {
                                    val elementName = machineSnapshot.key
                                    if (elementName != "position") {
                                        val availability =
                                            machineSnapshot.child("availability").getValue(String::class.java) ?: ""
                                        availabilityMap[elementName.toString()] = availability
                                    }
                                }
                                for (i in 0 until elementListView.childCount) {
                                    val view = elementListView.getChildAt(i)
                                    val elementName = elementList[i]
                                    val availability = availabilityMap[elementName]
                                    if (availability == "si") {
                                        view.setBackgroundColor(Color.GREEN)
                                    } else if (availability == "no") {
                                        view.setBackgroundColor(Color.RED)
                                    } else {
                                        view.setBackgroundColor(Color.TRANSPARENT)
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    this@InfoNoAuthActivity,
                                    "Operazione di load dei dati fallita",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@InfoNoAuthActivity,
                    "Operazione di load macchinetta fallita",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showEditElementDialog(luogo: String?, elementName: String, database: FirebaseDatabase) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_element, null)
        val availabilityRadioGroup = dialogView.findViewById<RadioGroup>(R.id.editAvailabilityRadioGroup)

        // variabile per memorizzare l'attuale disponibilità dell'elemento
        var availability = ""

        val usersRef = database.reference.child("users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(usersSnapshot: DataSnapshot) {
                for (userSnapshot in usersSnapshot.children) {
                    val luogoSnapshot = userSnapshot.child(luogo!!)
                    if (luogoSnapshot.exists()) {
                        // riferimento al nodo del luogo
                        val luogoRef = luogoSnapshot.ref

                        luogoRef.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                availability = snapshot.child(elementName).child("availability").getValue(String::class.java) ?: ""

                                // imposta il valore esistente nella dialog di modifica
                                if (availability == "si") {
                                    availabilityRadioGroup.check(R.id.editYesRadioButton)
                                } else if (availability == "no") {
                                    availabilityRadioGroup.check(R.id.editNoRadioButton)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@InfoNoAuthActivity, "Operazione di load del prodotto fallita", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@InfoNoAuthActivity, "Operazione di load macchinetta fallita", Toast.LENGTH_SHORT).show()
            }
        })

        val dialog = AlertDialog.Builder(this)
            .setTitle("Disponibilità : ")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val editedAvailability = when (availabilityRadioGroup.checkedRadioButtonId) {
                    R.id.editYesRadioButton -> "si"
                    R.id.editNoRadioButton -> "no"
                    else -> ""
                }

                if (editedAvailability.isNotEmpty()) {
                    if (editedAvailability != availability) {
                        editElementInDatabase(luogo, elementName, editedAvailability, database)
                    } else {
                        Toast.makeText(this@InfoNoAuthActivity, "Nessuna modifica apportata", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@InfoNoAuthActivity, "Per favore seleziona la disponibilità", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annulla", null)
            .create()

        dialog.show()
    }

    private fun editElementInDatabase(luogo: String?, elementName: String, availability: String, database: FirebaseDatabase) {
        val usersRef = database.reference.child("users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(usersSnapshot: DataSnapshot) {
                for (userSnapshot in usersSnapshot.children) {
                    val luogoSnapshot = userSnapshot.child(luogo!!)
                    if (luogoSnapshot.exists()) {
                        // riferimento al nodo del luogo
                        val luogoRef = luogoSnapshot.ref

                        luogoRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                    for (machineSnapshot in snapshot.children) {

                                        val existingAvailability = machineSnapshot.child("availability").getValue(String::class.java) ?: ""
                                        if (existingAvailability != availability) {

                                            // modifica solo se il valore di availability è diverso
                                            luogoRef.child(elementName).child("availability").setValue(availability)
                                                .addOnSuccessListener {
                                                    Log.e("InfoNoAuthActivity", "Operazione effettuata")

                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(this@InfoNoAuthActivity, "Operazione di update del prodotto fallita", Toast.LENGTH_SHORT).show()
                                                }
                                        } else {
                                            Log.e("InfoNoAuthActivity", "Operazione di update del prodotto non effettuata")
                                        }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@InfoNoAuthActivity, "Load del prodotto non riuscito", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@InfoNoAuthActivity, "Operazione di load macchinetta fallita", Toast.LENGTH_SHORT).show()
            }
        })

    }
}