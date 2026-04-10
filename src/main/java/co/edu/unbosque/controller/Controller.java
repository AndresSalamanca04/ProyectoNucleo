package co.edu.unbosque.controller;

import co.edu.unbosque.model.*;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Named("controller")
@SessionScoped
public class Controller implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String URL = "jdbc:mariadb://localhost:3306/SistemaAcademico";
    private final String USER = "root";
    private final String PASS = "admin";

    private String usuario;
    private String clave;

    private List<Docente> listaDocentes;
    private List<Docente> docentesFiltrados; 
    
    private List<Curso> listaCursos;
    private List<Aula> listaAulas;
    
    private List<Horario> listaHorarios;
    private List<Horario> horariosFiltrados; 
    
    private List<Estudiante> listaEstudiantes;

    private Docente docenteActual = new Docente();
    private Curso cursoActual = new Curso();
    private Aula aulaActual = new Aula();
    private Horario horarioActual = new Horario();
    private Estudiante estudianteActual = new Estudiante();

    private String estudianteSeleccionadoId;
    private int horarioSeleccionadoId;
    
    // VARIABLES DEL MOTOR
    private List<String> cursosSeleccionadosParaMotor = new ArrayList<>();
    private List<String> diasSeleccionadosParaMotor = new ArrayList<>();
    private List<String> aulasSeleccionadasParaMotor = new ArrayList<>(); 
    
    // NUEVO: VARIABLE PARA EL FILTRO DE CALENDARIO
    private String diaFiltro = "";

    @PostConstruct
    public void init() { actualizarTodasLasListas(); }

    private Connection getConnection() throws Exception {
        Class.forName("org.mariadb.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public void actualizarTodasLasListas() {
        cargarDocentes(); cargarCursos(); cargarAulas(); cargarHorarios(); cargarEstudiantes();
    }

    public String login() {
        if ("admin".equals(usuario) && "123".equals(clave)) {
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("usuarioLogueado", true);
            return "/sistema/inicio?faces-redirect=true";
        }
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Credenciales incorrectas."));
        return null; 
    }

    public String cerrarSesion() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/sistema/login?faces-redirect=true";
    }

    // ==========================================
    // MÉTODO PARA MOSTRAR HORARIOS FILTRADOS POR DÍA (CALENDARIO)
    // ==========================================
    public List<Horario> getHorariosMostrados() {
        if (listaHorarios == null) return new ArrayList<>();
        // Si el filtro está vacío, retorna la semana completa
        if (diaFiltro == null || diaFiltro.isEmpty()) {
            return listaHorarios;
        }
        // Si hay un día seleccionado, filtra la lista
        return listaHorarios.stream()
                .filter(h -> diaFiltro.equals(h.getDia()))
                .collect(Collectors.toList());
    }

    // ==========================================
    // MOTOR DE GENERACIÓN SEMI-AUTOMÁTICA (CON DOCENTE IMPLÍCITO)
    // ==========================================
    public void generarHorariosAutomaticos() {
        if (cursosSeleccionadosParaMotor == null || cursosSeleccionadosParaMotor.isEmpty() ||
            diasSeleccionadosParaMotor == null || diasSeleccionadosParaMotor.isEmpty() ||
            aulasSeleccionadasParaMotor == null || aulasSeleccionadasParaMotor.isEmpty()) {
            
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Faltan Parámetros", "Debe seleccionar al menos una materia, un día y un aula."));
            return;
        }

        String[] horasPosibles = {"08:00 - 10:00", "10:00 - 12:00", "14:00 - 16:00", "16:00 - 18:00"};
        int sesionesAsignadas = 0;

        List<Curso> cursosAProcesar = listaCursos.stream().filter(c -> cursosSeleccionadosParaMotor.contains(c.getCodigo())).collect(Collectors.toList());
        List<Aula> aulasAProcesar = listaAulas.stream().filter(a -> aulasSeleccionadasParaMotor.contains(a.getNumero())).collect(Collectors.toList());

        for (Curso curso : cursosAProcesar) {
            String docenteDelCurso = curso.getDocenteAsignado();
            
            if (docenteDelCurso == null || docenteDelCurso.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Curso Ignorado", "La materia " + curso.getNombre() + " no tiene un docente asignado."));
                continue; 
            }

            for (String dia : diasSeleccionadosParaMotor) {
                boolean asignadoEnEsteDia = false;
                for (String hora : horasPosibles) {
                    if (asignadoEnEsteDia) break;
                    for (Aula aula : aulasAProcesar) { 
                        if (!existeCruce("docente", docenteDelCurso, dia, hora) && 
                            !existeCruce("aula", aula.getNumero(), dia, hora)) {
                            
                            String sql = "INSERT INTO horarios (dia, hora, docente, curso, aula, estado) VALUES (?, ?, ?, ?, ?, 'BORRADOR')";
                            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                                ps.setString(1, dia); ps.setString(2, hora); ps.setString(3, docenteDelCurso);
                                ps.setString(4, curso.getCodigo()); ps.setString(5, aula.getNumero());
                                ps.executeUpdate();
                                
                                sesionesAsignadas++;
                                asignadoEnEsteDia = true; 
                                break;
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                }
            }
        }
        
        cargarHorarios();
        cursosSeleccionadosParaMotor.clear();
        diasSeleccionadosParaMotor.clear();
        aulasSeleccionadasParaMotor.clear();
        
        if (sesionesAsignadas > 0) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Motor Finalizado", "Se generaron " + sesionesAsignadas + " sesiones de clase."));
        } else {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Sin Espacio", "No hay disponibilidad cruzando los días y aulas seleccionados."));
        }
    }

    public void publicarHorario(Horario h) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE horarios SET estado = 'PUBLICADO' WHERE id = ?")) {
            ps.setInt(1, h.getId()); ps.executeUpdate(); cargarHorarios();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Oficializado", "El horario ya está visible."));
        } catch (Exception e) { manejarError(e); }
    }

    public void borrarHorario(Horario h) {
        try (Connection conn = getConnection()) {
            boolean borrado = false;
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM horarios WHERE id=?")) { ps.setInt(1, h.getId()); if (ps.executeUpdate() > 0) borrado = true; }
            if (!borrado) {
                try (PreparedStatement ps2 = conn.prepareStatement("DELETE FROM horarios WHERE dia=? AND hora=? AND aula=?")) {
                    ps2.setString(1, h.getDia()); ps2.setString(2, h.getHora()); ps2.setString(3, h.getAula());
                    if (ps2.executeUpdate() > 0) borrado = true;
                }
            }
            if (borrado) { cargarHorarios(); FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Eliminado", "La clase se eliminó."));
            } else { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Fallo", "No se encontró la clase.")); }
        } catch (SQLIntegrityConstraintViolationException e) { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Alumnos matriculados."));
        } catch (Exception e) { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage())); }
    }

    // ==========================================
    // CRUD RESTANTES
    // ==========================================
    public void cargarHorarios() {
        listaHorarios = new ArrayList<>();
        String sql = "SELECT h.*, c.nombre AS curso_nombre FROM horarios h LEFT JOIN cursos c ON h.curso = c.codigo";
        try (Connection conn = getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) { 
                String est = rs.getString("estado"); 
                String nombreCurso = rs.getString("curso_nombre");
                if (nombreCurso == null) nombreCurso = rs.getString("curso"); 
                
                listaHorarios.add(new Horario(rs.getInt("id"), rs.getString("dia"), rs.getString("hora"), rs.getString("docente"), rs.getString("curso"), rs.getString("aula"), (est != null ? est : "BORRADOR"), nombreCurso)); 
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    public List<Horario> getHorariosPublicados() { if (listaHorarios == null) return new ArrayList<>(); return listaHorarios.stream().filter(h -> "PUBLICADO".equals(h.getEstado())).collect(Collectors.toList()); }
    public String guardarHorario() {
        if (existeCruce("docente", horarioActual.getDocente(), horarioActual.getDia(), horarioActual.getHora())) { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cruce Docente", "Ocupado.")); return null; }
        if (existeCruce("aula", horarioActual.getAula(), horarioActual.getDia(), horarioActual.getHora())) { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cruce Aula", "Ocupada.")); return null; }
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO horarios (dia, hora, docente, curso, aula, estado) VALUES (?, ?, ?, ?, ?, 'BORRADOR')")) {
            ps.setString(1, horarioActual.getDia()); ps.setString(2, horarioActual.getHora()); ps.setString(3, horarioActual.getDocente()); ps.setString(4, horarioActual.getCurso()); ps.setString(5, horarioActual.getAula());
            ps.executeUpdate(); cargarHorarios(); return "horario?faces-redirect=true";
        } catch (Exception e) { return manejarError(e); }
    }
    public String editarHorario(Horario h) { this.horarioActual = h; return "editarHorario?faces-redirect=true"; }
    public String actualizarHorario() { try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE horarios SET docente=?, curso=? WHERE dia=? AND hora=? AND aula=?")) { ps.setString(1, horarioActual.getDocente()); ps.setString(2, horarioActual.getCurso()); ps.setString(3, horarioActual.getDia()); ps.setString(4, horarioActual.getHora()); ps.setString(5, horarioActual.getAula()); ps.executeUpdate(); cargarHorarios(); return "horario?faces-redirect=true"; } catch (Exception e) { return manejarError(e); } }

    public void cargarDocentes() { listaDocentes = new ArrayList<>(); try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM docentes")) { while (r.next()) listaDocentes.add(new Docente(r.getString("nombre"), r.getString("correo"), r.getString("tipo"), r.getString("departamento"))); } catch (Exception e) { e.printStackTrace(); } }
    public String guardarDocente() { try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement("INSERT INTO docentes (nombre, correo, tipo, departamento) VALUES (?,?,?,?)")) { p.setString(1, docenteActual.getNombre()); p.setString(2, docenteActual.getCorreo()); p.setString(3, docenteActual.getTipo()); p.setString(4, docenteActual.getDepartamento()); p.executeUpdate(); cargarDocentes(); return "docentes?faces-redirect=true"; } catch (Exception e) { return manejarError(e); } }
    public String editarDocente(Docente d) { this.docenteActual = d; return "editarDocentes?faces-redirect=true"; }
    public String actualizarDocente() { try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE docentes SET nombre=?, tipo=?, departamento=? WHERE correo=?")) { ps.setString(1, docenteActual.getNombre()); ps.setString(2, docenteActual.getTipo()); ps.setString(3, docenteActual.getDepartamento()); ps.setString(4, docenteActual.getCorreo()); ps.executeUpdate(); cargarDocentes(); return "docentes?faces-redirect=true"; } catch (Exception e) { return manejarError(e); } }
    public void borrarDocente(Docente d) { try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM docentes WHERE correo=?")) { ps.setString(1, d.getCorreo()); ps.executeUpdate(); cargarDocentes(); } catch (Exception e) { e.printStackTrace(); } }

    public void cargarCursos() { listaCursos = new ArrayList<>(); try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM cursos")) { while (r.next()) listaCursos.add(new Curso(r.getString("codigo"), r.getString("nombre"), r.getInt("creditos"), r.getString("docente_asignado"))); } catch (Exception e) { e.printStackTrace(); } }
    public String guardarCurso() { try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement("INSERT INTO cursos (codigo, nombre, creditos, docente_asignado) VALUES (?, ?, ?, ?)")) { p.setString(1, cursoActual.getCodigo()); p.setString(2, cursoActual.getNombre()); p.setInt(3, cursoActual.getCreditos()); p.setString(4, cursoActual.getDocenteAsignado()); p.executeUpdate(); cargarCursos(); return "cursos?faces-redirect=true"; } catch (Exception e) { return manejarError(e); } }
    public String editarCurso(Curso c) { this.cursoActual = c; return "editarCursos?faces-redirect=true"; }
    public String actualizarCurso() { try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE cursos SET nombre=?, creditos=?, docente_asignado=? WHERE codigo=?")) { ps.setString(1, cursoActual.getNombre()); ps.setInt(2, cursoActual.getCreditos()); ps.setString(3, cursoActual.getDocenteAsignado()); ps.setString(4, cursoActual.getCodigo()); ps.executeUpdate(); cargarCursos(); return "cursos?faces-redirect=true"; } catch (Exception e) { return manejarError(e); } }
    public void borrarCurso(Curso c) { try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM cursos WHERE codigo=?")) { ps.setString(1, c.getCodigo()); ps.executeUpdate(); cargarCursos(); } catch (Exception e) { e.printStackTrace(); } }

    public void cargarAulas() { listaAulas = new ArrayList<>(); try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM aulas")) { while (r.next()) listaAulas.add(new Aula(r.getString("numero"), r.getString("tipo"), r.getInt("capacidad"))); } catch (Exception e) { e.printStackTrace(); } }
    public String guardarAula() { try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement("INSERT INTO aulas (numero, tipo, capacidad) VALUES (?, ?, ?)")) { p.setString(1, aulaActual.getNumero()); p.setString(2, aulaActual.getTipo()); p.setInt(3, aulaActual.getCapacidad()); p.executeUpdate(); cargarAulas(); return "aulas?faces-redirect=true"; } catch (Exception e) { return manejarError(e); } }
    public String editarAula(Aula a) { this.aulaActual = a; return "editarAulas?faces-redirect=true"; }
    public String actualizarAula() { try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE aulas SET tipo=?, capacidad=? WHERE numero=?")) { ps.setString(1, aulaActual.getTipo()); ps.setInt(2, aulaActual.getCapacidad()); ps.setString(3, aulaActual.getNumero()); ps.executeUpdate(); cargarAulas(); return "aulas?faces-redirect=true"; } catch (Exception e) { return manejarError(e); } }
    public void borrarAula(Aula a) { try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM aulas WHERE numero=?")) { ps.setString(1, a.getNumero()); ps.executeUpdate(); cargarAulas(); } catch (Exception e) { e.printStackTrace(); } }

    public void cargarEstudiantes() { listaEstudiantes = new ArrayList<>(); try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM estudiantes")) { while (r.next()) listaEstudiantes.add(new Estudiante(r.getString("documento"), r.getString("nombre"), r.getString("carrera"), r.getInt("semestre_actual"), r.getInt("creditos_matriculados"))); } catch (Exception e) { e.printStackTrace(); } }
    public String guardarEstudiante() { try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement("INSERT INTO estudiantes (documento, nombre, carrera, semestre_actual, creditos_matriculados) VALUES (?,?,?,?,0)")) { p.setString(1, estudianteActual.getDocumento()); p.setString(2, estudianteActual.getNombre()); p.setString(3, estudianteActual.getCarrera()); p.setInt(4, estudianteActual.getSemestreActual()); p.executeUpdate(); cargarEstudiantes(); return "estudiantes?faces-redirect=true"; } catch (Exception e) { return manejarError(e); } }
    public String editarEstudiante(Estudiante e) { this.estudianteActual = e; return "editarEstudiantes?faces-redirect=true"; }
    public String actualizarEstudiante() { try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE estudiantes SET nombre=?, carrera=?, semestre_actual=? WHERE documento=?")) { ps.setString(1, estudianteActual.getNombre()); ps.setString(2, estudianteActual.getCarrera()); ps.setInt(3, estudianteActual.getSemestreActual()); ps.setString(4, estudianteActual.getDocumento()); ps.executeUpdate(); cargarEstudiantes(); return "estudiantes?faces-redirect=true"; } catch (Exception e) { return manejarError(e); } }
    public void borrarEstudiante(Estudiante e) { try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM estudiantes WHERE documento=?")) { ps.setString(1, e.getDocumento()); ps.executeUpdate(); cargarEstudiantes(); } catch (Exception ex) { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Estudiante tiene historial.")); } }

    public String inscribirMateria() {
        try {
            String codigoCurso = "";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT curso FROM horarios WHERE id = ? AND estado = 'PUBLICADO'")) {
                ps.setInt(1, horarioSeleccionadoId); ResultSet rs = ps.executeQuery();
                if (rs.next()) codigoCurso = rs.getString(1); else { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Clase no disponible.")); return null; }
            }
            if (!validarAforo(horarioSeleccionadoId)) { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Aforo Lleno", "El aula no tiene capacidad.")); return null; }
            if (!validarPrerrequisitos(estudianteSeleccionadoId, codigoCurso)) { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Prerrequisitos", "Materias previas requeridas.")); return null; }
            if (!validarCargaAcademica(estudianteSeleccionadoId, codigoCurso)) { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Carga Excedida", "Supera 18 créditos.")); return null; }

            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO inscripciones (documento_estudiante, id_horario) VALUES (?, ?)")) { ps.setString(1, estudianteSeleccionadoId); ps.setInt(2, horarioSeleccionadoId); ps.executeUpdate(); }
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE estudiantes SET creditos_matriculados = creditos_matriculados + (SELECT creditos FROM cursos WHERE codigo = ?) WHERE documento = ?")) { ps.setString(1, codigoCurso); ps.setString(2, estudianteSeleccionadoId); ps.executeUpdate(); }

            cargarEstudiantes(); FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Matriculado correctamente.")); return "estudiantes?faces-redirect=true";
        } catch (Exception e) { return manejarError(e); }
    }
    private boolean validarAforo(int idHorario) throws Exception { int capacidad = 0; int inscritos = 0; try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT a.capacidad FROM horarios h JOIN aulas a ON h.aula = a.numero WHERE h.id = ?")) { ps.setInt(1, idHorario); ResultSet rs = ps.executeQuery(); if (rs.next()) capacidad = rs.getInt(1); } try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM inscripciones WHERE id_horario = ?")) { ps.setInt(1, idHorario); ResultSet rs = ps.executeQuery(); if (rs.next()) inscritos = rs.getInt(1); } return inscritos < capacidad; }
    private boolean validarPrerrequisitos(String documento, String codigoCurso) throws Exception { List<String> reqs = new ArrayList<>(); try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT codigo_requerido FROM prerrequisitos WHERE codigo_curso = ?")) { ps.setString(1, codigoCurso); ResultSet rs = ps.executeQuery(); while (rs.next()) reqs.add(rs.getString(1)); } if (reqs.isEmpty()) return true; try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT aprobado FROM historial_academico WHERE documento_estudiante = ? AND codigo_curso = ?")) { for (String r : reqs) { ps.setString(1, documento); ps.setString(2, r); ResultSet rs = ps.executeQuery(); if (!rs.next() || !rs.getBoolean(1)) return false; } } return true; }
    private boolean validarCargaAcademica(String documento, String codigoCurso) throws Exception { int credCurso = 0; int credActual = 0; try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT creditos FROM cursos WHERE codigo = ?")) { ps.setString(1, codigoCurso); ResultSet rs = ps.executeQuery(); if (rs.next()) credCurso = rs.getInt(1); } try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT creditos_matriculados FROM estudiantes WHERE documento = ?")) { ps.setString(1, documento); ResultSet rs = ps.executeQuery(); if (rs.next()) credActual = rs.getInt(1); } return (credActual + credCurso) <= 18; }
    private boolean existeCruce(String columna, String valor, String dia, String hora) { String sql = "SELECT COUNT(*) FROM horarios WHERE " + columna + " = ? AND dia = ? AND hora = ?"; try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setString(1, valor); ps.setString(2, dia); ps.setString(3, hora); ResultSet rs = ps.executeQuery(); if (rs.next()) return rs.getInt(1) > 0; } catch (Exception e) { e.printStackTrace(); } return false; }
    private String manejarError(Exception e) { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error BD", e.getMessage())); return null; }

    public String irAgregarDocente() { this.docenteActual = new Docente(); return "agregarDocentes?faces-redirect=true"; }
    public String irAgregarCurso() { this.cursoActual = new Curso(); return "agregarCursos?faces-redirect=true"; }
    public String irAgregarAula() { this.aulaActual = new Aula(); return "agregarAulas?faces-redirect=true"; }
    public String irAgregarHorario() { this.horarioActual = new Horario(); return "agregarHorario?faces-redirect=true"; }

    public String getUsuario() { return usuario; } public void setUsuario(String u) { this.usuario = u; }
    public String getClave() { return clave; } public void setClave(String c) { this.clave = c; }
    public List<Docente> getListaDocentes() { return listaDocentes; } public List<Curso> getListaCursos() { return listaCursos; }
    public List<Aula> getListaAulas() { return listaAulas; } public List<Horario> getListaHorarios() { return listaHorarios; }
    public List<Estudiante> getListaEstudiantes() { return listaEstudiantes; } 
    public Docente getDocenteActual() { return docenteActual; } public void setDocenteActual(Docente d) { this.docenteActual = d; }
    public Curso getCursoActual() { return cursoActual; } public void setCursoActual(Curso c) { this.cursoActual = c; }
    public Aula getAulaActual() { return aulaActual; } public void setAulaActual(Aula a) { this.aulaActual = a; }
    public Horario getHorarioActual() { return horarioActual; } public void setHorarioActual(Horario h) { this.horarioActual = h; }
    public Estudiante getEstudianteActual() { return estudianteActual; } public void setEstudianteActual(Estudiante e) { this.estudianteActual = e; }
    public String getEstudianteSeleccionadoId() { return estudianteSeleccionadoId; } public void setEstudianteSeleccionadoId(String e) { this.estudianteSeleccionadoId = e; }
    public int getHorarioSeleccionadoId() { return horarioSeleccionadoId; } public void setHorarioSeleccionadoId(int h) { this.horarioSeleccionadoId = h; }
    
    public List<String> getCursosSeleccionadosParaMotor() { return cursosSeleccionadosParaMotor; } public void setCursosSeleccionadosParaMotor(List<String> c) { this.cursosSeleccionadosParaMotor = c; }
    public List<String> getDiasSeleccionadosParaMotor() { return diasSeleccionadosParaMotor; } public void setDiasSeleccionadosParaMotor(List<String> d) { this.diasSeleccionadosParaMotor = d; }
    public List<String> getAulasSeleccionadasParaMotor() { return aulasSeleccionadasParaMotor; } public void setAulasSeleccionadasParaMotor(List<String> a) { this.aulasSeleccionadasParaMotor = a; }
    public List<Docente> getDocentesFiltrados() { return docentesFiltrados; } public void setDocentesFiltrados(List<Docente> docentesFiltrados) { this.docentesFiltrados = docentesFiltrados; }
    public List<Horario> getHorariosFiltrados() { return horariosFiltrados; } public void setHorariosFiltrados(List<Horario> horariosFiltrados) { this.horariosFiltrados = horariosFiltrados; }
    
    // GETTERS Y SETTERS DEL FILTRO CALENDARIO
    public String getDiaFiltro() { return diaFiltro; }
    public void setDiaFiltro(String diaFiltro) { this.diaFiltro = diaFiltro; }
}