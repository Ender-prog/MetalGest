import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

class Libros { 
    String nombre;
    int cant;
    int id;

    public Libros(String nombre, int cant, int id) {
        this.nombre = nombre;
        this.cant = cant;
        this.id = id;
    }

    public void mostrar() {
        System.out.println("ID: " + id + ", Nombre: " + nombre + ", Cantidad: " + cant);
    }
}

public class pruebaGUI {
    static int idContador = 0;
    static ArrayList<Libros> librosDisponibles = new ArrayList<>();

    public static void recargarTabla(DefaultTableModel modelo) {
        modelo.setRowCount(0);
        for (Libros libro : librosDisponibles) { 
            Object[] fila = {libro.id, libro.nombre, libro.cant};
            modelo.addRow(fila);
        }        
    }

    public static void cargarmain(){
        JFrame vent = new JFrame("principal");
        vent.setSize(500, 500);
        vent.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        vent.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new BorderLayout());
        vent.add(panel);

        JPanel panelEntrada = new JPanel();
        JLabel eti = new JLabel("Nombre:");
        panelEntrada.add(eti);

        JTextField txtn = new JTextField(10);
        panelEntrada.add(txtn);

        JLabel eti2 = new JLabel("Cantidad:");
        panelEntrada.add(eti2);

        JTextField txtc = new JTextField(5);
        panelEntrada.add(txtc);

        JButton btnA = new JButton("añadir");
        panelEntrada.add(btnA);
        
        panel.add(panelEntrada, BorderLayout.NORTH);
        
        String[] columnas = {"ID", "Nombre del Libro", "Cantidad disponible"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0);

        for (Libros libro : librosDisponibles) { 
            Object[] fila = {libro.id, libro.nombre, libro.cant};
            modelo.addRow(fila);
        }

        JTable tabla = new JTable(modelo);
        JScrollPane scrollPane = new JScrollPane(tabla);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel panelInferior = new JPanel();
        JButton btnR = new JButton("recargar");
        panelInferior.add(btnR);
        panel.add(panelInferior, BorderLayout.SOUTH);
        
        vent.setVisible(true);
        
        btnR.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                recargarTabla(modelo);
            }
        });
        
        btnA.addActionListener(e -> {
            try {
                String nombre = txtn.getText(); 
                String cantTexto = txtc.getText();

                if (!nombre.isEmpty() && !cantTexto.isEmpty()) {
                    int cant = Integer.parseInt(cantTexto);
                    
                    idContador++; 
                    
                    Libros nuevoLibro = new Libros(nombre, cant, idContador);
                    librosDisponibles.add(nuevoLibro);
                    
                    JOptionPane.showMessageDialog(vent, "Exito");   
                    
                    txtn.setText("");
                    txtc.setText("");
                    
                    recargarTabla(modelo);
                } else {
                    JOptionPane.showMessageDialog(vent, "Error: Incompleto");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(vent, "Error: invalido");
            }
        });
    }

    public static void main(String[] args) {
        cargarmain();
    }
}
