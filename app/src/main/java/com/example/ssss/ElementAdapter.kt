package com.example.ssss

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// sottoclasse di ArrayAdapter per gestire la visualizzzione degli elementi della lista nell interfaccia utente
// il costruttore prende come paramentri il contesto, isd alla risorsa del layout, lista degli ogetti da visualizzare, nome della macchina
class ElementAdapter(context: Context, resource: Int, objects: List<String>, private val machineName: String?) : ArrayAdapter<String>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // funzione chiamata per ogni elemento della lista e restituisce la vista dell elemento
        val view = super.getView(position, convertView, parent)
        val databaseRef = FirebaseDatabase.getInstance().reference
        val elementName = getItem(position)

        // ottengo il riferimento all'elemento nel database
        val elementRef = databaseRef.child("users")
            .child(FirebaseAuth.getInstance().currentUser?.uid ?: "")
            .child(machineName.toString())
            .child(elementName ?: "")
            .child("availability")

        elementRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val availability = snapshot.value as String?
                if (availability == "si") {
                    view.setBackgroundColor(Color.GREEN)
                } else if (availability == "no") {
                    view.setBackgroundColor(Color.RED)
                } else {
                    view.setBackgroundColor(Color.TRANSPARENT)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Errore", Toast.LENGTH_SHORT).show()
            }

        })

        return view
    }
}