package cliente;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ClienteSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaLogin().setVisible(true));
    }
}

// ═══════════════════════════════════════════════════════════
//  VENTANA DE LOGIN
// ═══════════════════════════════════════════════════════════
class VentanaLogin extends JFrame {

    private JTextField txtNombre;
    private JLabel lblError;
    private JButton btnConectar;

    public VentanaLogin() {
        setTitle("Chat - Conectarse");
        setSize(380, 290);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));
        panel.setBackground(Color.WHITE);

        JLabel titulo = new JLabel("Chat con Sockets");
        titulo.setFont(new Font("Arial", Font.BOLD, 20));
        titulo.setForeground(new Color(28, 78, 128));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitulo = new JLabel("Ingresa tu nombre de usuario");
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitulo.setForeground(Color.GRAY);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel reglas = new JLabel("(3-15 chars, solo letras y numeros, sin espacios)");
        reglas.setFont(new Font("Arial", Font.PLAIN, 10));
        reglas.setForeground(Color.GRAY);
        reglas.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtNombre = new JTextField();
        txtNombre.setFont(new Font("Arial", Font.PLAIN, 14));
        txtNombre.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtNombre.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180)),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        lblError = new JLabel(" ");
        lblError.setFont(new Font("Arial", Font.PLAIN, 11));
        lblError.setForeground(Color.RED);
        lblError.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnConectar = new JButton("Conectarse");
        btnConectar.setFont(new Font("Arial", Font.BOLD, 13));
        btnConectar.setBackground(new Color(28, 78, 128));
        btnConectar.setForeground(Color.WHITE);
        btnConectar.setFocusPainted(false);
        btnConectar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnConectar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnConectar.addActionListener(e -> intentarConectar());
        txtNombre.addActionListener(e -> intentarConectar());

        panel.add(titulo);
        panel.add(Box.createVerticalStrut(4));
        panel.add(subtitulo);
        panel.add(Box.createVerticalStrut(2));
        panel.add(reglas);
        panel.add(Box.createVerticalStrut(16));
        panel.add(txtNombre);
        panel.add(Box.createVerticalStrut(4));
        panel.add(lblError);
        panel.add(Box.createVerticalStrut(10));
        panel.add(btnConectar);

        add(panel);
        txtNombre.requestFocusInWindow();
    }

    private void intentarConectar() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty())              { mostrarError("El nombre no puede estar vacio."); return; }
        if (nombre.length() < 3)           { mostrarError("Minimo 3 caracteres."); return; }
        if (nombre.length() > 15)          { mostrarError("Maximo 15 caracteres."); return; }
        if (nombre.contains(" "))          { mostrarError("El nombre no puede tener espacios."); return; }
        if (!nombre.matches("[a-zA-Z0-9]+")) { mostrarError("Solo letras y numeros, sin caracteres especiales."); return; }

        try {
            Socket socket = new Socket("localhost", 5000);
            btnConectar.setEnabled(false);
            lblError.setText(" ");
            VentanaChat chat = new VentanaChat(socket, nombre);
            chat.setVisible(true);
            dispose();
        } catch (ConnectException ex) {
            mostrarError("No se pudo conectar. El servidor esta iniciado?");
        } catch (IOException ex) {
            mostrarError("Error de conexion: " + ex.getMessage());
        }
    }

    private void mostrarError(String msg) {
        lblError.setText(msg);
        txtNombre.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.RED),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }
}


// ═══════════════════════════════════════════════════════════
//  VENTANA DE CHAT
// ═══════════════════════════════════════════════════════════
class VentanaChat extends JFrame {

    private JTextPane areaChat;
    private JTextField txtMensaje;
    private JTextField txtDestinatario;
    private JTextField txtBusqueda;
    private JList<String> listaUsuarios;
    private DefaultListModel<String> modeloLista;
    private JLabel lblUsuario;
    private JLabel lblContadorChars;
    private JLabel lblContadorUsuarios;
    private JPanel header;

    private PrintWriter salida;
    private BufferedReader entrada;
    private Socket socket;
    private String nombreUsuario;
    private String horaConexion;
    private boolean temaOscuro = false;

    // Colores
    private Color COLOR_PROPIO    = new Color(0, 120, 212);
    private Color COLOR_SERVIDOR  = new Color(120, 120, 120);
    private Color COLOR_PRIVADO   = new Color(160, 0, 160);
    private Color COLOR_BROADCAST = new Color(0, 140, 70);
    private Color COLOR_OTRO      = new Color(40, 40, 40);
    private Color COLOR_FONDO     = new Color(245, 245, 245);
    private Color COLOR_CHAT_BG   = Color.WHITE;
    private Color COLOR_HEADER    = new Color(28, 78, 128);

    private List<String[]> todosMensajes = new ArrayList<>();

    public VentanaChat(Socket socket, String nombrePedido) throws IOException {
        this.socket = socket;
        this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.salida  = new PrintWriter(socket.getOutputStream(), true);
        this.horaConexion = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        setTitle("Chat");
        setSize(880, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                salida.println("SALIR");
                try { socket.close(); } catch (IOException ex) {}
                dispose(); System.exit(0);
            }
        });

        construirUI();
        salida.println(nombrePedido);
        new Thread(this::recibirMensajes).start();
    }

    private void construirUI() {
        setLayout(new BorderLayout(5, 5));
        getContentPane().setBackground(COLOR_FONDO);

        // HEADER
        header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_HEADER);
        header.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel titulo = new JLabel("Chat con Sockets");
        titulo.setFont(new Font("Arial", Font.BOLD, 16));
        titulo.setForeground(Color.WHITE);

        JButton btnTema = new JButton("Tema oscuro");
        btnTema.setFont(new Font("Arial", Font.PLAIN, 11));
        btnTema.setFocusPainted(false);
        btnTema.setBackground(new Color(50, 100, 160));
        btnTema.setForeground(Color.WHITE);
        btnTema.setBorder(new EmptyBorder(4, 10, 4, 10));
        btnTema.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTema.addActionListener(e -> {
            temaOscuro = !temaOscuro;
            btnTema.setText(temaOscuro ? "Tema claro" : "Tema oscuro");
            aplicarTema();
        });

        JPanel headerIzq = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerIzq.setOpaque(false);
        headerIzq.add(titulo);
        headerIzq.add(btnTema);

        JPanel headerDer = new JPanel(new GridLayout(2, 1));
        headerDer.setOpaque(false);
        lblUsuario = new JLabel("Conectando...", SwingConstants.RIGHT);
        lblUsuario.setFont(new Font("Arial", Font.PLAIN, 12));
        lblUsuario.setForeground(new Color(180, 210, 240));
        lblContadorUsuarios = new JLabel("", SwingConstants.RIGHT);
        lblContadorUsuarios.setFont(new Font("Arial", Font.PLAIN, 11));
        lblContadorUsuarios.setForeground(new Color(150, 220, 150));
        headerDer.add(lblUsuario);
        headerDer.add(lblContadorUsuarios);

        header.add(headerIzq, BorderLayout.WEST);
        header.add(headerDer, BorderLayout.EAST);

        // AREA CHAT
        areaChat = new JTextPane();
        areaChat.setEditable(false);
        areaChat.setFont(new Font("Monospaced", Font.PLAIN, 13));
        areaChat.setBackground(COLOR_CHAT_BG);
        JScrollPane scrollChat = new JScrollPane(areaChat);
        scrollChat.setBorder(BorderFactory.createEmptyBorder());

        // PANEL DERECHO
        JPanel panelDerecho = new JPanel(new BorderLayout(0, 6));
        panelDerecho.setPreferredSize(new Dimension(195, 0));
        panelDerecho.setBackground(COLOR_FONDO);
        panelDerecho.setBorder(new EmptyBorder(0, 5, 0, 5));

        JLabel lblConectados = new JLabel("Conectados:");
        lblConectados.setFont(new Font("Arial", Font.BOLD, 12));
        lblConectados.setForeground(COLOR_HEADER);

        JPanel panelBusqueda = new JPanel(new BorderLayout(3, 0));
        panelBusqueda.setBackground(COLOR_FONDO);
        JLabel lblBuscar = new JLabel("Buscar:");
        lblBuscar.setFont(new Font("Arial", Font.PLAIN, 11));
        txtBusqueda = new JTextField();
        txtBusqueda.setFont(new Font("Arial", Font.PLAIN, 11));
        txtBusqueda.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { filtrarChat(txtBusqueda.getText().trim()); }
        });
        panelBusqueda.add(lblBuscar, BorderLayout.WEST);
        panelBusqueda.add(txtBusqueda, BorderLayout.CENTER);

        modeloLista = new DefaultListModel<>();
        listaUsuarios = new JList<>(modeloLista);
        listaUsuarios.setFont(new Font("Arial", Font.PLAIN, 12));
        listaUsuarios.setBackground(Color.WHITE);
        listaUsuarios.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        listaUsuarios.addListSelectionListener(e -> {
            String sel = listaUsuarios.getSelectedValue();
            if (sel != null) {
                String n = sel.contains(" (") ? sel.substring(0, sel.indexOf(" (")) : sel;
                if (!n.equals(nombreUsuario)) txtDestinatario.setText(n);
            }
        });

        JPanel panelBotones = new JPanel(new GridLayout(0, 1, 0, 3));
        panelBotones.setBackground(COLOR_FONDO);
        panelBotones.add(botonRapido("FECHA",     () -> salida.println("FECHA")));
        panelBotones.add(botonRapido("LISTA",     () -> salida.println("LISTA")));
        panelBotones.add(botonRapido("AYUDA",     () -> salida.println("AYUDA")));
        panelBotones.add(botonRapido("RESOLVE",   () -> pedirExpresion()));
        panelBotones.add(botonRapido("DADO",      () -> salida.println("DADO")));
        panelBotones.add(botonRapido("COUNT",     () -> salida.println("COUNT")));
        panelBotones.add(botonRapido("HISTORIAL", () -> salida.println("HISTORIAL")));
        panelBotones.add(botonRapido("TRADUCIR",  () -> pedirTraduccion()));
        panelBotones.add(botonRapido("MAYUS",     () -> pedirTexto("MAYUS", "Texto a convertir a MAYUSCULAS:")));
        panelBotones.add(botonRapido("MINUS",     () -> pedirTexto("MINUS", "Texto a convertir a minusculas:")));
        panelBotones.add(botonRapido("REV",       () -> pedirTexto("REV",   "Texto a invertir:")));
        panelBotones.add(botonRapido("PAL",       () -> pedirTexto("PAL",   "Texto a verificar palindromo:")));
        panelBotones.add(botonRapido("PAR",       () -> pedirTexto("PAR",   "Numero a verificar par/impar:")));
        JButton btnLimpiar = botonRapido("Limpiar", () -> { areaChat.setText(""); todosMensajes.clear(); });
        btnLimpiar.setForeground(new Color(180, 60, 60));
        panelBotones.add(btnLimpiar);

        JPanel panelNorteDer = new JPanel(new BorderLayout(0, 4));
        panelNorteDer.setBackground(COLOR_FONDO);
        panelNorteDer.add(lblConectados, BorderLayout.NORTH);
        panelNorteDer.add(panelBusqueda, BorderLayout.CENTER);

        panelDerecho.add(panelNorteDer, BorderLayout.NORTH);
        panelDerecho.add(new JScrollPane(listaUsuarios), BorderLayout.CENTER);
        panelDerecho.add(panelBotones, BorderLayout.SOUTH);

        // PANEL INFERIOR
        JPanel panelInferior = new JPanel(new BorderLayout(5, 5));
        panelInferior.setBorder(new EmptyBorder(5, 5, 5, 5));
        panelInferior.setBackground(COLOR_FONDO);

        JPanel filaDestino = new JPanel(new BorderLayout(5, 0));
        filaDestino.setBackground(COLOR_FONDO);
        JLabel lblDest = new JLabel("Para:");
        lblDest.setFont(new Font("Arial", Font.PLAIN, 12));
        txtDestinatario = new JTextField();
        txtDestinatario.setFont(new Font("Arial", Font.PLAIN, 12));
        txtDestinatario.setToolTipText("Vacio=comando, ALL=todos, Nombre=privado");
        filaDestino.add(lblDest, BorderLayout.WEST);
        filaDestino.add(txtDestinatario, BorderLayout.CENTER);

        JPanel filaMensaje = new JPanel(new BorderLayout(5, 0));
        filaMensaje.setBackground(COLOR_FONDO);

        txtMensaje = new JTextField();
        txtMensaje.setFont(new Font("Arial", Font.PLAIN, 13));
        txtMensaje.addActionListener(e -> enviarMensaje());

        lblContadorChars = new JLabel("0/200");
        lblContadorChars.setFont(new Font("Arial", Font.PLAIN, 10));
        lblContadorChars.setForeground(Color.GRAY);
        txtMensaje.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                int len = txtMensaje.getText().length();
                lblContadorChars.setText(len + "/200");
                lblContadorChars.setForeground(len > 180 ? Color.RED : Color.GRAY);
            }
        });

        JButton btnEnviar = new JButton("Enviar");
        btnEnviar.setFont(new Font("Arial", Font.BOLD, 13));
        btnEnviar.setBackground(COLOR_HEADER);
        btnEnviar.setForeground(Color.WHITE);
        btnEnviar.setFocusPainted(false);
        btnEnviar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEnviar.addActionListener(e -> enviarMensaje());

        JButton btnSalir = new JButton("Salir");
        btnSalir.setFont(new Font("Arial", Font.PLAIN, 12));
        btnSalir.setForeground(Color.RED);
        btnSalir.setFocusPainted(false);
        btnSalir.addActionListener(e -> {
            salida.println("SALIR");
            try { socket.close(); } catch (IOException ex) {}
            dispose(); System.exit(0);
        });

        JPanel botonesEnvio = new JPanel(new GridLayout(1, 2, 4, 0));
        botonesEnvio.setBackground(COLOR_FONDO);
        botonesEnvio.add(btnEnviar);
        botonesEnvio.add(btnSalir);

        filaMensaje.add(lblContadorChars, BorderLayout.WEST);
        filaMensaje.add(txtMensaje, BorderLayout.CENTER);
        filaMensaje.add(botonesEnvio, BorderLayout.EAST);

        panelInferior.add(filaDestino, BorderLayout.NORTH);
        panelInferior.add(filaMensaje, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(scrollChat, BorderLayout.CENTER);
        add(panelDerecho, BorderLayout.EAST);
        add(panelInferior, BorderLayout.SOUTH);
    }

    private void aplicarTema() {
        if (temaOscuro) {
            COLOR_FONDO   = new Color(30, 30, 30);
            COLOR_CHAT_BG = new Color(20, 20, 20);
            COLOR_HEADER  = new Color(15, 50, 90);
            COLOR_OTRO    = new Color(210, 210, 210);
            COLOR_SERVIDOR= new Color(160, 160, 160);
        } else {
            COLOR_FONDO   = new Color(245, 245, 245);
            COLOR_CHAT_BG = Color.WHITE;
            COLOR_HEADER  = new Color(28, 78, 128);
            COLOR_OTRO    = new Color(40, 40, 40);
            COLOR_SERVIDOR= new Color(120, 120, 120);
        }
        areaChat.setBackground(COLOR_CHAT_BG);
        getContentPane().setBackground(COLOR_FONDO);
        header.setBackground(COLOR_HEADER);
        repaint();
    }

    private JButton botonRapido(String texto, Runnable accion) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Arial", Font.PLAIN, 11));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> accion.run());
        return btn;
    }

    private void pedirExpresion() {
        String expr = JOptionPane.showInputDialog(this,
            "Ingresa la expresion matematica:", "RESOLVE", JOptionPane.QUESTION_MESSAGE);
        if (expr != null && !expr.trim().isEmpty()) salida.println("RESOLVE " + expr.trim());
    }

    private void pedirTraduccion() {
        String texto = JOptionPane.showInputDialog(this,
            "Ingresa el texto a traducir al ingles:", "TRADUCIR", JOptionPane.QUESTION_MESSAGE);
        if (texto != null && !texto.trim().isEmpty()) salida.println("TRADUCIR " + texto.trim());
    }

    private void pedirTexto(String comando, String descripcion) {
        String texto = JOptionPane.showInputDialog(this,
            descripcion, comando, JOptionPane.QUESTION_MESSAGE);
        if (texto != null && !texto.trim().isEmpty()) salida.println(comando + " " + texto.trim());
    }

    private void enviarMensaje() {
        String msg  = txtMensaje.getText().trim();
        String dest = txtDestinatario.getText().trim();
        if (msg.isEmpty()) return;
        if (msg.length() > 200) {
            JOptionPane.showMessageDialog(this, "El mensaje no puede superar los 200 caracteres.",
                "Mensaje muy largo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String cmd;
        if (dest.isEmpty())                  cmd = msg;
        else if (dest.equalsIgnoreCase("ALL")) cmd = "*ALL " + msg;
        else                                   cmd = "*" + dest + " " + msg;
        salida.println(cmd);
        txtMensaje.setText("");
        lblContadorChars.setText("0/200");
    }

    private void recibirMensajes() {
        try {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                final String msg = linea;
                SwingUtilities.invokeLater(() -> procesarMensaje(msg));
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> agregarMensaje("[Desconectado del servidor]", COLOR_SERVIDOR));
        }
    }

    private void procesarMensaje(String msg) {
        // Detectar nombre asignado
        if (msg.contains("Bienvenido/a al Chat,")) {
            int ini = msg.indexOf("Chat,") + 6;
            int fin = msg.indexOf("!");
            if (ini > 0 && fin > ini) {
                nombreUsuario = msg.substring(ini, fin).trim();
                setTitle("Chat - " + nombreUsuario);
                lblUsuario.setText("Usuario: " + nombreUsuario + "  |  Conectado: " + horaConexion);
            }
        }

        // Actualizar lista
        if (msg.startsWith("[SERVIDOR] Clientes conectados")) {
            actualizarLista(msg);
            agregarMensaje(msg, COLOR_SERVIDOR);
            return;
        }

        // Mensaje privado recibido → sonido + popup
        if (msg.contains("(de ") && nombreUsuario != null && !msg.contains("(de " + nombreUsuario + ")")) {
            Toolkit.getDefaultToolkit().beep();
            if (!isFocused()) {
                String tituloOriginal = getTitle();
                setTitle("*** MENSAJE PRIVADO *** - " + tituloOriginal);
                JOptionPane.showMessageDialog(this,
                    "Nuevo mensaje privado:\n\n" + msg,
                    "Mensaje privado recibido", JOptionPane.INFORMATION_MESSAGE);
                setTitle(tituloOriginal);
            }
            agregarMensaje(msg, COLOR_PRIVADO);
            return;
        }

        // Colorear
        if (msg.startsWith("[SERVIDOR]") || msg.startsWith("[ERROR]") || msg.startsWith("[ENVIADO")
                || msg.startsWith("=") || msg.startsWith("-") || msg.startsWith(" ")) {
            agregarMensaje(msg, COLOR_SERVIDOR);
        } else if (msg.contains("*ALL")) {
            agregarMensaje(msg, COLOR_BROADCAST);
        } else if (msg.contains("(de " + nombreUsuario + ")")) {
            agregarMensaje(msg, COLOR_PROPIO);
        } else {
            agregarMensaje(msg, COLOR_OTRO);
        }
    }

    private void agregarMensaje(String msg, Color color) {
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        todosMensajes.add(new String[]{hora, msg,
            String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue())});
        agregarMensajeSinGuardar("[" + hora + "] " + msg, color);
    }

    private void actualizarLista(String msg) {
        int idx = msg.indexOf("): ");
        if (idx == -1) return;
        String[] usuarios = msg.substring(idx + 3).split(", ");
        modeloLista.clear();
        for (String u : usuarios) {
            String n = u.trim();
            if (n.equals(nombreUsuario)) modeloLista.addElement(n + " (vos, " + horaConexion + ")");
            else modeloLista.addElement(n);
        }
        lblContadorUsuarios.setText(usuarios.length + " usuario(s) online");
    }

    private void filtrarChat(String filtro) {
        areaChat.setText("");
        for (String[] m : todosMensajes) {
            if (filtro.isEmpty() || m[1].toLowerCase().contains(filtro.toLowerCase())) {
                agregarMensajeSinGuardar("[" + m[0] + "] " + m[1], Color.decode(m[2]));
            }
        }
    }

    private void agregarMensajeSinGuardar(String msg, Color color) {
        javax.swing.text.StyledDocument doc = areaChat.getStyledDocument();
        javax.swing.text.Style style = areaChat.addStyle("s", null);
        javax.swing.text.StyleConstants.setForeground(style, color);
        javax.swing.text.StyleConstants.setFontFamily(style, "Monospaced");
        javax.swing.text.StyleConstants.setFontSize(style, 13);
        try {
            doc.insertString(doc.getLength(), msg + "\n", style);
            areaChat.setCaretPosition(doc.getLength());
        } catch (Exception e) { e.printStackTrace(); }
    }
}