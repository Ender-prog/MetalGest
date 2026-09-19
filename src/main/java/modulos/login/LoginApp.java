package modulos.login;

import modulos.admin.AdminPanel;
import modulos.cliente.PruebaGUI;
import modulos.common.ApiClient;
import modulos.common.JsonUtil;
import modulos.produccion.ProductionPanel;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoginApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginApp::showLogin);
    }

    private static void showLogin() {
        JTextField userField = new JTextField(14);
        JPasswordField passField = new JPasswordField(14);

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("Usuario"));
        panel.add(userField);
        panel.add(new JLabel("Contrasena"));
        panel.add(passField);

        int result = JOptionPane.showConfirmDialog(null, panel, "MetalGest - Ingreso", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Completa usuario y contrasena.", "MetalGest", JOptionPane.WARNING_MESSAGE);
            showLogin();
            return;
        }

        try {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("usuario", user);
            data.put("contrasena", pass);
            String response = ApiClient.post("login", data);
            if (!ApiClient.isSuccess(response)) {
                JOptionPane.showMessageDialog(null, "Credenciales incorrectas.", "MetalGest", JOptionPane.ERROR_MESSAGE);
                showLogin();
                return;
            }
            Map<String, String> usuario = extraerUsuario(response);
            abrirModulo(usuario);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "No se pudo conectar con la API de MetalGest: " + e.getMessage(), "Conexion", JOptionPane.ERROR_MESSAGE);
            showLogin();
        }
    }

    private static Map<String, String> extraerUsuario(String response) {
        int index = response.indexOf("\"usuario\":");
        if (index < 0) {
            return new LinkedHashMap<>();
        }
        int start = response.indexOf('{', index);
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < response.length(); i++) {
            char c = response.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\' && inString) {
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return JsonUtil.parseObject(response.substring(start, i + 1));
                }
            }
        }
        return new LinkedHashMap<>();
    }

    private static void abrirModulo(Map<String, String> usuario) {
        String rol = usuario.getOrDefault("rol", "");
        int idUsuario = parseInt(usuario.get("id_usuario"), 1);
        if ("Administracion".equalsIgnoreCase(rol) || "Gerencia".equalsIgnoreCase(rol)) {
            AdminPanel.openAdmin(idUsuario);
        } else if ("Produccion".equalsIgnoreCase(rol)) {
            ProductionPanel.openProduccion(idUsuario);
        } else if ("Cliente".equalsIgnoreCase(rol)) {
            PruebaGUI.openCliente();
        } else {
            JOptionPane.showMessageDialog(null, "El rol " + rol + " todavia no tiene modulo activo.", "MetalGest", JOptionPane.INFORMATION_MESSAGE);
            showLogin();
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }
}
