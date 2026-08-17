package com.example.pedtrauma;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Paciente cadastrado pelo profissional.
 * Documento em usuarios/{uid}/pacientes/{pacienteId}.
 * Os campos "ultima*" são um resumo da avaliação mais recente,
 * atualizados a cada nova avaliação (evita consulta extra com índice).
 */
public class Paciente {

    private String nome;
    /** Anos inteiros — mantido para compatibilidade com registros antigos. */
    private int idade;
    /** Idade total em meses (fonte canônica, atende lactentes). */
    private Integer idadeMeses;
    private String sexo;

    private Integer ultimaNota;
    private String ultimaInterpretacao;
    private Date ultimaData;

    @ServerTimestamp
    private Date criadoEm;

    public Paciente() {
    }

    public Paciente(String nome, int idadeMeses, String sexo) {
        this.nome = nome;
        this.idade = Idade.anosInteiros(idadeMeses);
        this.idadeMeses = idadeMeses;
        this.sexo = sexo;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public int getIdade() { return idade; }
    public void setIdade(int idade) { this.idade = idade; }

    public Integer getIdadeMeses() { return idadeMeses; }
    public void setIdadeMeses(Integer idadeMeses) { this.idadeMeses = idadeMeses; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public Integer getUltimaNota() { return ultimaNota; }
    public void setUltimaNota(Integer ultimaNota) { this.ultimaNota = ultimaNota; }

    public String getUltimaInterpretacao() { return ultimaInterpretacao; }
    public void setUltimaInterpretacao(String ultimaInterpretacao) { this.ultimaInterpretacao = ultimaInterpretacao; }

    public Date getUltimaData() { return ultimaData; }
    public void setUltimaData(Date ultimaData) { this.ultimaData = ultimaData; }

    public Date getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Date criadoEm) { this.criadoEm = criadoEm; }
}
