package com.example.pedtrauma;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Modelo do usuário salvo no Firestore (coleção "usuarios").
 * O documento usa o UID do Firebase Auth como ID.
 *
 * Obs.: o construtor vazio é obrigatório para o Firestore
 * desserializar o objeto automaticamente.
 */
public class Usuario {

    private String nome;
    private String registro;
    private String cpf;
    private String email;

    /** Foto de perfil em JPEG comprimido, codificada em Base64. */
    private String fotoBase64;

    @ServerTimestamp
    private Date criadoEm;

    public Usuario() {
    }

    public Usuario(String nome, String registro, String cpf, String email) {
        this.nome = nome;
        this.registro = registro;
        this.cpf = cpf;
        this.email = email;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getRegistro() { return registro; }
    public void setRegistro(String registro) { this.registro = registro; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFotoBase64() { return fotoBase64; }
    public void setFotoBase64(String fotoBase64) { this.fotoBase64 = fotoBase64; }

    public Date getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Date criadoEm) { this.criadoEm = criadoEm; }
}
