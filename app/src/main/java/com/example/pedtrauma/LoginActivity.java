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

import androidx.appcompat.app.AlertDialog;
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
        Ui.aplicarInsets(findViewById(R.id.main));

        auth = FirebaseAuth.getInstance();

        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        btnEntrar = findViewById(R.id.btnEntrar);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        progressLogin = findViewById(R.id.progressLogin);

        btnEntrar.setOnClickListener(v -> fazerLogin());

        btnRegistrar.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        findViewById(R.id.tvEsqueciSenha).setOnClickListener(v -> abrirRedefinicaoSenha());
    }

    /** Diálogo "Esqueci minha senha": envia o link de redefinição por e-mail. */
    private void abrirRedefinicaoSenha() {
        EditText edtEmailRedefinicao = new EditText(this);
        edtEmailRedefinicao.setHint(R.string.hint_email);
        edtEmailRedefinicao.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                | android.text.InputType.TYPE_CLASS_TEXT);
        edtEmailRedefinicao.setText(edtEmail.getText().toString().trim());

        int margem = (int) (20 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout moldura = new android.widget.FrameLayout(this);
        moldura.setPadding(margem, 0, margem, 0);
        moldura.addView(edtEmailRedefinicao);

        new AlertDialog.Builder(this)
                .setTitle(R.string.redefinir_senha_titulo)
                .setMessage(R.string.redefinir_senha_msg)
                .setView(moldura)
                .setPositiveButton(R.string.btn_enviar, (dialogo, w) ->
                        enviarEmailRedefinicao(edtEmailRedefinicao.getText().toString().trim()))
                .setNegativeButton(R.string.btn_cancelar, null)
                .show();
    }

    private void enviarEmailRedefinicao(String email) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.erro_email_invalido, Toast.LENGTH_LONG).show();
            return;
        }

        mostrarCarregando(true);
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    mostrarCarregando(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                R.string.email_redefinicao_enviado,
                                Toast.LENGTH_LONG).show();
                    } else {
                        String motivo = task.getException() == null ? ""
                                : task.getException().getLocalizedMessage();
                        Toast.makeText(this,
                                getString(R.string.erro_redefinicao, motivo),
                                Toast.LENGTH_LONG).show();
                    }
                });
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
