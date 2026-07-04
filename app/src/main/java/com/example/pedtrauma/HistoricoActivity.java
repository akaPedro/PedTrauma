package com.example.pedtrauma;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Histórico cronológico de avaliações do profissional logado
 * (mais recentes primeiro), em cartões azuis como no mockup.
 * Tocar em um cartão permite exportar a avaliação:
 * salvar em PDF, compartilhar ou imprimir.
 */
public class HistoricoActivity extends AppCompatActivity {

    private static final String AUTORIDADE_PROVIDER = "com.example.pedtrauma.fileprovider";

    private final List<Avaliacao> avaliacoes = new ArrayList<>();
    private HistoricoAdapter adapter;

    private Usuario profissional;
    private File arquivoPendente;
    private ActivityResultLauncher<String> salvarPdfLauncher;

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

        salvarPdfLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/pdf"),
                uri -> {
                    if (uri != null && arquivoPendente != null) {
                        copiarParaUri(arquivoPendente, uri);
                    }
                });

        carregarProfissional();
        carregar();
    }

    private void carregarProfissional() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener(doc -> profissional = doc.toObject(Usuario.class));
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
                    findViewById(R.id.txtDicaExportar).setVisibility(
                            avaliacoes.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    // ---------- exportação ----------

    private void mostrarOpcoesExportar(Avaliacao avaliacao) {
        String[] opcoes = {
                getString(R.string.opcao_salvar_pdf),
                getString(R.string.opcao_compartilhar),
                getString(R.string.opcao_imprimir)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.exportar_titulo)
                .setItems(opcoes, (dialogo, escolha) -> exportar(avaliacao, escolha))
                .show();
    }

    private void exportar(Avaliacao avaliacao, int escolha) {
        File pdf;
        try {
            pdf = GeradorPdf.gerar(this, avaliacao, profissional);
        } catch (Exception e) {
            Toast.makeText(this,
                    getString(R.string.erro_pdf, e.getLocalizedMessage()),
                    Toast.LENGTH_LONG).show();
            return;
        }

        switch (escolha) {
            case 0: // salvar: o usuário escolhe a pasta (SAF)
                arquivoPendente = pdf;
                salvarPdfLauncher.launch(GeradorPdf.nomeArquivo(avaliacao));
                break;
            case 1:
                compartilhar(pdf);
                break;
            case 2:
                imprimir(pdf);
                break;
        }
    }

    private void copiarParaUri(File origem, Uri destino) {
        try (InputStream entrada = new FileInputStream(origem);
             OutputStream saida = getContentResolver().openOutputStream(destino)) {
            if (saida == null) throw new IllegalStateException("Destino indisponível");
            byte[] buffer = new byte[8192];
            int lidos;
            while ((lidos = entrada.read(buffer)) > 0) {
                saida.write(buffer, 0, lidos);
            }
            Toast.makeText(this, R.string.pdf_salvo, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this,
                    getString(R.string.erro_pdf, e.getLocalizedMessage()),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void compartilhar(File pdf) {
        Uri uri = FileProvider.getUriForFile(this, AUTORIDADE_PROVIDER, pdf);
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("application/pdf")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent,
                getString(R.string.opcao_compartilhar)));
    }

    private void imprimir(File pdf) {
        PrintManager printManager = (PrintManager) getSystemService(PRINT_SERVICE);
        printManager.print(pdf.getName(), new AdaptadorImpressaoPdf(pdf), null);
    }

    // ---------- lista ----------

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
            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    mostrarOpcoesExportar(avaliacoes.get(pos));
                }
            });
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
