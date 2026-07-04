package com.example.pedtrauma;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

/**
 * Uma avaliação PTS de um paciente.
 * Documento em usuarios/{uid}/avaliacoes/{avaliacaoId}.
 * Avaliações antigas nunca são editadas (histórico cronológico).
 */
public class Avaliacao {

    private String pacienteId;
    private String pacienteNome;
    private int pacienteIdade;
    private String sexo;

    private String tipoTrauma;
    private String horaOcorrencia;
    private String horaAvaliacao;
    private long tempoDecorridoMin;

    /** Índice da opção escolhida em cada parâmetro (0 = +2, 1 = +1, 2 = -1). */
    private List<Integer> respostas;
    private int pontuacao;
    private String interpretacao;

    private Boolean exameImagem;
    private String achados;

    @ServerTimestamp
    private Date criadoEm;

    public Avaliacao() {
    }

    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }

    public String getPacienteNome() { return pacienteNome; }
    public void setPacienteNome(String pacienteNome) { this.pacienteNome = pacienteNome; }

    public int getPacienteIdade() { return pacienteIdade; }
    public void setPacienteIdade(int pacienteIdade) { this.pacienteIdade = pacienteIdade; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getTipoTrauma() { return tipoTrauma; }
    public void setTipoTrauma(String tipoTrauma) { this.tipoTrauma = tipoTrauma; }

    public String getHoraOcorrencia() { return horaOcorrencia; }
    public void setHoraOcorrencia(String horaOcorrencia) { this.horaOcorrencia = horaOcorrencia; }

    public String getHoraAvaliacao() { return horaAvaliacao; }
    public void setHoraAvaliacao(String horaAvaliacao) { this.horaAvaliacao = horaAvaliacao; }

    public long getTempoDecorridoMin() { return tempoDecorridoMin; }
    public void setTempoDecorridoMin(long tempoDecorridoMin) { this.tempoDecorridoMin = tempoDecorridoMin; }

    public List<Integer> getRespostas() { return respostas; }
    public void setRespostas(List<Integer> respostas) { this.respostas = respostas; }

    public int getPontuacao() { return pontuacao; }
    public void setPontuacao(int pontuacao) { this.pontuacao = pontuacao; }

    public String getInterpretacao() { return interpretacao; }
    public void setInterpretacao(String interpretacao) { this.interpretacao = interpretacao; }

    public Boolean getExameImagem() { return exameImagem; }
    public void setExameImagem(Boolean exameImagem) { this.exameImagem = exameImagem; }

    public String getAchados() { return achados; }
    public void setAchados(String achados) { this.achados = achados; }

    public Date getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Date criadoEm) { this.criadoEm = criadoEm; }
}
