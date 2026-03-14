package co.edu.unbosque.model;
import java.io.Serializable;

public class Horario implements Serializable {
    private static final long serialVersionUID = 1L;
    private String dia; private String hora; private String docente; private String curso; private String aula;

    public Horario() {}
    public Horario(String dia, String hora, String docente, String curso, String aula) {
        this.dia = dia; this.hora = hora; this.docente = docente; this.curso = curso; this.aula = aula;
    }
    public String getDia() { return dia; } public void setDia(String dia) { this.dia = dia; }
    public String getHora() { return hora; } public void setHora(String hora) { this.hora = hora; }
    public String getDocente() { return docente; } public void setDocente(String docente) { this.docente = docente; }
    public String getCurso() { return curso; } public void setCurso(String curso) { this.curso = curso; }
    public String getAula() { return aula; } public void setAula(String aula) { this.aula = aula; }
}