package com.example.pedtrauma;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Nova avaliação de um paciente já cadastrado (2ª tela do mockup).
 * O nome funciona como busca (autocomplete); ao escolher, mostra a
 * última avaliação e permite registrar um novo trauma.
 */
public class PacienteRegistradoActivity extends AppCompatActivity {

    private AutoCompleteTextView edtBuscaPaciente;
    private EditText edtTipoTrauma;
    private TextView txtHoraOcorrencia, txtHoraAvaliacao, cartaoUltimaAvaliacao;

    private final List<String> ids = new ArrayList<>();
    private final List<Paciente> pacientes = new ArrayList<>();
    private int selecionado = -1;

    private String horaOcorrencia = null;
    private String horaAvaliacao = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente_registrado);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        edtBuscaPaciente = findViewById(R.id.edtBuscaPaciente);
        edtTipoTrauma = findViewById(R.id.edtTipoTrauma);
        txtHoraOcorrencia = findViewById(R.id.txtHoraOcorrencia);
        txtHoraAvaliacao = findViewById(R.id.txtHoraAvaliacao);
        cartaoUltimaAvaliacao = findViewById(R.id.cartaoUltimaAvaliacao);

        txtHoraOcorrencia.setOnClickListener(v -> escolherHora(true));
        txtHoraAvaliacao.setOnClickListener(v -> escolherHora(false));

        findViewById(R.id.btnRegistrarTrauma).setOnClickListener(v -> registrar());

        carregarPacientes();
    }

    private void carregarPacientes() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("usuarios").document(uid)
                .collection("pacientes")
                .orderBy("nome")
                .get()
                .addOnSuccessListener(snapshot -> {
                    ids.clear();
                    pacientes.clear();
                    List<String> nomes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Paciente p = doc.toObject(Paciente.class);
                        ids.add(doc.getId());
                        pacientes.add(p);
                        nomes.add(p.getNome());
                    }
                    if (nomes.isEmpty()) {
                        Toast.makeText(this, R.string.nenhum_paciente, Toast.LENGTH_LONG).show();
                        return;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_dropdown_item_1line, nomes);
                    edtBuscaPaciente.setAdapter(adapter);
                    edtBuscaPaciente.setThreshold(1);
                    edtBuscaPaciente.setOnItemClickListener((parent, view, pos, id) -> {
                        String nome = (String) parent.getItemAtPosition(pos);
                        selecionarPorNome(nome);
                    });
                });
    }

    private void selecionarPorNome(String nome) {
        selecionado = -1;
        for (int i = 0; i < pacientes.size(); i++) {
            if (pacientes.get(i).getNome().equals(nome)) {
                selecionado = i;
                break;
            }
        }
        mostrarUltimaAvaliacao();
    }

    private void mostrarUltimaAvaliacao() {
        if (selecionado < 0) {
            cartaoUltimaAvaliacao.setVisibility(View.GONE);
            return;
        }
        Paciente p = pacientes.get(selecionado);
        cartaoUltimaAvaliacao.setVisibility(View.VISIBLE);

        if (p.getUltimaNota() == null || p.getUltimaData() == null) {
            cartaoUltimaAvaliacao.setText(R.string.sem_avaliacao);
            return;
        }
        String data = new SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
                .format(p.getUltimaData());
        cartaoUltimaAvaliacao.setText(getString(R.string.cartao_paciente,
                p.getNome(), p.getIdade(), data, p.getUltimaNota(), p.getUltimaInterpretacao()));
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
        }, 12, 0, true).show();
    }

    private void registrar() {
        String tipoTrauma = edtTipoTrauma.getText().toString().trim();

        if (selecionado < 0) {
            Toast.makeText(this, R.string.erro_selecione_paciente, Toast.LENGTH_SHORT).show();
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

        Paciente p = pacientes.get(selecionado);
        Intent intent = new Intent(this, AvaliacaoActivity.class);
        intent.putExtra(AvaliacaoActivity.EXTRA_PACIENTE_ID, ids.get(selecionado));
        intent.putExtra(AvaliacaoActivity.EXTRA_PACIENTE_NOME, p.getNome());
        intent.putExtra(AvaliacaoActivity.EXTRA_PACIENTE_IDADE, p.getIdade());
        intent.putExtra(AvaliacaoActivity.EXTRA_SEXO, p.getSexo());
        intent.putExtra(AvaliacaoActivity.EXTRA_TIPO_TRAUMA, tipoTrauma);
        intent.putExtra(AvaliacaoActivity.EXTRA_HORA_OCORRENCIA, horaOcorrencia);
        intent.putExtra(AvaliacaoActivity.EXTRA_HORA_AVALIACAO, horaAvaliacao);
        startActivity(intent);
        finish();
    }
}
