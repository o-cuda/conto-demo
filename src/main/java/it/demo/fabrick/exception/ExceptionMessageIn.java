package it.demo.fabrick.exception;

import java.util.UUID;

public class ExceptionMessageIn extends Exception {

	private static final String ERROR_MESSAGE = "Message in input differente rispetto all'input ATTESO. Cercare la copy sui log/Kibana con il seguente codice: ";
	private static final long serialVersionUID = 1L;

	public ExceptionMessageIn(UUID uuid) {
		super(ERROR_MESSAGE + uuid);
	}

}
