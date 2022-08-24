package it.demo.fabrick.vertx.client;

public class TestClient_ALL_TOGHETHER {

	public static void main(String[] args) {

		String[] myArgs = { "localhost" };

		for (int i = 0; i < 500; i++) {

			TestSAL.main(myArgs);
			TestLIS.main(myArgs);
			TestBON.main(myArgs);
		}

	}

}
