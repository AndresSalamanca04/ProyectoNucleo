package co.edu.unbosque.model;
import java.io.Serializable;

public class Aula implements Serializable {
    private static final long serialVersionUID = 1L;
    private String numero; private String tipo; private int capacidad;

    public Aula() {}
    public Aula(String numero, String tipo, int capacidad) {
        this.numero = numero; this.tipo = tipo; this.capacidad = capacidad;
    }
    public String getNumero() { return numero; } public void setNumero(String numero) { this.numero = numero; }
    public String getTipo() { return tipo; } public void setTipo(String tipo) { this.tipo = tipo; }
    public int getCapacidad() { return capacidad; } public void setCapacidad(int capacidad) { this.capacidad = capacidad; }
}