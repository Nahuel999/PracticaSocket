package cliente;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 5000);

            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            PrintWriter salida = new PrintWriter(
                    socket.getOutputStream(), true
            );

            // ── Hilo receptor: muestra mensajes del servidor en cualquier momento ──
            Thread receptor = new Thread(() -> {
                try {
                    String mensaje;
                    while ((mensaje = entrada.readLine()) != null) {
                        System.out.println(mensaje);
                    }
                } catch (IOException e) {
                    System.out.println("[CLIENTE] Conexión con el servidor cerrada.");
                }
            });
            receptor.setDaemon(true);
            receptor.start();

            // ── Hilo principal: lectura desde consola y envío al servidor ──────────
            Scanner scanner = new Scanner(System.in);
            String mensaje;

            while (true) {
                mensaje = scanner.nextLine();

                if (mensaje == null || mensaje.isBlank()) continue;

                salida.println(mensaje);

                if (mensaje.equalsIgnoreCase("SALIR")) {
                    break;
                }
            }

            socket.close();
            System.out.println("[CLIENTE] Desconectado.");

        } catch (ConnectException e) {
            System.out.println("[ERROR] No se pudo conectar al servidor. ¿Está iniciado?");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}