package com.example.ssss

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ElementInfoActivity : AppCompatActivity() {

    private lateinit var elementListView: ListView
    private lateinit var elementAdapter: ArrayAdapter<String>
    private val elementList: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_element_info)

        val addButton = findViewById<ImageButton>(R.id.addElementButton)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        // recupero il nome della macchina della ListView
        val machineName = intent.getStringExtra("machineName")

        elementListView = findViewById(R.id.elementListView)
        elementAdapter = ElementAdapter(this, android.R.layout.simple_list_item_1, elementList, machineName)
        elementListView.adapter = elementAdapter

        // per popolare la listview
        setupListView(machineName)

        // per eliminare elemento selezionato dalla listview
        elementListView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedElement = elementList[position]
            if (machineName != null) {
                showDeleteConfirmationDialog(machineName, selectedElement)
            } else {
            Toast.makeText(this@ElementInfoActivity, "Elemento null", Toast.LENGTH_SHORT).show()
            }
            true
        }

        // per aggiungere un nuovo elemento
        addButton.setOnClickListener {
            showAddElementDialog(machineName)
        }

        // click su un elemento della ListView
        elementListView.setOnItemClickListener { _, _, position, _ ->
            val selectedElement = elementList[position]
            showEditElementDialog(machineName, selectedElement)
        }

        // chiude l'Activity corrente e torna all'Activity precedente
        backButton.setOnClickListener {
            val intent = Intent(this@ElementInfoActivity, TabaActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupListView(machineName: String?) {
        // riferimento della macchinetta nel db
        machineName?.let {
            val elementsRef = machineName.let {
                FirebaseDatabase.getInstance().reference
                    .child("users")
                    .child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .child(it)
            }

            elementsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // iterato ciascun figlio della macchinetta selezionata
                    for (machineSnapshot in snapshot.children) {
                        val machineElementName = machineSnapshot.key
                        if (machineElementName != "position") {
                            elementList.add(machineElementName!!)
                        }
                    }
                    elementAdapter.notifyDataSetChanged()
                    setListViewItemColors(machineName)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ElementInfoActivity, "Caricamento macchinette fallito", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun setListViewItemColors(machineName: String?) {
        machineName?.let {
            val elementsRef = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .child(it)

            elementsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    // oggetto map che associa il nome dell elemento alla sua disponibilità
                    // iterazione su tutti i figli del nodo snapshot(elemRef)
                    val availabilityMap = snapshot.children.associate { child ->
                        child.key to child.child("availability").getValue(String::class.java)
                    }

                    // per ogni elemento della listview
                    for (position in 0 until elementAdapter.count) {
                        // vengono recuperati il nome e la disponibilità
                        val element = elementAdapter.getItem(position)
                        val availability = availabilityMap[element]
                        // visualizzazione dell'elemento nella listview in base alla sua posizione ed eventuali scorrimenti
                        val listItemView = elementListView.getChildAt(position - elementListView.firstVisiblePosition)

                        if (listItemView != null) {
                            val availabilityColor = if (availability == "si") {
                                Color.GREEN
                            } else if (availability == "no"){
                                Color.RED
                            } else {
                                Color.TRANSPARENT
                            }
                            listItemView.setBackgroundColor(availabilityColor)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ElementInfoActivity, "Operazione di load della disponibilità fallita", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun showAddElementDialog(machineName: String?) {
        // dialog con layout predefinito
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_element, null)
        val elementNameEditText = dialogView.findViewById<EditText>(R.id.elementNameEditText)
        val availabilityRadioGroup = dialogView.findViewById<RadioGroup>(R.id.availabilityRadioGroup)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Element")
            .setView(dialogView)
                // aggiunto un pulsante add alla dialog
            .setPositiveButton("Add") { _, _ ->
                // ottengo nome dell elemento e disponibilità selezionata
                val elementName = elementNameEditText.text.toString().trim()
                val availability = when (availabilityRadioGroup.checkedRadioButtonId) {
                    R.id.yesRadioButton -> "si"
                    R.id.noRadioButton -> "no"
                    else -> ""
                }

                if (elementName.isNotEmpty() && availability.isNotEmpty()) {
                    addElementToListView(elementName)
                    addElementToDatabase(machineName, elementName, availability)
                } else {
                    Toast.makeText(this, "Per favore inserisci il nome della macchinetta e la disponibilità", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun addElementToListView(elementName: String) {
            elementList.add(elementName)
            elementAdapter.notifyDataSetChanged()

    }

    private fun addElementToDatabase(machineName: String?, elementName: String, availability: String) {
        machineName?.let {
            val databaseRef = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                    // it è il valore di machineName non nullo
                .child(it)
                    // vengono creati nodi con riferimento a questi due valori
                .child(elementName)
                .child("availability")

            // viene impostato il valore di avaiability
            databaseRef.setValue(availability)
                .addOnSuccessListener {
                    Toast.makeText(this, "Prodotto aggiunto", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Prodotto non aggiunto", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showDeleteConfirmationDialog(machineName: String, selectedElement: String) {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Elimina macchinetta")
        alertDialog.setMessage("Sei sicura di volerla eliminare?")

        alertDialog.setPositiveButton("Elimina") { dialog, _ ->
            deleteElementMachine(machineName, selectedElement)
            dialog.dismiss()
        }

        alertDialog.setNegativeButton("Annulla") { dialog, _ ->
            dialog.dismiss()
        }

        alertDialog.show()
    }

    private fun deleteElementMachine(machineName: String, elementName: String) {
        machineName.let {
            FirebaseDatabase.getInstance().reference
                .child("users")
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .child(it)
                .child(elementName)
                .removeValue()
                .addOnSuccessListener {
                    elementList.remove(elementName)
                    elementAdapter.notifyDataSetChanged()
                    Toast.makeText(this, "Macchinetta eliminata", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Operazione di eliminazione macchinetta fallita", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showEditElementDialog(machineName: String?, elementName: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_element, null)
        val availabilityRadioGroup = dialogView.findViewById<RadioGroup>(R.id.editAvailabilityRadioGroup)

        // variabile per memorizzare l'attuale disponibilità dell'elemento
        var availability = ""

        // imposta il valore esistente nella dialog di modifica
        machineName?.let {
            val elementRef = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .child(it)
                .child(elementName)
                .child("availability")

            elementRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // coverto il valore di snapshot in una stringa
                    availability = snapshot.getValue(String::class.java) ?: ""

                    // imposto la disponibilità dell elemento sul radiobutton
                    if (availability == "si") {
                        availabilityRadioGroup.check(R.id.editYesRadioButton)
                    } else if (availability == "no") {
                        availabilityRadioGroup.check(R.id.editNoRadioButton)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ElementInfoActivity, "Elemento non aggiunto", Toast.LENGTH_SHORT).show()
                }
            })
        }

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
                        editElementInDatabase(machineName, elementName, editedAvailability)
                    } else {
                        Toast.makeText(this, "Nessuna modifica apportata", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Per favore seleziona la disponibilità", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annulla", null)
            .create()

        dialog.show()
    }

    private fun editElementInDatabase(machineName: String?, elementName: String, availability: String) {
        machineName?.let {
            val elementRef = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .child(it)
                .child(elementName)
                .child("availability")

            elementRef.setValue(availability)
                .addOnSuccessListener {
                    Toast.makeText(this, "Elemento aggiunto", Toast.LENGTH_SHORT).show()
                    // Aggiorna il colore dell'elemento nella ListView
                    elementAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Elemento non aggiunto", Toast.LENGTH_SHORT).show()
                }
        }
    }
}