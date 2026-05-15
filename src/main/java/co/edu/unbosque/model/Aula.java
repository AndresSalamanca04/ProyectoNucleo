package co.edu.unbosque.model;

import java.io.Serializable;

/**
 * Clase que representa un Aula en el sistema.
 */
public class Aula implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Número o identificador del aula.
     */
    private String numero;

    /**
     * Tipo de aula.
     */
    private String tipo;

    /**
     * Capacidad máxima de personas en el aula.
     */
    private int capacidad;

    /**
     * Constructor por defecto.
     */
    public Aula() {
    }

    /**
     * Constructor con parámetros.
     * * @param numero    El número del aula.
     * @param tipo      El tipo de aula.
     * @param capacidad La capacidad del aula.
     */
    public Aula(String numero, String tipo, int capacidad) {
        this.numero = numero;
        this.tipo = tipo;
        this.capacidad = capacidad;
    }

    /**
     * Obtiene el número del aula.
     * * @return El número del aula.
     */
    public String getNumero() {
        return numero;
    }

    /**
     * Establece el número del aula.
     * * @param numero El nuevo número del aula.
     */
    public void setNumero(String numero) {
        this.numero = numero;
    }

    /**
     * Obtiene el tipo de aula.
     * * @return El tipo de aula.
     */
    public String getTipo() {
        return tipo;
    }

    /**
     * Establece el tipo de aula.
     * * @param tipo El nuevo tipo de aula.
     */
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    /**
     * Obtiene la capacidad del aula.
     * * @return La capacidad del aula.
     */
    public int getCapacidad() {
        return capacidad;
    }

    /**
     * Establece la capacidad del aula.
     * * @param capacidad La nueva capacidad del aula.
     */
    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }
}