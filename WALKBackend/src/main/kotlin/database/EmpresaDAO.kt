package br.com.database

import br.com.database.DatabaseFactory.getConnection
import br.com.model.Empresa

class EmpresaDAO {
    init {
        createTable()
    }

    private fun createTable() {
        val createEmpresaSQL = """
            CREATE TABLE IF NOT EXISTS Empresa (
                usuario     VARCHAR(255) PRIMARY KEY,
                senha       VARCHAR(255) NOT NULL,
                name        VARCHAR(255) NOT NULL,
                description TEXT NOT NULL,
                imageURL    VARCHAR(255),
                email       VARCHAR(255) NOT NULL,
                cnpj        VARCHAR(20)   NOT NULL
            );
        """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(createEmpresaSQL).use { it.execute() }
        }
    }

    /**
     * Insere uma nova empresa no banco.
     * Retorna true se inseriu com sucesso, false caso contrário.
     */
    fun insertEmpresa(e: Empresa): Boolean {
        val sql = """
            INSERT INTO Empresa (usuario, senha, name, description, imageURL, email, cnpj)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, e.usuario)
                stmt.setString(2, e.senha)
                stmt.setString(3, e.name)
                stmt.setString(4, e.description)
                stmt.setString(5, e.imageUrl)
                stmt.setString(6, e.email)
                stmt.setString(7, e.cnpj)
                return stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * Retorna a lista de todas as empresas cadastradas.
     */
    fun getAllEmpresas(): List<Empresa> {
        val list = mutableListOf<Empresa>()
        val sql = "SELECT * FROM Empresa"

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    list += Empresa(
                        usuario     = rs.getString("usuario"),
                        name        = rs.getString("name"),
                        description = rs.getString("description"),
                        imageUrl    = rs.getString("imageURL"),
                        senha       = rs.getString("senha"),
                        email       = rs.getString("email"),
                        cnpj        = rs.getString("cnpj")
                    )
                }
            }
        }
        return list
    }

    /**
     * Busca uma empresa pelo campo 'usuario'.
     * Retorna a instância de Empresa ou null se não encontrar.
     */
    fun getEmpresaByUsuario(usuario: String): Empresa? {
        val sql = "SELECT * FROM Empresa WHERE usuario = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    return Empresa(
                        usuario     = rs.getString("usuario"),
                        name        = rs.getString("name"),
                        description = rs.getString("description"),
                        imageUrl    = rs.getString("imageURL"),
                        senha       = rs.getString("senha"),
                        email       = rs.getString("email"),
                        cnpj        = rs.getString("cnpj")
                    )
                }
            }
        }
        return null
    }

    fun getEmpresaByUsuarioESenha(usuario: String, senha: String): Empresa? {
        val sql = "SELECT * FROM Empresa WHERE usuario = ? AND senha = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                stmt.setString(2, senha)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    return Empresa(
                        usuario     = rs.getString("usuario"),
                        name        = rs.getString("name"),
                        description = rs.getString("description"),
                        imageUrl    = rs.getString("imageURL"),
                        senha       = rs.getString("senha"),
                        email       = rs.getString("email"),
                        cnpj        = rs.getString("cnpj")
                    )
                }
            }
        }
        return null
    }


    /**
     * Atualiza os dados de uma empresa existente (identificada pelo campo 'usuario').
     * Retorna true se encontrou e atualizou, false caso contrário.
     */
    fun updateEmpresa(e: Empresa): Boolean {
        val sql = """
            UPDATE Empresa
               SET name        = ?,
                   description = ?,
                   imageURL    = ?,
                   email       = ?,
                   cnpj        = ?
             WHERE usuario     = ?
        """.trimIndent()

        getConnection().use { conn ->
            conn.autoCommit = false
            try {
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, e.name)
                    stmt.setString(2, e.description)
                    stmt.setString(3, e.imageUrl)
                    stmt.setString(4, e.email)
                    stmt.setString(5, e.cnpj)
                    stmt.setString(6, e.usuario)
                    val updated = stmt.executeUpdate() > 0
                    if (!updated) {
                        conn.rollback()
                        return false
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

    /**
     * Exclui uma empresa e retorna true se excluiu com sucesso, false caso contrário.
     */
    fun deleteEmpresa(usuario: String): Boolean {
        getConnection().use { conn ->
            conn.autoCommit = false
            try {
                // 1) Eventos vinculados à empresa
                val eventIds = mutableListOf<Int>()
                conn.prepareStatement("SELECT idEvento FROM Evento WHERE administratorUser = ?").use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeQuery().use { rs -> while (rs.next()) eventIds += rs.getInt("idEvento") }
                }

                if (eventIds.isNotEmpty()) {
                    val inClause = eventIds.joinToString(prefix = "(", postfix = ")")
                    // Remove dependências de eventos
                    conn.prepareStatement(
                        "DELETE FROM ComentarioPasseioEvento WHERE idPasseioEvento IN (SELECT idPasseioEvento FROM PasseioEvento WHERE idEvento IN $inClause)"
                    ).executeUpdate()
                    conn.prepareStatement("DELETE FROM PasseioEvento WHERE idEvento IN $inClause").executeUpdate()
                    conn.prepareStatement("DELETE FROM EventoFavorito WHERE idEvento IN $inClause").executeUpdate()
                    conn.prepareStatement("DELETE FROM EventoTag WHERE idEvento IN $inClause").executeUpdate()
                    conn.prepareStatement("DELETE FROM EventoMunicipio WHERE idEvento IN $inClause").executeUpdate()
                }

                // 2) Apaga eventos
                conn.prepareStatement("DELETE FROM Evento WHERE administratorUser = ?").use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeUpdate()
                }

                // 3) Seguidores da empresa
                val seguidores = mutableListOf<String>()
                conn.prepareStatement(
                    "SELECT usuario FROM PessoaSegueEmpresa WHERE empresaUsuario = ?"
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeQuery().use { rs -> while (rs.next()) seguidores += rs.getString("usuario") }
                }

                // 4) Apaga os seguimentos
                conn.prepareStatement(
                    "DELETE FROM PessoaSegueEmpresa WHERE empresaUsuario = ?"
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeUpdate()
                }

                // 5) Decrementa contador de empresas para cada seguidor
                seguidores.forEach { seguidor ->
                    conn.prepareStatement(
                        "UPDATE Pessoa SET qtEmpresas = GREATEST(qtEmpresas - 1, 0) WHERE usuario = ?"
                    ).use { stmt ->
                        stmt.setString(1, seguidor)
                        stmt.executeUpdate()
                    }
                }

                // 6) Apaga a empresa
                val deleted = conn.prepareStatement(
                    "DELETE FROM Empresa WHERE usuario = ?"
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeUpdate() > 0
                }

                conn.commit()
                return deleted
            } catch (ex: Exception) {
                conn.rollback()
                throw ex
            } finally {
                conn.autoCommit = true
            }
        }
    }


    fun getEmpresaByCnpjESenha(cnpj: String, senha: String): Empresa? {
        val sql = "SELECT * FROM Empresa WHERE cnpj = ? AND senha = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, cnpj)
                stmt.setString(2, senha)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    return Empresa(
                        usuario     = rs.getString("usuario"),
                        name        = rs.getString("name"),
                        description = rs.getString("description"),
                        imageUrl    = rs.getString("imageURL"),
                        senha       = rs.getString("senha"),
                        email       = rs.getString("email"),
                        cnpj        = rs.getString("cnpj")
                    )
                }
            }
        }
        return null
    }
}
