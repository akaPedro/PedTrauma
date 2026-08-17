package com.example.pedtrauma;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

/**
 * Cadastro de um novo paciente (1ª tela do mockup).
 * Ao "Registrar trauma", salva o paciente no Firestore e
 * abre o carrossel de avaliação (AvaliacaoActivity).
 */
public class CadastroPacienteActivity extends AppCompatActivity {

    private EditText edtNomePaciente, edtIdade, edtTipoTrauma;
    private TextView btnMasculino, btnFeminino;
    private TextView btnAnos, btnMeses, txtIdadeResumo;
    private TextView txtHoraOcorrencia, txtHoraAvaliacao, txtTempoDecorrido;
    private ProgressBar progresso;

    private String sexoSelecionado = null;
    private boolean idadeEmMeses = false;
    private String horaOcorrencia = null;
    private String horaAvaliacao = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_paciente);
        Ui.aplicarInsets(findViewById(R.id.main));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        edtNomePaciente = findViewById(R.id.edtNomePaciente);
        edtIdade = findViewById(R.id.edtIdade);
        edtTipoTrauma = findViewById(R.id.edtTipoTrauma);
        btnMasculino = findViewById(R.id.btnMasculino);
        btnFeminino = findViewById(R.id.btnFeminino);
        btnAnos = findViewById(R.id.btnAnos);
        btnMeses = findViewById(R.id.btnMeses);
        txtIdadeResumo = findViewById(R.id.txtIdadeResumo);
        txtHoraOcorrencia = findViewById(R.id.txtHoraOcorrencia);
        txtHoraAvaliacao = findViewById(R.id.txtHoraAvaliacao);
        txtTempoDecorrido = findViewById(R.id.txtTempoDecorrido);
        progresso = findViewById(R.id.progressCadastroPaciente);

        btnMasculino.setOnClickListener(v -> selecionarSexo(getString(R.string.sexo_masculino)));
        btnFeminino.setOnClickListener(v -> selecionarSexo(getString(R.string.sexo_feminino)));

        btnAnos.setOnClickListener(v -> selecionarUnidade(false));
        btnMeses.setOnClickListener(v -> selecionarUnidade(true));
        edtIdade.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                atualizarResumoIdade();
            }
        });

        txtHoraOcorrencia.setOnClickListener(v -> escolherHora(true));
        txtHoraAvaliacao.setOnClickListener(v -> escolherHora(false));

        findViewById(R.id.btnRegistrarTrauma).setOnClickListener(v -> registrar());

        restaurarEstado(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@androidx.annotation.NonNull Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putString("sexo", sexoSelecionado);
        estado.putBoolean("idadeEmMeses", idadeEmMeses);
        estado.putString("horaOcorrencia", horaOcorrencia);
        estado.putString("horaAvaliacao", horaAvaliacao);
    }

    private void restaurarEstado(Bundle estado) {
        if (estado == null) return;
        String sexo = estado.getString("sexo");
        if (sexo != null) selecionarSexo(sexo);
        selecionarUnidade(estado.getBoolean("idadeEmMeses", false));

        horaOcorrencia = estado.getString("horaOcorrencia");
        horaAvaliacao = estado.getString("horaAvaliacao");
        if (horaOcorrencia != null) txtHoraOcorrencia.setText(horaOcorrencia);
        if (horaAvaliacao != null) txtHoraAvaliacao.setText(horaAvaliacao);
        atualizarTempoDecorrido();
    }

    private void selecionarSexo(String sexo) {
        sexoSelecionado = sexo;
        boolean masculino = sexo.equals(getString(R.string.sexo_masculino));
        btnMasculino.setAlpha(masculino ? 1f : 0.45f);
        btnFeminino.setAlpha(masculino ? 0.45f : 1f);
    }

    private void selecionarUnidade(boolean meses) {
        idadeEmMeses = meses;
        btnAnos.setAlpha(meses ? 0.45f : 1f);
        btnMeses.setAlpha(meses ? 1f : 0.45f);
        atualizarResumoIdade();
    }

    /** Total de meses digitado; 0 quando vazio ou fora da faixa. */
    private int mesesInformados() {
        String texto = edtIdade.getText().toString().replaceAll("[^0-9]", "");
        if (TextUtils.isEmpty(texto)) return 0;
        int valor = Integer.parseInt(texto);
        if (valor <= 0) return 0;
        int meses = idadeEmMeses ? valor : valor * 12;
        return meses > Idade.MESES_MAXIMO ? 0 : meses;
    }

    private void atualizarResumoIdade() {
        int meses = mesesInformados();
        if (meses > 0) {
            txtIdadeResumo.setText(Idade.formatar(this, meses));
            txtIdadeResumo.setVisibility(View.VISIBLE);
        } else {
            txtIdadeResumo.setVisibility(View.GONE);
        }
    }

    private void escolherHora(boolean ocorrencia) {
        new TimePickerDialog(this, (view, hora, minuto) -> {
            String texto = String.format(Locale.getDefault(), "%02d:%02d", hora, minuto);
            if (ocorrencia) {
                horaOcorrencia = texto;
                txtHoraOcorrencia.setText(texto);
            } else {
                horaAvaliacao = texto;
                txtHoraAvaliacao.setText(texto);
            }
            atualizarTempoDecorrido();
        }, 12, 0, true).show();
    }

    private void atualizarTempoDecorrido() {
        long minutos = calcularTempoDecorrido(horaOcorrencia, horaAvaliacao);
        if (minutos >= 0) {
            txtTempoDecorrido.setText(getString(R.string.tempo_decorrido, minutos));
            txtTempoDecorrido.setVisibility(View.VISIBLE);
        } else {
            txtTempoDecorrido.setVisibility(View.GONE);
        }
    }

    /**
     * Diferença em minutos entre as horas (HH:mm). Se a avaliação for
     * "antes" da ocorrência, assume que cruzou a meia-noite (+24h).
     * Retorna -1 se alguma hora estiver vazia.
     */
    static long calcularTempoDecorrido(String ocorrencia, String avaliacao) {
        if (ocorrencia == null || avaliacao == null) return -1;
        String[] o = ocorrencia.split(":");
        String[] a = avaliacao.split(":");
        int minOcorrencia = Integer.parseInt(o[0]) * 60 + Integer.parseInt(o[1]);
        int minAvaliacao = Integer.parseInt(a[0]) * 60 + Integer.parseInt(a[1]);
        int diff = minAvaliacao - minOcorrencia;
        if (diff < 0) diff += 24 * 60;
        return diff;
    }

    private void registrar() {
        String nome = edtNomePaciente.getText().toString().trim();
        String tipoTrauma = edtTipoTrauma.getText().toString().trim();
        int meses = mesesInformados();

        if (TextUtils.isEmpty(nome)) {
            edtNomePaciente.setError(getString(R.string.erro_campo_obrigatorio));
            edtNomePaciente.requestFocus();
            return;
        }
        if (meses <= 0) {
            edtIdade.setError(getString(R.string.erro_idade_invalida));
            edtIdade.requestFocus();
            return;
        }
        if (sexoSelecionado == null) {
            Toast.makeText(this, R.string.erro_selecione_sexo, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(tipoTrauma)) {
            edtTipoTrauma.setError(getString(R.string.erro_campo_obrigatorio));
            edtTipoTrauma.requestFocus();
            return;
        }
        if (horaOcorrencia == null || horaAvaliacao == null) {
            Toast.makeText(this, R.string.erro_informe_horas, Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        Paciente paciente = new Paciente(nome, meses, sexoSelecionado);

        progresso.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance()
                .collection("usuarios").document(uid)
                .collection("pacientes")
                .add(paciente)
                .addOnSuccessListener(ref -> {
                    progresso.setVisibility(View.GONE);
                    abrirAvaliacao(ref.getId(), nome, meses, tipoTrauma);
                })
                .addOnFailureListener(e -> {
                    progresso.setVisibility(View.GONE);
                    Toast.makeText(this,
                            getString(R.string.erro_cadastro, e.getLocalizedMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void abrirAvaliacao(String pacienteId, String nome, int meses, String tipoTrauma) {
        Intent intent = new Intent(this, AvaliacaoActivity.class);
        intent.putExtra(AvaliacaoActivity.EXTRA_PACIENTE_ID, pacienteId);
        intent.putExtra(AvaliacaoActivity.EXTRA_PACIENTE_NOME, nome);
        intent.putExtra(AvaliacaoActivity.EXTRA_PACIENTE_IDADE_MESES, meses);
        intent.putExtra(AvaliacaoActivity.EXTRA_SEXO, sexoSelecionado);
        intent.putExtra(AvaliacaoActivity.EXTRA_TIPO_TRAUMA, tipoTrauma);
        intent.putExtra(AvaliacaoActivity.EXTRA_HORA_OCORRENCIA, horaOcorrencia);
        intent.putExtra(AvaliacaoActivity.EXTRA_HORA_AVALIACAO, horaAvaliacao);
        startActivity(intent);
        finish();
    }
}
