package Handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet; // Importado
import java.util.Set;

/**
 * Maneja toda la lógica para un cliente individual conectado al servidor.
 * Cada instancia de esta clase se ejecuta en su propio hilo.
 *
 * @author sergi
 */
public class ClientHandler implements Runnable {

    // --- Variables de Instancia ---
    private final Socket socket;                 // Conexión de este cliente
    private final Set<ClientHandler> clients;  // Lista compartida de *todos* los clientes
    private final Set<String> blockList;       // Lista compartida de IPs bloqueadas
    private String username;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isAdmin = false;             // Flag de permisos de administrador

    // Lista personal de usuarios que este cliente no quiere leer
    private Set<String> ignoredUsers = new HashSet<>();

    /**
     * Constructor para el manejador de cliente.
     */
    public ClientHandler(Socket clientSocket, Set<ClientHandler> clients, Set<String> blockList) {
        this.socket = clientSocket;
        this.clients = clients;
        this.blockList = blockList;
    }

    /**
     * Metodo principal del hilo. Contiene el ciclo de vida del cliente.
     */
    @Override
    public void run() {
        try {
            // Inicializa los flujos de entrada y salida (true = autoFlush)
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // --- 1. PROCESO DE OBTENER USERNAME ---
            sendMessage("Conexión establecida. Introduce tu nombre de usuario:");
            this.username = in.readLine();

            // Asigna un nombre por defecto si está vacío
            if (this.username == null || this.username.trim().isEmpty()) {
                this.username = "Usuario" + (int)(Math.random() * 1000);
            }

            // Comprueba si el usuario es un administrador
            if (this.username.equalsIgnoreCase("admin")) {
                this.isAdmin = true;
                sendMessage("Nivel de administrador concedido.");
            }

            System.err.println(username + " se ha unido al chat");

            // Notifica a todos los demás que un nuevo usuario se ha unido
            broadcastMessage( username + " se ha unido al chat.", this, null); // null = remitente es el Sistema
            sendMessage(" ¡Bienvenido " + this.username + "! Escribe /help para ver los comandos.");


            // --- 2. BUCLE PRINCIPAL DE MENSAJES ---
            // Lee líneas del cliente hasta que se desconecte (readLine() == null)
            String inputMessage;
            while ((inputMessage = in.readLine()) != null) {

                // --- INICIO DEL PARSER DE COMANDOS ---
                if (inputMessage.equalsIgnoreCase("/exit")) {
                    break; // Sale del bucle while

                } else if (inputMessage.startsWith("/changename ")) {
                    String[] parts = inputMessage.split(" ", 2);
                    if (parts.length == 2 && !parts[1].trim().isEmpty()) {
                        handleChangeUserName(parts[1].trim());
                    } else {
                        sendMessage("/changename [nuevo_nombre]");
                    }

                } else if (inputMessage.startsWith("/w ")) {
                    String[] parts = inputMessage.split(" ", 3);
                    if (parts.length == 3) {
                        handlePrivateMessage(parts[1], parts[2]);
                    } else {
                        sendMessage("/w [usuario] [mensaje]");
                    }

                } else if (inputMessage.startsWith("/ignore ")) {
                    String[] parts = inputMessage.split(" ", 2);
                    if (parts.length == 2) {
                        handleIgnoreUser(parts[1].trim());
                    } else {
                        sendMessage("/ignore [usuario]");
                    }
                } else if (inputMessage.startsWith("/unignore ")) {
                    String[] parts = inputMessage.split(" ", 2);
                    if (parts.length == 2) {
                        handleUnignoreUser(parts[1].trim());
                    } else {
                        sendMessage("/unignore [usuario]");
                    }

                } else if (inputMessage.startsWith("/block ")) {
                    // Solo permite /block si es admin
                    if (isAdmin) {
                        String[] parts = inputMessage.split(" ", 2);
                        if (parts.length == 2) {
                            handleBlockUser(parts[1]);
                        } else {
                            sendMessage(" /block [usuario]");
                        }
                    } else {
                        sendMessage("No tienes permisos para usar este comando.");
                    }

                } else if (inputMessage.equalsIgnoreCase("/help")) {
                    handleHelpCommand();

                } else {
                    // Si no es un comando, es un mensaje global
                    handleGlobalMessage(inputMessage);
                }
                // --- FIN DEL PARSER DE COMANDOS ---
            }

        } catch (IOException ex) {
            // Captura excepciones si el cliente se desconecta abruptamente
            System.out.println("Cliente desconectado (Error): " + ex.getMessage());
        } finally {
            // --- 3. PROCESO DE LIMPIEZA ---
            // Se ejecuta siempre (al salir con /exit o por un error)
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.getLogger(ClientHandler.class.getName()).log(System.Logger.Level.ERROR, "Error al cerrar socket", e);
            }

            // Elimina al cliente de la lista compartida
            clients.remove(this);
            if (this.username != null) {
                // Notifica al servidor y a los demás usuarios
                System.err.println(this.username + " ha abandonado el chat.");
                broadcastMessage(this.username + " ha abandonado el chat.", this, null);
            }
        }
    }

    // --- MÉTODOS DE ACCIÓN (Manejo de Comandos) ---

    /**
     * Cambia el nombre de usuario de este cliente.
     */
    public void handleChangeUserName(String newName) {
        // Revisa si el nombre ya está en uso
        boolean nameExists = false;
        for (ClientHandler client : clients) {
            if (client.username != null && client.username.equalsIgnoreCase(newName)) {
                nameExists = true;
                break;
            }
        }

        if (nameExists) {
            sendMessage("Error: El nombre '" + newName + "' ya está en uso.");
        } else {
            // Actualiza el nombre y notifica a todos
            String oldName = this.username;
            this.username = newName;
            sendMessage(" Tu nombre ha sido cambiado a: " + newName);
            broadcastMessage(oldName + " ahora es " + newName, this, null);
        }
    }

    /**
     * Envía un mensaje privado a un usuario específico.
     */
    public void handlePrivateMessage(String targetUsername, String message) {
        ClientHandler targetClient = null;

        // Busca al cliente destinatario
        for (ClientHandler client : clients) {
            if (client.username != null && client.username.equalsIgnoreCase(targetUsername)) {
                targetClient = client;
                break;
            }
        }

        if (targetClient != null) {
            // No se puede auto-enviar
            if (targetClient == this) {
                sendMessage("No puedes enviarte mensajes privados a ti mismo.");
                return;
            }

            // FILTRO DE IGNORADOS: Comprueba si el destinatario está ignorando al remitente
            if (targetClient.ignoredUsers.contains(this.username.toLowerCase())) {
                sendMessage( targetClient.username + " no puede recibir tus mensajes (te ha ignorado).");
                return; // No envía el mensaje
            }

            // Envía el mensaje al destinatario y la confirmación al remitente
            targetClient.sendMessage("(Privado de " + this.username + "): " + message);
            sendMessage("(Mensaje a " + targetClient.username + "): " + message);

        } else {
            sendMessage(" Error: Usuario '" + targetUsername + "' no encontrado.");
        }
    }

    /**
     * Procesa un mensaje global (broadcast) de este usuario.
     */
    public void handleGlobalMessage(String message) {
        String globalMessage = this.username + ": " + message;
        // Llama a broadcastMessage especificando 'this' como el remitente para el filtro
        broadcastMessage(globalMessage, null, this);
    }


    // --- MÉTODOS AUXILIARES ---

    /**
     * Envía un mensaje solo a ESTE cliente.
     */
    public void sendMessage(String message) {
        out.println(message); // out tiene autoFlush
    }

    /**
     * Envía un mensaje a todos los clientes, aplicando filtros.
     * 'excludeUser' no recibe el mensaje (usado para no-eco).
     * 'sender' se usa para el filtro de ignorados.
     */
    public void broadcastMessage(String message, ClientHandler excludeUser, ClientHandler sender) {
        for (ClientHandler client : clients) {
            // 1. Omitir al usuario excluido
            if (client == excludeUser) {
                continue;
            }

            // 2. FILTRO DE IGNORADOS: Si hay remitente Y el cliente actual lo ignora...
            if (sender != null && client.ignoredUsers.contains(sender.username.toLowerCase())) {
                continue; // ...no enviar el mensaje.
            }

            // 3. Enviar mensaje
            client.sendMessage(message);
        }
    }

    /**
     * (Admin) Bloquea la IP de un usuario y lo expulsa.
     */
    public void handleBlockUser(String targetUsername) {
        ClientHandler targetClient = null;

        // Busca al cliente a bloquear
        for (ClientHandler client : clients) {
            if (client.username != null && client.username.equalsIgnoreCase(targetUsername)) {
                targetClient = client;
                break;
            }
        }

        if (targetClient == this) {
            sendMessage("No te puedes bloquear a ti mismo.");
            return;
        }

        if (targetClient != null) {
            // Obtiene la IP y la añade a la lista de bloqueo compartida
            String targetIP = targetClient.socket.getInetAddress().getHostAddress();
            blockList.add(targetIP);

            System.err.println("El usuario " + targetClient.username + " (IP: " + targetIP + ") ha sido añadido a la blocklist por " + this.username);

            // Notifica al usuario y cierra su socket para expulsarlo
            targetClient.sendMessage(" Has sido bloqueado y desconectado por un administrador.");
            try {
                targetClient.socket.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar el socket del usuario bloqueado: " + e.getMessage());
            }

            sendMessage(" El usuario " + targetUsername + " (IP: " + targetIP + ") ha sido bloqueado y expulsado.");

        } else {
            sendMessage("Error: Usuario '" + targetUsername + "' no encontrado.");
        }
    }

    /**
     * Envía la lista de comandos disponibles a este cliente.
     */
    public void handleHelpCommand() {
        sendMessage(" --- LISTA DE COMANDOS DISPONIBLES ---");
        sendMessage(" /help                 - Muestra esta lista de ayuda.");
        sendMessage(" /changename [nuevo]   - Cambia tu nombre de usuario.");
        sendMessage(" /w [usuario] [msg]    - Envía un mensaje privado a [usuario].");
        sendMessage(" /ignore [usuario]     - Oculta todos los mensajes de [usuario].");
        sendMessage(" /unignore [usuario]   - Vuelve a mostrar los mensajes de [usuario].");
        sendMessage(" (cualquier texto)     - Envía un mensaje global a todos.");
        sendMessage(" /exit                 - Te desconecta del chat.");

        // Muestra comandos de admin solo si tiene permisos
        if (isAdmin) {
            sendMessage("--- COMANDOS DE ADMINISTRADOR ---");
            sendMessage(" /block [usuario]      - Bloquea la IP del usuario y lo expulsa.");
        }
        sendMessage("-----------------------------------------");
    }

    /**
     * Añade un usuario a la lista personal de ignorados de este cliente.
     */
    public void handleIgnoreUser(String targetUsername) {
        if (targetUsername.equalsIgnoreCase(this.username)) {
            sendMessage(" No puedes ignorarte a ti mismo.");
            return;
        }

        // Añade el nombre a la lista (en minúsculas para ser case-insensitive)
        ignoredUsers.add(targetUsername.toLowerCase());
        sendMessage("Ahora estás ignorando a '" + targetUsername + "'.");
    }

    /**
     * Quita a un usuario de la lista personal de ignorados.
     */
    public void handleUnignoreUser(String targetUsername) {
        // Intenta quitar el nombre (en minúsculas)
        if (ignoredUsers.remove(targetUsername.toLowerCase())) {
            sendMessage("Ya no estás ignorando a '" + targetUsername + "'.");
        } else {
            sendMessage("No estabas ignorando a '" + targetUsername + "'.");
        }
    }
}