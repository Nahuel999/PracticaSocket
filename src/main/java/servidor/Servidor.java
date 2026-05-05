package servidor;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Servidor {

    static Map<String, ClienteHandler> clientesConectados = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try {
            ServerSocket servidor = new ServerSocket(5000);
            System.out.println("=== SERVIDOR INICIADO EN PUERTO 5000 ===");
            System.out.println("[" + ahora() + "] Esperando clientes...\n");

            while (true) {
                Socket socket = servidor.accept();
                ClienteHandler handler = new ClienteHandler(socket);
                new Thread(handler).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static synchronized String generarNombre(String nombreBase) {
        if (!clientesConectados.containsKey(nombreBase)) {
            return nombreBase;
        }
        int i = 2;
        while (clientesConectados.containsKey(nombreBase + i)) {
            i++;
        }
        return nombreBase + i;
    }

    static void broadcast(String mensaje, String emisor) {
        String texto = "[" + ahora() + "] *ALL " + emisor + ": " + mensaje;
        System.out.println("[BROADCAST] " + texto);
        for (ClienteHandler c : clientesConectados.values()) {
            c.enviar(texto);
        }
    }

    static boolean enviarA(String destino, String mensaje, String emisor) {
        ClienteHandler destinatario = clientesConectados.get(destino);
        if (destinatario == null) return false;
        String texto = "[" + ahora() + "] *" + destino + " (de " + emisor + "): " + mensaje;
        destinatario.enviar(texto);
        System.out.println("[MSG " + emisor + " -> " + destino + "] " + mensaje);
        return true;
    }

    static String ahora() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}


class ClienteHandler implements Runnable {

    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String nombre;

    public ClienteHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // UTF-8 en entrada y salida para evitar signos de pregunta
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            salida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            // 1. Pedir nombre de usuario
            salida.println("Ingresa tu nombre de usuario:");
            String nombreBase = entrada.readLine();
            if (nombreBase == null || nombreBase.isBlank()) nombreBase = "Usuario";
            nombreBase = nombreBase.trim();

            nombre = Servidor.generarNombre(nombreBase);
            Servidor.clientesConectados.put(nombre, this);

            if (!nombre.equals(nombreBase)) {
                salida.println("[SERVIDOR] El nombre '" + nombreBase + "' ya existe. Se te asigno: " + nombre);
            }

            System.out.println("[" + Servidor.ahora() + "] Cliente conectado: " + nombre
                    + " desde " + socket.getInetAddress().getHostAddress());

            // 2. Menu de bienvenida
            enviarMenu();

            // Notificar al resto
            Servidor.broadcast("!" + nombre + " se conecto al chat!", "SERVIDOR");

            // 3. Bucle principal
            String mensaje;
            while ((mensaje = entrada.readLine()) != null) {

                System.out.println("[" + Servidor.ahora() + "] " + nombre + ": " + mensaje);

                if (mensaje.equalsIgnoreCase("SALIR")) {
                    salida.println("[SERVIDOR] Hasta luego, " + nombre + "!");
                    break;

                } else if (mensaje.equalsIgnoreCase("AYUDA")) {
                    enviarMenu();

                } else if (mensaje.equalsIgnoreCase("FECHA")) {
                    String fechaHora = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                    salida.println("[SERVIDOR] Fecha y hora: " + fechaHora);

                } else if (mensaje.startsWith("RESOLVE ")) {
                    resolverExpresion(mensaje.substring(8).trim());

                } else if (mensaje.equalsIgnoreCase("LISTA")) {
                    listarClientes();

                } else if (mensaje.startsWith("*ALL ")) {
                    String texto = mensaje.substring(5).trim();
                    Servidor.broadcast(texto, nombre);

                } else if (mensaje.startsWith("*")) {
                    int espacio = mensaje.indexOf(' ');
                    if (espacio == -1) {
                        salida.println("[SERVIDOR] Formato: *Destino mensaje  o  *Dest1,Dest2 mensaje");
                    } else {
                        String destinos = mensaje.substring(1, espacio);
                        String texto = mensaje.substring(espacio + 1).trim();
                        enviarPrivado(destinos, texto);
                    }

                } else if (mensaje.startsWith("PAL ")) {
                    palindromo(mensaje.substring(4).trim());

                } else if (mensaje.startsWith("MAYUS ")) {
                    salida.println("Resultado: " + mensaje.substring(6).trim().toUpperCase());

                } else if (mensaje.startsWith("MINUS ")) {
                    salida.println("Resultado: " + mensaje.substring(6).trim().toLowerCase());

                } else if (mensaje.startsWith("REV ")) {
                    String t = mensaje.substring(4).trim();
                    salida.println("Resultado: " + new StringBuilder(t).reverse());

                } else if (mensaje.startsWith("PAR ")) {
                    try {
                        int n = Integer.parseInt(mensaje.substring(4).trim());
                        salida.println("Resultado: " + (n % 2 == 0 ? "Es par" : "Es impar"));
                    } catch (NumberFormatException e) {
                        salida.println("[ERROR] Ingresa un numero valido.");
                    }

                } else {
                    salida.println("[SERVIDOR] Comando no reconocido. Escribe AYUDA para ver los comandos.");
                }
            }

        } catch (IOException e) {
            System.out.println("[" + Servidor.ahora() + "] Conexion perdida con: "
                    + (nombre != null ? nombre : "desconocido"));
        } finally {
            desconectar();
        }
    }

    public void enviar(String mensaje) {
        salida.println(mensaje);
    }

    private void enviarMenu() {
        salida.println("+===========================================+");
        salida.println("|     Bienvenido/a al Chat, " + nombre + "!");
        salida.println("+-------------------------------------------+");
        salida.println("|  COMANDOS DISPONIBLES:                    |");
        salida.println("|  FECHA            -> fecha y hora actual  |");
        salida.println("|  LISTA            -> ver clientes online  |");
        salida.println("|  RESOLVE <expr>   -> resolver matematica  |");
        salida.println("|  *ALL <msg>       -> mensaje a todos      |");
        salida.println("|  *Nombre <msg>    -> mensaje privado      |");
        salida.println("|  *N1,N2 <msg>     -> mensaje a varios     |");
        salida.println("|  PAL <texto>      -> detectar palindromo  |");
        salida.println("|  MAYUS <texto>    -> convertir mayusculas |");
        salida.println("|  MINUS <texto>    -> convertir minusculas |");
        salida.println("|  REV <texto>      -> invertir texto       |");
        salida.println("|  PAR <numero>     -> par o impar          |");
        salida.println("|  AYUDA            -> mostrar este menu    |");
        salida.println("|  SALIR            -> desconectarse        |");
        salida.println("+===========================================+");
    }

    private void listarClientes() {
        Set<String> clientes = Servidor.clientesConectados.keySet();
        salida.println("[SERVIDOR] Clientes conectados (" + clientes.size() + "): "
                + String.join(", ", clientes));
    }

    private void resolverExpresion(String expr) {
        try {
            double resultado = evaluar(expr);
            if (resultado == Math.floor(resultado)) {
                salida.println("Resultado: " + (long) resultado);
            } else {
                salida.println("Resultado: " + resultado);
            }
        } catch (Exception e) {
            salida.println("[ERROR] No se pudo resolver la expresion: " + expr);
        }
    }

    private double evaluar(String expr) {
        return new Evaluador(expr.replaceAll("\\s+", "")).parse();
    }

    private void palindromo(String texto) {
        String inv = new StringBuilder(texto).reverse().toString();
        if (texto.equalsIgnoreCase(inv)) {
            salida.println("Resultado: '" + texto + "' ES palindromo");
        } else {
            salida.println("Resultado: '" + texto + "' NO es palindromo");
        }
    }

    private void enviarPrivado(String destinos, String texto) {
        String[] lista = destinos.split(",");
        List<String> noEncontrados = new ArrayList<>();

        for (String dest : lista) {
            dest = dest.trim();
            if (dest.equalsIgnoreCase(nombre)) {
                salida.println("[SERVIDOR] No podes mandarte un mensaje a vos mismo.");
                continue;
            }
            boolean ok = Servidor.enviarA(dest, texto, nombre);
            if (!ok) noEncontrados.add(dest);
        }

        List<String> encontrados = new ArrayList<>(Arrays.asList(lista));
        encontrados.removeAll(noEncontrados);
        if (!encontrados.isEmpty()) {
            salida.println("[ENVIADO a " + String.join(", ", encontrados) + "] " + texto);
        }
        if (!noEncontrados.isEmpty()) {
            salida.println("[SERVIDOR] No se encontraron los siguientes usuarios: "
                    + String.join(", ", noEncontrados)
                    + ". El mensaje fue enviado a los demas disponibles.");
        }
    }

    private void desconectar() {
        if (nombre != null) {
            Servidor.clientesConectados.remove(nombre);
            System.out.println("[" + Servidor.ahora() + "] " + nombre + " se desconecto.");
            Servidor.broadcast(nombre + " se desconecto.", "SERVIDOR");
        }
        try { if (socket != null) socket.close(); } catch (IOException e) { /* ignorar */ }
    }
}


class Evaluador {
    private final String expr;
    private int pos = -1, ch;

    Evaluador(String expr) { this.expr = expr; }

    void nextChar() { ch = (++pos < expr.length()) ? expr.charAt(pos) : -1; }

    boolean eat(int c) {
        while (ch == ' ') nextChar();
        if (ch == c) { nextChar(); return true; }
        return false;
    }

    double parse() {
        nextChar();
        double v = parseExpr();
        if (pos < expr.length()) throw new RuntimeException("Expresion invalida");
        return v;
    }

    double parseExpr() {
        double v = parseTerm();
        for (;;) {
            if      (eat('+')) v += parseTerm();
            else if (eat('-')) v -= parseTerm();
            else return v;
        }
    }

    double parseTerm() {
        double v = parseFactor();
        for (;;) {
            if      (eat('*')) v *= parseFactor();
            else if (eat('/')) v /= parseFactor();
            else return v;
        }
    }

    double parseFactor() {
        if (eat('+')) return parseFactor();
        if (eat('-')) return -parseFactor();
        double v;
        int start = pos;
        if (eat('(')) { v = parseExpr(); eat(')'); }
        else {
            while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
            v = Double.parseDouble(expr.substring(start, pos));
        }
        if (eat('^')) v = Math.pow(v, parseFactor());
        return v;
    }
}