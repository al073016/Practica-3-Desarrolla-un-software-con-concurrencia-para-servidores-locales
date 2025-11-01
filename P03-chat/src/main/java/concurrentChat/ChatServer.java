package concurrentChat;

import Handlers.ClientHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * Clase principal del servidor de chat.
 * @author sergi
 */
public class ChatServer {
    private static final int PORT = 8080;

    // Almacena todos los manejadores de clientes conectados (thread-safe).
    private static Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    // Almacena las IPs bloqueadas (thread-safe).
    private static Set<String> blockList = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        // Crea un pool de hilos para manejar clientes de forma eficiente.
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket server = new ServerSocket(PORT)){

            System.err.println("Server active in localhost:"+ PORT);

            // Bucle infinito para aceptar nuevas conexiones de clientes.
            while (true) {
                System.out.println("Esperando conexión...");
                Socket clientSocket = server.accept();
                String clientIP = clientSocket.getInetAddress().getHostAddress();

                // Verifica si la IP del cliente está en la lista de bloqueo.
                if (blockList.contains(clientIP)) {
                    System.err.println("Conexión rechazada: IP bloqueada -> " + clientIP);
                    clientSocket.close(); // Cierra la conexión inmediatamente.
                    continue; // Salta al siguiente ciclo del bucle.
                }

                System.err.println("New client IP:" + clientIP);

                // Crea un nuevo manejador para el cliente.
                // Pasa la lista de clientes y la lista de bloqueo.
                ClientHandler client = new ClientHandler (clientSocket, clients, blockList);
                clients.add(client);

                // Asigna un hilo del pool al nuevo cliente.
                pool.execute(client);
            }

        } catch (IOException ex) {
            System.getLogger(ChatServer.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }
}