package com.example.pedtrauma;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Tela de cadastro (mockup da direita).
 * 1. Cria a conta no Firebase Authentication (email/senha).
 * 2. Salva o perfil (nome, registro, cpf, email) no Firestore,
 *    na coleção "usuarios", usando o UID como ID do documento.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText edtNome, edtRegistro, edtCpf, edtEmail, edtSenha, edtConfirmarSenha;
    private Button btnCadastrar;
    private ProgressBar progressCadastro;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Ui.aplicarInsets(findViewById(R.id.main));

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtNome = findViewById(R.id.edtNome);
        edtRegistro = findViewById(R.id.edtRegistro);
        edtCpf = findViewById(R.id.edtCpf);
        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        edtConfirmarSenha = findViewById(R.id.edtConfirmarSenha);
        btnCadastrar = findViewById(R.id.btnCadastrar);
        progressCadastro = findViewById(R.id.progressCadastro);

        btnCadastrar.setOnClickListener(v -> cadastrar());
    }

    private void cadastrar() {
        String nome = edtNome.getText().toString().trim();
        String registro = edtRegistro.getText().toString().trim();
        String cpf = edtCpf.getText().toString().replaceAll("[^0-9]", "");
        String email = edtEmail.getText().toString().trim();
        String senha = edtSenha.getText().toString();
        String confirmar = edtConfirmarSenha.getText().toString();

        if (!validarCampos(nome, registro, cpf, email, senha, confirmar)) return;

        mostrarCarregando(true);

        auth.createUserWithEmailAndPassword(email, senha)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    salvarPerfil(uid, new Usuario(nome, registro, cpf, email));
                })
                .addOnFailureListener(e -> {
                    mostrarCarregando(false);
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        edtEmail.setError(getString(R.string.erro_email_em_uso));
                        edtEmail.requestFocus();
                    } else {
                        Toast.makeText(this,
                                getString(R.string.erro_cadastro, e.getLocalizedMessage()),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void salvarPerfil(String uid, Usuario usuario) {
        db.collection("usuarios").document(uid)
                .set(usuario)
                .addOnSuccessListener(unused -> {
                    mostrarCarregando(false);
                    Toast.makeText(this,
                            R.string.cadastro_sucesso,
                            Toast.LENGTH_LONG).show();
                    // Volta para o login; o usuário entra com a conta criada
                    finish();
                })
                .addOnFailureListener(e -> {
                    mostrarCarregando(false);
                    Toast.makeText(this,
                            getString(R.string.erro_cadastro, e.getLocalizedMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    private boolean validarCampos(String nome, String registro, String cpf,
                                  String email, String senha, String confirmar) {
        if (TextUtils.isEmpty(nome)) {
            edtNome.setError(getString(R.string.erro_campo_obrigatorio));
            edtNome.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(registro)) {
            edtRegistro.setError(getString(R.string.erro_campo_obrigatorio));
            edtRegistro.requestFocus();
            return false;
        }
        if (!cpfValido(cpf)) {
            edtCpf.setError(getString(R.string.erro_cpf_invalido));
            edtCpf.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError(getString(R.string.erro_email_invalido));
            edtEmail.requestFocus();
            return false;
        }
        if (senha.length() < 6) { // mínimo exigido pelo Firebase Auth
            edtSenha.setError(getString(R.string.erro_senha_curta));
            edtSenha.requestFocus();
            return false;
        }
        if (!senha.equals(confirmar)) {
            edtConfirmarSenha.setError(getString(R.string.erro_senhas_diferentes));
            edtConfirmarSenha.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Validação completa de CPF (dígitos verificadores).
     */
    private boolean cpfValido(String cpf) {
        if (cpf == null || cpf.length() != 11) return false;
        if (cpf.chars().distinct().count() == 1) return false; // 111.111.111-11 etc.

        try {
            int soma = 0;
            for (int i = 0; i < 9; i++) {
                soma += (cpf.charAt(i) - '0') * (10 - i);
            }
            int digito1 = 11 - (soma % 11);
            if (digito1 >= 10) digito1 = 0;
            if (digito1 != cpf.charAt(9) - '0') return false;

            soma = 0;
            for (int i = 0; i < 10; i++) {
                soma += (cpf.charAt(i) - '0') * (11 - i);
            }
            int digito2 = 11 - (soma % 11);
            if (digito2 >= 10) digito2 = 0;
            return digito2 == cpf.charAt(10) - '0';
        } catch (Exception e) {
            return false;
        }
    }

    private void mostrarCarregando(boolean carregando) {
        progressCadastro.setVisibility(carregando ? View.VISIBLE : View.GONE);
        btnCadastrar.setEnabled(!carregando);
    }
}
