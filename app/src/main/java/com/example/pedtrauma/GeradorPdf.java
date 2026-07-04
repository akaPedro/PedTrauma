package com.example.pedtrauma;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Gera o PDF de uma avaliação PTS conforme o documento de conteúdo:
 * logo, dados do profissional e do paciente, data e hora, parâmetros
 * escolhidos com pontuação individual, pontuação total, interpretação,
 * observações, resultado do exame de imagem e campo para assinatura manual.
 *
 * Tudo em uma única página A4: a assinatura fica fixa no rodapé e os
 * textos longos são cortados com reticências para não invadi-la.
 */
final class GeradorPdf {

    private static final int LARGURA = 595;  // A4 em pontos
    private static final int ALTURA = 842;
    private static final int MARGEM = 40;
    private static final int LARGURA_UTIL = LARGURA - 2 * MARGEM;

    /** Limite vertical do conteúdo; abaixo fica a área da assinatura. */
    private static final int LIMITE_CONTEUDO = ALTURA - 140;

    private static final int AZUL_MARINHO = 0xFF123160;
    private static final int AZUL = 0xFF4593E5;
    private static final int VERMELHO = 0xFFD42027;
    private static final int PRETO = 0xFF1A1A1A;
    private static final int CINZA = 0xFF9E9E9E;

    private static final float ALTURA_LINHA = 16f;

    private final Context contexto;
    private final PdfDocument documento = new PdfDocument();
    private Canvas canvas;
    private float y;

    private GeradorPdf(Context contexto) {
        this.contexto = contexto;
    }

    /** Gera o arquivo em cache (compartilhado via FileProvider). */
    static File gerar(Context contexto, Avaliacao avaliacao, Usuario profissional)
            throws IOException {
        return new GeradorPdf(contexto).construir(avaliacao, profissional);
    }

    private File construir(Avaliacao a, Usuario profissional) throws IOException {
        PdfDocument.Page pagina = documento.startPage(
                new PdfDocument.PageInfo.Builder(LARGURA, ALTURA, 1).create());
        canvas = pagina.getCanvas();
        y = MARGEM + 8;

        cabecalho(a);

        secao("Dados do profissional");
        if (profissional != null) {
            linha("Nome", profissional.getNome());
            linha("Registro no Conselho", profissional.getRegistro());
            linha("E-mail", profissional.getEmail());
        }

        secao("Dados do paciente");
        linha("Nome", a.getPacienteNome());
        linha("Idade", a.getPacienteIdade() + " anos");
        linha("Sexo", a.getSexo());

        secao("Avaliação");
        linha("Tipo de trauma", null);
        paragrafo(a.getTipoTrauma(), 3);
        linha("Hora da ocorrência", a.getHoraOcorrencia());
        linha("Hora da avaliação", a.getHoraAvaliacao());
        linha("Tempo decorrido", a.getTempoDecorridoMin() + " minutos");

        secao("Parâmetros avaliados");
        List<Integer> respostas = a.getRespostas();
        if (respostas != null) {
            for (int i = 0; i < Pts.PERGUNTAS.length && i < respostas.size(); i++) {
                int opcao = respostas.get(i);
                if (opcao < 0 || opcao > 2) continue;
                int pontos = Pts.PONTOS[opcao];
                String valor = Pts.OPCOES[i][opcao]
                        + "  (" + (pontos > 0 ? "+" : "") + pontos
                        + (Math.abs(pontos) == 1 ? " ponto)" : " pontos)");
                linha(capitalizar(Pts.PERGUNTAS[i]), valor);
            }
        }

        secao("Resultado");
        int pts = a.getPontuacao();
        textoDestaque("Pontuação total (PTS): " + pts + " pontos",
                pts <= Pts.LIMITE ? VERMELHO : AZUL);
        linha("Interpretação", a.getInterpretacao());

        secao("Observações");
        if (a.getObservacoes() != null && !a.getObservacoes().isEmpty()) {
            paragrafo(a.getObservacoes(), 3);
        } else {
            linha("Sem observações registradas", null);
        }

        secao("Exame de imagem");
        boolean fezExame = Boolean.TRUE.equals(a.getExameImagem());
        linha("Realizou exame de imagem?", fezExame ? "Sim" : "Não");
        if (fezExame && a.getAchados() != null && !a.getAchados().isEmpty()) {
            linha("Resultado / Achados do exame", null);
            paragrafo(a.getAchados(), 3);
        }

        assinatura();

        documento.finishPage(pagina);

        File pasta = new File(contexto.getCacheDir(), "pdfs");
        if (!pasta.exists() && !pasta.mkdirs()) {
            throw new IOException("Não foi possível criar a pasta de PDFs");
        }
        File arquivo = new File(pasta, nomeArquivo(a));
        try (FileOutputStream saida = new FileOutputStream(arquivo)) {
            documento.writeTo(saida);
        } finally {
            documento.close();
        }
        return arquivo;
    }

    static String nomeArquivo(Avaliacao a) {
        String nome = a.getPacienteNome() == null ? "paciente"
                : a.getPacienteNome().trim().replaceAll("[^\\p{L}\\p{N}]+", "_");
        String data = new SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault())
                .format(a.getCriadoEm() == null ? new Date() : a.getCriadoEm());
        return "PTS_" + nome + "_" + data + ".pdf";
    }

    // ---------- blocos do documento ----------

    private void cabecalho(Avaliacao a) {
        Bitmap logo = BitmapFactory.decodeResource(
                contexto.getResources(), R.drawable.logpng);
        if (logo != null) {
            canvas.drawBitmap(Bitmap.createScaledBitmap(logo, 60, 60, true),
                    MARGEM, y, null);
        }

        canvas.drawText("PedTrauma", MARGEM + 72, y + 24,
                paint(19, AZUL_MARINHO, true));

        Paint subtitulo = paint(10.5f, PRETO, false);
        canvas.drawText("Avaliação do Escore de Trauma Pediátrico (PTS)",
                MARGEM + 72, y + 41, subtitulo);

        String data = new SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
                .format(a.getCriadoEm() == null ? new Date() : a.getCriadoEm());
        canvas.drawText("Data e hora da avaliação: " + data,
                MARGEM + 72, y + 56, subtitulo);

        y += 70;
        Paint p = paint(1, CINZA, false);
        p.setStrokeWidth(1);
        canvas.drawLine(MARGEM, y, MARGEM + LARGURA_UTIL, y, p);
        y += 2;
    }

    private void secao(String titulo) {
        if (y > LIMITE_CONTEUDO - 30) return;
        y += 12;
        canvas.drawText(titulo, MARGEM, y, paint(12, AZUL_MARINHO, true));
        y += 5;
        Paint p = paint(1, AZUL, false);
        p.setStrokeWidth(1.2f);
        canvas.drawLine(MARGEM, y, MARGEM + LARGURA_UTIL, y, p);
        y += 12;
    }

    private void linha(String rotulo, String valor) {
        if (y > LIMITE_CONTEUDO) return;
        Paint negrito = paint(10.5f, PRETO, true);
        String texto = rotulo + ":";
        canvas.drawText(texto, MARGEM, y, negrito);
        if (valor != null) {
            float larguraRotulo = negrito.measureText(texto + "  ");
            canvas.drawText(valor, MARGEM + larguraRotulo, y,
                    paint(10.5f, PRETO, false));
        }
        y += ALTURA_LINHA;
    }

    /**
     * Texto com quebra de linha automática, limitado a {@code maxLinhas}
     * (corta com reticências) para caber na página única.
     */
    private void paragrafo(String texto, int maxLinhas) {
        if (texto == null || texto.isEmpty() || y > LIMITE_CONTEUDO) return;

        int linhasDisponiveis = (int) ((LIMITE_CONTEUDO - y) / ALTURA_LINHA);
        int linhas = Math.max(1, Math.min(maxLinhas, linhasDisponiveis));

        TextPaint tp = new TextPaint(paint(10.5f, PRETO, false));
        StaticLayout layout = StaticLayout.Builder
                .obtain(texto, 0, texto.length(), tp, LARGURA_UTIL)
                .setMaxLines(linhas)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build();

        canvas.save();
        canvas.translate(MARGEM, y - 10);
        layout.draw(canvas);
        canvas.restore();
        y += layout.getHeight() + 6;
    }

    private void textoDestaque(String texto, int cor) {
        if (y > LIMITE_CONTEUDO) return;
        y += 2;
        canvas.drawText(texto, MARGEM, y, paint(13, cor, true));
        y += 19;
    }

    /** Campo de assinatura manual, fixo no rodapé da página. */
    private void assinatura() {
        float linhaY = ALTURA - 80;
        float centro = LARGURA / 2f;

        Paint p = paint(1, PRETO, false);
        p.setStrokeWidth(1);
        canvas.drawLine(centro - 130, linhaY, centro + 130, linhaY, p);

        Paint rotulo = paint(10, PRETO, false);
        String texto = "Assinatura do profissional";
        canvas.drawText(texto, centro - rotulo.measureText(texto) / 2,
                linhaY + 14, rotulo);
    }

    // ---------- infraestrutura ----------

    private Paint paint(float tamanho, int cor, boolean negrito) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(tamanho);
        p.setColor(cor);
        p.setTypeface(negrito ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        return p;
    }

    private static String capitalizar(String texto) {
        String limpo = texto.replace("?", "").trim().toLowerCase(Locale.getDefault());
        return limpo.isEmpty() ? texto
                : Character.toUpperCase(limpo.charAt(0)) + limpo.substring(1);
    }
}
