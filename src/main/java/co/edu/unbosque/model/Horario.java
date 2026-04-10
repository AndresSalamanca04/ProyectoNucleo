package co.edu.unbosque.model;
import java.io.Serializable;

public class Horario implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String dia;
    private String hora;
    private String docente;
    private String curso; // Código (Ej: A1)
    private String aula;
    private String estado;
    private String cursoNombre; // NUEVO: Nombre real (Ej: Cálculo)

    public Horario() {}
    public Horario(int id, String dia, String hora, String docente, String curso, String aula, String estado, String cursoNombre) {
        this.id = id; this.dia = dia; this.hora = hora; this.docente = docente; this.curso = curso; 
        this.aula = aula; this.estado = estado; this.cursoNombre = cursoNombre;
    }
    public int getId() { return id; } public void setId(int id) { this.id = id; }
    public String getDia() { return dia; } public void setDia(String dia) { this.dia = dia; }
    public String getHora() { return hora; } public void setHora(String hora) { this.hora = hora; }
    public String getDocente() { return docente; } public void setDocente(String docente) { this.docente = docente; }
    public String getCurso() { return curso; } public void setCurso(String curso) { this.curso = curso; }
    public String getAula() { return aula; } public void setAula(String aula) { this.aula = aula; }
    public String getEstado() { return estado; } public void setEstado(String estado) { this.estado = estado; }
    public String getCursoNombre() { return cursoNombre; } public void setCursoNombre(String cursoNombre) { this.cursoNombre = cursoNombre; }
}