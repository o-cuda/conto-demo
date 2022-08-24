package it.demo.fabrick.vertx.client;

public class TestBONAmauntIntero {

	private static final int port = 9221;

	public static void main(String[] args) {

		String host = "localhost";

		if (args != null && args.length > 0) {
			host = args[0];
		}

		try {
			StringBuilder sOut = new StringBuilder();
			sOut.append("BON");
			sOut.append(padLeft("14537780", 20));
			sOut.append(padLeft("John Doe", 50));
			sOut.append("IT23A0336844430152923804660");
			sOut.append("SELBIT2BXXX");
			sOut.append(padLeft("Payment invoice 75/2017", 500));
			sOut.append(padLeft("10", 20));
			sOut.append("EUR");
			sOut.append("SHA");
			sOut.append("45685475");
			sOut.append("L449");
			sOut.append("false");
			sOut.append("56258745832");
			sOut.append(padLeft("NATURAL_PERSON", 20));
			sOut.append("MRLFNC81L04A859L");

			sOut.append("END-OF-BUFFER");

			AbstractTestClient.sentRequestToAppContainer(host, port, sOut.toString());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String padRight(String stringa, int lunghezza) {
		return String.format("%-" + lunghezza + "s", stringa);
	}

	public static String padLeft(String stringa, int lunghezza) {
		return String.format("%" + lunghezza + "s", stringa);
	}

}
