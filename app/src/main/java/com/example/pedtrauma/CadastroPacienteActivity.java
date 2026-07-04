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
    private TextView txtHoraOcorrencia, txtHoraAvaliacao, txtTempoDecorrido;
    private ProgressBar progresso;

    private String sexoSelecionado = null;
    private String horaOcorrencia = null;
    private String horaAvaliacao = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_paciente);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        edtNomePaciente = findViewById(R.id.edtNomePaciente);
        edtIdade = findViewById(R.id.edtIdade);
        edtTipoTrauma = findViewById(R.id.edtTipoTrauma);
        btnMasculino = findViewById(R.id.btnMasculino);
        btnFeminino = findViewById(R.id.btnFeminino);
        txtHoraOcorrencia = findViewById(R.id.txtHoraOcorrencia);
        txtHoraAvaliacao = findViewById(R.id.txtHoraAvaliacao);
        txtTempoDecorrido = findViewById(R.id.txtTempoDecorrido);
        progresso = findViewById(R.id.progressCadastroPaciente);

        btnMasculino.setOnClickListener(v -> selecionarSexo(getString(R.string.sexo_masculino)));
        btnFeminino.setOnClickListener(v -> selecionarSexo(getString(R.string.sexo_feminino)));

        txtHoraOcorrencia.setOnClickListener(v -> escolherHora(true));
        txtHoraAvaliacao.setOnClickListener(v -> escolherHora(false));

        findViewById(R.id.btnRegistrarTrauma).setOnClickListener(v -> registrar());
    }

    private void selecionarSexo(String sexo) {
        sexoSelecionado = sexo;
        boolean masculino = sexo.equals(getString(R.string.sexo_masculino));
        btnMasculino.setAlpha(masculino ? 1f : 0.45f);
        btnFeminino.setAlpha(masculino ? 0.45f : 1f);
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
        String idadeTexto = edtIdade.getText().toString().trim();
        String tipoTrauma = edtTipoTrauma.getText().toString().trim();

        if (TextUtils.isEmpty(nome)) {
            edtNomePaciente.setError(getString(R.string.erro_campo_obrigatorio));
            edtNomePaciente.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(idadeTexto)) {
            edtIdade.setError(getString(R.string.erro_campo_obrigatorio));
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

        int idade = Integer.parseInt(idadeTexto);
        Paciente paciente = new Paciente(nome, idade, sexoSelecionado);

        progresso.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance()
                .collection("usuarios").document(uid)
                .collection("pacientes")
                .add(paciente)
                .addOnSuccessListener(ref -> {
                    progresso.setVisibility(View.GONE);
                    abrirAvaliacao(ref.getId(), nome, idade, tipoTrauma);
                })
                .addOnFailureListener(e -> {
                    progresso.setVisibility(View.GONE);
                    Toast.makeText(this,
                            getString(R.string.erro_cadastro, e.getLocalizedMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void abrirAvaliacao(String pacienteId, String nome, int idade, String tipoTrauma) {
        Intent intent = new Intent(this, AvaliacaoActivity.class);
        intent.putExtra(AvaliacaoActivity.EXTRA_PACIENTE_ID, pacienteId);
        intent.putExtra(AvaliacaoActivity.EXTRA_PACIENTE_NOME, nome);
        intent.putExtra(AvaliacaoActivity.EXTRA_PACIENTE_IDADE, idade);
        intent.putExtra(AvaliacaoActivity.EXTRA_SEXO, sexoSelecionado);
        intent.putExtra(AvaliacaoActivity.EXTRA_TIPO_TRAUMA, tipoTrauma);
        intent.putExtra(AvaliacaoActivity.EXTRA_HORA_OCORRENCIA, horaOcorrencia);
        intent.putExtra(AvaliacaoActivity.EXTRA_HORA_AVALIACAO, horaAvaliacao);
        startActivity(intent);
        finish();
    }
}
