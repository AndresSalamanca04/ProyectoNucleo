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
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Named("controller")
@SessionScoped
public class Controller implements Serializable {

    private static final long serialVersionUID = 1L;
    
    // Ruta donde se guardará el archivo en tu computadora (Carpeta de Usuario)
    private final String RUTA_ARCHIVO = System.getProperty("user.home") + "/datos_universidad.dat";

    // --- VARIABLES DE LOGIN ---
    private String usuario;
    private String clave;

    // --- LISTAS DE DATOS ---
    private List<Docente> listaDocentes;
    private List<Curso> listaCursos;
    private List<Aula> listaAulas;
    private List<Horario> listaHorarios;

    // --- OBJETOS ACTUALES ---
    private Docente docenteActual;
    private Curso cursoActual;
    private Aula aulaActual;
    private Horario horarioActual;

    @PostConstruct
    public void init() {
        cargarDatosDeArchivo();
        docenteActual = new Docente();
        cursoActual = new Curso();
        aulaActual = new Aula();
        horarioActual = new Horario();
    }

    // ==========================================
    //          PERSISTENCIA DE DATOS
    // ==========================================
    private void guardarDatosEnArchivo() {
        try (FileOutputStream fos = new FileOutputStream(RUTA_ARCHIVO);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            Object[] datos = {listaDocentes, listaCursos, listaAulas, listaHorarios};
            oos.writeObject(datos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void cargarDatosDeArchivo() {
        File archivo = new File(RUTA_ARCHIVO);
        if (archivo.exists()) {
            try (FileInputStream fis = new FileInputStream(archivo);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                Object[] datos = (Object[]) ois.readObject();
                listaDocentes = (List<Docente>) datos[0];
                listaCursos = (List<Curso>) datos[1];
                listaAulas = (List<Aula>) datos[2];
                listaHorarios = (List<Horario>) datos[3];
            } catch (Exception e) {
                e.printStackTrace();
                inicializarListasVacias();
            }
        } else {
            inicializarListasVacias();
        }
    }

    private void inicializarListasVacias() {
        listaDocentes = new ArrayList<>();
        listaCursos = new ArrayList<>();
        listaAulas = new ArrayList<>();
        listaHorarios = new ArrayList<>();
    }

    // ==========================================
    //          MÉTODO DE LOGIN Y LOGOUT
    // ==========================================
    public String login() {
        if ("admin".equals(usuario) && "123".equals(clave)) {
            // Creamos la "credencial" en el mapa de sesión para el AuthFilter
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("usuarioLogueado", true);
            // Redirección con ruta completa para evitar bloqueos del filtro
            return "/sistema/inicio?faces-redirect=true";
        } else {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Usuario o contraseña incorrectos."));
            return null; 
        }
    }

    public String cerrarSesion() {
        // Invalidamos la sesión por completo (elimina la credencial)
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        // Limpiamos variables de login por seguridad
        this.usuario = null;
        this.clave = null;
        return "/sistema/login?faces-redirect=true";
    }

    // ==========================================
    //          MÉTODOS PARA DOCENTES
    // ==========================================
    public String irAgregarDocente() { this.docenteActual = new Docente(); return "agregarDocentes?faces-redirect=true"; }
    public String guardarDocente() { 
        this.listaDocentes.add(docenteActual); 
        guardarDatosEnArchivo(); 
        return "docentes?faces-redirect=true"; 
    }
    public String editarDocente(Docente d) { this.docenteActual = d; return "editarDocentes?faces-redirect=true"; }
    public String actualizarDocente() { guardarDatosEnArchivo(); return "docentes?faces-redirect=true"; }
    public void borrarDocente(Docente d) { this.listaDocentes.remove(d); guardarDatosEnArchivo(); }

    // ==========================================
    //          MÉTODOS PARA CURSOS
    // ==========================================
    public String irAgregarCurso() { this.cursoActual = new Curso(); return "agregarCursos?faces-redirect=true"; }
    public String guardarCurso() { this.listaCursos.add(cursoActual); guardarDatosEnArchivo(); return "cursos?faces-redirect=true"; }
    public String editarCurso(Curso c) { this.cursoActual = c; return "editarCursos?faces-redirect=true"; }
    public String actualizarCurso() { guardarDatosEnArchivo(); return "cursos?faces-redirect=true"; }
    public void borrarCurso(Curso c) { this.listaCursos.remove(c); guardarDatosEnArchivo(); }

    // ==========================================
    //          MÉTODOS PARA AULAS
    // ==========================================
    public String irAgregarAula() { this.aulaActual = new Aula(); return "agregarAulas?faces-redirect=true"; }
    public String guardarAula() { this.listaAulas.add(aulaActual); guardarDatosEnArchivo(); return "aulas?faces-redirect=true"; }
    public String editarAula(Aula a) { this.aulaActual = a; return "editarAulas?faces-redirect=true"; }
    public String actualizarAula() { guardarDatosEnArchivo(); return "aulas?faces-redirect=true"; }
    public void borrarAula(Aula a) { this.listaAulas.remove(a); guardarDatosEnArchivo(); }

    // ==========================================
    //          MÉTODOS PARA HORARIOS
    // ==========================================
    public String irAgregarHorario() { this.horarioActual = new Horario(); return "anadirHorario?faces-redirect=true"; }
    public String guardarHorario() { this.listaHorarios.add(horarioActual); guardarDatosEnArchivo(); return "horario?faces-redirect=true"; }
    public String editarHorario(Horario h) { this.horarioActual = h; return "editarHorario?faces-redirect=true"; }
    public String actualizarHorario() { guardarDatosEnArchivo(); return "horario?faces-redirect=true"; }
    public void borrarHorario(Horario h) { this.listaHorarios.remove(h); guardarDatosEnArchivo(); }

    // ==========================================
    //          GETTERS Y SETTERS
    // ==========================================
    public String getUsuario() { return usuario; } public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getClave() { return clave; } public void setClave(String clave) { this.clave = clave; }
    public List<Docente> getListaDocentes() { return listaDocentes; } public void setListaDocentes(List<Docente> listaDocentes) { this.listaDocentes = listaDocentes; }
    public List<Curso> getListaCursos() { return listaCursos; } public void setListaCursos(List<Curso> listaCursos) { this.listaCursos = listaCursos; }
    public List<Aula> getListaAulas() { return listaAulas; } public void setListaAulas(List<Aula> listaAulas) { this.listaAulas = listaAulas; }
    public List<Horario> getListaHorarios() { return listaHorarios; } public void setListaHorarios(List<Horario> listaHorarios) { this.listaHorarios = listaHorarios; }
    public Docente getDocenteActual() { return docenteActual; } public void setDocenteActual(Docente docenteActual) { this.docenteActual = docenteActual; }
    public Curso getCursoActual() { return cursoActual; } public void setCursoActual(Curso cursoActual) { this.cursoActual = cursoActual; }
    public Aula getAulaActual() { return aulaActual; } public void setAulaActual(Aula aulaActual) { this.aulaActual = aulaActual; }
    public Horario getHorarioActual() { return horarioActual; } public void setHorarioActual(Horario horarioActual) { this.horarioActual = horarioActual; }
}