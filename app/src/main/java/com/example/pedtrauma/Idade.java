package com.example.pedtrauma;

import android.content.Context;

/**
 * Idade do paciente. A fonte canônica é o total em <b>meses</b>, para
 * atender lactentes (casos de poucos meses de vida). O campo de anos
 * continua sendo gravado para compatibilidade com os registros antigos.
 */
final class Idade {

    static final int MESES_MAXIMO = 216; // 18 anos

    private Idade() {
    }

    /** Total de meses, convertendo registros antigos que só têm anos. */
    static int mesesTotais(int anos, Integer meses) {
        if (meses != null && meses > 0) return meses;
        return anos * 12;
    }

    /** Anos inteiros correspondentes (o que vai no campo de anos). */
    static int anosInteiros(int meses) {
        return meses / 12;
    }

    /**
     * Texto da idade: "8 meses", "1 ano", "2 anos", "2 anos e 6 meses".
     * Abaixo de 2 anos mostra só em meses, como é usual na pediatria.
     */
    static String formatar(Context contexto, int meses) {
        if (meses <= 0) return contexto.getString(R.string.idade_nao_informada);

        if (meses < 24) {
            return contexto.getResources()
                    .getQuantityString(R.plurals.idade_meses, meses, meses);
        }

        int anos = anosInteiros(meses);
        int resto = meses % 12;
        String textoAnos = contexto.getResources()
                .getQuantityString(R.plurals.idade_anos, anos, anos);
        if (resto == 0) return textoAnos;

        String textoMeses = contexto.getResources()
                .getQuantityString(R.plurals.idade_meses, resto, resto);
        return contexto.getString(R.string.idade_anos_e_meses, textoAnos, textoMeses);
    }

    /** Atalho a partir do paciente. */
    static String formatar(Context contexto, Paciente paciente) {
        return formatar(contexto, mesesTotais(paciente.getIdade(), paciente.getIdadeMeses()));
    }

    /** Atalho a partir da avaliação. */
    static String formatar(Context contexto, Avaliacao avaliacao) {
        return formatar(contexto,
                mesesTotais(avaliacao.getPacienteIdade(), avaliacao.getPacienteIdadeMeses()));
    }
}
