package com.example.pedtrauma;

import android.content.Intent;
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


public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtSenha;
    private Button btnEntrar, btnRegistrar;
    private ProgressBar progressLogin;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        btnEntrar = findViewById(R.id.btnEntrar);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        progressLogin = findViewById(R.id.progressLogin);

        btnEntrar.setOnClickListener(v -> fazerLogin());

        btnRegistrar.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void fazerLogin() {
        String email = edtEmail.getText().toString().trim();
        String senha = edtSenha.getText().toString();

        if (!validarCampos(email, senha)) return;

        mostrarCarregando(true);

        auth.signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener(task -> {
                    mostrarCarregando(false);

                    if (task.isSuccessful()) {
                        irParaHome();
                    } else {
                        Toast.makeText(this,
                                R.string.erro_login,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validarCampos(String email, String senha) {
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError(getString(R.string.erro_campo_obrigatorio));
            edtEmail.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError(getString(R.string.erro_email_invalido));
            edtEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(senha)) {
            edtSenha.setError(getString(R.string.erro_campo_obrigatorio));
            edtSenha.requestFocus();
            return false;
        }
        return true;
    }

    private void mostrarCarregando(boolean carregando) {
        progressLogin.setVisibility(carregando ? View.VISIBLE : View.GONE);
        btnEntrar.setEnabled(!carregando);
        btnRegistrar.setEnabled(!carregando);
    }

    private void irParaHome() {
        Intent intent = new Intent(this, MainActivity.class);
        // Limpa a pilha: "voltar" na home não retorna ao login
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
