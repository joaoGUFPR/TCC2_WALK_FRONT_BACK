package br.com.database

import br.com.database.DatabaseFactory.getConnection
import br.com.model.ComentarioChat
import java.sql.Statement

class ComentarioChatDAO {
    init {
        createTable()
    }

    private fun createTable() {
        val createComentarioChatTableSQL = """
        CREATE TABLE IF NOT EXISTS ComentarioChat (
            idComentarioChat SERIAL PRIMARY KEY,
            idChat INTEGER NOT NULL,
            usuario VARCHAR(255) NOT NULL,
            horario VARCHAR(255) NOT NULL,
            descricaoComentario TEXT NOT NULL,
            FOREIGN KEY (idChat) REFERENCES ChatPasseio(idChat),
            FOREIGN KEY (usuario) REFERENCES Pessoa(usuario)
        );
    """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(createComentarioChatTableSQL).use { stmt ->
                stmt.execute()
            }
        }
    }

    /**
     * Insere um comentário no chat.
     * @param comentario Objeto ComentarioChat (contém horário e descrição)
     * @param idPasseio Identificador do passeio associado ao chat
     * @param idChat Identificador do chat
     * @param usuario Identificador (username) da Pessoa que envia o comentário
     */
    fun insertComentarioChat(
        comentario: ComentarioChat,
        idChat: Int,
        usuario: String
    ): Boolean {
        val sql = """
        INSERT INTO ComentarioChat (idChat, usuario, horario, descricaoComentario)
        VALUES (?, ?, ?, ?)
    """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setInt(1, idChat)
                stmt.setString(2, usuario) // Insere o identificador do usuário
                stmt.setString(3, comentario.horario)
                stmt.setString(4, comentario.descricaoComentario)
                val affectedRows = stmt.executeUpdate()
                if (affectedRows > 0) {
                    val rsKeys = stmt.generatedKeys
                    if (rsKeys.next()) {
                        comentario.idComentarioChat = rsKeys.getInt(1)
                    }
                    return true
                }
            }
        }
        return false
    }


    /**
     * Recupera os comentários de um chat específico, realizando um JOIN com Pessoa
     * para obter o nome do usuário (nomePessoa) a ser exibido.
     */
    fun getComentariosByChat(idChat: Int): List<ComentarioChat> {
        val comentarios = mutableListOf<ComentarioChat>()
        val sql = """
        SELECT cc.idComentarioChat, cc.usuario, cc.horario, cc.descricaoComentario, 
               COALESCE(p.name, '') AS nomePessoa
        FROM ComentarioChat cc
        JOIN Pessoa p ON cc.usuario = p.usuario
        WHERE cc.idChat = ?
    """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, idChat)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    comentarios.add(
                        ComentarioChat(
                            idComentarioChat = rs.getInt("idComentarioChat"),
                            usuario = rs.getString("usuario"), // Recupera o identificador do usuário
                            nomePessoa = rs.getString("nomePessoa"),
                            horario = rs.getString("horario"),
                            descricaoComentario = rs.getString("descricaoComentario")
                        )
                    )
                }
            }
        }
        return comentarios
    }


}
