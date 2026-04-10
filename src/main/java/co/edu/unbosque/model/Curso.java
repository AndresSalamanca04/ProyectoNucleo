package co.edu.unbosque.model;
import java.io.Serializable;

public class Curso implements Serializable {
    private static final long serialVersionUID = 1L;
    private String codigo;
    private String nombre;
    private int creditos;
    private String docenteAsignado; // NUEVO

    public Curso() {}
    public Curso(String codigo, String nombre, int creditos, String docenteAsignado) {
        this.codigo = codigo; this.nombre = nombre; this.creditos = creditos; this.docenteAsignado = docenteAsignado;
    }
    public String getCodigo() { return codigo; } public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; } public void setNombre(String nombre) { this.nombre = nombre; }
    public int getCreditos() { return creditos; } public void setCreditos(int creditos) { this.creditos = creditos; }
    public String getDocenteAsignado() { return docenteAsignado; } public void setDocenteAsignado(String docenteAsignado) { this.docenteAsignado = docenteAsignado; }
}