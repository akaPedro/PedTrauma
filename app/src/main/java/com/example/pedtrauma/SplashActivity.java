package com.example.pedtrauma;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Tela de carregamento (splash).
 * Exibe o logo com uma barra de progresso e, ao final,
 * decide para onde ir:
 *  - usuário já logado  -> MainActivity
 *  - usuário deslogado  -> LoginActivity
 */
public class SplashActivity extends AppCompatActivity {

    private static final long DURACAO_SPLASH_MS = 2000; // 2 segundos
    private static final int INTERVALO_MS = 20;         // atualização da barra

    private ProgressBar progressBar;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int progresso = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.progressBar);
        iniciarProgresso();
    }

    private void iniciarProgresso() {
        final int passo = (int) (100 / (DURACAO_SPLASH_MS / INTERVALO_MS));

        handler.post(new Runnable() {
            @Override
            public void run() {
                progresso += Math.max(passo, 1);
                progressBar.setProgress(progresso);

                if (progresso < 100) {
                    handler.postDelayed(this, INTERVALO_MS);
                } else {
                    irParaProximaTela();
                }
            }
        });
    }

    private void irParaProximaTela() {
        boolean logado = FirebaseAuth.getInstance().getCurrentUser() != null;

        Intent intent = new Intent(this,
                logado ? MainActivity.class : LoginActivity.class);
        startActivity(intent);
        finish(); // remove a splash da pilha (voltar não retorna para cá)
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // evita vazamento
    }
}
