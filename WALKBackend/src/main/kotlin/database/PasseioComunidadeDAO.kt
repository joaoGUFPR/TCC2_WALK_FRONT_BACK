package br.com.database

import br.com.database.DatabaseFactory.getConnection
import br.com.model.Comunidade
import br.com.model.PasseioComunidade
import br.com.model.Pessoa
import java.sql.Connection
import java.sql.Statement

class PasseioComunidadeDAO {
    init {
        createTable()
    }

    private fun createTable() {
        val createPasseioComunidadeTableSQL = """
            CREATE TABLE IF NOT EXISTS PasseioComunidade (
                idPasseioComunidade SERIAL,
                idComunidade INTEGER NOT NULL,
                usuario VARCHAR(255) NOT NULL,
                horario VARCHAR(255) NOT NULL,
                descricaoPasseio TEXT,
                localizacao VARCHAR(255),
                UNIQUE (idPasseioComunidade),
                PRIMARY KEY (idPasseioComunidade, idComunidade, usuario),
                FOREIGN KEY (idComunidade) REFERENCES Comunidade(idComunidade),
                FOREIGN KEY (usuario) REFERENCES Pessoa(usuario)
            );
        """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(createPasseioComunidadeTableSQL).use { stmt ->
                stmt.execute()
            }
        }
    }

    fun insertPasseioComunidade(passeio: PasseioComunidade, pessoa: Pessoa, comunidade: Comunidade): Boolean {
        val sql = """
            INSERT INTO PasseioComunidade (idComunidade, usuario, horario, descricaoPasseio, localizacao)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setInt(1, comunidade.idComunidade)
                stmt.setString(2, pessoa.usuario)
                stmt.setString(3, passeio.horario)
                stmt.setString(4, passeio.descricaoPasseio)
                stmt.setString(5, passeio.localizacao)
                val affectedRows = stmt.executeUpdate()
                if (affectedRows > 0) {
                    val rsKeys = stmt.generatedKeys
                    if (rsKeys.next()) {
                        passeio.idPasseioComunidade = rsKeys.getInt(1)
                    }
                    return true
                }
            }
        }
        return false
    }

    fun getAllPasseios(): List<PasseioComunidade> {
        val passeios = mutableListOf<PasseioComunidade>()
        // O JOIN busca a imagem (imageURL) e o nome do usuário na tabela Pessoa.
        val sql = """
            SELECT pc.idPasseioComunidade, pc.usuario,pc.idComunidade, pc.usuario, 
                   p.imageURL AS imagem, p.name AS nomeUsuario, 
                   pc.horario, pc.descricaoPasseio, pc.localizacao
            FROM PasseioComunidade pc
            JOIN Pessoa p ON pc.usuario = p.usuario
        """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val idPasseioComunidade = rs.getInt("idPasseioComunidade")
                    val idComunidade = rs.getInt("idComunidade")
                    val usuario = rs.getString("usuario")
                    val imagem = rs.getString("imagem")
                    val nomeUsuario = rs.getString("nomeUsuario") // opcional, para referência
                    val horario = rs.getString("horario")
                    val descricaoPasseio = rs.getString("descricaoPasseio")
                    val localizacao = rs.getString("localizacao")
                    passeios.add(
                        PasseioComunidade(
                            idPasseioComunidade = idPasseioComunidade,
                            usuario = usuario,
                            nomePessoa = nomeUsuario,
                            imagem = imagem,
                            horario = horario,
                            descricaoPasseio = descricaoPasseio,
                            localizacao = localizacao
                        )
                    )
                }
            }
        }
        return passeios
    }

    fun getPasseiosByComunidade(idComunidadeParam: Int): List<PasseioComunidade> {
        val passeios = mutableListOf<PasseioComunidade>()
        val sql = """
        SELECT pc.idPasseioComunidade, pc.idComunidade, pc.usuario, 
               p.imageURL AS imagem, p.name AS nomeUsuario, 
               pc.horario, pc.descricaoPasseio, pc.localizacao
        FROM PasseioComunidade pc
        JOIN Pessoa p ON pc.usuario = p.usuario
        WHERE pc.idComunidade = ? 
    """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, idComunidadeParam)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val idPasseioComunidade = rs.getInt("idPasseioComunidade")
                    val usuario = rs.getString("usuario")
                    val imagem = rs.getString("imagem")
                    val nomeUsuario = rs.getString("nomeUsuario")
                    val horario = rs.getString("horario")
                    val descricaoPasseio = rs.getString("descricaoPasseio")
                    val localizacao = rs.getString("localizacao")
                    passeios.add(
                        PasseioComunidade(
                            idPasseioComunidade = idPasseioComunidade,
                            usuario = usuario,
                            imagem = imagem,
                            nomePessoa = nomeUsuario,
                            horario = horario,
                            descricaoPasseio = descricaoPasseio,
                            localizacao = localizacao
                        )
                    )
                }
            }
        }
        return passeios
    }

    fun deletePasseioComunidade(idPasseioComunidade: Int): Boolean {
        val connection: Connection = getConnection()
        connection.use {
            connection.autoCommit = false
            try {
                // 1) Deleta comentários associados ao passeio
                connection.prepareStatement(
                    "DELETE FROM ComentarioPasseio WHERE idPasseioComunidade = ?"
                ).use { stmt ->
                    stmt.setInt(1, idPasseioComunidade)
                    stmt.executeUpdate()
                }

                // 2) Deleta chats vinculados ao passeio
                // Primeiro, remove comentários de chat
                connection.prepareStatement(
                    "DELETE FROM ComentarioChat WHERE idChat IN (SELECT idChat FROM ChatPasseio WHERE idPasseio = ?)"
                ).use { stmt ->
                    stmt.setInt(1, idPasseioComunidade)
                    stmt.executeUpdate()
                }
                // Depois, removemos participações em Pessoa_Chat
                connection.prepareStatement(
                    "DELETE FROM Pessoa_Chat WHERE idChat IN (SELECT idChat FROM ChatPasseio WHERE idPasseio = ?)"
                ).use { stmt ->
                    stmt.setInt(1, idPasseioComunidade)
                    stmt.executeUpdate()
                }
                // Em seguida, removemos os registros de ChatPasseio
                connection.prepareStatement(
                    "DELETE FROM ChatPasseio WHERE idPasseio = ?"
                ).use { stmt ->
                    stmt.setInt(1, idPasseioComunidade)
                    stmt.executeUpdate()
                }

                // 3) Deleta o próprio passeio
                val deleted = connection.prepareStatement(
                    "DELETE FROM PasseioComunidade WHERE idPasseioComunidade = ?"
                ).use { stmt ->
                    stmt.setInt(1, idPasseioComunidade)
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

    fun getUsuarioByPasseio(idPasseioComunidade: Int): String? {
        val sql = "SELECT usuario FROM PasseioComunidade WHERE idPasseioComunidade = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, idPasseioComunidade)
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


    fun getPasseiosByUsuario(usuarioParam: String): List<PasseioComunidade> {
        val passeios = mutableListOf<PasseioComunidade>()
        val sql = """
        SELECT pc.idPasseioComunidade, pc.usuario, pc.idComunidade, pc.usuario,
               p.imageURL AS imagem, p.name AS nomeUsuario,
               pc.horario, pc.descricaoPasseio, pc.localizacao
        FROM PasseioComunidade pc
        JOIN Pessoa p ON pc.usuario = p.usuario
        WHERE pc.usuario = ?
        """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuarioParam)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    passeios.add(
                        PasseioComunidade(
                            idPasseioComunidade = rs.getInt("idPasseioComunidade"),
                            usuario = rs.getString("usuario"),
                            nomePessoa = rs.getString("nomeUsuario"),
                            imagem = rs.getString("imagem"),
                            horario = rs.getString("horario"),
                            descricaoPasseio = rs.getString("descricaoPasseio"),
                            localizacao = rs.getString("localizacao")
                        )
                    )
                }
            }
        }
        return passeios
    }

}
