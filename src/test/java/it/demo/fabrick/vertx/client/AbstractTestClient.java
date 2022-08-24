package it.demo.fabrick.vertx.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public abstract class AbstractTestClient {

//    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTestClient.class);

    protected static final int port = 9221;

    protected static void sentRequestToAppContainer(String host, int port, String sOut) throws IOException {

        try (Socket client = new Socket(host, port)) {

            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), "cp280"));
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), "cp280"));

            out.write(sOut);
            out.flush();

            char[] inchar = new char[3100];

			System.out.println("start read");
            int nBytesRec = 0;
            nBytesRec = in.read(inchar);
            if (nBytesRec > 0) {
				System.out.println(new String(inchar));
            }

			System.out.println("end read");

        }
    }
}
