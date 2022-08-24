package it.demo.fabrick.vertx.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

public class SocketClient {

//    private static final Logger LOGGER = LoggerFactory.getLogger(SocketClient.class);

    private static String cobolIn001 =
            "001FRFRE9C517S002001001NO06708Y2019-12-07                                                                                                                                                                                       END-OF-BUFFER";

    private static String cobolIn002 =
            "002FRFRE9C517S002001001NO06708Y2019-12-07                                                                                                                                                                                       END-OF-BUFFER";

    private static String cobolIn003 =
            "003FRFRE9C517S002001001NO06708Y2019-12-07D0                                                                                                                                                                                     END-OF-BUFFER";

    public static void main(String[] args) {

//		String host = "localhost";
        String host = "SCRSCN06";
//		String host = "samape00";

        if (args != null && args.length > 0) {
            host = args[0];
        }

        final String[] array = { cobolIn001, cobolIn002, cobolIn003 };
        String hostname = "localhost";
//		String hostname = "SCRSCN06";
        int port = 9221;

        for (int i = 0; i < 1; i++) {
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {

                    Random r = new Random();
                    int numeroRandom = r.nextInt(3);

//					richiamaSocket(hostname, port, array[numeroRandom], (numeroRandom + 1));
                    richiamaSocket(hostname, port, array[0], 1);
                    richiamaSocket(hostname, port, array[1], 2);
                    richiamaSocket(hostname, port, array[2], 3);
                }
            });
            t.start();
        }
    }

    private static void richiamaSocket(String hostname, int port, String cobolIn, int numeroConfigurazione) {
        try (Socket socket = new Socket(hostname, port)) {

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(cobolIn);

            InputStream input = socket.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String line;

            while ((line = reader.readLine()) != null) {

				System.out.println("(conf:" + numeroConfigurazione + line);
            }
        } catch (UnknownHostException ex) {

			ex.printStackTrace();
        } catch (IOException ex) {

			ex.printStackTrace();
        }
    }

    public class MyRunnable implements Runnable {

        private int var;

        public MyRunnable(int var) {
            this.var = var;
        }

        @Override
        public void run() {
            String hostname = "localhost";
            int port = 9221;

            richiamaSocket(hostname, port, cobolIn001, 1);
            richiamaSocket(hostname, port, cobolIn002, 2);
            richiamaSocket(hostname, port, cobolIn003, 3);
        }
    }

}
