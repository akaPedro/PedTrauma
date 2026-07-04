package com.example.pedtrauma;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
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

    /**
     * Decodifica a foto de perfil (Base64) em um drawable circular.
     * Retorna null se o texto for nulo, vazio ou inválido.
     */
    static Drawable fotoCircular(Resources recursos, String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
            Bitmap foto = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (foto == null) return null;
            RoundedBitmapDrawable circular =
                    RoundedBitmapDrawableFactory.create(recursos, foto);
            circular.setCircular(true);
            return circular;
        } catch (IllegalArgumentException e) {
            return null; // Base64 corrompido
        }
    }
}
