package com.example.pedtrauma;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carrossel da avaliação PTS (Escore de Trauma Pediátrico).
 * 6 perguntas (uma por página) + página final de resultado.
 * A pontuação é atualizada automaticamente a cada seleção,
 * conforme o documento de conteúdo do app.
 */
public class AvaliacaoActivity extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_ID = "pacienteId";
    public static final String EXTRA_PACIENTE_NOME = "pacienteNome";
    public static final String EXTRA_PACIENTE_IDADE = "pacienteIdade";
    public static final String EXTRA_SEXO = "sexo";
    public static final String EXTRA_TIPO_TRAUMA = "tipoTrauma";
    public static final String EXTRA_HORA_OCORRENCIA = "horaOcorrencia";
    public static final String EXTRA_HORA_AVALIACAO = "horaAvaliacao";

    /** Pontos por opção: 1ª = +2, 2ª = +1, 3ª = -1. */
    private static final int[] PONTOS = {2, 1, -1};
    private static final int SEM_RESPOSTA = -1;
    private static final int TOTAL_PERGUNTAS = 6;
    private static final int PAGINA_RESULTADO = TOTAL_PERGUNTAS; // 7ª página
    private static final int LIMITE_PTS = 8;

    private String[] perguntas;
    private String[][] opcoes;

    private final int[] respostas = new int[TOTAL_PERGUNTAS];
    private Boolean exameImagem = null;
    private String achados = "";
    private boolean salvando = false;

    private ViewPager2 pager;
    private LinearLayout layoutIndicadores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avaliacao);
        Ui.aplicarInsets(findViewById(R.id.main));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        perguntas = new String[]{
                "PESO DO PACIENTE?",
                "VIA AÉREA",
                "PRESSÃO ARTERIAL SISTÓLICA",
                "SISTEMA NERVOSO CENTRAL",
                "FERIDA ABERTA",
                "ESQUELETO"
        };
        opcoes = new String[][]{
                {"≥ 20 kg", "10 - 19 kg", "≤ 10 kg"},
                {"Normal", "Mantida", "Não mantida"},
                {"> 90 mmHg", "50 - 89 mmHg", "< 50 mmHg"},
                {"Acordado", "Obnubilado ou perda de consciência", "Coma ou descerebração"},
                {"Nenhuma", "Menor", "Maior ou penetrante"},
                {"Nenhuma", "Fratura fechada", "Fratura aberta ou múltipla"}
        };
        Arrays.fill(respostas, SEM_RESPOSTA);

        pager = findViewById(R.id.pagerPerguntas);
        layoutIndicadores = findViewById(R.id.layoutIndicadores);

        pager.setAdapter(new PaginasAdapter());
        criarIndicadores();
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                atualizarIndicadores(position);
            }
        });
    }

    // ---------- pontuação ----------

    private boolean todasRespondidas() {
        for (int r : respostas) {
            if (r == SEM_RESPOSTA) return false;
        }
        return true;
    }

    private int pontuacao() {
        int total = 0;
        for (int r : respostas) {
            if (r != SEM_RESPOSTA) total += PONTOS[r];
        }
        return total;
    }

    private String interpretacao(int pts) {
        return pts <= LIMITE_PTS
                ? getString(R.string.interpretacao_maior_potencial)
                : getString(R.string.interpretacao_menor_potencial);
    }

    // ---------- indicadores (bolinhas) ----------

    private void criarIndicadores() {
        int tamanho = (int) (10 * getResources().getDisplayMetrics().density);
        int margem = (int) (5 * getResources().getDisplayMetrics().density);
        for (int i = 0; i <= TOTAL_PERGUNTAS; i++) {
            View bolinha = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tamanho, tamanho);
            lp.setMargins(margem, 0, margem, 0);
            bolinha.setLayoutParams(lp);
            bolinha.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_circulo_azul));
            layoutIndicadores.addView(bolinha);
        }
        atualizarIndicadores(0);
    }

    private void atualizarIndicadores(int paginaAtual) {
        for (int i = 0; i < layoutIndicadores.getChildCount(); i++) {
            View bolinha = layoutIndicadores.getChildAt(i);
            // Página atual em cinza, demais em azul (como no mockup)
            bolinha.setBackground(ContextCompat.getDrawable(this,
                    i == paginaAtual ? R.drawable.bg_circulo_cinza : R.drawable.bg_circulo_azul));
        }
    }

    // ---------- salvar ----------

    private void confirmarSalvar() {
        if (!todasRespondidas() || exameImagem == null) {
            Toast.makeText(this, R.string.erro_responda_tudo, Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirmar_salvar)
                .setPositiveButton(R.string.opcao_sim, (d, w) -> salvar())
                .setNegativeButton(R.string.opcao_nao, null)
                .show();
    }

    private void salvar() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || salvando) return;
        salvando = true;
        notificarResultado();

        Intent i = getIntent();
        int pts = pontuacao();
        String interpretacaoTexto = interpretacao(pts);

        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setPacienteId(i.getStringExtra(EXTRA_PACIENTE_ID));
        avaliacao.setPacienteNome(i.getStringExtra(EXTRA_PACIENTE_NOME));
        avaliacao.setPacienteIdade(i.getIntExtra(EXTRA_PACIENTE_IDADE, 0));
        avaliacao.setSexo(i.getStringExtra(EXTRA_SEXO));
        avaliacao.setTipoTrauma(i.getStringExtra(EXTRA_TIPO_TRAUMA));
        avaliacao.setHoraOcorrencia(i.getStringExtra(EXTRA_HORA_OCORRENCIA));
        avaliacao.setHoraAvaliacao(i.getStringExtra(EXTRA_HORA_AVALIACAO));
        avaliacao.setTempoDecorridoMin(CadastroPacienteActivity.calcularTempoDecorrido(
                i.getStringExtra(EXTRA_HORA_OCORRENCIA), i.getStringExtra(EXTRA_HORA_AVALIACAO)));

        List<Integer> listaRespostas = new ArrayList<>();
        for (int r : respostas) listaRespostas.add(r);
        avaliacao.setRespostas(listaRespostas);
        avaliacao.setPontuacao(pts);
        avaliacao.setInterpretacao(interpretacaoTexto);
        avaliacao.setExameImagem(exameImagem);
        avaliacao.setAchados(Boolean.TRUE.equals(exameImagem) ? achados : null);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("usuarios").document(uid)
                .collection("avaliacoes")
                .add(avaliacao)
                .addOnSuccessListener(ref -> atualizarResumoPaciente(db, uid, pts, interpretacaoTexto))
                .addOnFailureListener(e -> {
                    salvando = false;
                    notificarResultado();
                    Toast.makeText(this,
                            getString(R.string.erro_salvar_avaliacao, e.getLocalizedMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void atualizarResumoPaciente(FirebaseFirestore db, String uid,
                                         int pts, String interpretacaoTexto) {
        String pacienteId = getIntent().getStringExtra(EXTRA_PACIENTE_ID);

        Map<String, Object> resumo = new HashMap<>();
        resumo.put("ultimaNota", pts);
        resumo.put("ultimaInterpretacao", interpretacaoTexto);
        resumo.put("ultimaData", new java.util.Date());

        db.collection("usuarios").document(uid)
                .collection("pacientes").document(pacienteId)
                .update(resumo)
                .addOnCompleteListener(t -> {
                    Toast.makeText(this, R.string.avaliacao_salva, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, HistoricoActivity.class));
                    finish();
                });
    }

    private void notificarResultado() {
        RecyclerView rv = (RecyclerView) pager.getChildAt(0);
        if (rv.getAdapter() != null) {
            rv.getAdapter().notifyItemChanged(PAGINA_RESULTADO);
        }
    }

    // ---------- adapter do carrossel ----------

    private static final int TIPO_PERGUNTA = 0;
    private static final int TIPO_RESULTADO = 1;

    private class PaginasAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            return position == PAGINA_RESULTADO ? TIPO_RESULTADO : TIPO_PERGUNTA;
        }

        @Override
        public int getItemCount() {
            return TOTAL_PERGUNTAS + 1;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TIPO_RESULTADO) {
                return new ResultadoHolder(inflater.inflate(R.layout.item_resultado, parent, false));
            }
            return new PerguntaHolder(inflater.inflate(R.layout.item_pergunta, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof PerguntaHolder) {
                ((PerguntaHolder) holder).bind(position);
            } else {
                ((ResultadoHolder) holder).bind();
            }
        }
    }

    private class PerguntaHolder extends RecyclerView.ViewHolder {
        final TextView txtPergunta;
        final RadioGroup grupo;
        final RadioButton[] botoes = new RadioButton[3];

        PerguntaHolder(View item) {
            super(item);
            txtPergunta = item.findViewById(R.id.txtPergunta);
            grupo = item.findViewById(R.id.grupoOpcoes);
            botoes[0] = item.findViewById(R.id.opcao0);
            botoes[1] = item.findViewById(R.id.opcao1);
            botoes[2] = item.findViewById(R.id.opcao2);
        }

        void bind(int posicao) {
            txtPergunta.setText(perguntas[posicao]);

            grupo.setOnCheckedChangeListener(null);
            grupo.clearCheck();
            for (int i = 0; i < 3; i++) {
                botoes[i].setText(opcoes[posicao][i]);
                botoes[i].setChecked(respostas[posicao] == i);
            }

            grupo.setOnCheckedChangeListener((g, checkedId) -> {
                if (checkedId == -1) return;
                int escolha = checkedId == R.id.opcao0 ? 0
                        : checkedId == R.id.opcao1 ? 1 : 2;
                respostas[getBindingAdapterPosition()] = escolha;
                notificarResultado(); // pontuação atualiza automaticamente
                // avança para a próxima pergunta
                pager.postDelayed(() ->
                        pager.setCurrentItem(getBindingAdapterPosition() + 1, true), 250);
            });
        }
    }

    private class ResultadoHolder extends RecyclerView.ViewHolder {
        final TextView badge, txtInterpretacao;
        final RadioGroup grupoExame;
        final RadioButton exameSim, exameNao;
        final EditText edtAchados;
        final View layoutAviso;
        final Button btnSalvar;
        final ProgressBar progressSalvar;

        ResultadoHolder(View item) {
            super(item);
            badge = item.findViewById(R.id.badgePontuacao);
            txtInterpretacao = item.findViewById(R.id.txtInterpretacao);
            grupoExame = item.findViewById(R.id.grupoExame);
            exameSim = item.findViewById(R.id.exameSim);
            exameNao = item.findViewById(R.id.exameNao);
            edtAchados = item.findViewById(R.id.edtAchados);
            layoutAviso = item.findViewById(R.id.layoutAviso);
            btnSalvar = item.findViewById(R.id.btnSalvarAvaliacao);
            progressSalvar = item.findViewById(R.id.progressSalvar);

            edtAchados.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void afterTextChanged(Editable s) {
                    achados = s.toString();
                }
            });

            btnSalvar.setOnClickListener(v -> confirmarSalvar());
        }

        void bind() {
            boolean completo = todasRespondidas();
            if (completo) {
                int pts = pontuacao();
                badge.setText(String.valueOf(pts));
                // Classificação com cores: vermelho ≤ 8, azul > 8
                badge.getBackground().setTint(ContextCompat.getColor(
                        AvaliacaoActivity.this,
                        pts <= LIMITE_PTS ? R.color.vermelho_pedtrauma : R.color.azul_pedtrauma));
                txtInterpretacao.setText(interpretacao(pts));
            } else {
                badge.setText(R.string.pontuacao_pendente);
                txtInterpretacao.setText(R.string.erro_responda_tudo);
            }

            grupoExame.setOnCheckedChangeListener(null);
            exameSim.setChecked(Boolean.TRUE.equals(exameImagem));
            exameNao.setChecked(Boolean.FALSE.equals(exameImagem));
            grupoExame.setOnCheckedChangeListener((g, checkedId) -> {
                exameImagem = (checkedId == R.id.exameSim);
                atualizarCamposExame();
            });

            atualizarCamposExame();
            progressSalvar.setVisibility(salvando ? View.VISIBLE : View.GONE);
            btnSalvar.setEnabled(!salvando);
        }

        private void atualizarCamposExame() {
            // SIM: descreve os achados; NÃO: aviso de hemorragia intracraniana
            boolean sim = Boolean.TRUE.equals(exameImagem);
            boolean nao = Boolean.FALSE.equals(exameImagem);
            edtAchados.setVisibility(sim ? View.VISIBLE : View.GONE);
            layoutAviso.setVisibility(nao ? View.VISIBLE : View.GONE);
        }
    }
}
