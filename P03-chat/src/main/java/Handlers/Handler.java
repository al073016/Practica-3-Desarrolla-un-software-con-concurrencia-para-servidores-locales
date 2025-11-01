
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *
 * @author sergi
 */
public class Handler {

    protected Socket socket;
    protected BufferedReader in;
    protected PrintWriter out;

    public Handler(Socket socket, String IP){

    }

    public Handler(Socket socket) {
        this.socket = socket;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

        } catch (IOException ex) {
            System.getLogger(Handler.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

    }

    public void dismiss() {
        try {

            socket.close();
            System.exit(0);
        } catch (IOException ex) {
            System.getLogger(WriteHandler.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

}
