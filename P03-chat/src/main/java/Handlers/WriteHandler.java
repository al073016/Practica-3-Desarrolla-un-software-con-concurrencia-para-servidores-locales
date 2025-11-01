
package Handlers;


import java.net.Socket;
import java.util.Scanner;

/**
 *
 * @author sergi
 */
public class WriteHandler extends Handler implements Runnable {

    private String messageTx = "_";

    public WriteHandler(Socket socket) {
        super(socket);
    }

    @Override
    public void run() {
        Scanner messageScanner = new Scanner(System.in);

        while (out!= null && !messageTx.equalsIgnoreCase("/exit")) {

            messageTx = messageScanner.nextLine();
            out.print(messageTx + "\r\n");
            System.out.println( "me: " + messageTx );
            out.flush();
        }
        System.out.println("com.uacam.p03_chat.WriteHandler.run()");
        dismiss();

    }
}