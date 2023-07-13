package com.example.ssss

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    // istanza per l'autenticazione con firebase
    private lateinit var auth: FirebaseAuth
    // istanza per accesso con google
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // imposto layout
        setContentView(R.layout.activity_main)

        // id generato con progetto api di google per richiedere il token id durante l'accesso con google
        val default_web_client_id =
            "488597209440-9hpndplsuhdalb03ep7087fa3pse6bdl.apps.googleusercontent.com"

        // configuro istanza googleSignIn
        // id token univoco generato da google per ogni utente
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(default_web_client_id)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = FirebaseAuth.getInstance()
        val user = findViewById<Button>(R.id.user)

        findViewById<Button>(R.id.signInBtn).setOnClickListener {
            if (isConnected(this)) {
                signInGoogle()
            } else {
                Toast.makeText(this, "Connessione di rete assente", Toast.LENGTH_SHORT).show()
            }
        }

        user?.setOnClickListener {
            if (isConnected(this)) {
                val intent = Intent(this, MapActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Connessione di rete assente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){  result ->
        // se il risultato dell'attività di accesso a google viene completato
        if (result.resultCode == RESULT_OK){
            // si ottine un task dell'account google con cui l'utente ha effettuato l'accesso
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResults(task)
        }else {
            // accesso non riuscito o annullato dall'utente
            Toast.makeText(this, "Accesso non riuscito", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInGoogle(){
        // intent per l'accesso tramite google
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        // se l'accesso ha avuto successo
        if (task.isSuccessful){
            val account : GoogleSignInAccount? = task.result
            // se account è valido
            if (account != null){
                updateUI(account)
            }
        }else{
            Toast.makeText(this, task.exception.toString() , Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {

        // viene creato un oggetto AuthCredential che rappresenta le credenziali di autenticazione
        // utilizzando le credenziali di google (ottenute con il token)
        val credential = GoogleAuthProvider.getCredential(account.idToken , null)

        // esecuzione accesso
        auth.signInWithCredential(credential).addOnCompleteListener {
            // se l'accesso a firebase è andato a buon fine
            if (it.isSuccessful){
                // l'utente puo accedere all'activity seguente
                val intent = Intent(this , TabaActivity::class.java)
                startActivity(intent)
            }else{
                Toast.makeText(this, it.exception.toString() , Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun isConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}