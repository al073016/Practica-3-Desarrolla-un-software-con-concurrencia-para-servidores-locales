package concurrentChat;

import Handlers.WriteHandler;
import Handlers.ReadHandler;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * Clase principal del lado cliente.
 * @author sergi
 */
public class Cliente {

    // Socket que mantiene la conexión con el servidor.
    public static Socket conection = null;

    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);

        // Solicita al usuario el comando de inicio
        System.out.println("Escribe 'start-conection' para iniciar:");
        String commands = scanner.nextLine();

        switch (commands) {
            case "start-conection":
                // Inicia la conexión pidiendo IP y puerto
                StartConection(scanner);
                break;
            default:
                // Sale si el comando es inválido
                System.out.println("Comando no reconocido. Saliendo.");
                return;
        }

        // Sale de la app si la conexión falló
        if (conection == null) {
            System.err.println("No se pudo establecer la conexión. Saliendo.");
            return;
        }

        // Prepara los manejadores de lectura y escritura
        System.out.println("¡Conectado! Iniciando hilos de lectura y escritura...");
        WriteHandler writer = new WriteHandler(conection);
        ReadHandler reader = new ReadHandler(conection);

        // Inicia los hilos para leer y escribir en el servidor
        Thread writeThread = new Thread(writer);
        Thread readThread = new Thread(reader);

        writeThread.start();
        readThread.start();
    }

    /**
     * Pide al usuario una IP y puerto, e intenta establecer la conexión.
     * Asigna el socket resultante a la variable estática 'conection'.
     */
    public static void StartConection(Scanner scanner) {
        System.out.print("Introduce la IP del servidor ( localhost o 127.0.0.1): ");
        String address = scanner.nextLine();

        System.out.print("Introduce el Puerto del servidor ( 8080): ");
        int port;

        // Valida que el puerto sea un número
        try {
            port = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Puerto inválido. Usando 8080 por defecto.");
            port = 8080;
        }

        // Intenta crear el socket y lo asigna
        try {
            conection = new Socket(address, port);
            System.out.println("Conexión establecida con " + address + ":" + port);
        } catch (IOException ex) {
            // Asegura que la conexión sea nula si falla
            System.err.println("Error al conectar: " + ex.getMessage());
            conection = null;
        }
    }
}