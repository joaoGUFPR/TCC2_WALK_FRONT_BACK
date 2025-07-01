package br.com.database

import br.com.database.DatabaseFactory.getConnection
import br.com.model.ChatPasseio
import java.sql.Statement

class ChatPasseioDAO {
    init {
        createTables()
    }

    private fun createTables() {
        // Cria ChatPasseio com idPasseio e tipo (EVENTO / COMUNIDADE)
        val createChatPasseioTableSQL = """
            CREATE TABLE IF NOT EXISTS ChatPasseio (
                idChat SERIAL PRIMARY KEY,
                idPasseio INTEGER NOT NULL,
                tipo VARCHAR(20) NOT NULL,
                usuarioAdministrador VARCHAR(255) NOT NULL,
                CONSTRAINT chk_tipo_valor CHECK (tipo IN ('EVENTO','COMUNIDADE')),
                FOREIGN KEY (usuarioAdministrador) REFERENCES Pessoa(usuario)
            );
        """.trimIndent()

        // Cria Pessoa_Chat sem alterações
        val createPessoaChatTableSQL = """
            CREATE TABLE IF NOT EXISTS Pessoa_Chat (
                usuario VARCHAR(255) NOT NULL,
                idChat INTEGER NOT NULL,
                PRIMARY KEY (usuario, idChat),
                FOREIGN KEY (usuario) REFERENCES Pessoa(usuario),
                FOREIGN KEY (idChat) REFERENCES ChatPasseio(idChat)
            );
        """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(createChatPasseioTableSQL).use { it.execute() }
            connection.prepareStatement(createPessoaChatTableSQL).use { it.execute() }
        }
    }

    /**
     * Retorna todos os chats em que o usuário participa,
     * mas agora exibindo o campo descricaoPasseio:
     *  - se for tipo = 'EVENTO', busca em PasseioEvento.descricaoPasseio
     *  - se for tipo = 'COMUNIDADE', busca em PasseioComunidade.descricaoPasseio
     */
    fun getChatByUsuario(usuario: String): List<ChatPasseio> {
        val chats = mutableListOf<ChatPasseio>()
        val sql = """
    SELECT 
        cp.idChat,
        cp.usuarioAdministrador,
        cp.tipo,
        CASE
          WHEN cp.tipo = 'EVENTO'
            THEN (SELECT pe.descricaoPasseio 
                  FROM PasseioEvento pe 
                  WHERE pe.idPasseioEvento = cp.idPasseio)
          WHEN cp.tipo = 'COMUNIDADE'
            THEN (SELECT pc.descricaoPasseio 
                  FROM PasseioComunidade pc 
                  WHERE pc.idPasseioComunidade = cp.idPasseio)
          ELSE ''
        END AS descricaoPasseio
    FROM ChatPasseio cp
    JOIN Pessoa_Chat pch ON cp.idChat = pch.idChat
    WHERE pch.usuario = ?
    """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val idChat = rs.getInt("idChat")

                    // Aqui foram adicionados os ?: "" para evitar NPE
                    val usuarioAdministrador = rs.getString("usuarioAdministrador") ?: ""
                    val tipoDoChat           = rs.getString("tipo")               ?: ""
                    val descricaoPasseio     = rs.getString("descricaoPasseio")     ?: ""

                    val listaUsuarios = arrayListOf<String>()
                    val sqlUsuarios = "SELECT usuario FROM Pessoa_Chat WHERE idChat = ?"
                    connection.prepareStatement(sqlUsuarios).use { stmtU ->
                        stmtU.setInt(1, idChat)
                        val rsU = stmtU.executeQuery()
                        while (rsU.next()) {
                            // assumimos que este campo nunca é nulo no seu schema
                            listaUsuarios.add(rsU.getString("usuario"))
                        }
                    }

                    chats.add(
                        ChatPasseio(
                            idChat               = idChat,
                            usuarioAdministrador = usuarioAdministrador,
                            nome                 = descricaoPasseio,
                            membros              = listaUsuarios,
                            tipo                 = tipoDoChat
                        )
                    )
                }
            }
        }
        return chats
    }


    /**
     * Busca um chat específico pelo idChat.
     * Aqui também exibimos description do Passeio:
     */
    fun getChatById(idChat: Int): ChatPasseio? {
        val sql = """
            SELECT 
                cp.idChat,
                cp.usuarioAdministrador,
                cp.tipo,
                CASE
                  WHEN cp.tipo = 'EVENTO'
                    THEN (SELECT pe.descricaoPasseio 
                          FROM PasseioEvento pe
                          WHERE pe.idPasseioEvento = cp.idPasseio)
                  WHEN cp.tipo = 'COMUNIDADE'
                    THEN (SELECT pc.descricaoPasseio 
                          FROM PasseioComunidade pc
                          WHERE pc.idPasseioComunidade = cp.idPasseio)
                  ELSE ''
                END AS descricaoPasseio
            FROM ChatPasseio cp
            WHERE cp.idChat = ?
        """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, idChat)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    val usuarioAdministrador = rs.getString("usuarioAdministrador")
                    val tipoDoChat           = rs.getString("tipo")
                    val descricaoPasseio     = rs.getString("descricaoPasseio")
                    val listaUsuarios        = arrayListOf<String>()

                    // Busca membros
                    val sqlUsuarios = "SELECT usuario FROM Pessoa_Chat WHERE idChat = ?"
                    connection.prepareStatement(sqlUsuarios).use { stmtU ->
                        stmtU.setInt(1, idChat)
                        val rsU = stmtU.executeQuery()
                        while (rsU.next()) {
                            listaUsuarios.add(rsU.getString("usuario"))
                        }
                    }

                    return ChatPasseio(
                        idChat               = idChat,
                        usuarioAdministrador = usuarioAdministrador,
                        nome                 = descricaoPasseio,
                        membros              = listaUsuarios,
                        tipo                 = tipoDoChat
                    )
                }
            }
        }
        return null
    }

    fun insertChatPasseio(idPasseio: Int, tipo: String, usuarioAdministrador: String): Int? {
        val sql = """
        INSERT INTO ChatPasseio (idPasseio, tipo, usuarioAdministrador)
        VALUES (?, ?, ?)
        """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setInt(1, idPasseio)
                stmt.setString(2, tipo)
                stmt.setString(3, usuarioAdministrador)
                val affectedRows = stmt.executeUpdate()
                if (affectedRows > 0) {
                    val rs = stmt.generatedKeys
                    if (rs.next()) {
                        return rs.getInt(1)
                    }
                }
            }
        }
        return null
    }

    fun isMember(usuario: String, idChat: Int): Boolean {
        val sql = "SELECT 1 FROM Pessoa_Chat WHERE usuario = ? AND idChat = ? LIMIT 1"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                stmt.setInt(2, idChat)
                val rs = stmt.executeQuery()
                return rs.next()
            }
        }
    }

    fun addPessoaToChat(usuario: String, idChat: Int): Boolean {
        if (isMember(usuario, idChat)) return false
        val sql = """
            INSERT INTO Pessoa_Chat (usuario, idChat)
            VALUES (?, ?)
        """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                stmt.setInt(2, idChat)
                return stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * Retorna todos os chats, exibindo a descricaoPasseio do passeio vinculado.
     */
    fun getAllChatPasseios(): List<ChatPasseio> {
        val chats = mutableListOf<ChatPasseio>()
        val sql = """
            SELECT 
                cp.idChat,
                cp.usuarioAdministrador,
                cp.tipo,
                CASE
                  WHEN cp.tipo = 'EVENTO'
                    THEN (SELECT pe.descricaoPasseio 
                          FROM PasseioEvento pe 
                          WHERE pe.idPasseioEvento = cp.idPasseio)
                  WHEN cp.tipo = 'COMUNIDADE'
                    THEN (SELECT pc.descricaoPasseio 
                          FROM PasseioComunidade pc 
                          WHERE pc.idPasseioComunidade = cp.idPasseio)
                  ELSE ''
                END AS descricaoPasseio
            FROM ChatPasseio cp
        """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val idChat               = rs.getInt("idChat")
                    val usuarioAdministrador = rs.getString("usuarioAdministrador")
                    val tipoDoChat           = rs.getString("tipo")
                    val descricaoPasseio     = rs.getString("descricaoPasseio")
                    val listaUsuarios        = arrayListOf<String>()

                    val sqlUsuarios = "SELECT usuario FROM Pessoa_Chat WHERE idChat = ?"
                    connection.prepareStatement(sqlUsuarios).use { stmtU ->
                        stmtU.setInt(1, idChat)
                        val rsU = stmtU.executeQuery()
                        while (rsU.next()) {
                            listaUsuarios.add(rsU.getString("usuario"))
                        }
                    }

                    chats.add(
                        ChatPasseio(
                            idChat               = idChat,
                            usuarioAdministrador = usuarioAdministrador,
                            nome                 = descricaoPasseio,
                            membros              = listaUsuarios,
                            tipo                 = tipoDoChat
                        )
                    )
                }
            }
        }
        return chats
    }

    /**
     * Retorna os chats de um determinado passeio (idPasseio), filtrando pelo tipo (“EVENTO” ou “COMUNIDADE”),
     * mas exibindo sempre a descricaoPasseio daquele passeio.
     */
    fun getChatsByPasseio(idPasseio: Int, tipoPasseio: String): List<ChatPasseio> {
        val chats = mutableListOf<ChatPasseio>()
        val sql = """
        SELECT 
            cp.idChat,
            cp.usuarioAdministrador,
            cp.tipo,
            CASE
              WHEN cp.tipo = 'EVENTO' 
                THEN (SELECT pe.descricaoPasseio 
                      FROM PasseioEvento pe 
                      WHERE pe.idPasseioEvento = cp.idPasseio)
              WHEN cp.tipo = 'COMUNIDADE' 
                THEN (SELECT pc.descricaoPasseio 
                      FROM PasseioComunidade pc 
                      WHERE pc.idPasseioComunidade = cp.idPasseio)
              ELSE ''
            END AS descricaoPasseio
        FROM ChatPasseio cp
        WHERE cp.idPasseio = ? AND cp.tipo = ?
        """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, idPasseio)
                stmt.setString(2, tipoPasseio)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val idChat               = rs.getInt("idChat")
                    val usuarioAdministrador = rs.getString("usuarioAdministrador")
                    val tipoDoChat           = rs.getString("tipo")
                    val descricaoPasseio     = rs.getString("descricaoPasseio")
                    val listaUsuarios        = arrayListOf<String>()

                    val sqlUsuarios = "SELECT usuario FROM Pessoa_Chat WHERE idChat = ?"
                    connection.prepareStatement(sqlUsuarios).use { stmtU ->
                        stmtU.setInt(1, idChat)
                        val rsU = stmtU.executeQuery()
                        while (rsU.next()) {
                            listaUsuarios.add(rsU.getString("usuario"))
                        }
                    }

                    chats.add(
                        ChatPasseio(
                            idChat               = idChat,
                            usuarioAdministrador = usuarioAdministrador,
                            nome                 = descricaoPasseio,
                            membros              = listaUsuarios,
                            tipo                 = tipoDoChat
                        )
                    )
                }
            }
        }
        return chats
    }

    fun deleteChat(idChat: Int): Boolean {
        val deleteCommentsSql = "DELETE FROM ComentarioChat WHERE idChat = ?"
        val deleteMembers      = "DELETE FROM Pessoa_Chat WHERE idChat = ?"
        val deleteChatSql      = "DELETE FROM ChatPasseio WHERE idChat = ?"

        getConnection().use { conn ->
            conn.autoCommit = false
            try {
                // 1) Remove todos os comentários daquele chat
                conn.prepareStatement(deleteCommentsSql).use { stmtComments ->
                    stmtComments.setInt(1, idChat)
                    stmtComments.executeUpdate()
                }
                // 2) Remove todos os membros (Pessoa_Chat)
                conn.prepareStatement(deleteMembers).use { stmtMembers ->
                    stmtMembers.setInt(1, idChat)
                    stmtMembers.executeUpdate()
                }
                // 3) Remove o próprio chat
                val affected = conn.prepareStatement(deleteChatSql).use { stmtChat ->
                    stmtChat.setInt(1, idChat)
                    stmtChat.executeUpdate()
                }
                conn.commit()
                return affected > 0
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
    }

    fun existsChatForPasseio(idPasseio: Int, tipoPasseio: String): Boolean {
        val sql = "SELECT 1 FROM ChatPasseio WHERE idPasseio = ? AND tipo = ? LIMIT 1"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, idPasseio)
                stmt.setString(2, tipoPasseio)
                val rs = stmt.executeQuery()
                return rs.next()
            }
        }
    }
}
