package co.edu.unbosque.controller;

import co.edu.unbosque.model.*;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
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
    private List<VersionHorario> listaVersiones;
    private List<Horario> detallesVersionSeleccionada; 

    private Docente docenteActual = new Docente();
    private Curso cursoActual = new Curso();
    private Aula aulaActual = new Aula();
    private Horario horarioActual = new Horario();
    private Estudiante estudianteActual = new Estudiante();

    private String estudianteSeleccionadoId;
    private int horarioSeleccionadoId;
    
    private List<String> cursosSeleccionadosParaMotor = new ArrayList<>();
    private List<String> diasSeleccionadosParaMotor = new ArrayList<>();
    private String diaFiltro = "";
    private String nombreNuevaVersion = "";
    private String nombreVersionSeleccionada = "";

    @PostConstruct
    public void init() { actualizarTodasLasListas(); }

    private Connection getConnection() throws Exception {
        Class.forName("org.mariadb.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public void actualizarTodasLasListas() {
        cargarDocentes(); cargarCursos(); cargarAulas(); cargarHorarios(); cargarEstudiantes(); cargarVersiones();
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
    // SISTEMA DE VERSIONES (SCRUM-104, 105, 106)
    // ==========================================
    public void cargarVersiones() {
        listaVersiones = new ArrayList<>();
        try (Connection conn = getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM versiones_horarios ORDER BY fecha_creacion DESC")) {
            while (rs.next()) {
                listaVersiones.add(new VersionHorario(rs.getInt("id"), rs.getString("nombre"), rs.getTimestamp("fecha_creacion")));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void guardarVersionActual() {
        if (nombreNuevaVersion == null || nombreNuevaVersion.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", "Por favor ingrese un nombre para la version."));
            return;
        }
        try (Connection conn = getConnection()) {
            int idVersion = 0;
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO versiones_horarios (nombre) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nombreNuevaVersion);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) idVersion = rs.getInt(1);
            }
            if (idVersion > 0) {
                String sqlCopia = "INSERT INTO detalles_version_horario (id_version, dia, hora, docente, curso, aula, estado) " +
                                  "SELECT ?, dia, hora, docente, curso, aula, estado FROM horarios";
                try (PreparedStatement ps = conn.prepareStatement(sqlCopia)) {
                    ps.setInt(1, idVersion);
                    ps.executeUpdate();
                }
            }
            nombreNuevaVersion = ""; 
            cargarVersiones(); 
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Exito", "Version guardada correctamente."));
        } catch (Exception e) { manejarError(e); }
    }

    public void eliminarVersion(VersionHorario v) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM versiones_horarios WHERE id=?")) {
            ps.setInt(1, v.getId());
            ps.executeUpdate();
            cargarVersiones();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Eliminado", "La version historica fue eliminada."));
        } catch (Exception e) { manejarError(e); }
    }

    public void verDetallesVersion(VersionHorario v) {
        this.nombreVersionSeleccionada = v.getNombre();
        this.detallesVersionSeleccionada = new ArrayList<>();
        String sql = "SELECT d.*, c.nombre AS curso_nombre " +
                     "FROM detalles_version_horario d " +
                     "LEFT JOIN cursos c ON d.curso = c.codigo " +
                     "WHERE d.id_version = ?";
                     
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, v.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String est = rs.getString("estado");
                String nombreCurso = rs.getString("curso_nombre"); 
                if (nombreCurso == null) nombreCurso = rs.getString("curso");
                
                detallesVersionSeleccionada.add(new Horario(
                    rs.getInt("id"), rs.getString("dia"), rs.getString("hora"), 
                    rs.getString("docente"), rs.getString("curso"), rs.getString("aula"), 
                    (est != null ? est : "BORRADOR"), nombreCurso, 0, 0
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // NUEVO MÉTODO: RESTAURAR VERSIÓN
    public void restaurarVersion(VersionHorario v) {
        try (Connection conn = getConnection()) {
            // 1. Limpiar inscripciones para evitar conflictos de llaves foráneas
            try (PreparedStatement ps1 = conn.prepareStatement("UPDATE estudiantes SET creditos_matriculados = 0")) { ps1.executeUpdate(); }
            try (PreparedStatement ps2 = conn.prepareStatement("DELETE FROM inscripciones")) { ps2.executeUpdate(); }
            
            // 2. Borrar el horario activo
            try (PreparedStatement ps3 = conn.prepareStatement("DELETE FROM horarios")) { ps3.executeUpdate(); }
            
            // 3. Insertar los datos de la versión histórica seleccionada
            String sqlRestaurar = "INSERT INTO horarios (dia, hora, docente, curso, aula, estado) " +
                                  "SELECT dia, hora, docente, curso, aula, estado FROM detalles_version_horario WHERE id_version = ?";
            try (PreparedStatement ps4 = conn.prepareStatement(sqlRestaurar)) {
                ps4.setInt(1, v.getId());
                ps4.executeUpdate();
            }
            
            actualizarTodasLasListas(); // Refrescar todo el sistema
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Restaurado", "El horario actual fue reemplazado por la version: " + v.getNombre()));
        } catch (Exception e) { manejarError(e); }
    }

    // ==========================================
    // MOTOR DE GENERACIÓN AUTOMÁTICA
    // ==========================================
    public void generarHorariosAutomaticos() {
        if (cursosSeleccionadosParaMotor == null || cursosSeleccionadosParaMotor.isEmpty() ||
            diasSeleccionadosParaMotor == null || diasSeleccionadosParaMotor.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Faltan Parametros", "Debe seleccionar al menos una materia y un dia."));
            return;
        }

        String[] horasPosibles = {"08:00 - 10:00", "10:00 - 12:00", "14:00 - 16:00", "16:00 - 18:00"};
        int sesionesAsignadas = 0;
        List<Curso> cursosAProcesar = listaCursos.stream().filter(c -> cursosSeleccionadosParaMotor.contains(c.getCodigo())).collect(Collectors.toList());
        
        List<Aula> aulasOptimizadas = new ArrayList<>(listaAulas);
        aulasOptimizadas.sort(Comparator.comparingInt(Aula::getCapacidad));

        for (Curso curso : cursosAProcesar) {
            String docenteDelCurso = curso.getDocenteAsignado();
            if (docenteDelCurso == null || docenteDelCurso.isEmpty()) continue;
            
            for (String dia : diasSeleccionadosParaMotor) {
                boolean asignadoEnEsteDia = false;
                for (String hora : horasPosibles) {
                    if (asignadoEnEsteDia) break;
                    for (Aula aula : aulasOptimizadas) { 
                        if (aula.getCapacidad() > 0) {
                            if (!existeCruce("docente", docenteDelCurso, dia, hora) && !existeCruce("aula", aula.getNumero(), dia, hora)) {
                                String sql = "INSERT INTO horarios (dia, hora, docente, curso, aula, estado) VALUES (?, ?, ?, ?, ?, 'BORRADOR')";
                                try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                                    ps.setString(1, dia); ps.setString(2, hora); ps.setString(3, docenteDelCurso);
                                    ps.setString(4, curso.getCodigo()); ps.setString(5, aula.getNumero());
                                    ps.executeUpdate(); 
                                    sesionesAsignadas++; asignadoEnEsteDia = true; break; 
                                } catch (Exception e) { manejarError(e); }
                            }
                        }
                    }
                }
            }
        }
        cargarHorarios();
        cursosSeleccionadosParaMotor.clear();
        diasSeleccionadosParaMotor.clear();
        if (sesionesAsignadas > 0) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Motor Finalizado", "Se generaron " + sesionesAsignadas + " clases correctamente."));
        }
    }

    // ==========================================
    // NOTIFICACIONES POR CORREO
    // ==========================================
    private void enviarCorreoGlobal(String asunto, String contenido) {
        new Thread(() -> {
            try {
                List<String> correos = new ArrayList<>();
                try (Connection conn = getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT correo FROM estudiantes WHERE correo IS NOT NULL AND correo != ''")) {
                    while(rs.next()) correos.add(rs.getString("correo"));
                }
                if (correos.isEmpty()) return;

                final String remitente = "andresfelipesalamanca2004@gmail.com";
                final String password = "ivhbsnlvaeuyxraf"; 
                
                Properties props = new Properties();
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(remitente, password);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(remitente, "Proyecto Nucleo Academico"));
                message.setSubject(asunto);
                message.setText(contenido);
                
                for (String correo : correos) {
                    message.addRecipient(Message.RecipientType.BCC, new InternetAddress(correo));
                }
                Transport.send(message);
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void notificarEstudiantesNuevoHorario(Horario h) {
        String asunto = "Nueva Clase Disponible: " + h.getCursoNombre();
        String contenido = "Hola,\n\nSe ha habilitado un nuevo horario en el sistema:\n\n"
                + "Materia: " + h.getCursoNombre() + "\n"
                + "Docente: " + h.getDocente() + "\n"
                + "Dia: " + h.getDia() + "\n"
                + "Hora: " + h.getHora() + "\n"
                + "Aula: " + h.getAula() + "\n\n"
                + "Ya puedes ingresar al portal para realizar tu inscripcion.\n\n"
                + "Saludos,\nAdministracion El Bosque";
        enviarCorreoGlobal(asunto, contenido);
    }

    private void notificarCambiosHorario(Horario h, String tipo) {
        String nombreMateria = (h.getCursoNombre() != null) ? h.getCursoNombre() : h.getCurso();
        String asunto = "";
        String contenido = "";

        if (tipo.equals("MODIFICADO")) {
            asunto = "Cambio en el Horario: " + nombreMateria;
            contenido = "Hola,\n\nTe informamos que ha habido una modificacion en un horario publicado:\n\n"
                    + "Materia: " + nombreMateria + "\n"
                    + "Docente: " + h.getDocente() + "\n"
                    + "Dia: " + h.getDia() + "\n"
                    + "Hora: " + h.getHora() + "\n"
                    + "Aula: " + h.getAula() + "\n\n"
                    + "Por favor revisa el portal para ver los detalles actualizados.\n\n"
                    + "Saludos,\nAdministracion El Bosque";
        } else if (tipo.equals("ELIMINADO")) {
            asunto = "Horario Cancelado: " + nombreMateria;
            contenido = "Hola,\n\nTe informamos que el siguiente horario ha sido cancelado y eliminado del sistema:\n\n"
                    + "Materia: " + nombreMateria + "\n"
                    + "Docente: " + h.getDocente() + "\n"
                    + "Dia: " + h.getDia() + "\n"
                    + "Hora: " + h.getHora() + "\n"
                    + "Aula: " + h.getAula() + "\n\n"
                    + "Si estabas inscrito en este horario, la materia ha sido retirada de tu carga academica.\n\n"
                    + "Saludos,\nAdministracion El Bosque";
        }
        enviarCorreoGlobal(asunto, contenido);
    }

    public void publicarHorario(Horario h) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE horarios SET estado = 'PUBLICADO' WHERE id = ?")) {
            ps.setInt(1, h.getId()); ps.executeUpdate(); 
            cargarHorarios();
            notificarEstudiantesNuevoHorario(h);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Oficializado", "El horario es publico y se estan enviando las notificaciones."));
        } catch (Exception e) { manejarError(e); }
    }

    public String editarHorario(Horario h) { 
        this.horarioActual = h; 
        return "editarHorario?faces-redirect=true"; 
    }

    public String actualizarHorario() { 
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE horarios SET docente=?, curso=? WHERE dia=? AND hora=? AND aula=?")) { 
            ps.setString(1, horarioActual.getDocente()); 
            ps.setString(2, horarioActual.getCurso()); 
            ps.setString(3, horarioActual.getDia()); 
            ps.setString(4, horarioActual.getHora()); 
            ps.setString(5, horarioActual.getAula()); 
            ps.executeUpdate(); 
            
            if ("PUBLICADO".equals(horarioActual.getEstado())) {
                notificarCambiosHorario(horarioActual, "MODIFICADO");
            }
            
            cargarHorarios(); 
            return "horario?faces-redirect=true"; 
        } catch (Exception e) { return manejarError(e); } 
    }

    public void borrarHorario(Horario h) {
        try (Connection conn = getConnection()) {
            boolean borrado = false;
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM horarios WHERE id=?")) { 
                ps.setInt(1, h.getId()); 
                if (ps.executeUpdate() > 0) borrado = true; 
            }
            if (!borrado) {
                try (PreparedStatement ps2 = conn.prepareStatement("DELETE FROM horarios WHERE dia=? AND hora=? AND aula=?")) {
                    ps2.setString(1, h.getDia()); ps2.setString(2, h.getHora()); ps2.setString(3, h.getAula());
                    if (ps2.executeUpdate() > 0) borrado = true;
                }
            }
            if (borrado) { 
                if ("PUBLICADO".equals(h.getEstado())) {
                    notificarCambiosHorario(h, "ELIMINADO");
                }
                cargarHorarios(); 
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Eliminado", "La clase se elimino correctamente."));
            } else { 
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Fallo", "No se pudo eliminar la clase.")); 
            }
        } catch (Exception e) { manejarError(e); }
    }

    // ==========================================
    // GESTIÓN DE INSCRIPCIONES
    // ==========================================
    public String inscribirMateria() {
        try {
            String codigoCurso = "";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT curso FROM horarios WHERE id = ? AND estado = 'PUBLICADO'")) {
                ps.setInt(1, horarioSeleccionadoId); ResultSet rs = ps.executeQuery();
                if (rs.next()) codigoCurso = rs.getString(1); else throw new Exception("Clase no disponible.");
            }
            if (!validarAforo(horarioSeleccionadoId)) throw new Exception("Aula sin cupo.");
            if (!validarCruceHorarioEstudiante(estudianteSeleccionadoId, horarioSeleccionadoId)) throw new Exception("Cruce detectado: Ya tienes clase en ese horario.");
            if (!validarCargaAcademica(estudianteSeleccionadoId, codigoCurso)) throw new Exception("Carga excedida (Limite 18 creditos).");

            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO inscripciones (documento_estudiante, id_horario) VALUES (?, ?)")) { ps.setString(1, estudianteSeleccionadoId); ps.setInt(2, horarioSeleccionadoId); ps.executeUpdate(); }
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE estudiantes SET creditos_matriculados = creditos_matriculados + (SELECT creditos FROM cursos WHERE codigo = ?) WHERE documento = ?")) { ps.setString(1, codigoCurso); ps.setString(2, estudianteSeleccionadoId); ps.executeUpdate(); }

            actualizarTodasLasListas(); 
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Exito", "Matriculado correctamente.")); 
            return "estudiantes?faces-redirect=true";
        } catch (Exception e) { return manejarError(e); }
    }

    public void retirarMateria(String documento, Horario h) {
        try (Connection conn = getConnection()) {
            int creditos = 0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT creditos FROM cursos WHERE codigo = ?")) {
                ps.setString(1, h.getCurso()); ResultSet rs = ps.executeQuery(); if (rs.next()) creditos = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM inscripciones WHERE documento_estudiante = ? AND id_horario = ?")) { ps.setString(1, documento); ps.setInt(2, h.getId()); ps.executeUpdate(); }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE estudiantes SET creditos_matriculados = creditos_matriculados - ? WHERE documento = ?")) { ps.setInt(1, creditos); ps.setString(2, documento); ps.executeUpdate(); }
            actualizarTodasLasListas();
        } catch (Exception e) { manejarError(e); }
    }

    public List<Horario> getMateriasInscritas(String documento) {
        List<Horario> inscritas = new ArrayList<>();
        String sql = "SELECT h.*, c.nombre FROM horarios h JOIN inscripciones i ON h.id = i.id_horario JOIN cursos c ON h.curso = c.codigo WHERE i.documento_estudiante = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, documento); ResultSet rs = ps.executeQuery();
            while (rs.next()) inscritas.add(new Horario(rs.getInt("id"), rs.getString("dia"), rs.getString("hora"), rs.getString("docente"), rs.getString("curso"), rs.getString("aula"), rs.getString("estado"), rs.getString("nombre"), 0, 0));
        } catch (Exception e) { e.printStackTrace(); }
        return inscritas;
    }

    private boolean validarCruceHorarioEstudiante(String documento, int idHorarioNuevo) throws Exception {
        String diaNuevo = "", horaNueva = "";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT dia, hora FROM horarios WHERE id = ?")) {
            ps.setInt(1, idHorarioNuevo); ResultSet rs = ps.executeQuery(); if (rs.next()) { diaNuevo = rs.getString("dia"); horaNueva = rs.getString("hora"); }
        }
        String sqlCruce = "SELECT COUNT(*) FROM inscripciones i JOIN horarios h ON i.id_horario = h.id WHERE i.documento_estudiante = ? AND h.dia = ? AND h.hora = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sqlCruce)) {
            ps.setString(1, documento); ps.setString(2, diaNuevo); ps.setString(3, horaNueva);
            ResultSet rs = ps.executeQuery(); if (rs.next() && rs.getInt(1) > 0) return false; 
        } return true; 
    }

    private boolean validarAforo(int idHorario) throws Exception { int cap = 0, ins = 0; try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT a.capacidad FROM horarios h JOIN aulas a ON h.aula = a.numero WHERE h.id = ?")) { ps.setInt(1, idHorario); ResultSet rs = ps.executeQuery(); if (rs.next()) cap = rs.getInt(1); } try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM inscripciones WHERE id_horario = ?")) { ps.setInt(1, idHorario); ResultSet rs = ps.executeQuery(); if (rs.next()) ins = rs.getInt(1); } return ins < cap; }
    private boolean validarCargaAcademica(String doc, String cod) throws Exception { int credCurso = 0, credAct = 0; try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT creditos FROM cursos WHERE codigo = ?")) { ps.setString(1, cod); ResultSet rs = ps.executeQuery(); if (rs.next()) credCurso = rs.getInt(1); } try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT creditos_matriculados FROM estudiantes WHERE documento = ?")) { ps.setString(1, doc); ResultSet rs = ps.executeQuery(); if (rs.next()) credAct = rs.getInt(1); } return (credAct + credCurso) <= 18; }
    private boolean existeCruce(String col, String val, String dia, String hora) { String sql = "SELECT COUNT(*) FROM horarios WHERE " + col + " = ? AND dia = ? AND hora = ?"; try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setString(1, val); ps.setString(2, dia); ps.setString(3, hora); ResultSet rs = ps.executeQuery(); if (rs.next()) return rs.getInt(1) > 0; } catch (Exception e) { e.printStackTrace(); } return false; }
    private String manejarError(Exception e) { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage())); return null; }

    // ==========================================
    // CRUDS GENERALES
    // ==========================================
    public void cargarHorarios() {
        listaHorarios = new ArrayList<>();
        String sql = "SELECT h.*, c.nombre AS curso_nombre, a.capacidad, (SELECT COUNT(*) FROM inscripciones i WHERE i.id_horario = h.id) AS inscritos FROM horarios h LEFT JOIN cursos c ON h.curso = c.codigo LEFT JOIN aulas a ON h.aula = a.numero";
        try (Connection conn = getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String est = rs.getString("estado"); String nombreCurso = rs.getString("curso_nombre"); if (nombreCurso == null) nombreCurso = rs.getString("curso"); 
                listaHorarios.add(new Horario(rs.getInt("id"), rs.getString("dia"), rs.getString("hora"), rs.getString("docente"), rs.getString("curso"), rs.getString("aula"), (est != null ? est : "BORRADOR"), nombreCurso, rs.getInt("inscritos"), rs.getInt("capacidad"))); 
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    public void cargarEstudiantes() { 
        listaEstudiantes = new ArrayList<>(); 
        try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM estudiantes")) { 
            while (r.next()) listaEstudiantes.add(new Estudiante(r.getString("documento"), r.getString("nombre"), r.getString("correo"), r.getString("carrera"), r.getInt("semestre_actual"), r.getInt("creditos_matriculados"))); 
        } catch (Exception e) { e.printStackTrace(); } 
    }

    public String guardarEstudiante() { 
        try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement("INSERT INTO estudiantes (documento, nombre, correo, carrera, semestre_actual, creditos_matriculados) VALUES (?,?,?,?,?,0)")) { 
            p.setString(1, estudianteActual.getDocumento()); p.setString(2, estudianteActual.getNombre()); p.setString(3, estudianteActual.getCorreo()); p.setString(4, estudianteActual.getCarrera()); p.setInt(5, estudianteActual.getSemestreActual()); p.executeUpdate(); cargarEstudiantes(); return "estudiantes?faces-redirect=true"; 
        } catch (Exception e) { return manejarError(e); } 
    }

    public String actualizarEstudiante() { 
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE estudiantes SET nombre=?, correo=?, carrera=?, semestre_actual=? WHERE documento=?")) { 
            ps.setString(1, estudianteActual.getNombre()); ps.setString(2, estudianteActual.getCorreo()); ps.setString(3, estudianteActual.getCarrera()); ps.setInt(4, estudianteActual.getSemestreActual()); ps.setString(5, estudianteActual.getDocumento()); ps.executeUpdate(); cargarEstudiantes(); return "estudiantes?faces-redirect=true"; 
        } catch (Exception e) { return manejarError(e); } 
    }

    public void cargarDocentes() { listaDocentes = new ArrayList<>(); try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM docentes")) { while (r.next()) listaDocentes.add(new Docente(r.getString("nombre"), r.getString("correo"), r.getString("tipo"), r.getString("departamento"))); } catch (Exception e) { e.printStackTrace(); } }
    public void cargarCursos() { listaCursos = new ArrayList<>(); try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM cursos")) { while (r.next()) listaCursos.add(new Curso(r.getString("codigo"), r.getString("nombre"), r.getInt("creditos"), r.getString("docente_asignado"))); } catch (Exception e) { e.printStackTrace(); } }
    public void cargarAulas() { listaAulas = new ArrayList<>(); try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM aulas")) { while (r.next()) listaAulas.add(new Aula(r.getString("numero"), r.getString("tipo"), r.getInt("capacidad"))); } catch (Exception e) { e.printStackTrace(); } }

    public String guardarDocente() { try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement("INSERT INTO docentes (nombre, correo, tipo, departamento) VALUES (?,?,?,?)")) { p.setString(1, docenteActual.getNombre()); p.setString(2, docenteActual.getCorreo()); p.setString(3, docenteActual.getTipo()); p.setString(4, docenteActual.getDepartamento()); p.executeUpdate(); cargarDocentes(); return "docentes?faces-redirect=true"; } catch (Exception e) { return manejarError(e); } }
    public String actualizarDocente() { try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE docentes SET nombre=?, tipo=?, departamento=? WHERE correo=?")) { ps.setString(1, docenteActual.getNombre()); ps.setString(2, docenteActual.getTipo()); ps.setString(3, docenteActual.getDepartamento()); ps.setString(4, docenteActual.getCorreo()); ps.executeUpdate(); cargarDocentes(); return "docentes?faces-redirect=true"; } catch (Exception e) { return manejarError(e); } }
    public void borrarDocente(Docente d) { try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM docentes WHERE correo=?")) { ps.setString(1, d.getCorreo()); ps.executeUpdate(); cargarDocentes(); } catch (Exception e) { manejarError(e); } }

    public String guardarCurso() { try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement("INSERT INTO cursos (codigo, nombre, creditos, docente_asignado) VALUES (?, ?, ?, ?)")) { p.setString(1, cursoActual.getCodigo()); p.setString(2, cursoActual.getNombre()); p.setInt(3, cursoActual.getCreditos()); p.setString(4, cursoActual.getDocenteAsignado()); p.executeUpdate(); cargarCursos(); return "cursos?faces-redirect=true"; } catch (Exception e) { return manejarError(e); } }
    public String actualizarCurso() { try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE cursos SET nombre=?, creditos=?, docente_asignado=? WHERE codigo=?")) { ps.setString(1, cursoActual.getNombre()); ps.setInt(2, cursoActual.getCreditos()); ps.setString(3, cursoActual.getDocenteAsignado()); ps.setString(4, cursoActual.getCodigo()); ps.executeUpdate(); cargarCursos(); return "cursos?faces-redirect=true"; } catch (Exception e) { return manejarError(e); } }
    public void borrarCurso(Curso c) { try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM cursos WHERE codigo=?")) { ps.setString(1, c.getCodigo()); ps.executeUpdate(); cargarCursos(); } catch (Exception e) { manejarError(e); } }

    public String guardarAula() { try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement("INSERT INTO aulas (numero, tipo, capacidad) VALUES (?, ?, ?)")) { p.setString(1, aulaActual.getNumero()); p.setString(2, aulaActual.getTipo()); p.setInt(3, aulaActual.getCapacidad()); p.executeUpdate(); cargarAulas(); return "aulas?faces-redirect=true"; } catch (Exception e) { return manejarError(e); } }
    public String actualizarAula() { try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE aulas SET tipo=?, capacidad=? WHERE numero=?")) { ps.setString(1, aulaActual.getTipo()); ps.setInt(2, aulaActual.getCapacidad()); ps.setString(3, aulaActual.getNumero()); ps.executeUpdate(); cargarAulas(); return "aulas?faces-redirect=true"; } catch (Exception e) { return manejarError(e); } }
    public void borrarAula(Aula a) { try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM aulas WHERE numero=?")) { ps.setString(1, a.getNumero()); ps.executeUpdate(); cargarAulas(); } catch (Exception e) { manejarError(e); } }

    // ==========================================
    // NAVEGACIÓN Y GETTERS/SETTERS COMPLETOS
    // ==========================================
    public String irAgregarDocente() { this.docenteActual = new Docente(); return "agregarDocentes?faces-redirect=true"; }
    public String editarDocente(Docente d) { this.docenteActual = d; return "editarDocentes?faces-redirect=true"; }
    
    public String irAgregarCurso() { this.cursoActual = new Curso(); return "agregarCursos?faces-redirect=true"; }
    public String editarCurso(Curso c) { this.cursoActual = c; return "editarCursos?faces-redirect=true"; }
    
    public String irAgregarAula() { this.aulaActual = new Aula(); return "agregarAulas?faces-redirect=true"; }
    public String editarAula(Aula a) { this.aulaActual = a; return "editarAulas?faces-redirect=true"; }
    
    public String irAgregarHorario() { this.horarioActual = new Horario(); return "agregarHorario?faces-redirect=true"; }
    
    public String irAgregarEstudiante() { this.estudianteActual = new Estudiante(); return "agregarEstudiantes?faces-redirect=true"; }
    public String editarEstudiante(Estudiante e) { this.estudianteActual = e; return "editarEstudiantes?faces-redirect=true"; }
    public void borrarEstudiante(Estudiante e) { try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM estudiantes WHERE documento=?")) { ps.setString(1, e.getDocumento()); ps.executeUpdate(); cargarEstudiantes(); } catch (Exception ex) { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se puede borrar.")); } }

    public String getUsuario() { return usuario; } public void setUsuario(String u) { this.usuario = u; }
    public String getClave() { return clave; } public void setClave(String c) { this.clave = c; }
    
    public List<Docente> getListaDocentes() { return listaDocentes; } public void setListaDocentes(List<Docente> ld) { this.listaDocentes = ld; }
    public List<Docente> getDocentesFiltrados() { return docentesFiltrados; } public void setDocentesFiltrados(List<Docente> df) { this.docentesFiltrados = df; }
    
    public List<Curso> getListaCursos() { return listaCursos; } public void setListaCursos(List<Curso> lc) { this.listaCursos = lc; }
    
    public List<Aula> getListaAulas() { return listaAulas; } public void setListaAulas(List<Aula> la) { this.listaAulas = la; }
    
    public List<Horario> getListaHorarios() { return listaHorarios; } public void setListaHorarios(List<Horario> lh) { this.listaHorarios = lh; }
    public List<Horario> getHorariosFiltrados() { return horariosFiltrados; } public void setHorariosFiltrados(List<Horario> hf) { this.horariosFiltrados = hf; }
    public List<Horario> getHorariosPublicados() { return listaHorarios.stream().filter(h -> "PUBLICADO".equals(h.getEstado())).collect(Collectors.toList()); }
    public List<Horario> getHorariosMostrados() { if (diaFiltro == null || diaFiltro.isEmpty()) return listaHorarios; return listaHorarios.stream().filter(h -> diaFiltro.equals(h.getDia())).collect(Collectors.toList()); }
    
    public List<Estudiante> getListaEstudiantes() { return listaEstudiantes; } public void setListaEstudiantes(List<Estudiante> le) { this.listaEstudiantes = le; }
    
    public List<VersionHorario> getListaVersiones() { return listaVersiones; } public void setListaVersiones(List<VersionHorario> lv) { this.listaVersiones = lv; }
    public String getNombreNuevaVersion() { return nombreNuevaVersion; } public void setNombreNuevaVersion(String nv) { this.nombreNuevaVersion = nv; }
    
    public List<Horario> getDetallesVersionSeleccionada() { return detallesVersionSeleccionada; } public void setDetallesVersionSeleccionada(List<Horario> d) { this.detallesVersionSeleccionada = d; }
    public String getNombreVersionSeleccionada() { return nombreVersionSeleccionada; } public void setNombreVersionSeleccionada(String n) { this.nombreVersionSeleccionada = n; }

    public Docente getDocenteActual() { return docenteActual; } public void setDocenteActual(Docente d) { this.docenteActual = d; }
    public Curso getCursoActual() { return cursoActual; } public void setCursoActual(Curso c) { this.cursoActual = c; }
    public Aula getAulaActual() { return aulaActual; } public void setAulaActual(Aula a) { this.aulaActual = a; }
    public Horario getHorarioActual() { return horarioActual; } public void setHorarioActual(Horario h) { this.horarioActual = h; }
    public Estudiante getEstudianteActual() { return estudianteActual; } public void setEstudianteActual(Estudiante e) { this.estudianteActual = e; }
    
    public String getEstudianteSeleccionadoId() { return estudianteSeleccionadoId; } public void setEstudianteSeleccionadoId(String e) { this.estudianteSeleccionadoId = e; }
    public int getHorarioSeleccionadoId() { return horarioSeleccionadoId; } public void setHorarioSeleccionadoId(int h) { this.horarioSeleccionadoId = h; }
    
    public List<String> getCursosSeleccionadosParaMotor() { return cursosSeleccionadosParaMotor; } public void setCursosSeleccionadosParaMotor(List<String> c) { this.cursosSeleccionadosParaMotor = c; }
    public List<String> getDiasSeleccionadosParaMotor() { return diasSeleccionadosParaMotor; } public void setDiasSeleccionadosParaMotor(List<String> d) { this.diasSeleccionadosParaMotor = d; }
    
    public String getDiaFiltro() { return diaFiltro; } public void setDiaFiltro(String diaFiltro) { this.diaFiltro = diaFiltro; }
}