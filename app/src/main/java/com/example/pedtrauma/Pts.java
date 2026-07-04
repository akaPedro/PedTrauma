package com.example.pedtrauma;

/**
 * Tabela do Escore de Trauma Pediátrico (PTS), conforme o
 * documento de conteúdo do app. Compartilhada entre o carrossel
 * de avaliação e a exportação em PDF.
 */
final class Pts {

    /** Pontos por opção: 1ª = +2, 2ª = +1, 3ª = -1. */
    static final int[] PONTOS = {2, 1, -1};

    /** PTS ≤ 8 indica maior potencial de mortalidade. */
    static final int LIMITE = 8;

    static final String[] PERGUNTAS = {
            "PESO DO PACIENTE?",
            "VIA AÉREA",
            "PRESSÃO ARTERIAL SISTÓLICA",
            "SISTEMA NERVOSO CENTRAL",
            "FERIDA ABERTA",
            "ESQUELETO"
    };

    static final String[][] OPCOES = {
            {"Maior ou igual a 20 kg", "10 - 19 kg", "Menor ou igual a 10 kg"},
            {"Normal", "Mantida", "Não mantida"},
            {"Maior 90 mmHg", "50 - 89 mmHg", "Menor 50 mmHg"},
            {"Acordado", "Obnubilado ou perda de consciência", "Coma ou descerebração"},
            {"Nenhuma", "Menor", "Maior ou penetrante"},
            {"Nenhuma", "Fratura fechada", "Fratura aberta ou múltipla"}
    };

    private Pts() {
    }
}
