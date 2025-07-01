package br.com.database

import br.com.database.DatabaseFactory.getConnection
import br.com.model.Evento
import br.com.model.PasseioEvento
import br.com.model.Pessoa
import java.sql.Connection
import java.sql.Statement

class PasseioEventoDAO {
    init {
        createTable()
    }

    private fun createTable() {
        val createPasseioEventoTableSQL = """
            CREATE TABLE IF NOT EXISTS PasseioEvento (
                idPasseioEvento SERIAL,
                idEvento INTEGER NOT NULL,
                usuario VARCHAR(255) NOT NULL,
                horario VARCHAR(255) NOT NULL,
                descricaoPasseio TEXT,
                UNIQUE (idPasseioEvento),
                PRIMARY KEY (idPasseioEvento, idEvento, usuario),
                FOREIGN KEY (idEvento) REFERENCES Evento(idEvento),
                FOREIGN KEY (usuario) REFERENCES Pessoa(usuario)
            );
        """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(createPasseioEventoTableSQL).use { stmt ->
                stmt.execute()
            }
        }
    }

    fun insertPasseioEvento(passeio: PasseioEvento, pessoa: Pessoa, evento: Evento): Boolean {
        val sql = """
        INSERT INTO PasseioEvento (idEvento, usuario, horario, descricaoPasseio)
        VALUES (?, ?, ?, ?)
    """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setInt(1, evento.idEvento)
                stmt.setString(2, pessoa.usuario)
                stmt.setString(3, passeio.horario)
                stmt.setString(4, passeio.descricaoPasseio)
                val affectedRows = stmt.executeUpdate()
                if (affectedRows > 0) {
                    val rsKeys = stmt.generatedKeys
                    if (rsKeys.next()) {
                        passeio.idPasseioEvento = rsKeys.getInt(1)
                    }
                    return true
                }
            }
        }
        return false
    }

    fun getAllPasseiosEvento(): List<PasseioEvento> {
        val passeios = mutableListOf<PasseioEvento>()
        // O JOIN busca a imagem (imageURL) e o nome do usuário na tabela Pessoa.
        val sql = """
            SELECT pc.idPasseioEvento, pc.usuario,pc.idEvento, pc.usuario, 
                   p.imageURL AS imagem, p.name AS nomeUsuario, 
                   pc.horario, pc.descricaoPasseio
            FROM PasseioEvento pc
            JOIN Pessoa p ON pc.usuario = p.usuario
        """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val idPasseioEvento = rs.getInt("idPasseioEvento")
                    val idEvento = rs.getInt("idEvento")
                    val usuario = rs.getString("usuario")
                    val imagem = rs.getString("imagem")
                    val nomeUsuario = rs.getString("nomeUsuario") // opcional, para referência
                    val horario = rs.getString("horario")
                    val descricaoPasseio = rs.getString("descricaoPasseio")
                    passeios.add(
                        PasseioEvento(
                            idPasseioEvento = idPasseioEvento,
                            usuario = usuario,
                            nomePessoa = nomeUsuario,
                            imagem = imagem,
                            horario = horario,
                            descricaoPasseio = descricaoPasseio,
                        )
                    )
                }
            }
        }
        return passeios
    }

    fun getPasseioByEvento(idEventoParam: Int): List<PasseioEvento> {
        val passeios = mutableListOf<PasseioEvento>()
        val sql = """
        SELECT pc.idPasseioEvento, pc.idEvento, pc.usuario, 
               p.imageURL AS imagem, p.name AS nomeUsuario, 
               pc.horario, pc.descricaoPasseio
        FROM PasseioEvento pc
        JOIN Pessoa p ON pc.usuario = p.usuario
        WHERE pc.idEvento = ? 
    """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, idEventoParam)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val idPasseioEvento = rs.getInt("idPasseioEvento")
                    val usuario = rs.getString("usuario")
                    val imagem = rs.getString("imagem")
                    val nomeUsuario = rs.getString("nomeUsuario")
                    val horario = rs.getString("horario")
                    val descricaoPasseio = rs.getString("descricaoPasseio")
                    passeios.add(
                        PasseioEvento(
                            idPasseioEvento = idPasseioEvento,
                            usuario = usuario,
                            imagem = imagem,
                            nomePessoa = nomeUsuario,
                            horario = horario,
                            descricaoPasseio = descricaoPasseio,
                        )
                    )
                }
            }
        }
        return passeios
    }

    fun deletePasseioEvento(idPasseioEvento: Int): Boolean {
        val connection: Connection = getConnection()
        connection.use {
            connection.autoCommit = false
            try {
                // 1) Deleta comentários associados ao passeio
                connection.prepareStatement(
                    "DELETE FROM ComentarioPasseioEvento WHERE idPasseioEvento = ?"
                ).use { stmt ->
                    stmt.setInt(1, idPasseioEvento)
                    stmt.executeUpdate()
                }

                // 2) Deleta chats vinculados ao passeio
                // Primeiro, remove comentários de chat
                connection.prepareStatement(
                    "DELETE FROM ComentarioChat WHERE idChat IN (SELECT idChat FROM ChatPasseio WHERE idPasseio = ?)"
                ).use { stmt ->
                    stmt.setInt(1, idPasseioEvento)
                    stmt.executeUpdate()
                }
                // Depois, removemos participações em Pessoa_Chat
                connection.prepareStatement(
                    "DELETE FROM Pessoa_Chat WHERE idChat IN (SELECT idChat FROM ChatPasseio WHERE idPasseio = ?)"
                ).use { stmt ->
                    stmt.setInt(1, idPasseioEvento)
                    stmt.executeUpdate()
                }
                // Em seguida, removemos os registros de ChatPasseio
                connection.prepareStatement(
                    "DELETE FROM ChatPasseio WHERE idPasseio = ?"
                ).use { stmt ->
                    stmt.setInt(1, idPasseioEvento)
                    stmt.executeUpdate()
                }

                // 3) Deleta o próprio passeio
                val deleted = connection.prepareStatement(
                    "DELETE FROM PasseioEvento WHERE idPasseioEvento = ?"
                ).use { stmt ->
                    stmt.setInt(1, idPasseioEvento)
                    stmt.executeUpdate() > 0
                }

                connection.commit()
                return deleted
            } catch (ex: Exception) {
                connection.rollback()
                throw ex
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun getUsuarioByPasseioEvento(idPasseioEvento: Int): String? {
        val sql = "SELECT usuario FROM PasseioEvento WHERE idPasseioEvento = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, idPasseioEvento)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getString("usuario") else null
                }
            }
        }
    }

    fun decrementaPasseios(usuario: String): Boolean {
        val sql = """
        UPDATE Pessoa
           SET qtPasseios = GREATEST(qtPasseios - 1, 0)
         WHERE usuario = ?
    """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                return stmt.executeUpdate() > 0
            }
        }
    }


    fun getPasseioEventoByUsuario(usuarioParam: String): List<PasseioEvento> {
        val passeios = mutableListOf<PasseioEvento>()
        val sql = """
        SELECT pc.idPasseioEvento, pc.usuario, pc.idEvento, pc.usuario,
               p.imageURL AS imagem, p.name AS nomeUsuario,
               pc.horario, pc.descricaoPasseio
        FROM PasseioEvento pc
        JOIN Pessoa p ON pc.usuario = p.usuario
        WHERE pc.usuario = ?
        """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuarioParam)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    passeios.add(
                        PasseioEvento(
                            idPasseioEvento = rs.getInt("idPasseioEvento"),
                            usuario = rs.getString("usuario"),
                            nomePessoa = rs.getString("nomeUsuario"),
                            imagem = rs.getString("imagem"),
                            horario = rs.getString("horario"),
                            descricaoPasseio = rs.getString("descricaoPasseio"),
                        )
                    )
                }
            }
        }
        return passeios
    }

}
