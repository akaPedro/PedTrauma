<p align="center">
  <img src="app/src/main/res/drawable/logpng.png" alt="Logo PedTrauma" width="140" />
</p>

<h1 align="center">PedTrauma</h1>

<p align="center">
  Aplicativo Android para cálculo do <strong>Escore de Trauma Pediátrico</strong> (Pediatric Trauma Score – PTS)
  <br />
  Universidade Estadual do Piauí (UESPI) · Campus Dra. Josefina Demes · Floriano–PI
</p>

---

## Sobre o projeto

O **PedTrauma** é um aplicativo desenvolvido para realizar o cálculo do Escore do Trauma Pediátrico (Pediatric Trauma Score – PTS). Esse escore é uma ferramenta amplamente utilizada na avaliação inicial de crianças vítimas de trauma, apresentando capacidade de predizer desfechos clínicos, como morbidade e mortalidade, além de estimar a probabilidade de sobrevida, independentemente do mecanismo da lesão.

O PedTrauma é fruto de uma revisão sistemática da literatura desenvolvida pelo acadêmico de Enfermagem **Francivaldo de Deus Coelho**, sob orientação da **Profa. Dra. Adriana da Silva Barros de Andrade**, da Universidade Estadual do Piauí (UESPI).

## Funcionalidades

- 🔐 **Autenticação de profissionais** — cadastro e login com e-mail/senha (Firebase Authentication), com validação de CPF e perfil profissional (nome, registro no conselho).
- 👤 **Perfil do profissional** — edição de nome/registro e foto de perfil (comprimida e salva no Firestore, exibida também na tela principal).
- 🧒 **Cadastro de pacientes** — nome, idade, sexo, tipo de trauma, hora da ocorrência e hora da avaliação, com **cálculo automático do tempo decorrido** entre o trauma e a avaliação.
- 📋 **Avaliação PTS em carrossel** — as 6 perguntas do escore em páginas deslizáveis, com **pontuação atualizada automaticamente** a cada seleção (sem botão "calcular") e avanço automático para a próxima pergunta.
- 🎯 **Resultado com interpretação** — pontuação total em destaque com classificação por cores (vermelho ≤ 8, azul > 8), pergunta sobre exame de imagem (com registro de achados ou alerta de risco de hemorragia intracraniana) e campo de observações.
- 📚 **Histórico cronológico** — todas as avaliações do profissional, da mais recente à mais antiga; avaliações antigas não podem ser editadas.
- 🗂 **Lista de pacientes** — pacientes registrados com resumo da última avaliação; ao tocar, permite **visualizar as avaliações anteriores** daquele paciente ou **iniciar uma nova avaliação** com ele pré-selecionado.
- 📄 **Exportação em PDF** — qualquer avaliação pode ser **salva em PDF, compartilhada ou impressa**, em página única A4 com logo, dados do profissional e do paciente, parâmetros escolhidos com pontuação individual, pontuação total, interpretação, observações, resultado do exame de imagem e **campo para assinatura manual** — pronta para anexar ao prontuário físico.
- 🌗 **Tema claro e escuro** — paleta derivada da logo, adaptada automaticamente ao tema do sistema.
- ℹ️ **Tela Sobre** — descrição do projeto, versão do app e ficha técnica.

## O Escore de Trauma Pediátrico (PTS)

Cada parâmetro recebe **+2**, **+1** ou **−1** pontos:

| Parâmetro | +2 | +1 | −1 |
|---|---|---|---|
| **Peso do paciente** | ≥ 20 kg | 10 – 19 kg | ≤ 10 kg |
| **Via aérea** | Normal | Mantida | Não mantida |
| **Pressão arterial sistólica** | > 90 mmHg | 50 – 89 mmHg | < 50 mmHg |
| **Sistema nervoso central** | Acordado | Obnubilado / perda de consciência | Coma / descerebração |
| **Ferida aberta** | Nenhuma | Menor | Maior ou penetrante |
| **Esqueleto** | Nenhuma | Fratura fechada | Fratura aberta ou múltipla |

**Interpretação do total** (−6 a +12):

- **PTS ≤ 8** → Maior potencial de mortalidade associado (exibido em vermelho)
- **PTS > 8** → Menor potencial de mortalidade associado (exibido em azul)

> ⚠️ Quando não é realizado exame de imagem, o app exibe o alerta: *"Atenção, o PTS ≤ 8, convulsões, fraturas e pouca idade pode indicar um quadro de hemorragia intracraniana."*

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 11 |
| UI | Android Views (Material 3, ViewPager2, RecyclerView, DrawerLayout) |
| Autenticação | Firebase Authentication (e-mail/senha) |
| Banco de dados | Cloud Firestore (com cache offline) |
| PDF | API nativa `PdfDocument` (sem bibliotecas externas) |
| Impressão | `PrintManager` + `PrintDocumentAdapter` |
| Compartilhamento | `FileProvider` + `ACTION_SEND` |
| Build | Gradle (AGP 8.12) · compileSdk 36 · minSdk 24 |

## Estrutura de dados (Firestore)

```
usuarios/{uid}                     ← perfil do profissional (nome, registro, cpf, email, fotoBase64)
 ├── pacientes/{pacienteId}        ← nome, idade, sexo + resumo da última avaliação
 └── avaliacoes/{avaliacaoId}      ← paciente, tipo de trauma, horas, respostas,
                                     pontuação, interpretação, exame de imagem,
                                     achados, observações, criadoEm
```

Cada profissional acessa apenas os próprios dados (ver regras abaixo).

## Configuração do Firebase

O arquivo `app/google-services.json` **não é versionado** (está no `.gitignore`). Para compilar:

1. Crie um projeto no [Firebase Console](https://console.firebase.google.com).
2. Registre o app Android com o pacote `com.example.pedtrauma`.
3. Baixe o `google-services.json` e coloque-o na pasta `app/`.
4. Em **Authentication → Sign-in method**, ative **Email/senha**.
5. Em **Firestore Database**, crie o banco (edição Standard) e aplique as regras:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /usuarios/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## Como executar

**Pré-requisitos:** Android Studio (com JDK 11+), dispositivo ou emulador com Android 7.0 (API 24) ou superior.

```bash
git clone https://github.com/akaPedro/PedTrauma.git
```

1. Abra o projeto no Android Studio.
2. Adicione o seu `google-services.json` em `app/` (ver seção anterior).
3. Sincronize o Gradle (**Sync Now**) e execute (**Run ▶**).

## Distribuição para testes

O app é distribuído aos testadores pelo **Firebase App Distribution**:

1. Gere o APK no Android Studio: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**
   (o arquivo fica em `app/build/outputs/apk/debug/app-debug.apk`).
2. No [Firebase Console](https://console.firebase.google.com) → **Release & Monitor → App Distribution**, envie o APK.
3. Adicione os e-mails dos testadores (ou um grupo). Cada um recebe o convite por e-mail e instala pelo link; novas versões notificam os testadores automaticamente.

> A cada nova versão distribuída, incremente o `versionCode` (e ajuste o `versionName`) em `app/build.gradle`.

## Fluxo do aplicativo

```
Splash → Login / Registro
              │
              ▼
        Tela principal ──────────── menu lateral: Histórico · Pacientes · Sobre · Sair
        │            │                              (topo: foto de perfil → Perfil)
        ▼            ▼
  Novo Paciente   Paciente Registrado (autocomplete + última avaliação)
        │            │
        └─────┬──────┘
              ▼
   Avaliação PTS (carrossel: 6 perguntas + resultado)
              │  confirma "Deseja salvar esta avaliação?"
              ▼
          Histórico ── toque no cartão → Salvar PDF · Compartilhar · Imprimir
```

## Ficha técnica

- **Conteúdo científico:** Francivaldo de Deus Coelho (acadêmico de Enfermagem, UESPI)
- **Orientação:** Profa. Dra. Adriana da Silva Barros de Andrade
- **Desenvolvimento do app:** Pedro Henrique de Sousa Jatobá e Samuel Oliveira da Silva
- **Instituição:** Universidade Estadual do Piauí (UESPI) — Campus Dra. Josefina Demes, Floriano–PI
- **Ano:** 2026
- **Direitos autorais de imagem:** ChatGPT
- **Contato:** francivaldodedeus05@gmail.com

---

<p align="center">Direitos reservados à UESPI</p>
