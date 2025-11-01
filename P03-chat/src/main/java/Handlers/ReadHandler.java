
package Handlers;


import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author sergi
 */
public class ReadHandler extends Handler implements Runnable {

    private String messageRx = "_";

    public ReadHandler(Socket socket) {
        super(socket);
    }

    @Override
    public void run() {
        try {
            do{
                if (in.ready()) {
                    messageRx = in.readLine();
                    System.out.println("sevidor: " + messageRx);
                }
            }while (!messageRx.equalsIgnoreCase("/exit"));

        } catch (EOFException e) {
            System.out.println("Cliente desconectado (EOF)");
        } catch (IOException ex) {
            System.getLogger(ReadHandler.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        } finally {

            dismiss();
        }
    }

}