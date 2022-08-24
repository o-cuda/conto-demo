package it.demo.fabrick.vertx.client;

public class TestLIS {

	private static final int port = 9221;

	public static void main(String[] args) {

		String host = "localhost";

		if (args != null && args.length > 0) {
			host = args[0];
		}

		try {
			String sOut;
			sOut = "LIS14537780            2019-01-012019-04-10END-OF-BUFFER";

			AbstractTestClient.sentRequestToAppContainer(host, port, sOut);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
