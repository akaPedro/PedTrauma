package com.example.pedtrauma;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Tela de perfil: avatar (foto escolhida pelo lápis), nome e registro.
 * A foto é comprimida (JPEG ~300px) e salva em Base64 no documento
 * do usuário no Firestore — sem depender do Firebase Storage.
 */
public class PerfilActivity extends AppCompatActivity {

    private static final int TAMANHO_FOTO = 300;   // px, quadrada
    private static final int QUALIDADE_JPEG = 75;

    private ImageView imgAvatar;
    private EditText edtNome, edtRegistro;
    private Button btnEditar;
    private ProgressBar progressPerfil;

    private FirebaseFirestore db;
    private String uid;
    private boolean editando = false;

    private ActivityResultLauncher<PickVisualMediaRequest> seletorFoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);
        Ui.aplicarInsets(findViewById(R.id.main));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        imgAvatar = findViewById(R.id.imgAvatar);
        edtNome = findViewById(R.id.edtNome);
        edtRegistro = findViewById(R.id.edtRegistro);
        btnEditar = findViewById(R.id.btnEditar);
        progressPerfil = findViewById(R.id.progressPerfil);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        seletorFoto = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) salvarFoto(uri);
                });

        findViewById(R.id.btnEditarFoto).setOnClickListener(v ->
                seletorFoto.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

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
                        exibirFoto(usuario.getFotoBase64());
                    }
                })
                .addOnFailureListener(e -> mostrarCarregando(false));
    }

    // ---------- foto de perfil ----------

    private void salvarFoto(Uri uri) {
        if (uid == null) return;

        try {
            Bitmap foto = carregarBitmapReduzido(uri);
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            foto.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, saida);
            String base64 = Base64.encodeToString(saida.toByteArray(), Base64.NO_WRAP);

            mostrarCarregando(true);
            Map<String, Object> mudanca = new HashMap<>();
            mudanca.put("fotoBase64", base64);
            db.collection("usuarios").document(uid)
                    .update(mudanca)
                    .addOnSuccessListener(unused -> {
                        mostrarCarregando(false);
                        exibirFoto(base64);
                        Toast.makeText(this, R.string.foto_atualizada,
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        mostrarCarregando(false);
                        Toast.makeText(this,
                                getString(R.string.erro_foto, e.getLocalizedMessage()),
                                Toast.LENGTH_LONG).show();
                    });
        } catch (Exception e) {
            Toast.makeText(this,
                    getString(R.string.erro_foto, e.getLocalizedMessage()),
                    Toast.LENGTH_LONG).show();
        }
    }

    /** Decodifica a imagem já reduzida e recortada em quadrado. */
    private Bitmap carregarBitmapReduzido(Uri uri) throws Exception {
        // 1ª passada: só as dimensões, para calcular a redução
        BitmapFactory.Options opcoes = new BitmapFactory.Options();
        opcoes.inJustDecodeBounds = true;
        try (InputStream entrada = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(entrada, null, opcoes);
        }
        int menorLado = Math.min(opcoes.outWidth, opcoes.outHeight);
        int reducao = 1;
        while (menorLado / (reducao * 2) >= TAMANHO_FOTO) {
            reducao *= 2;
        }

        // 2ª passada: decodifica reduzido
        opcoes = new BitmapFactory.Options();
        opcoes.inSampleSize = reducao;
        Bitmap bruto;
        try (InputStream entrada = getContentResolver().openInputStream(uri)) {
            bruto = BitmapFactory.decodeStream(entrada, null, opcoes);
        }
        if (bruto == null) throw new IllegalStateException("Imagem inválida");

        // recorte central quadrado + escala final
        int lado = Math.min(bruto.getWidth(), bruto.getHeight());
        Bitmap quadrado = Bitmap.createBitmap(bruto,
                (bruto.getWidth() - lado) / 2,
                (bruto.getHeight() - lado) / 2,
                lado, lado);
        return Bitmap.createScaledBitmap(quadrado, TAMANHO_FOTO, TAMANHO_FOTO, true);
    }

    private void exibirFoto(String base64) {
        android.graphics.drawable.Drawable circular = Ui.fotoCircular(getResources(), base64);
        if (circular != null) imgAvatar.setImageDrawable(circular);
    }

    // ---------- nome e registro ----------

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
