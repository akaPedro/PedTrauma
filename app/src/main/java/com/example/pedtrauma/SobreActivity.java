package com.example.pedtrauma;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * Tela "Sobre o aplicativo": logo, descrição do PedTrauma e
 * ficha técnica (versão, universidade, autores e direitos).
 */
public class SobreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sobre);
        Ui.aplicarInsets(findViewById(R.id.main));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView txtInfoSobre = findViewById(R.id.txtInfoSobre);
        txtInfoSobre.setText(getString(R.string.sobre_info, versaoDoApp()));
    }

    private String versaoDoApp() {
        try {
            return getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }
}
