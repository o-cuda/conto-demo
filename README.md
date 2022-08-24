### Breve descrizione

Applicazione di Test che si basa su alcune API di Fabrick.
Siccome tra i requisiti non era richiesta un FE di esposizione del dato, ho preso spunto da una applicazione fatta da me che veniva usata per gestire la comunicazione via messaggi in plain string, con i vari campi posizionali in input, attraverso un socket TCP, e restituisce sempre un messaggio di testo

L'applicazione si basa su SpringBoot + Vert-x rendeno l'applicazione reattiva

Al momento e' mancante della parte di test automatici, ho solo fatto tre test di integrazione che prendano diano il risultato come volevo (vero che nei word dei requisiti era espressamente richiesta particolare attenzione ai test, ma in generale preferisco porre attenzione quando possibile a quelli di integrazione per vedere il risultato, perchè non è richiesto un grande lavoro "logico" sulle API o altri algoritmi completti)

Per i test occorre far partire l'applicazione:
	- spring parte sulla 9090 -> questo semplicemente per la parte degli actuator di spring (interessante per gli health check )
	- il server socket di vertx parte sulla 9221
	- lancio manuale dei TEST come "programma java", sono dei main semplici che scrivono il messaggio aprendo un client socket
	
Passo successivo sara' vare a capire se si possono rendere automatico con una sorta di avvio propedeuto dell'accplicazione in modo da poterli renderli automatici su maven e mockando le API di fabrick

Per quanto riguarda il DB, al momento  è prese sente ma non l'ho usato per la scrittura su DB dei movimenti, lo vorrei fare in un secondo step.
Il db per poterlo fare è presente, è un H2 che parte in memory, ed è usato per la lettura delle configurazioni dei messaggi che arrivano sul socket.
In pratica i primi 3 caratteri che arrivano sul messaggio identificano l'OPERAZIONE che si vuole effettuare e vanno a prendersi sul DB la configurazione da usare
- LIS: lista delle transazioni
- BON: effettuare il bonifico
- SAL: per la visualizzazione del SALDO

## descrizione architettura

L'applicazione è un microservizio unico. All'interno della compilazione è anche presente la compilazione di una immagine docker (non l'ho provata direttamente al momento perchè non ho un docker instalato a portata di mano, ma dovrebbe funzionare correttamente, immagine base usata una OpenJDK Amazon Corretto ultima versione 17.0.4-alpine3.15)

L'applicazione usa vertx come framework reactive. I vari Verticle colloquiano tra loro attraverso l'event bus come fossero del publisher / subscriber

Il logging dell'applicazione è affidata a Logback, con in più la possibilità si loggare in maniera asicrona attraverso configurazione, ed anche in formato json se si vuole utilizzare uno stack elastic + kibana ad esempio
Siccome siamo un contesto reattivo vertx, il problema qua è che si perdeva la possibilità di mettere nell'MDC di Logback un ID per contrassegnare ed identificare i LOG di una univoca richiesta. Per ovviare a questo problema si è utilizzato reactiverse-contextual-logging: [link alla documentazione]( https://reactiverse.io/reactiverse-contextual-logging/)

Per quanto rigurada le letture/scritture su DB sono fatte senza l'utilizzo di un ORM ma utilizzando direttamente il JDBC client di vertx: [link alla documentazione](https://vertx.io/docs/vertx-jdbc-client/java/)
Questo perchè i JDBC normali bloccano i thread, mentre questo di vertx è asincrono

Il DB H2 è solo in memoria, quindi le eventuali scritture vanno per perse ogni volta si riavvia l'applicazione, ma tanto questa è una app banco di prova e ne sono conscio
Se si volesse mettere un DB vero, per quanto riguarda le configurazioni le si probbero semplicemente mettere in cache (il primo modo che mi viene in mente sarebbe usare la [Cache di Spring](https://spring.io/guides/gs/caching/))
## analisi sul come funziona

 