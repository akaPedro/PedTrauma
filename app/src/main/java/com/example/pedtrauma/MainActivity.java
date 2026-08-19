package com.example.pedtrauma;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Tela principal (após login).
 * - Toolbar preta com menu lateral (gaveta) e atalho para o perfil.
 * - Botões "Novo Paciente" e "Paciente Registrado".
 * - Ao entrar, exibe o diálogo de boas-vindas explicando o PedTrauma.
 */
public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Ui.aplicarInsets(findViewById(R.id.main));

        drawerLayout = findViewById(R.id.drawerLayout);

        configurarToolbar();
        configurarGaveta();

        findViewById(R.id.btnNovoPaciente).setOnClickListener(v ->
                startActivity(new Intent(this, CadastroPacienteActivity.class)));
        findViewById(R.id.btnPacienteRegistrado).setOnClickListener(v ->
                startActivity(new Intent(this, PacienteRegistradoActivity.class)));

        // Evita reabrir o diálogo ao girar a tela
        if (savedInstanceState == null) {
            mostrarDialogoBemVindo();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarFotoPerfil(); // atualiza ao voltar da tela de Perfil
        carregarUltimasAvaliacoes();
    }

    /** Mostra a foto do usuário no ícone de perfil da toolbar. */
    private void carregarFotoPerfil() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    Usuario usuario = doc.toObject(Usuario.class);
                    if (usuario == null) return;
                    Drawable foto = Ui.fotoCircular(getResources(), usuario.getFotoBase64());
                    if (foto == null) return;

                    Toolbar toolbar = findViewById(R.id.toolbar);
                    MenuItem itemPerfil = toolbar.getMenu().findItem(R.id.itemPerfil);
                    if (itemPerfil != null) itemPerfil.setIcon(foto);
                });
    }

    private void configurarToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        toolbar.inflateMenu(R.menu.menu_toolbar_main);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.itemPerfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                return true;
            }
            return false;
        });
    }

    private void configurarGaveta() {
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.itemHistorico) {
                startActivity(new Intent(this, HistoricoActivity.class));
            } else if (id == R.id.itemPacientes) {
                startActivity(new Intent(this, PacientesActivity.class));
            } else if (id == R.id.itemSobre) {
                startActivity(new Intent(this, SobreActivity.class));
            } else if (id == R.id.itemSair) {
                sair();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void mostrarDialogoBemVindo() {
        View viewDialogo = getLayoutInflater()
                .inflate(R.layout.dialog_bem_vindo, null);

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setView(viewDialogo)
                .create();

        // Fundo transparente para aparecer só o cartão arredondado do layout
        if (dialogo.getWindow() != null) {
            dialogo.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT));
        }

        viewDialogo.findViewById(R.id.btnOk)
                .setOnClickListener(v -> dialogo.dismiss());

        dialogo.show();
    }

    /** Busca as três avaliações mais recentes para o atalho de reavaliação. */
    private void carregarUltimasAvaliacoes() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("usuarios").document(uid)
                .collection("avaliacoes")
                .orderBy("criadoEm", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(this::mostrarUltimasAvaliacoes);
    }

    private void mostrarUltimasAvaliacoes(QuerySnapshot snapshot) {
        View secao = findViewById(R.id.secaoUltimas);
        LinearLayout lista = findViewById(R.id.listaUltimas);
        lista.removeAllViews();

        // Sem avaliações, a seção nem aparece
        if (snapshot.isEmpty()) {
            secao.setVisibility(View.GONE);
            return;
        }
        secao.setVisibility(View.VISIBLE);

        LayoutInflater inflater = getLayoutInflater();
        SimpleDateFormat formato =
                new SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault());

        for (QueryDocumentSnapshot documento : snapshot) {
            Avaliacao a = documento.toObject(Avaliacao.class);
            View item = inflater.inflate(R.layout.item_ultima_avaliacao, lista, false);

            TextView txtNomeIdade = item.findViewById(R.id.txtNomeIdade);
            TextView txtData = item.findViewById(R.id.txtData);
            TextView txtNota = item.findViewById(R.id.txtNota);

            txtNomeIdade.setText(getString(R.string.paciente_nome_idade,
                    a.getPacienteNome(), Idade.formatar(this, a)));
            txtData.setText(a.getCriadoEm() == null ? "" : formato.format(a.getCriadoEm()));
            txtNota.setText(String.valueOf(a.getPontuacao()));
            // Nota colorida pela classificação: vermelho <= 8, azul > 8
            txtNota.getBackground().mutate().setTint(ContextCompat.getColor(this,
                    a.getPontuacao() <= Pts.LIMITE
                            ? R.color.vermelho_pedtrauma : R.color.azul_pedtrauma));

            String pacienteId = a.getPacienteId();
            item.setOnClickListener(v -> {
                if (pacienteId == null) return;
                Intent intent = new Intent(this, PacienteRegistradoActivity.class);
                intent.putExtra(PacienteRegistradoActivity.EXTRA_PACIENTE_ID, pacienteId);
                startActivity(intent);
            });

            lista.addView(item);
        }
    }

    private void sair() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
