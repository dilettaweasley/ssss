package com.example.ssss

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*
import android.content.Intent
import android.widget.ImageButton

class TabaActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var machineListView: ListView
    private lateinit var machineAdapter: ArrayAdapter<String>
    private val machineList: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_taba)

        // carica le macchinette dell'utente e popola la ListView
        loadUserMachines()

        val backButton = findViewById<ImageButton>(R.id.backButton)
        // per fare logout
        val default_web_client_id = "488597209440-9hpndplsuhdalb03ep7087fa3pse6bdl.apps.googleusercontent.com"
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(default_web_client_id)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!

        machineListView = findViewById(R.id.machineListView)
        // popoolare listview con i dati di machineList
        machineAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, machineList)
        // viene impostato adapter sulla listview, per visualizzare in modo corretto i dati
        machineListView.adapter = machineAdapter

        // per eliminare element listview
        machineListView.setOnItemLongClickListener { _, _, position, _ ->
            // estratto nome macchientta in base alla posizione
            val selectedMachine = machineList[position]
            showDeleteConfirmationDialog(selectedMachine)
            //long click avvenuto, invece del click normale
            true
        }

        // per gestire click su elementi della listview
        machineListView.setOnItemClickListener { _, _, position, _ ->
            val selectedMachine = machineList[position]
            val intent = Intent(this@TabaActivity, ElementInfoActivity::class.java)
            intent.putExtra("machineName", selectedMachine)
            startActivity(intent)
        }

        findViewById<Button>(R.id.signOutBtn).setOnClickListener {
            signOut()
        }

        findViewById<ImageButton>(R.id.addMachine).setOnClickListener {
            val intent = Intent(this@TabaActivity, AddTaba::class.java)
            startActivity(intent)
        }

        // chiude l'Activity corrente e torna all'Activity precedente
        backButton.setOnClickListener {
            val intent = Intent(this@TabaActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadUserMachines() {
        // riferimento alle macchinette dell utente
        val userMachinesRef = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)

        userMachinesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // ciclo di tutte le macchinette
                for (machineSnapshot in snapshot.children) {
                    val machineName = machineSnapshot.key
                    machineList.add(machineName!!)
                }
                // aggiornamento listview
                machineAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TabaActivity, "Operazione di load delle macchinette fallita", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun signOut() {
        // logout utente da firebase
        FirebaseAuth.getInstance().signOut()
        // logout da googleSignIn
        googleSignInClient.signOut().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                startActivity(Intent(this@TabaActivity, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this@TabaActivity, "Logout non riuscito", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmationDialog(machineName: String) {
        // finestra di dialog di conferma per l'eliminazione di un elemento dalla listview
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Elimina macchinetta")
        alertDialog.setMessage("Sei sicuro di volerla eliminare?")

        alertDialog.setPositiveButton("Elimina") { dialog, _ ->
            deleteMachine(machineName)
            dialog.dismiss()
        }

        alertDialog.setNegativeButton("Annulla") { dialog, _ ->
            dialog.dismiss()
        }
        alertDialog.show()
    }

    private fun deleteMachine(machineName: String) {
        // eliminazione della macchientta machineName dal db e dalla listview
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child(machineName)
            .removeValue()
            .addOnSuccessListener {
                machineList.remove(machineName)
                machineAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Macchinetta eliminata", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Macchientta non eliminata", Toast.LENGTH_SHORT).show()
            }
    }
}