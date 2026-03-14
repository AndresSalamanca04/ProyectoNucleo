package co.edu.unbosque.model;
import java.io.Serializable;

public class Docente implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nombre; private String correo; private String tipo;
    private String departamento; private String disponibilidad;

    public Docente() {}
    public Docente(String nombre, String correo, String tipo, String departamento, String disponibilidad) {
        this.nombre = nombre; this.correo = correo; this.tipo = tipo; this.departamento = departamento; this.disponibilidad = disponibilidad;
    }
    public String getNombre() { return nombre; } public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; } public void setCorreo(String correo) { this.correo = correo; }
    public String getTipo() { return tipo; } public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDepartamento() { return departamento; } public void setDepartamento(String departamento) { this.departamento = departamento; }
    public String getDisponibilidad() { return disponibilidad; } public void setDisponibilidad(String disponibilidad) { this.disponibilidad = disponibilidad; }
}