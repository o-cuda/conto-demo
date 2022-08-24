Breve descrizione:

Applicazione di Test che si basa su alcune API di Fabrick.
Siccome tra i requisiti non era richiesta un FE di esposizione del dato, ho preso spunto da una vecchia applicazione fatta da me che veniva usata per gestire la comunicazione via messaggi tra un socket client TCP ed il mondo dei servizi "rest"

L'applicazione si basa su SpringBoot + Vert-x rendeno l'applicazione reattiva

Al momento è mancante della parte di test automatici, ho solo fatto tre test di integrazione che prendano diano il risultato come volevo (vero che nei word dei requisiti era espressamente richiesta particolare attenzione ai test, ma in generale preferisco porre attenzione quando possibile a quelli di integrazione per vedere il risultato, perchè non è richiesto un grande lavoro "logico" sulle API o altri algoritmi completti)

Per i test occorre far partire l'applicazione:
	- spring parte sulla 9090 -> questo semplicemente per la parte degli actuator di spring (interessante per gli health check )
	- il server socket di vertx parte sulla 9221
	- lancio manuale dei TEST come "programma java", sono dei main semplici che scrivono il messaggio aprendo un client socket
	
Passo successivo sarà provare a capire se si possono rendere automatico con una sorta di avvio propedeuto dell'accplicazione in modo da poterli renderli automatici su maven e mockando le API di fabrick

Per quanto riguarda il DB, al momento è presente ma non l'ho usato per la scrittura su DB dei movimenti, lo vorrei fare in un secondo step.
Il db per poterlo fare è presente, è un H2 che parte in memory, ed è usato per la lettura delle configurazioni dei messaggi che arrivano sul socket.
In pratica i primi 3 caratteri che arrivano sul messaggio identificano l'OPERAZIONE che si vuole effettuare e vanno a prendersi sul DB la configurazione da usare
- LIS: lista dei risultati
- BON: effettuare il bonifico
- SAL: per la visualizzazione del SALDO

 