package co.edu.unbosque.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class VersionHorario implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int id;
    private String nombre;
    private Timestamp fechaCreacion;

    public VersionHorario() {}

    public VersionHorario(int id, String nombre, Timestamp fechaCreacion) {
        this.id = id;
        this.nombre = nombre;
        this.fechaCreacion = fechaCreacion;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    
    // Método para mostrar la fecha de forma amigable en la tabla
    public String getFechaFormateada() {
        if (fechaCreacion == null) return "";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(fechaCreacion);
    }
}