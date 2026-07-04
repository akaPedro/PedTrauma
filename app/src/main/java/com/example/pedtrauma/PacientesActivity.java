package com.example.pedtrauma;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lista de pacientes registrados (menu lateral): nome, idade e
 * resumo da última avaliação, em cartões verdes. Tocar em um
 * paciente abre a tela de nova avaliação com ele pré-selecionado.
 */
public class PacientesActivity extends AppCompatActivity {

    private final List<String> ids = new ArrayList<>();
    private final List<Paciente> pacientes = new ArrayList<>();
    private PacientesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pacientes);
        Ui.aplicarInsets(findViewById(R.id.main));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView lista = findViewById(R.id.listaPacientes);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PacientesAdapter();
        lista.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar(); // recarrega ao voltar de uma nova avaliação
    }

    private void carregar() {
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
                    for (QueryDocumentSnapshot doc : snapshot) {
                        ids.add(doc.getId());
                        pacientes.add(doc.toObject(Paciente.class));
                    }
                    adapter.notifyDataSetChanged();
                    findViewById(R.id.txtVazio).setVisibility(
                            pacientes.isEmpty() ? View.VISIBLE : View.GONE);
                    findViewById(R.id.txtDica).setVisibility(
                            pacientes.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void mostrarOpcoes(int posicao) {
        Paciente paciente = pacientes.get(posicao);
        String pacienteId = ids.get(posicao);

        String[] opcoes = {
                getString(R.string.opcao_ver_avaliacoes),
                getString(R.string.opcao_nova_avaliacao)
        };
        new AlertDialog.Builder(this)
                .setTitle(paciente.getNome())
                .setItems(opcoes, (dialogo, escolha) -> {
                    if (escolha == 0) {
                        verAvaliacoesAnteriores(pacienteId, paciente.getNome());
                    } else {
                        abrirNovaAvaliacao(pacienteId);
                    }
                })
                .show();
    }

    private void verAvaliacoesAnteriores(String pacienteId, String nome) {
        Intent intent = new Intent(this, HistoricoActivity.class);
        intent.putExtra(HistoricoActivity.EXTRA_PACIENTE_ID, pacienteId);
        intent.putExtra(HistoricoActivity.EXTRA_PACIENTE_NOME, nome);
        startActivity(intent);
    }

    private void abrirNovaAvaliacao(String pacienteId) {
        Intent intent = new Intent(this, PacienteRegistradoActivity.class);
        intent.putExtra(PacienteRegistradoActivity.EXTRA_PACIENTE_ID, pacienteId);
        startActivity(intent);
    }

    private class PacientesAdapter extends RecyclerView.Adapter<CartaoHolder> {

        @NonNull
        @Override
        public CartaoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cartao_verde, parent, false);
            return new CartaoHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CartaoHolder holder, int position) {
            Paciente p = pacientes.get(position);

            String texto = getString(R.string.paciente_nome_idade,
                    p.getNome(), p.getIdade());
            if (p.getUltimaNota() != null && p.getUltimaData() != null) {
                String data = new SimpleDateFormat("dd/MM/yyyy - HH:mm",
                        Locale.getDefault()).format(p.getUltimaData());
                texto += "\n" + getString(R.string.ultima_avaliacao_resumo,
                        data, p.getUltimaNota(), p.getUltimaInterpretacao());
            } else {
                texto += "\n" + getString(R.string.sem_avaliacao);
            }
            holder.texto.setText(texto);

            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    mostrarOpcoes(pos);
                }
            });
        }

        @Override
        public int getItemCount() {
            return pacientes.size();
        }
    }

    private static class CartaoHolder extends RecyclerView.ViewHolder {
        final TextView texto;

        CartaoHolder(View item) {
            super(item);
            texto = item.findViewById(R.id.txtCartao);
        }
    }
}
