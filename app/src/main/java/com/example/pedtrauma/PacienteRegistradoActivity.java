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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Nova avaliação de um paciente já cadastrado (2ª tela do mockup).
 * O nome funciona como busca (autocomplete); ao escolher, mostra a
 * última avaliação e permite registrar um novo trauma.
 */
public class PacienteRegistradoActivity extends AppCompatActivity {

    /** Extra opcional: pré-seleciona o paciente com este id. */
    public static final String EXTRA_PACIENTE_ID = "pacienteId";

    private AutoCompleteTextView edtBuscaPaciente;
    private EditText edtTipoTrauma;
    private TextView txtHoraOcorrencia, txtHoraAvaliacao, txtTempoDecorrido,
            cartaoUltimaAvaliacao, txtDicaContinuar;
    private boolean buscandoUltimoCaso = false;

    private final List<String> ids = new ArrayList<>();
    private final List<Paciente> pacientes = new ArrayList<>();
    private final List<String> rotulos = new ArrayList<>();
    /** Rótulo exibido no autocomplete -> índice na lista (trata nomes repetidos). */
    private final Map<String, Integer> indicePorRotulo = new HashMap<>();
    private int selecionado = -1;
    private String idPendenteRestauracao = null;

    private String horaOcorrencia = null;
    private String horaAvaliacao = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente_registrado);
        Ui.aplicarInsets(findViewById(R.id.main));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        edtBuscaPaciente = findViewById(R.id.edtBuscaPaciente);
        edtTipoTrauma = findViewById(R.id.edtTipoTrauma);
        txtHoraOcorrencia = findViewById(R.id.txtHoraOcorrencia);
        txtHoraAvaliacao = findViewById(R.id.txtHoraAvaliacao);
        txtTempoDecorrido = findViewById(R.id.txtTempoDecorrido);
        cartaoUltimaAvaliacao = findViewById(R.id.cartaoUltimaAvaliacao);
        txtDicaContinuar = findViewById(R.id.txtDicaContinuar);

        // Tocar no cartão continua a avaliação com o mesmo caso registrado
        cartaoUltimaAvaliacao.setOnClickListener(v -> continuarCasoRegistrado());

        txtHoraOcorrencia.setOnClickListener(v -> escolherHora(true));
        txtHoraAvaliacao.setOnClickListener(v -> escolherHora(false));

        findViewById(R.id.btnRegistrarTrauma).setOnClickListener(v -> registrar());

        restaurarEstado(savedInstanceState);
        if (idPendenteRestauracao == null) {
            // veio da lista de Pacientes com um paciente já escolhido
            idPendenteRestauracao = getIntent().getStringExtra(EXTRA_PACIENTE_ID);
        }
        carregarPacientes();
    }

    @Override
    protected void onSaveInstanceState(@androidx.annotation.NonNull Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putString("horaOcorrencia", horaOcorrencia);
        estado.putString("horaAvaliacao", horaAvaliacao);
        estado.putString("pacienteId", selecionado >= 0 ? ids.get(selecionado) : null);
    }

    private void restaurarEstado(Bundle estado) {
        if (estado == null) return;
        horaOcorrencia = estado.getString("horaOcorrencia");
        horaAvaliacao = estado.getString("horaAvaliacao");
        if (horaOcorrencia != null) txtHoraOcorrencia.setText(horaOcorrencia);
        if (horaAvaliacao != null) txtHoraAvaliacao.setText(horaAvaliacao);
        atualizarTempoDecorrido();
        // reaplicado quando a lista de pacientes terminar de carregar
        idPendenteRestauracao = estado.getString("pacienteId");
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
                    indicePorRotulo.clear();
                    rotulos.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Paciente p = doc.toObject(Paciente.class);
                        ids.add(doc.getId());
                        pacientes.add(p);

                        // Nomes repetidos ganham a idade (e um número) no rótulo
                        String rotulo = p.getNome();
                        if (indicePorRotulo.containsKey(rotulo)) {
                            rotulo = p.getNome() + " - " + p.getIdade() + " anos";
                        }
                        int sequencia = 2;
                        while (indicePorRotulo.containsKey(rotulo)) {
                            rotulo = p.getNome() + " (" + sequencia++ + ")";
                        }
                        indicePorRotulo.put(rotulo, pacientes.size() - 1);
                        rotulos.add(rotulo);
                    }
                    if (rotulos.isEmpty()) {
                        Toast.makeText(this, R.string.nenhum_paciente, Toast.LENGTH_LONG).show();
                        return;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_dropdown_item_1line, rotulos);
                    edtBuscaPaciente.setAdapter(adapter);
                    edtBuscaPaciente.setThreshold(1);
                    edtBuscaPaciente.setOnItemClickListener((parent, view, pos, id) -> {
                        Integer indice = indicePorRotulo.get(
                                (String) parent.getItemAtPosition(pos));
                        selecionado = indice == null ? -1 : indice;
                        mostrarUltimaAvaliacao();
                    });

                    // Restaura a seleção (rotação) ou aplica a pré-seleção (extra)
                    if (idPendenteRestauracao != null) {
                        int indice = ids.indexOf(idPendenteRestauracao);
                        idPendenteRestauracao = null;
                        if (indice >= 0) {
                            selecionado = indice;
                            edtBuscaPaciente.setText(rotulos.get(indice), false);
                            mostrarUltimaAvaliacao();
                        }
                    }
                });
    }

    private void mostrarUltimaAvaliacao() {
        if (selecionado < 0) {
            cartaoUltimaAvaliacao.setVisibility(View.GONE);
            txtDicaContinuar.setVisibility(View.GONE);
            return;
        }
        Paciente p = pacientes.get(selecionado);
        cartaoUltimaAvaliacao.setVisibility(View.VISIBLE);

        if (p.getUltimaNota() == null || p.getUltimaData() == null) {
            cartaoUltimaAvaliacao.setText(R.string.sem_avaliacao);
            txtDicaContinuar.setVisibility(View.GONE);
            return;
        }
        String data = new SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
                .format(p.getUltimaData());
        cartaoUltimaAvaliacao.setText(getString(R.string.cartao_paciente,
                p.getNome(), p.getIdade(), data, p.getUltimaNota(), p.getUltimaInterpretacao()));
        txtDicaContinuar.setVisibility(View.VISIBLE);
    }

    /**
     * Continua a avaliação com o mesmo caso já registrado: busca a
     * última avaliação do paciente e reaproveita o tipo de trauma e
     * os horários, indo direto para o carrossel de perguntas.
     */
    private void continuarCasoRegistrado() {
        if (selecionado < 0 || buscandoUltimoCaso) return;

        Paciente p = pacientes.get(selecionado);
        if (p.getUltimaNota() == null) {
            Toast.makeText(this, R.string.sem_avaliacao, Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        String pacienteId = ids.get(selecionado);

        buscandoUltimoCaso = true;
        FirebaseFirestore.getInstance()
                .collection("usuarios").document(uid)
                .collection("avaliacoes")
                .whereEqualTo("pacienteId", pacienteId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    buscandoUltimoCaso = false;

                    // a mais recente (ordenação local dispensa índice composto)
                    Avaliacao ultima = null;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Avaliacao a = doc.toObject(Avaliacao.class);
                        if (ultima == null || (a.getCriadoEm() != null
                                && (ultima.getCriadoEm() == null
                                || a.getCriadoEm().after(ultima.getCriadoEm())))) {
                            ultima = a;
                        }
                    }
                    if (ultima == null) {
                        Toast.makeText(this, R.string.sem_avaliacao, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Intent intent = new Intent(this, AvaliacaoActivity.class);
                    intent.putExtra(AvaliacaoActivity.EXTRA_PACIENTE_ID, pacienteId);
                    intent.putExtra(AvaliacaoActivity.EXTRA_PACIENTE_NOME, p.getNome());
                    intent.putExtra(AvaliacaoActivity.EXTRA_PACIENTE_IDADE, p.getIdade());
                    intent.putExtra(AvaliacaoActivity.EXTRA_SEXO, p.getSexo());
                    intent.putExtra(AvaliacaoActivity.EXTRA_TIPO_TRAUMA, ultima.getTipoTrauma());
                    intent.putExtra(AvaliacaoActivity.EXTRA_HORA_OCORRENCIA, ultima.getHoraOcorrencia());
                    intent.putExtra(AvaliacaoActivity.EXTRA_HORA_AVALIACAO, ultima.getHoraAvaliacao());
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> buscandoUltimoCaso = false);
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
        long minutos = CadastroPacienteActivity
                .calcularTempoDecorrido(horaOcorrencia, horaAvaliacao);
        if (minutos >= 0) {
            txtTempoDecorrido.setText(getString(R.string.tempo_decorrido, minutos));
            txtTempoDecorrido.setVisibility(View.VISIBLE);
        } else {
            txtTempoDecorrido.setVisibility(View.GONE);
        }
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
