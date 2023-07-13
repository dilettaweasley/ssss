# ssss

L'applicazione offre la possibilità di aggiungere e  visualizzare le macchinette dei tabacchini con i relativi elementi disponibili.

# Activities
MainActivity : l'utente può scegliere se effetuare il login con FirebaseAuthentiction con google come proprietario di un tabacchino/macchinetta o se continuare come cliente (senza login).

TabaActivity : mostra la lista delle macchinette appartennti al proprietario che ha effettuato il login. L'utente può: fare il logout, eliminare una macchinetta, aggiungerne una nuova  o visualizzare i prodotti di una macchinetta. Gli elementi vengono salvati in un database Realtime di Firebase.

AddTaba : permette di aggiungere un nuova macchinetta al database Realtime di Firebase.  Selezionando il nome della macchinetta da aggiugnere e il luogo con Autocomplete Fragment di Google Places API.

ElementInfoActivity : mostra la lista dei prodotti relativi ad un macchinetta, ogni prodotto viene visualizzato con un colore diverso in base alla sua disponibilità, rosso se non disponibile e verde se disponibile. L'utente può: aggiungere un nuovo prodotto, eliminare un prodotto, modificare la disponibilità di un prodotto.

ElementAdapter : è una sottoclasse di ArrayAdapter per gestire la visualizzzione degli elementi della lista nell'interfaccia utente.

MapActivity : mostra la mappa in base alla posizione dell'utente utilizzando Fused Location Provider API ottenuta in modo asincrono. Aggiunge i markers delle macchientte al luogo in cui si trovano in modo asincrono con GlobalScope.launch. L'utente può aprire un markers e visualizzare i prodotti.

InfoNoAuthActivity : mostra i prodotti relativi alla macchientta  cliccata in precedenza, in verde quelli disponibili e in rosso quelli non disponibili. L'utente può modificare la disponibilità dei prodotti in modo tale da aiutare gli altri utenti nella ricerca del prdotto desiderato.
