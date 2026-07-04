package com.example.pedtrauma;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Utilidades de janela. Com o edge-to-edge do Android 15+ o conteúdo
 * desenha atrás das barras do sistema; este helper encaixa a tela
 * entre a barra de status e os botões de navegação.
 */
final class Ui {

    private Ui() {
    }

    /**
     * Aplica as barras do sistema (status/navegação) como padding da raiz.
     * Também considera o teclado (IME): quando aberto, o conteúdo é
     * empurrado para cima para o campo em foco continuar visível.
     */
    static void aplicarInsets(View raiz) {
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets teclado = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(barras.left, barras.top, barras.right,
                    Math.max(barras.bottom, teclado.bottom));
            return insets;
        });
    }
}
