package modulos.produccion;

import modulos.common.ApiClient;
import modulos.common.JsonUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProductionPanel {
    private static int usuarioId = 2;
    private static final List<Map<String, String>> maquinas = new ArrayList<>();

    public static void openProduccion() {
        openProduccion(2);
    }

    public static void openProduccion(int idUsuario) {
        usuarioId = idUsuario;
        SwingUtilities.invokeLater(ProductionPanel::cargarMain);
    }

    public static void cargarMain() {
        JFrame ventana = new JFrame("MetalGest - Produccion");
        ventana.setSize(1050, 650);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ventana.setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Ordenes recibidas", crearPanelOrdenes());
        tabs.addTab("Historial de avance", crearPanelHistorial());
        tabs.addTab("Aviso a mantenimiento", crearPanelMantenimiento());
        ventana.add(tabs);

        ventana.setVisible(true);
    }

    private static JPanel crearPanelOrdenes() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        DefaultTableModel modelo = new DefaultTableModel(
                new String[]{"ID", "Pedido", "Cliente", "Descripcion", "Material", "Cantidad", "Estado", "Avance"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tabla = new JTable(modelo);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(2, 5, 8, 8));
        JTextField cantidad = new JTextField("0");
        JTextField avance = new JTextField("0");
        JTextArea observaciones = new JTextArea(3, 24);
        JButton registrar = new JButton("Registrar avance");
        JButton recargar = new JButton("Recargar");
        form.add(new JLabel("Cantidad producida"));
        form.add(new JLabel("Avance %"));
        form.add(new JLabel("Observaciones"));
        form.add(new JLabel(""));
        form.add(new JLabel(""));
        form.add(cantidad);
        form.add(avance);
        form.add(new JScrollPane(observaciones));
        form.add(registrar);
        form.add(recargar);
        panel.add(form, BorderLayout.SOUTH);

        recargarOrdenes(modelo);

        recargar.addActionListener(e -> recargarOrdenes(modelo));
        registrar.addActionListener(e -> {
            int row = tabla.getSelectedRow();
            if (row < 0) {
                mostrarAviso("Selecciona una orden de trabajo.");
                return;
            }
            Map<String, String> data = new LinkedHashMap<>();
            data.put("id_orden", modelo.getValueAt(row, 0).toString());
            data.put("cantidad_producida", cantidad.getText().trim());
            data.put("avance", avance.getText().trim());
            data.put("observaciones", observaciones.getText().trim());
            if (postOk("produccion_register", data, "Avance registrado.")) {
                cantidad.setText("0");
                avance.setText("0");
                observaciones.setText("");
                recargarOrdenes(modelo);
            }
        });

        return panel;
    }

    private static JPanel crearPanelHistorial() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        DefaultTableModel modelo = new DefaultTableModel(
                new String[]{"ID", "Orden", "Cliente", "Material", "Cantidad", "Avance", "Inicio", "Fin", "Observaciones"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tabla = new JTable(modelo);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        JButton recargar = new JButton("Recargar historial");
        panel.add(recargar, BorderLayout.SOUTH);
        recargarHistorial(modelo);
        recargar.addActionListener(e -> recargarHistorial(modelo));
        return panel;
    }

    private static JPanel crearPanelMantenimiento() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        DefaultTableModel modelo = new DefaultTableModel(
                new String[]{"ID", "Identificacion", "Marca", "Modelo", "Ubicacion", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tabla = new JTable(modelo);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel form = new JPanel(new BorderLayout(8, 8));
        JTextArea problema = new JTextArea(4, 50);
        JButton reportar = new JButton("Reportar falla");
        JButton recargar = new JButton("Recargar maquinas");
        JPanel botones = new JPanel();
        botones.add(reportar);
        botones.add(recargar);
        form.add(new JScrollPane(problema), BorderLayout.CENTER);
        form.add(botones, BorderLayout.EAST);
        panel.add(form, BorderLayout.SOUTH);

        recargarMaquinas(modelo);

        recargar.addActionListener(e -> recargarMaquinas(modelo));
        reportar.addActionListener(e -> {
            int row = tabla.getSelectedRow();
            if (row < 0) {
                mostrarAviso("Selecciona la maquina con falla.");
                return;
            }
            if (problema.getText().trim().isEmpty()) {
                mostrarAviso("Describe la falla para Mantenimiento.");
                return;
            }
            Map<String, String> data = new LinkedHashMap<>();
            data.put("id_maquina", modelo.getValueAt(row, 0).toString());
            data.put("id_usuario", String.valueOf(usuarioId));
            data.put("tipo", "Correctivo");
            data.put("problema", problema.getText().trim());
            if (postOk("mantenimiento_report", data, "Aviso enviado a Mantenimiento.")) {
                problema.setText("");
                recargarMaquinas(modelo);
            }
        });

        return panel;
    }

    private static void recargarOrdenes(DefaultTableModel modelo) {
        modelo.setRowCount(0);
        for (Map<String, String> row : getRows("ordenes_list")) {
            modelo.addRow(new Object[]{
                    row.get("id_orden"),
                    row.get("id_pedido"),
                    row.get("razon_social"),
                    row.get("descripcion"),
                    row.get("material"),
                    row.get("cantidad"),
                    row.get("estado"),
                    row.get("avance") + "%"
            });
        }
    }

    private static void recargarHistorial(DefaultTableModel modelo) {
        modelo.setRowCount(0);
        for (Map<String, String> row : getRows("produccion_list")) {
            modelo.addRow(new Object[]{
                    row.get("id_produccion"),
                    row.get("id_orden"),
                    row.get("razon_social"),
                    row.get("material"),
                    row.get("cantidad_producida"),
                    row.get("avance") + "%",
                    row.get("fecha_inicio"),
                    row.get("fecha_fin"),
                    row.get("observaciones")
            });
        }
    }

    private static void recargarMaquinas(DefaultTableModel modelo) {
        modelo.setRowCount(0);
        maquinas.clear();
        maquinas.addAll(getRows("maquinas_list"));
        for (Map<String, String> row : maquinas) {
            modelo.addRow(new Object[]{
                    row.get("id_maquina"),
                    row.get("numero_identificacion"),
                    row.get("marca"),
                    row.get("modelo"),
                    row.get("ubicacion"),
                    row.get("estado")
            });
        }
    }

    private static List<Map<String, String>> getRows(String action) {
        try {
            return JsonUtil.parseArray(ApiClient.get(action));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "No se pudo conectar con MetalGest: " + e.getMessage(), "Conexion", JOptionPane.ERROR_MESSAGE);
            return java.util.Collections.emptyList();
        }
    }

    private static boolean postOk(String action, Map<String, String> data, String okMessage) {
        try {
            String response = ApiClient.post(action, data);
            if (ApiClient.isSuccess(response)) {
                JOptionPane.showMessageDialog(null, okMessage, "MetalGest", JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
            JOptionPane.showMessageDialog(null, "La operacion no pudo completarse: " + response, "MetalGest", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "No se pudo conectar con MetalGest: " + e.getMessage(), "Conexion", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    private static void mostrarAviso(String mensaje) {
        JOptionPane.showMessageDialog(null, mensaje, "MetalGest", JOptionPane.WARNING_MESSAGE);
    }

    public static void main(String[] args) {
        openProduccion();
    }
}
