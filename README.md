# PrototipoPasseios

Este repositório, na branch TCC contém o **front‑end Android** e o **back‑end Ktor** para o aplicativo WALK.
Siga este guia para instalar e executar tanto o servidor quanto o app em sua máquina e dispositivo Android.

---

## 📋 Pré‑requisitos

* **Java JDK 11+**
* **Android Studio Arctic Fox** (ou superior)
* **PostgreSQL 13+**
* **Conexão à mesma rede Wi‑Fi** – tanto o computador quanto o dispositivo Android devem estar na mesma rede local.

---

## 📂 Estrutura do Projeto

```
PrototipoPasseios/
├── backend/            ← Código do servidor Ktor + DAO + rotas
└── android-app/        ← Código do app Android em Kotlin
```

---

## 🔄 Clonando o Repositório

```bash
git clone https://github.com/joaoGUFPR/TCC2_WALK_FRONT_BACK.git
```

---

## 🗄️ Configuração do Banco de Dados (PostgreSQL)

1. Acesse seu PostgreSQL e crie um banco de dados chamado **PrototipoPasseio**:

   ```sql
   CREATE DATABASE PrototipoPasseio;
   ```
2. No módulo **backend**, abra o arquivo:
   `backend/src/main/kotlin/br/com/database/DatabaseFactory.kt`
3. Ajuste as credenciais de conexão:

   ```kotlin
   val url      = "jdbc:postgresql://localhost:5432/PrototipoPasseio"
   val user     = "SEU_USUARIO_AQUI"
   val password = "SUA_SENHA_AQUI"
   ```
4. O DAO (`DatabaseFactory`) criará automaticamente todas as tabelas necessárias ao inicializar.

---

## 🚀 Configuração do Back‑end (Ktor)

1. Abra o projeto **backend** no IntelliJ IDEA ou Android Studio.
2. Em `backend/src/main/kotlin/br/com/Routing.kt`, localize a configuração de host:

   ```kotlin
   // Linha 30
   val HOST = "192.168.0.89"
   ```
3. Substitua `"192.168.0.89"` pelo **IP da sua máquina** na rede local (use `ipconfig`/`ifconfig` para descobrir).

   ```kotlin
   embeddedServer(Netty, port = 8080, host = HOST) { … }
   ```

## 📱 Configuração do Front‑end (Android)

1. Abra a pasta **android-app** no Android Studio.

2. Em `android-app/src/main/java/com/example/prototipopasseios/controller/RetrofitClient.kt`, ajuste:

   ```kotlin
   // Linha 11
   private const val BASE_URL = "http://192.168.0.89:8080/"
   ```

   → Use `http://<SEU_IP_LOCAL>:8080/`.

3. Abra `ControllerChatPasseio.kt` dentro da pasta controller e ajuste:

   ```kotlin
   // Linha 40
   private val webSocketUrl = "ws://192.168.0.89:8080/chat-socket"
   ```

   → Use `ws://<SEU_IP_LOCAL>:8080/chat-socket`.

4. Para instalar no dispositivo:

   * Ative o **Modo Desenvolvedor** e **Depuração USB** no Android.
   * Conecte o aparelho via USB ou selecione um emulador no Android Studio e clique em **Run ▶**.

---

## ▶️ Executando o Back‑end

1. Na IDE, do módulo **backend**.
2. Clique em **Run**.



## ▶️ Executando o Front‑end

1. No Android Studio, selecione o módulo **android-app**.
2. Escolha o dispositivo físico ou emulador.
3. Clique em **Run ▶** e aguarde a instalação do aplicativo.

---

## 🐞 Troubleshooting

* **Retrofit não conecta**

  * Verifique se o IP/porta no `BASE_URL` estão corretos e coincidem com o servidor.
  * Confirme se o firewall ou VPN não bloqueia a porta `8080`.

* **Erro no banco de dados**

  * Assegure que o usuário tenha permissão de leitura/escrita.
  * Verifique se o banco `PrototipoPasseio` existe e está acessível.



