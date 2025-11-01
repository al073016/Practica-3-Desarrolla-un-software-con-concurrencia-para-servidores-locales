package concurrentChat;

import Handlers.WriteHandler;
import Handlers.ReadHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Un servidor simple que maneja UN solo cliente.
 *
 * @author sergi
 */
public class Servidor {

    // Puerto en el que el servidor escuchará.
    private static final int PORT = 8080;

    public static void main(String[] args) {

        try {
            // Inicia el servidor en el puerto especificado.
            ServerSocket server = new ServerSocket(PORT);
            System.err.println("Server active in localhost:" + PORT);

            // Espera y acepta una ÚNICA conexión de cliente.
            // El programa se bloquea aquí hasta que un cliente se conecta.
            Socket socket = server.accept();
            System.out.println("new client IP:" + socket.getInetAddress());

            // Prepara los manejadores de lectura y escritura para ese cliente.
            WriteHandler writer = new WriteHandler(socket);
            ReadHandler reader = new ReadHandler(socket);

            // Crea hilos separados para leer y escribir.
            Thread writeThread = new Thread(writer);
            Thread readThread = new Thread(reader);

            // Inicia ambos hilos.
            writeThread.start();
            readThread.start();

        } catch (IOException e) {
            // Maneja errores si el servidor no puede iniciarse.
            System.err.println("Error starting server: " + e.getMessage());
        }
    }
}