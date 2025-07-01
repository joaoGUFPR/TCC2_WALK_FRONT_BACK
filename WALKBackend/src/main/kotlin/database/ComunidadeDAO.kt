package br.com.database

import br.com.database.DatabaseFactory.getConnection
import br.com.model.Comunidade
import br.com.model.Municipio
import java.sql.Connection
import java.sql.Statement


class ComunidadeDAO {
    init {
        createTable()
        seedMunicipios()
    }

    fun insertComunidade(comunidade: Comunidade): Boolean {
        val sql = """
        INSERT INTO Comunidade (imageUrl, name, administratorUser, descricao, regras, municipio)
        VALUES (?, ?, ?, ?, ?, ?)
    """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setString(1, comunidade.imageUrl)
                stmt.setString(2, comunidade.name)
                stmt.setString(3, comunidade.administratorUser)
                stmt.setString(4, comunidade.descricao)
                stmt.setString(5, comunidade.regras) // Se regras for opcional, pode enviar "" ou null (se a coluna permitir)
                stmt.setString(6, comunidade.municipio)
                val affectedRows = stmt.executeUpdate()
                if (affectedRows > 0) {
                    val rsKeys = stmt.generatedKeys
                    if (rsKeys.next()) {
                        val idComunidade = rsKeys.getInt(1)
                        // Para cada tag, insere ou recupera o idTag e associa na tabela ComunidadeTag
                        comunidade.tags.forEach { tagName ->
                            val idTag = getOrInsertTag(connection, tagName)
                            val sqlCT = "INSERT INTO ComunidadeTag (idComunidade, idTag) VALUES (?, ?)"
                            connection.prepareStatement(sqlCT).use { stmtCT ->
                                stmtCT.setInt(1, idComunidade)
                                stmtCT.setInt(2, idTag)
                                stmtCT.executeUpdate()
                            }
                        }
                        return true
                    }
                }
            }
        }
        return false
    }


    private fun getOrInsertTag(connection: java.sql.Connection, tagName: String): Int {
        val selectSQL = "SELECT idTag FROM Tag WHERE nome = ?"
        connection.prepareStatement(selectSQL).use { selectStmt ->
            selectStmt.setString(1, tagName)
            val rs = selectStmt.executeQuery()
            if (rs.next()) {
                return rs.getInt("idTag")
            }
        }
        val insertSQL = "INSERT INTO Tag (nome) VALUES (?)"
        connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS).use { insertStmt ->
            insertStmt.setString(1, tagName)
            insertStmt.executeUpdate()
            val rsKeys = insertStmt.generatedKeys
            if (rsKeys.next()) {
                return rsKeys.getInt(1)
            } else {
                throw Exception("Erro ao inserir tag: $tagName")
            }
        }
    }

    private fun createTable() {
        val createComunidadeTableSQL = """
        CREATE TABLE IF NOT EXISTS Comunidade (
            idComunidade SERIAL PRIMARY KEY,
            imageUrl VARCHAR(255),
            name VARCHAR(255) NOT NULL,
            administratorUser VARCHAR(255) REFERENCES Pessoa(usuario),
            descricao TEXT,
            regras TEXT,
            municipio VARCHAR(255)
        );
    """.trimIndent()

        val createTagTableSQL = """
        CREATE TABLE IF NOT EXISTS Tag (
            idTag SERIAL PRIMARY KEY,
            nome VARCHAR(255) UNIQUE NOT NULL
        );
    """.trimIndent()

        val createComunidadeTagTableSQL = """
        CREATE TABLE IF NOT EXISTS ComunidadeTag (
            idComunidade INTEGER REFERENCES Comunidade(idComunidade),
            idTag INTEGER REFERENCES Tag(idTag),
            PRIMARY KEY (idComunidade, idTag)
        );
    """.trimIndent()

        val createComunidadeFavoritaTableSQL = """
        CREATE TABLE IF NOT EXISTS ComunidadeFavorita (
            usuario VARCHAR(255) REFERENCES Pessoa(usuario),
            idComunidade INTEGER REFERENCES Comunidade(idComunidade),
            PRIMARY KEY (usuario, idComunidade)
        );
    """.trimIndent()

        val createMunicipioTableSQL = """
        CREATE TABLE IF NOT EXISTS Municipio (
            nomeMunicipio VARCHAR(255) ,
            estado VARCHAR(255),
            pais VARCHAR(255),
            PRIMARY KEY (nomeMunicipio, estado, pais)
        );
    """.trimIndent()

        getConnection().use { connection ->
                connection.prepareStatement(createComunidadeTableSQL).use { stmt ->
                    stmt.execute()
                }
                connection.prepareStatement(createTagTableSQL).use { stmt ->
                    stmt.execute()
                }
                connection.prepareStatement(createComunidadeTagTableSQL).use { stmt ->
                    stmt.execute()
                }
                connection.prepareStatement(createComunidadeFavoritaTableSQL).use { stmt -> stmt.execute()
                }
                connection.prepareStatement(createMunicipioTableSQL).use { stmt -> stmt.execute()
                }
        }
    }

    fun getAllComunidades(): List<Comunidade> {
        val comunidades = mutableListOf<Comunidade>()
        // Realiza o join para obter o nome do administrador (p.name)
        val sql = """
        SELECT c.idComunidade, c.imageUrl, c.name, c.administratorUser, p.name AS administrator, 
               c.descricao, c.regras, c.municipio
        FROM Comunidade c
        INNER JOIN Pessoa p ON c.administratorUser = p.usuario
    """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val idComunidade = rs.getInt("idComunidade")
                    val imageUrl = rs.getString("imageUrl")
                    val name = rs.getString("name")
                    val administratorUser = rs.getString("administratorUser")
                    val administrator = rs.getString("administrator")
                    val descricao = rs.getString("descricao")
                    val regras = rs.getString("regras")
                    val municipio = rs.getString("municipio")
                    // Recupera as tags associadas à comunidade
                    val tags = getTagsForComunidade(connection, idComunidade)
                    comunidades.add(
                        Comunidade(
                            idComunidade,
                            imageUrl,
                            name,
                            administratorUser,
                            administrator,
                            descricao,
                            regras,
                            municipio,
                            tags
                        )
                    )
                }
            }
        }
        return comunidades
    }

    fun getComunidadeById(idComunidade: Int): Comunidade? {
        // Realiza o join para obter o nome do administrador (p.name)
        val sql = """
        SELECT c.idComunidade, c.imageUrl, c.name, c.administratorUser, p.name AS administrator, 
               c.descricao, c.regras, c.municipio
        FROM Comunidade c
        INNER JOIN Pessoa p ON c.administratorUser = p.usuario
        WHERE c.idComunidade = ?
    """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, idComunidade)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    val imageUrl = rs.getString("imageUrl")
                    val name = rs.getString("name")
                    val administratorUser = rs.getString("administratorUser")
                    val administrator = rs.getString("administrator")
                    val descricao = rs.getString("descricao")
                    val regras = rs.getString("regras")
                    val municipio = rs.getString("municipio")
                    val tags = getTagsForComunidade(connection, idComunidade)
                    return Comunidade(
                        idComunidade,
                        imageUrl,
                        name,
                        administratorUser,
                        administrator,
                        descricao,
                        regras,
                        municipio,
                        tags
                    )
                }
            }
        }
        return null
    }
    private fun getTagsForComunidade(connection: java.sql.Connection, idComunidade: Int): ArrayList<String> {
        val tags = arrayListOf<String>()
        val sql = """
            SELECT t.nome FROM Tag t
            INNER JOIN ComunidadeTag ct ON t.idTag = ct.idTag
            WHERE ct.idComunidade = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, idComunidade)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                tags.add(rs.getString("nome"))
            }
        }
        return tags
    }

    fun favoriteComunidade(usuario: String, idComunidade: Int): Boolean {
        val sql = "INSERT INTO ComunidadeFavorita (usuario, idComunidade) VALUES (?, ?)"
        return try {
            getConnection().use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.setInt(2, idComunidade)
                    stmt.executeUpdate() > 0
                }
            }
        } catch (e: org.postgresql.util.PSQLException) {
            // Se violação de unicidade (SQLState 23505), já era favorito
            if (e.sqlState == "23505") false else throw e
        }
    }

    fun unfavoriteComunidade(usuario: String, idComunidade: Int): Boolean {
        val sql = "DELETE FROM ComunidadeFavorita WHERE usuario = ? AND idComunidade = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                stmt.setInt(2, idComunidade)
                return stmt.executeUpdate() > 0
            }
        }
    }

    fun isFavorita(usuario: String, idComunidade: Int): Boolean {
        val sql = "SELECT 1 FROM ComunidadeFavorita WHERE usuario = ? AND idComunidade = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                stmt.setInt(2, idComunidade)
                val rs = stmt.executeQuery()
                return rs.next()
            }
        }
    }
    fun getComunidadesByUsuario(usuario: String): List<Comunidade> {
        val comunidades = mutableListOf<Comunidade>()
        // Consulta semelhante à getAllComunidades, mas filtrando pelo usuário administrador
        val sql = """
        SELECT c.idComunidade, c.imageUrl, c.name, c.administratorUser, p.name AS administrator, 
               c.descricao, c.regras, c.municipio
        FROM Comunidade c
        INNER JOIN Pessoa p ON c.administratorUser = p.usuario
        WHERE c.administratorUser = ?
    """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val idComunidade = rs.getInt("idComunidade")
                    val imageUrl = rs.getString("imageUrl")
                    val name = rs.getString("name")
                    val administratorUser = rs.getString("administratorUser")
                    val administrator = rs.getString("administrator")
                    val descricao = rs.getString("descricao")
                    val regras = rs.getString("regras")
                    val municipio = rs.getString("municipio")
                    val tags = getTagsForComunidade(connection, idComunidade)
                    comunidades.add(
                        Comunidade(
                            idComunidade,
                            imageUrl,
                            name,
                            administratorUser,
                            administrator,
                            descricao,
                            regras,
                            municipio,
                            tags
                        )
                    )
                }
            }
        }
        return comunidades
    }

    fun getComunidadeFavoritaByUsuario(usuario: String): List<Comunidade> {
        val comunidades = mutableListOf<Comunidade>()
        val sql = """
        SELECT c.idComunidade, c.imageUrl, c.name, c.administratorUser, p.name AS administrator, 
               c.descricao, c.regras, c.municipio
        FROM ComunidadeFavorita cf
        INNER JOIN Comunidade c ON cf.idComunidade = c.idComunidade
        INNER JOIN Pessoa p ON c.administratorUser = p.usuario
        WHERE cf.usuario = ?
    """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val idComunidade = rs.getInt("idComunidade")
                    val imageUrl = rs.getString("imageUrl")
                    val name = rs.getString("name")
                    val administratorUser = rs.getString("administratorUser")
                    val administrator = rs.getString("administrator")
                    val descricao = rs.getString("descricao")
                    val regras = rs.getString("regras")
                    val municipio = rs.getString("municipio")
                    // Recupera as tags associadas à comunidade
                    val tags = getTagsForComunidade(connection, idComunidade)
                    comunidades.add(
                        Comunidade(
                            idComunidade,
                            imageUrl,
                            name,
                            administratorUser,
                            administrator,
                            descricao,
                            regras,
                            municipio,
                            tags
                        )
                    )
                }
            }
        }
        return comunidades
    }

    fun deleteComunidade(idComunidade: Int): Boolean {
        getConnection().use { conn ->
            conn.autoCommit = false
            try {
                // 1) Apaga comentários de passeio
                conn.prepareStatement(
                    """
                DELETE FROM ComentarioPasseio 
                WHERE idPasseioComunidade IN (
                    SELECT idPasseioComunidade 
                    FROM PasseioComunidade 
                    WHERE idComunidade = ?
                )
                """.trimIndent()
                ).use { ps ->
                    ps.setInt(1, idComunidade)
                    ps.executeUpdate()
                }

                // 2) Apaga comentários de chat (IMPORTANTE: antes de remover o chat)
                conn.prepareStatement(
                    """
                DELETE FROM ComentarioChat 
                WHERE idChat IN (
                    SELECT idChat 
                    FROM ChatPasseio 
                    WHERE idPasseio = ?
                )
                """.trimIndent()
                ).use { ps ->
                    ps.setInt(1, idComunidade)
                    ps.executeUpdate()
                }

                // 3) Apaga participações em chat
                conn.prepareStatement(
                    """
                DELETE FROM Pessoa_Chat 
                WHERE idChat IN (
                    SELECT idChat 
                    FROM ChatPasseio 
                    WHERE idPasseio = ?
                )
                """.trimIndent()
                ).use { ps ->
                    ps.setInt(1, idComunidade)
                    ps.executeUpdate()
                }

                // 4) Apaga os próprios chats
                conn.prepareStatement(
                    """
                DELETE FROM ChatPasseio 
                WHERE idPasseio = ?
                """.trimIndent()
                ).use { ps ->
                    ps.setInt(1, idComunidade)
                    ps.executeUpdate()
                }

                // 5) Apaga os próprios passeios de comunidade
                conn.prepareStatement(
                    """
                DELETE FROM PasseioComunidade 
                WHERE idComunidade = ?
                """.trimIndent()
                ).use { ps ->
                    ps.setInt(1, idComunidade)
                    ps.executeUpdate()
                }

                // 6) Remove associações de tags
                conn.prepareStatement(
                    "DELETE FROM ComunidadeTag WHERE idComunidade = ?"
                ).use { ps ->
                    ps.setInt(1, idComunidade)
                    ps.executeUpdate()
                }

                // 7) Remove favoritos
                conn.prepareStatement(
                    "DELETE FROM ComunidadeFavorita WHERE idComunidade = ?"
                ).use { ps ->
                    ps.setInt(1, idComunidade)
                    ps.executeUpdate()
                }

                // 8) Finalmente, apaga a própria Comunidade
                val rows = conn.prepareStatement(
                    "DELETE FROM Comunidade WHERE idComunidade = ?"
                ).use { ps ->
                    ps.setInt(1, idComunidade)
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

    fun updateComunidade(comunidade: Comunidade): Boolean {
        getConnection().use { connection ->
            connection.autoCommit = false
            try {
                val sql = """
                UPDATE Comunidade 
                SET imageUrl = ?, name = ?, administratorUser = ?, descricao = ?, regras = ?, municipio = ?
                WHERE idComunidade = ?
            """.trimIndent()
                connection.prepareStatement(sql).use { stmt ->
                    // Se imageUrl for nulo (ou o cliente não enviou um novo arquivo), pode ser necessário manter o valor atual.
                    stmt.setString(1, comunidade.imageUrl)
                    stmt.setString(2, comunidade.name)
                    stmt.setString(3, comunidade.administratorUser) // Deve vir um valor válido (não "Desconhecido")
                    stmt.setString(4, comunidade.descricao)
                    stmt.setString(5, comunidade.regras)
                    stmt.setString(6, comunidade.municipio)
                    stmt.setInt(7, comunidade.idComunidade)
                    val updated = stmt.executeUpdate() > 0
                    if (!updated) {
                        connection.rollback()
                        return false
                    }
                }
                // Atualiza as associações das tags: remove as antigas e insere as novas
                connection.prepareStatement("DELETE FROM ComunidadeTag WHERE idComunidade = ?").use { stmt ->
                    stmt.setInt(1, comunidade.idComunidade)
                    stmt.executeUpdate()
                }
                comunidade.tags.forEach { tagName ->
                    val idTag = getOrInsertTag(connection, tagName)
                    val sqlCT = "INSERT INTO ComunidadeTag (idComunidade, idTag) VALUES (?, ?)"
                    connection.prepareStatement(sqlCT).use { stmtCT ->
                        stmtCT.setInt(1, comunidade.idComunidade)
                        stmtCT.setInt(2, idTag)
                        stmtCT.executeUpdate()
                    }
                }
                connection.commit()
                return true
            } catch (ex: Exception) {
                connection.rollback()
                throw ex
            } finally {
                connection.autoCommit = true
            }
        }
    }

    /**
     * Retorna todos os municípios cadastrados.
     */
    fun getAllMunicipios(): List<Municipio> {
        val sql = "SELECT nomeMunicipio, estado, pais FROM Municipio ORDER BY nomeMunicipio"
        val lista = mutableListOf<Municipio>()
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    lista += Municipio(
                        nomeMunicipio = rs.getString("nomeMunicipio"),
                        estado        = rs.getString("estado"),
                        pais          = rs.getString("pais")
                    )
                }
            }
        }
        return lista
    }


        private fun seedMunicipios() {
            val municipios = listOf(
                // 27 capitais + DF
                Triple("Rio Branco",            "Acre",                 "Brasil"),
                Triple("Maceió",                "Alagoas",              "Brasil"),
                Triple("Macapá",                "Amapá",                "Brasil"),
                Triple("Manaus",                "Amazonas",             "Brasil"),
                Triple("Salvador",              "Bahia",                "Brasil"),
                Triple("Fortaleza",             "Ceará",                "Brasil"),
                Triple("Brasília",              "Distrito Federal",     "Brasil"),
                Triple("Vitória",               "Espírito Santo",       "Brasil"),
                Triple("Goiânia",               "Goiás",                "Brasil"),
                Triple("São Luís",              "Maranhão",             "Brasil"),
                Triple("Cuiabá",                "Mato Grosso",          "Brasil"),
                Triple("Campo Grande",          "Mato Grosso do Sul",   "Brasil"),
                Triple("Belo Horizonte",        "Minas Gerais",         "Brasil"),
                Triple("Belém",                 "Pará",                 "Brasil"),
                Triple("João Pessoa",           "Paraíba",              "Brasil"),
                Triple("Curitiba",              "Paraná",               "Brasil"),
                Triple("Recife",                "Pernambuco",           "Brasil"),
                Triple("Teresina",              "Piauí",                "Brasil"),
                Triple("Rio de Janeiro",        "Rio de Janeiro",       "Brasil"),
                Triple("Natal",                 "Rio Grande do Norte",  "Brasil"),
                Triple("Porto Alegre",          "Rio Grande do Sul",    "Brasil"),
                Triple("Boa Vista",             "Roraima",              "Brasil"),
                Triple("Florianópolis",         "Santa Catarina",       "Brasil"),
                Triple("São Paulo",             "São Paulo",            "Brasil"),
                Triple("Aracaju",               "Sergipe",              "Brasil"),
                Triple("Palmas",                "Tocantins",            "Brasil"),

                // +173 maiores cidades (somando 200)
                Triple("Guarulhos",             "São Paulo",            "Brasil"),
                Triple("Campinas",              "São Paulo",            "Brasil"),
                Triple("São Gonçalo",           "Rio de Janeiro",       "Brasil"),
                Triple("Duque de Caxias",       "Rio de Janeiro",       "Brasil"),
                Triple("Nova Iguaçu",           "Rio de Janeiro",       "Brasil"),
                Triple("São Bernardo do Campo","São Paulo",            "Brasil"),
                Triple("São José dos Campos",   "São Paulo",            "Brasil"),
                Triple("Jaboatão dos Guararapes","Pernambuco",          "Brasil"),
                Triple("Santo André",           "São Paulo",            "Brasil"),
                Triple("Osasco",                "São Paulo",            "Brasil"),
                Triple("Sorocaba",              "São Paulo",            "Brasil"),
                Triple("Uberlândia",            "Minas Gerais",         "Brasil"),
                Triple("Ribeirão Preto",        "São Paulo",            "Brasil"),
                Triple("Contagem",              "Minas Gerais",         "Brasil"),
                Triple("Joinville",             "Santa Catarina",       "Brasil"),
                Triple("Feira de Santana",      "Bahia",                "Brasil"),
                Triple("Londrina",              "Paraná",               "Brasil"),
                Triple("Juiz de Fora",          "Minas Gerais",         "Brasil"),
                Triple("Aparecida de Goiânia",  "Goiás",                "Brasil"),
                Triple("Serra",                 "Espírito Santo",       "Brasil"),
                Triple("Campos dos Goytacazes", "Rio de Janeiro",       "Brasil"),
                Triple("Belford Roxo",          "Rio de Janeiro",       "Brasil"),
                Triple("Niterói",               "Rio de Janeiro",       "Brasil"),
                Triple("São José do Rio Preto", "São Paulo",            "Brasil"),
                Triple("Ananindeua",            "Pará",                 "Brasil"),
                Triple("Vila Velha",            "Espírito Santo",       "Brasil"),
                Triple("Caxias do Sul",         "Rio Grande do Sul",    "Brasil"),
                Triple("Porto Velho",           "Rondônia",             "Brasil"),
                Triple("Mogi das Cruzes",       "São Paulo",            "Brasil"),
                Triple("Jundiaí",               "São Paulo",            "Brasil"),
                Triple("São João de Meriti",    "Rio de Janeiro",       "Brasil"),
                Triple("Piracicaba",            "São Paulo",            "Brasil"),
                Triple("Campina Grande",        "Paraíba",              "Brasil"),
                Triple("Santos",                "São Paulo",            "Brasil"),
                Triple("Mauá",                  "São Paulo",            "Brasil"),
                Triple("Montes Claros",         "Minas Gerais",         "Brasil"),
                Triple("Betim",                 "Minas Gerais",         "Brasil"),
                Triple("Maringá",               "Paraná",               "Brasil"),
                Triple("Anápolis",              "Goiás",                "Brasil"),
                Triple("Diadema",               "São Paulo",            "Brasil"),
                Triple("Carapicuíba",           "São Paulo",            "Brasil"),
                Triple("Petrolina",             "Pernambuco",           "Brasil"),
                Triple("Bauru",                 "São Paulo",            "Brasil"),
                Triple("Caruaru",               "Pernambuco",           "Brasil"),
                Triple("Vitória da Conquista",  "Bahia",                "Brasil"),
                Triple("Itaquaquecetuba",       "São Paulo",            "Brasil"),
                Triple("Blumenau",              "Santa Catarina",       "Brasil"),
                Triple("Ponta Grossa",          "Paraná",               "Brasil"),
                Triple("Caucaia",               "Ceará",                "Brasil"),
                Triple("Cariacica",             "Espírito Santo",       "Brasil"),
                Triple("Franca",                "São Paulo",            "Brasil"),
                Triple("Olinda",                "Pernambuco",           "Brasil"),
                Triple("Praia Grande",          "São Paulo",            "Brasil"),
                Triple("Cascavel",              "Paraná",               "Brasil"),
                Triple("Canoas",                "Rio Grande do Sul",    "Brasil"),
                Triple("Paulista",              "Pernambuco",           "Brasil"),
                Triple("Foz do Iguaçu",         "Paraná",           "Brasil")
            )

            getConnection().use { conn ->
                conn.autoCommit = false
                conn.prepareStatement("""
                INSERT INTO Municipio (nomeMunicipio, estado, pais)
                VALUES (?, ?, ?)
                ON CONFLICT DO NOTHING
            """.trimIndent()).use { stmt ->
                    municipios.forEach { (nome, estado, pais) ->
                        stmt.setString(1, nome)
                        stmt.setString(2, estado)
                        stmt.setString(3, pais)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
                conn.commit()
            }
        }








}