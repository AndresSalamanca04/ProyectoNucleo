package co.edu.unbosque.model;

public class Aula {
    private String numero;
    private String tipo;
    private int capacidad;

    public Aula() {}
    public Aula(String numero, String tipo, int capacidad) {
        this.numero = numero; this.tipo = tipo; this.capacidad = capacidad;
    }
    // Getters y Setters
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }
}