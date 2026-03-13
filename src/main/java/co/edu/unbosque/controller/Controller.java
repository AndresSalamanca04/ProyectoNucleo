package co.edu.unbosque.controller;

import co.edu.unbosque.model.Aula;
import co.edu.unbosque.model.Curso;
import co.edu.unbosque.model.Docente;
import co.edu.unbosque.model.Horario;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("controller")
@SessionScoped
public class Controller implements Serializable {

    private static final long serialVersionUID = 1L;

    // --- VARIABLES DE LOGIN ---
    private String usuario;
    private String clave;

    // --- LISTAS DE DATOS ---
    private List<Docente> listaDocentes;
    private List<Curso> listaCursos;
    private List<Aula> listaAulas;
    private List<Horario> listaHorarios;

    // --- OBJETOS ACTUALES (Para enlazar con los formularios) ---
    private Docente docenteActual;
    private Curso cursoActual;
    private Aula aulaActual;
    private Horario horarioActual;

    @PostConstruct
    public void init() {
        // 1. Inicializar las listas completamente vacías
        listaDocentes = new ArrayList<>();
        listaCursos = new ArrayList<>();
        listaAulas = new ArrayList<>();
        listaHorarios = new ArrayList<>();

        // 2. Inicializar los objetos para que los formularios estén listos para recibir datos
        docenteActual = new Docente();
        cursoActual = new Curso();
        aulaActual = new Aula();
        horarioActual = new Horario();
    }

    // ==========================================
    //          MÉTODO DE LOGIN
    // ==========================================
    public String login() {
        if ("admin".equals(usuario) && "123".equals(clave)) {
            // Si es correcto, redirige a la página de inicio
            return "inicio?faces-redirect=true";
        } else {
            // Si es incorrecto, muestra un mensaje de error y se queda en el login
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error de Acceso", "Usuario o contraseña incorrectos."));
            return null; 
        }
    }

    // ==========================================
    //          MÉTODOS PARA DOCENTES
    // ==========================================
    public String irAgregarDocente() {
        this.docenteActual = new Docente(); // Limpia el formulario
        return "agregarDocentes?faces-redirect=true";
    }

    public String guardarDocente() {
        this.listaDocentes.add(docenteActual);
        return "docentes?faces-redirect=true";
    }

    public String editarDocente(Docente d) {
        this.docenteActual = d; // Carga el objeto a editar en el formulario
        return "editarDocentes?faces-redirect=true";
    }

    public String actualizarDocente() {
        return "docentes?faces-redirect=true";
    }

    public void borrarDocente(Docente d) {
        this.listaDocentes.remove(d);
    }

    // ==========================================
    //          MÉTODOS PARA CURSOS
    // ==========================================
    public String irAgregarCurso() {
        this.cursoActual = new Curso();
        return "agregarCursos?faces-redirect=true";
    }

    public String guardarCurso() {
        this.listaCursos.add(cursoActual);
        return "cursos?faces-redirect=true";
    }

    public String editarCurso(Curso c) {
        this.cursoActual = c;
        return "editarCursos?faces-redirect=true";
    }

    public String actualizarCurso() {
        return "cursos?faces-redirect=true";
    }

    public void borrarCurso(Curso c) {
        this.listaCursos.remove(c);
    }

    // ==========================================
    //          MÉTODOS PARA AULAS
    // ==========================================
    public String irAgregarAula() {
        this.aulaActual = new Aula();
        return "agregarAulas?faces-redirect=true";
    }

    public String guardarAula() {
        this.listaAulas.add(aulaActual);
        return "aulas?faces-redirect=true";
    }

    public String editarAula(Aula a) {
        this.aulaActual = a;
        return "editarAulas?faces-redirect=true";
    }

    public String actualizarAula() {
        return "aulas?faces-redirect=true";
    }

    public void borrarAula(Aula a) {
        this.listaAulas.remove(a);
    }

    // ==========================================
    //          MÉTODOS PARA HORARIOS
    // ==========================================
    public String irAgregarHorario() {
        this.horarioActual = new Horario();
        return "anadirHorario?faces-redirect=true";
    }

    public String guardarHorario() {
        this.listaHorarios.add(horarioActual);
        return "horario?faces-redirect=true";
    }

    public String editarHorario(Horario h) {
        this.horarioActual = h;
        return "editarHorario?faces-redirect=true";
    }

    public String actualizarHorario() {
        return "horario?faces-redirect=true";
    }

    public void borrarHorario(Horario h) {
        this.listaHorarios.remove(h);
    }

    // ==========================================
    //          GETTERS Y SETTERS
    // ==========================================

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public List<Docente> getListaDocentes() { return listaDocentes; }
    public void setListaDocentes(List<Docente> listaDocentes) { this.listaDocentes = listaDocentes; }

    public List<Curso> getListaCursos() { return listaCursos; }
    public void setListaCursos(List<Curso> listaCursos) { this.listaCursos = listaCursos; }

    public List<Aula> getListaAulas() { return listaAulas; }
    public void setListaAulas(List<Aula> listaAulas) { this.listaAulas = listaAulas; }

    public List<Horario> getListaHorarios() { return listaHorarios; }
    public void setListaHorarios(List<Horario> listaHorarios) { this.listaHorarios = listaHorarios; }

    public Docente getDocenteActual() { return docenteActual; }
    public void setDocenteActual(Docente docenteActual) { this.docenteActual = docenteActual; }

    public Curso getCursoActual() { return cursoActual; }
    public void setCursoActual(Curso cursoActual) { this.cursoActual = cursoActual; }

    public Aula getAulaActual() { return aulaActual; }
    public void setAulaActual(Aula aulaActual) { this.aulaActual = aulaActual; }

    public Horario getHorarioActual() { return horarioActual; }
    public void setHorarioActual(Horario horarioActual) { this.horarioActual = horarioActual; }

}