package com.example.pedtrauma;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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
                startActivity(new Intent(this, PacienteRegistradoActivity.class));
            } else if (id == R.id.itemSobre) {
                mostrarDialogoBemVindo();
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

    private void sair() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
