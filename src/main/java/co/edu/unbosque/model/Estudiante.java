package co.edu.unbosque.model;
import java.io.Serializable;

public class Estudiante implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String documento;
    private String nombre;
    private String correo; // NUEVO CAMPO
    private String carrera;
    private int semestreActual;
    private int creditosMatriculados;

    public Estudiante() {}

    public Estudiante(String documento, String nombre, String correo, String carrera, int semestreActual, int creditosMatriculados) {
        this.documento = documento;
        this.nombre = nombre;
        this.correo = correo; // ASIGNADO
        this.carrera = carrera;
        this.semestreActual = semestreActual;
        this.creditosMatriculados = creditosMatriculados;
    }

    public String getDocumento() { return documento; } public void setDocumento(String documento) { this.documento = documento; }
    public String getNombre() { return nombre; } public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; } public void setCorreo(String correo) { this.correo = correo; } // GET/SET
    public String getCarrera() { return carrera; } public void setCarrera(String carrera) { this.carrera = carrera; }
    public int getSemestreActual() { return semestreActual; } public void setSemestreActual(int semestreActual) { this.semestreActual = semestreActual; }
    public int getCreditosMatriculados() { return creditosMatriculados; } public void setCreditosMatriculados(int creditosMatriculados) { this.creditosMatriculados = creditosMatriculados; }
}