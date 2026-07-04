package com.example.pedtrauma;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Tela de perfil (mockup): avatar, nome e registro do usuário logado.
 * "Editar informações" libera os campos; "Salvar" grava no Firestore
 * (documento usuarios/{uid}, os mesmos campos criados no cadastro).
 */
public class PerfilActivity extends AppCompatActivity {

    private EditText edtNome, edtRegistro;
    private Button btnEditar;
    private ProgressBar progressPerfil;

    private FirebaseFirestore db;
    private String uid;
    private boolean editando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        edtNome = findViewById(R.id.edtNome);
        edtRegistro = findViewById(R.id.edtRegistro);
        btnEditar = findViewById(R.id.btnEditar);
        progressPerfil = findViewById(R.id.progressPerfil);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        btnEditar.setOnClickListener(v -> {
            if (editando) salvar();
            else entrarModoEdicao();
        });

        carregarPerfil();
    }

    private void carregarPerfil() {
        if (uid == null) return;

        mostrarCarregando(true);
        db.collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    mostrarCarregando(false);
                    Usuario usuario = doc.toObject(Usuario.class);
                    if (usuario != null) {
                        edtNome.setText(usuario.getNome());
                        edtRegistro.setText(usuario.getRegistro());
                    }
                })
                .addOnFailureListener(e -> mostrarCarregando(false));
    }

    private void entrarModoEdicao() {
        editando = true;
        edtNome.setEnabled(true);
        edtRegistro.setEnabled(true);
        edtNome.requestFocus();
        btnEditar.setText(R.string.btn_salvar);
    }

    private void salvar() {
        String nome = edtNome.getText().toString().trim();
        String registro = edtRegistro.getText().toString().trim();

        if (TextUtils.isEmpty(nome)) {
            edtNome.setError(getString(R.string.erro_campo_obrigatorio));
            edtNome.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(registro)) {
            edtRegistro.setError(getString(R.string.erro_campo_obrigatorio));
            edtRegistro.requestFocus();
            return;
        }
        if (uid == null) return;

        mostrarCarregando(true);

        Map<String, Object> mudancas = new HashMap<>();
        mudancas.put("nome", nome);
        mudancas.put("registro", registro);

        db.collection("usuarios").document(uid)
                .update(mudancas)
                .addOnSuccessListener(unused -> {
                    mostrarCarregando(false);
                    sairModoEdicao();
                    Toast.makeText(this,
                            R.string.perfil_atualizado,
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    mostrarCarregando(false);
                    Toast.makeText(this,
                            getString(R.string.erro_atualizar, e.getLocalizedMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void sairModoEdicao() {
        editando = false;
        edtNome.setEnabled(false);
        edtRegistro.setEnabled(false);
        btnEditar.setText(R.string.btn_editar_informacoes);
    }

    private void mostrarCarregando(boolean carregando) {
        progressPerfil.setVisibility(carregando ? View.VISIBLE : View.GONE);
        btnEditar.setEnabled(!carregando);
    }
}
