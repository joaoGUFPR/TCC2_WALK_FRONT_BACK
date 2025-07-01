package database

import br.com.database.DatabaseFactory.getConnection
import model.Notificacao

class NotificacaoDAO {
    init {
        createTable()
    }

    private fun createTable() {
        val sql = """
           CREATE TABLE IF NOT EXISTS Notificacao (
                usuarioDestinado  VARCHAR(255) NOT NULL,
                usuarioRemetente  VARCHAR(255) NOT NULL,
                horario           VARCHAR(255) NOT NULL,
                descricao         TEXT,
                lido              BOOLEAN NOT NULL DEFAULT FALSE,
                PRIMARY KEY (usuarioDestinado, usuarioRemetente),
                FOREIGN KEY (usuarioDestinado) REFERENCES Pessoa(usuario),
                FOREIGN KEY (usuarioRemetente) REFERENCES Pessoa(usuario)
            );
        """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { it.execute() }
        }
    }

    fun insert(notif: Notificacao): Boolean {
        val sql = """
           INSERT INTO Notificacao
             (usuarioDestinado, usuarioRemetente, horario, descricao, lido)
           VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, notif.usuarioDestinado)
                stmt.setString(2, notif.usuarioRemetente)
                stmt.setString(3, notif.horario)
                stmt.setString(4, notif.descricao)
                stmt.setBoolean(5, notif.lido)
                return try {
                    stmt.executeUpdate() > 0
                } catch (e: java.sql.SQLException) {
                    // 23505 = violação de unique/PK
                    if (e.sqlState == "23505") false else throw e
                }
            }
        }
    }

    fun markRead(usuarioDestinado: String, usuarioRemetente: String): Boolean {
        val sql = """
            UPDATE Notificacao
               SET lido = TRUE
             WHERE usuarioDestinado = ? AND usuarioRemetente = ?
        """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuarioDestinado)
                stmt.setString(2, usuarioRemetente)
                return stmt.executeUpdate() > 0
            }
        }
    }

    fun find(usuarioDestinado: String, usuarioRemetente: String): Notificacao? {
        val sql = """
           SELECT usuarioDestinado, usuarioRemetente, horario, descricao, lido
             FROM Notificacao
            WHERE usuarioDestinado = ? AND usuarioRemetente = ?
        """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuarioDestinado)
                stmt.setString(2, usuarioRemetente)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    return Notificacao(
                        usuarioDestinado = rs.getString("usuarioDestinado"),
                        usuarioRemetente = rs.getString("usuarioRemetente"),
                        descricao        = rs.getString("descricao") ?: "",
                        horario          = rs.getString("horario"),
                        lido             = rs.getBoolean("lido")
                    )
                }
            }
        }
        return null
    }

    fun findByUsuarioDestinado(usuario: String): List<Notificacao> {
        val sql = """
           SELECT usuarioDestinado, usuarioRemetente, horario, descricao, lido
             FROM Notificacao
            WHERE usuarioDestinado = ?
         ORDER BY horario DESC
        """.trimIndent()

        val result = mutableListOf<Notificacao>()
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    result += Notificacao(
                        usuarioDestinado = rs.getString("usuarioDestinado"),
                        usuarioRemetente = rs.getString("usuarioRemetente"),
                        descricao        = rs.getString("descricao") ?: "",
                        horario          = rs.getString("horario"),
                        lido             = rs.getBoolean("lido")
                    )
                }
            }
        }
        return result
    }

    fun deleteNotificacao(usuarioDestinado: String, usuarioRemetente: String): Boolean {
        val sql = """
        DELETE FROM Notificacao
         WHERE usuarioDestinado = ? AND usuarioRemetente = ?
    """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuarioDestinado)
                stmt.setString(2, usuarioRemetente)
                return stmt.executeUpdate() > 0
            }
        }
    }


}
