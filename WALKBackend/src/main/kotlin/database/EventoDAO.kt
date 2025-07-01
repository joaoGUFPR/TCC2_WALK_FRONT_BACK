package br.com.database

import br.com.database.DatabaseFactory.getConnection
import br.com.model.Comunidade
import br.com.model.Evento
import br.com.model.Municipio
import java.sql.Statement

class EventoDAO {
    init {
        createTable()
    }

    private fun createTable() {
        // NOTA: Aqui alteramos o FK para referenciar Empresa(usuario) em vez de Pessoa(usuario)
        val createEventoTableSQL = """
            CREATE TABLE IF NOT EXISTS Evento (
                idEvento SERIAL PRIMARY KEY,
                imageUrl VARCHAR(255),
                name VARCHAR(255) NOT NULL,
                administratorUser VARCHAR(255) REFERENCES Empresa(usuario),
                administrador VARCHAR(255) NOT NULL,
                descricao TEXT,
                dataEvento VARCHAR(50) NOT NULL,
                local VARCHAR(255)
            );
        """.trimIndent()

        val createEventoMunicipioTableSQL = """
            CREATE TABLE IF NOT EXISTS EventoMunicipio (
                idEvento INTEGER REFERENCES Evento(idEvento) ON DELETE CASCADE,
                municipio VARCHAR(255) NOT NULL,
                PRIMARY KEY (idEvento, municipio)
            );
        """.trimIndent()

        val createTagTableSQL = """
            CREATE TABLE IF NOT EXISTS Tag (
                idTag SERIAL PRIMARY KEY,
                nome VARCHAR(255) UNIQUE NOT NULL
            );
        """.trimIndent()

        val createEventoTagTableSQL = """
            CREATE TABLE IF NOT EXISTS EventoTag (
                idEvento INTEGER REFERENCES Evento(idEvento) ON DELETE CASCADE,
                idTag   INTEGER REFERENCES Tag(idTag),
                PRIMARY KEY (idEvento, idTag)
            );
        """.trimIndent()

        val createEventoFavoritoTableSQL = """
        CREATE TABLE IF NOT EXISTS EventoFavorito (
            usuario VARCHAR(255) REFERENCES Pessoa(usuario),
            idEvento INTEGER REFERENCES Evento(idEvento),
            PRIMARY KEY (usuario, idEvento)
        );
    """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(createEventoTableSQL).use { it.execute() }
            conn.prepareStatement(createEventoMunicipioTableSQL).use { it.execute() }
            conn.prepareStatement(createTagTableSQL).use { it.execute() }
            conn.prepareStatement(createEventoFavoritoTableSQL).use { it.execute() }
            conn.prepareStatement(createEventoTagTableSQL).use { it.execute() }
        }
    }

    private fun getOrInsertTag(connection: java.sql.Connection, tagName: String): Int {
        val selectSQL = "SELECT idTag FROM Tag WHERE nome = ?"
        connection.prepareStatement(selectSQL).use { selectStmt ->
            selectStmt.setString(1, tagName)
            val rs = selectStmt.executeQuery()
            if (rs.next()) return rs.getInt("idTag")
        }
        val insertSQL = "INSERT INTO Tag (nome) VALUES (?)"
        connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS).use { insertStmt ->
            insertStmt.setString(1, tagName)
            insertStmt.executeUpdate()
            val rsKeys = insertStmt.generatedKeys
            if (rsKeys.next()) return rsKeys.getInt(1)
        }
        throw Exception("Erro ao inserir tag: $tagName")
    }

    fun insertEvento(evento: Evento): Boolean {
        val sql = """
            INSERT INTO Evento (imageUrl, name, administratorUser, administrador, descricao, dataEvento, local)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setString(1, evento.imageUrl)
                stmt.setString(2, evento.name)
                stmt.setString(3, evento.administratorUser)
                stmt.setString(4, evento.administrator)
                stmt.setString(5, evento.descricao)
                stmt.setString(6, evento.dataEvento)
                stmt.setString(7, evento.local)
                val affected = stmt.executeUpdate()
                if (affected > 0) {
                    val rs = stmt.generatedKeys
                    if (rs.next()) {
                        val idEvento = rs.getInt(1)

                        // 1) Inserir municípios
                        evento.municipios.forEach { m ->
                            conn.prepareStatement(
                                "INSERT INTO EventoMunicipio (idEvento, municipio) VALUES (?, ?)"
                            ).use { stmtM ->
                                stmtM.setInt(1, idEvento)
                                stmtM.setString(2, m)
                                stmtM.executeUpdate()
                            }
                        }

                        // 2) Inserir tags
                        evento.tags.forEach { tagName ->
                            val idTag = getOrInsertTag(conn, tagName)
                            conn.prepareStatement(
                                "INSERT INTO EventoTag (idEvento, idTag) VALUES (?, ?)"
                            ).use { stmtT ->
                                stmtT.setInt(1, idEvento)
                                stmtT.setInt(2, idTag)
                                stmtT.executeUpdate()
                            }
                        }
                        return true
                    }
                }
            }
        }
        return false
    }

    fun getEventosFavoritosByUsuario(usuario: String): List<Evento> {
        val eventos = mutableListOf<Evento>()
        val sql = """
        SELECT e.idEvento,
               e.imageUrl,
               e.name,
               e.administratorUser,
               emp.name   AS administrator,
               e.descricao,
               e.dataEvento,
               e.local
        FROM EventoFavorito ef
        INNER JOIN Evento e ON ef.idEvento = e.idEvento
        INNER JOIN Empresa emp ON e.administratorUser = emp.usuario
        WHERE ef.usuario = ?
    """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val idEvento          = rs.getInt("idEvento")
                    val imageUrl          = rs.getString("imageUrl")
                    val name              = rs.getString("name")
                    val administratorUser = rs.getString("administratorUser")
                    val administrator     = rs.getString("administrator")
                    val descricao         = rs.getString("descricao")
                    val dataEvento        = rs.getString("dataEvento")
                    val local             = rs.getString("local")

                    // recupera municípios associados
                    val municipios = getMunicipiosForEvento(conn, idEvento)
                    // recupera tags associadas
                    val tags = getTagsForEvento(conn, idEvento)

                    eventos += Evento(
                        idEvento          = idEvento,
                        imageUrl          = imageUrl,
                        name              = name,
                        administratorUser = administratorUser,
                        administrator     = administrator,
                        descricao         = descricao,
                        dataEvento        = dataEvento,
                        municipios        = municipios,
                        tags              = tags,
                        local             = local
                    )
                }
            }
        }
        return eventos
    }

    fun getAllEventos(): List<Evento> {
        val resultados = mutableListOf<Evento>()
        val sql = """
            SELECT e.idEvento,
                   e.imageUrl,
                   e.name,
                   e.administratorUser,
                   e.administrador,
                   e.descricao,
                   e.dataEvento,
                   e.local
            FROM Evento e
        """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val idEvento          = rs.getInt("idEvento")
                    val imageUrl          = rs.getString("imageUrl")
                    val name              = rs.getString("name")
                    val administratorUser = rs.getString("administratorUser")
                    val administrador     = rs.getString("administrador")
                    val descricao         = rs.getString("descricao")
                    val dataEvento        = rs.getString("dataEvento")
                    val local        = rs.getString("local")

                    val municipios = getMunicipiosForEvento(conn, idEvento)
                    val tags       = getTagsForEvento(conn, idEvento)

                    resultados += Evento(
                        idEvento     = idEvento,
                        imageUrl         = imageUrl,
                        name             = name,
                        administratorUser = administratorUser,
                        administrator    = administrador,
                        descricao        = descricao,
                        dataEvento       = dataEvento,
                        municipios       = municipios,
                        tags             = tags,
                        local = local
                    )
                }
            }
        }
        return resultados
    }

    fun getEventoById(idEvento: Int): Evento? {
        val sql = """
            SELECT e.idEvento,
                   e.imageUrl,
                   e.name,
                   e.administratorUser,
                   e.administrador,
                   e.descricao,
                   e.dataEvento,
                   e.local
            FROM Evento e
            WHERE e.idEvento = ?
        """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, idEvento)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    val imageUrl          = rs.getString("imageUrl")
                    val name              = rs.getString("name")
                    val administratorUser = rs.getString("administratorUser")
                    val administrador     = rs.getString("administrador")
                    val descricao         = rs.getString("descricao")
                    val dataEvento        = rs.getString("dataEvento")
                    val local        = rs.getString("local")


                    val municipios = getMunicipiosForEvento(conn, idEvento)
                    val tags       = getTagsForEvento(conn, idEvento)

                    return Evento(
                        idEvento     = idEvento,
                        imageUrl         = imageUrl,
                        name             = name,
                        administratorUser = administratorUser,
                        administrator    = administrador,
                        descricao        = descricao,
                        dataEvento       = dataEvento,
                        municipios       = municipios,
                        tags             = tags,
                        local = local
                    )
                }
            }
        }
        return null
    }

    private fun getMunicipiosForEvento(
        connection: java.sql.Connection,
        idEvento: Int
    ): ArrayList<String> {
        val lista = arrayListOf<String>()
        val sql = "SELECT municipio FROM EventoMunicipio WHERE idEvento = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, idEvento)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                lista.add(rs.getString("municipio"))
            }
        }
        return lista
    }

    private fun getTagsForEvento(
        connection: java.sql.Connection,
        idEvento: Int
    ): ArrayList<String> {
        val lista = arrayListOf<String>()
        val sql = """
            SELECT t.nome
            FROM Tag t
            JOIN EventoTag et ON t.idTag = et.idTag
            WHERE et.idEvento = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, idEvento)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                lista.add(rs.getString("nome"))
            }
        }
        return lista
    }

    fun updateEvento(evento: Evento): Boolean {
        getConnection().use { conn ->
            conn.autoCommit = false
            try {
                // 1) Atualiza dados principais
                val sql = """
                    UPDATE Evento
                    SET imageUrl         = ?,
                        name             = ?,
                        administratorUser = ?,
                        administrador    = ?,
                        descricao        = ?,
                        dataEvento       = ?,
                        local = ?
                    WHERE idEvento = ?
                """.trimIndent()

                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, evento.imageUrl)
                    stmt.setString(2, evento.name)
                    stmt.setString(3, evento.administratorUser)
                    stmt.setString(4, evento.administrator)
                    stmt.setString(5, evento.descricao)
                    stmt.setString(6, evento.dataEvento)
                    stmt.setString(7, evento.local)             // <— local VARCHAR
                    stmt.setInt(8, evento.idEvento)
                    val updated = stmt.executeUpdate() > 0
                    if (!updated) {
                        conn.rollback()
                        return false
                    }
                }

                // 2) Atualiza municípios (apaga todos e insere de novo)
                conn.prepareStatement("DELETE FROM EventoMunicipio WHERE idEvento = ?").use { stmtM ->
                    stmtM.setInt(1, evento.idEvento)
                    stmtM.executeUpdate()
                }
                evento.municipios.forEach { m ->
                    conn.prepareStatement(
                        "INSERT INTO EventoMunicipio (idEvento, municipio) VALUES (?, ?)"
                    ).use { stmtM ->
                        stmtM.setInt(1, evento.idEvento)
                        stmtM.setString(2, m)
                        stmtM.executeUpdate()
                    }
                }

                // 3) Atualiza tags (apaga todas e insere de novo)
                conn.prepareStatement("DELETE FROM EventoTag WHERE idEvento = ?").use { stmtT ->
                    stmtT.setInt(1, evento.idEvento)
                    stmtT.executeUpdate()
                }
                evento.tags.forEach { tagName ->
                    val idTag = getOrInsertTag(conn, tagName)
                    conn.prepareStatement(
                        "INSERT INTO EventoTag (idEvento, idTag) VALUES (?, ?)"
                    ).use { stmtT ->
                        stmtT.setInt(1, evento.idEvento)
                        stmtT.setInt(2, idTag)
                        stmtT.executeUpdate()
                    }
                }

                conn.commit()
                return true
            } catch (ex: Exception) {
                conn.rollback()
                throw ex
            } finally {
                conn.autoCommit = true
            }
        }
    }

    fun getEventosByUsuario(usuario: String): List<Evento> {
        val eventos = mutableListOf<Evento>()
        val sql = """
      SELECT ev.idEvento,
             ev.imageUrl,
             ev.name,
             ev.administratorUser,
             emp.name AS administrator,
             ev.descricao,
             ev.dataEvento,
             ev.local
      FROM Evento ev
      INNER JOIN Empresa emp ON ev.administratorUser = emp.usuario
      WHERE ev.administratorUser = ?
    """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                // Passe aqui exatamente o string “@j” (sem strip):
                stmt.setString(1, usuario)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val idEvento          = rs.getInt("idEvento")
                    val imageUrl          = rs.getString("imageUrl")
                    val name              = rs.getString("name")
                    val administratorUser = rs.getString("administratorUser")
                    val administrator     = rs.getString("administrator")
                    val descricao         = rs.getString("descricao")
                    val dataEvento        = rs.getString("dataEvento")
                    val local        = rs.getString("local")

                    // municípios
                    val municipios = ArrayList<String>()
                    connection.prepareStatement(
                        "SELECT municipio FROM EventoMunicipio WHERE idEvento = ?"
                    ).use { stmtMun ->
                        stmtMun.setInt(1, idEvento)
                        val rsMun = stmtMun.executeQuery()
                        while (rsMun.next()) {
                            municipios.add(rsMun.getString("municipio"))
                        }
                    }

                    // tags
                    val tags = ArrayList<String>()
                    connection.prepareStatement(
                        """
                    SELECT t.nome
                    FROM EventoTag et
                    INNER JOIN Tag t ON et.idTag = t.idTag
                    WHERE et.idEvento = ?
                    """.trimIndent()
                    ).use { stmtTag ->
                        stmtTag.setInt(1, idEvento)
                        val rsTag = stmtTag.executeQuery()
                        while (rsTag.next()) {
                            tags.add(rsTag.getString("nome"))
                        }
                    }

                    eventos.add(
                        Evento(
                            idEvento          = idEvento,
                            imageUrl          = imageUrl,
                            name              = name,
                            administratorUser = administratorUser,
                            administrator     = administrator,
                            descricao         = descricao,
                            dataEvento        = dataEvento,
                            municipios        = municipios,
                            tags              = tags,
                            local = local
                        )
                    )
                }
            }
        }
        return eventos
    }

    fun deleteEvento(idEvento: Int): Boolean {
        getConnection().use { conn ->
            conn.autoCommit = false
            try {
                // 1) Apaga comentários de passeio
                conn.prepareStatement(
                    "DELETE FROM ComentarioPasseioEvento WHERE idPasseioEvento IN " +
                            "(SELECT idPasseioEvento FROM PasseioEvento WHERE idEvento = ?)"
                ).use { it.setInt(1, idEvento); it.executeUpdate() }

                // 2) Apaga chats e participações
                conn.prepareStatement(
                    "DELETE FROM Pessoa_Chat WHERE idChat IN " +
                            "(SELECT idChat FROM ChatPasseio WHERE idPasseio = ?)"
                ).use { it.setInt(1, idEvento); it.executeUpdate() }
                conn.prepareStatement(
                    "DELETE FROM ChatPasseio WHERE idPasseio = ?"
                ).use { it.setInt(1, idEvento); it.executeUpdate() }

                // 3) Apaga os próprios passeios
                conn.prepareStatement(
                    "DELETE FROM PasseioEvento WHERE idEvento = ?"
                ).use { it.setInt(1, idEvento); it.executeUpdate() }

                // 4) Apaga associações em EventoFavorito para evitar violação de FK
                conn.prepareStatement(
                    "DELETE FROM EventoFavorito WHERE idEvento = ?"
                ).use { stmt ->
                    stmt.setInt(1, idEvento)
                    stmt.executeUpdate()
                }
                // 5) Finalmente, apaga a Evento
                val rows = conn.prepareStatement(
                    "DELETE FROM Evento WHERE idEvento = ?"
                ).use { ps ->
                    ps.setInt(1, idEvento)
                    ps.executeUpdate()
                }

                conn.commit()
                return rows > 0
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
    }

    fun favoriteEvento(usuario: String, idEvento: Int): Boolean {
        val sql = "INSERT INTO EventoFavorito (usuario, idEvento) VALUES (?, ?)"
        return try {
            getConnection().use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.setInt(2, idEvento)
                    stmt.executeUpdate() > 0
                }
            }
        } catch (e: org.postgresql.util.PSQLException) {
            // Se violação de unicidade (SQLState 23505), já era favorito
            if (e.sqlState == "23505") false else throw e
        }
    }

    fun unfavoriteEvento(usuario: String, idEvento: Int): Boolean {
        val sql = "DELETE FROM EventoFavorito WHERE usuario = ? AND idEvento = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                stmt.setInt(2, idEvento)
                return stmt.executeUpdate() > 0
            }
        }
    }

    fun isFavorita(usuario: String, idEvento: Int): Boolean {
        val sql = "SELECT 1 FROM EventoFavorito WHERE usuario = ? AND idEvento = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                stmt.setInt(2, idEvento)
                val rs = stmt.executeQuery()
                return rs.next()
            }
        }
    }

}
