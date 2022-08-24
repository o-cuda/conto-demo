package it.demo.fabrick.exception;

public class ExceptionMessageIn extends Exception {

	private static final String ERROR_MESSAGE = "Message in input differente rispetto all'input ATTESO. Cercare utilizzando i log/Kibana";
	private static final long serialVersionUID = 1L;

	public ExceptionMessageIn() {
		super(ERROR_MESSAGE);
	}

}
