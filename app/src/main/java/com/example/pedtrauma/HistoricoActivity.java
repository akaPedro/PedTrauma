package com.example.pedtrauma;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Histórico cronológico de avaliações do profissional logado
 * (mais recentes primeiro), em cartões azuis como no mockup.
 */
public class HistoricoActivity extends AppCompatActivity {

    private final List<Avaliacao> avaliacoes = new ArrayList<>();
    private HistoricoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico);
        Ui.aplicarInsets(findViewById(R.id.main));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView lista = findViewById(R.id.listaHistorico);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoricoAdapter();
        lista.setAdapter(adapter);

        carregar();
    }

    private void carregar() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("usuarios").document(uid)
                .collection("avaliacoes")
                .orderBy("criadoEm", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    avaliacoes.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        avaliacoes.add(doc.toObject(Avaliacao.class));
                    }
                    adapter.notifyDataSetChanged();
                    findViewById(R.id.txtVazio).setVisibility(
                            avaliacoes.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private class HistoricoAdapter extends RecyclerView.Adapter<CartaoHolder> {

        @NonNull
        @Override
        public CartaoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cartao_azul, parent, false);
            return new CartaoHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CartaoHolder holder, int position) {
            Avaliacao a = avaliacoes.get(position);
            String data = a.getCriadoEm() == null ? ""
                    : new SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
                            .format(a.getCriadoEm());
            holder.texto.setText(getString(R.string.cartao_paciente,
                    a.getPacienteNome(), a.getPacienteIdade(), data,
                    a.getPontuacao(), a.getInterpretacao()));
        }

        @Override
        public int getItemCount() {
            return avaliacoes.size();
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
