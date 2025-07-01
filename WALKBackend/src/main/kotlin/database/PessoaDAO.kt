package br.com.database

import br.com.database.DatabaseFactory.getConnection
import br.com.model.Empresa
import br.com.model.Pessoa

class PessoaDAO {
    init {
        createTable()
        ensureAdminUser()
        createFollowEmpresaTable()
    }

    fun insertPessoa(p: Pessoa): Boolean {
        val sql = """
        INSERT INTO Pessoa (usuario, senha, name, description, imageURL,
                            qtAmigos, qtPasseios, municipio, email, nascimento, qtEmpresas)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, p.usuario)
                stmt.setString(2, p.senha)
                stmt.setString(3, p.name)
                stmt.setString(4, p.description)
                stmt.setString(5, p.imageUrl)
                stmt.setInt(6, p.qtAmigos)
                stmt.setInt(7, p.qtPasseios)
                stmt.setString(8, p.municipio)
                stmt.setString(9, p.email)
                stmt.setString(10, p.nascimento)
                stmt.setInt(11, p.qtEmpresas)
                return stmt.executeUpdate() > 0
            }
        }
    }

    private fun createTable() {
        val createPessoaSQL = """
        CREATE TABLE IF NOT EXISTS Pessoa (
            usuario VARCHAR(255) PRIMARY KEY,
            senha VARCHAR(255) NOT NULL,
            name VARCHAR(255) NOT NULL,
            description TEXT NOT NULL,
            imageURL VARCHAR(255),
            qtAmigos INTEGER DEFAULT 0,
            qtPasseios INTEGER DEFAULT 0,
            municipio VARCHAR(255),
            email VARCHAR(255),
            nascimento VARCHAR(255),
            qtEmpresas INTEGER DEFAULT 0
        );
        """.trimIndent()

        val createPessoaAmigoSQL = """
        CREATE TABLE IF NOT EXISTS PessoaAmigo (
            usuario VARCHAR(255) NOT NULL,
            amigo  VARCHAR(255) NOT NULL,
            criado_em TIMESTAMP DEFAULT now(),
            PRIMARY KEY (usuario, amigo),
            FOREIGN KEY (usuario) REFERENCES Pessoa(usuario) ON DELETE CASCADE,
            FOREIGN KEY (amigo)  REFERENCES Pessoa(usuario) ON DELETE CASCADE
        );
        """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(createPessoaSQL).use { it.execute() }
            conn.prepareStatement(createPessoaAmigoSQL).use { it.execute() }
        }
    }

    fun getAllPessoas(): List<Pessoa> {
        val list = mutableListOf<Pessoa>()
        val sql = "SELECT * FROM Pessoa"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    list += Pessoa(
                        usuario   = rs.getString("usuario"),
                        name      = rs.getString("name"),
                        description = rs.getString("description"),
                        imageUrl  = rs.getString("imageURL"),
                        qtAmigos  = rs.getInt("qtAmigos"),
                        qtPasseios= rs.getInt("qtPasseios"),
                        municipio = rs.getString("municipio"),
                        senha     = rs.getString("senha"),
                        email     = rs.getString("email"),
                        nascimento= rs.getString("nascimento"),
                        qtEmpresas = rs.getInt("qtEmpresas")
                    )
                }
            }
        }
        return list.filter { !it.usuario.endsWith("@adm")}
    }

    fun getPessoaByusuario(usuario: String): Pessoa? {
        val sql = "SELECT * FROM Pessoa WHERE usuario = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    return Pessoa(
                        usuario   = rs.getString("usuario"),
                        name      = rs.getString("name"),
                        description = rs.getString("description"),
                        imageUrl  = rs.getString("imageURL"),
                        qtAmigos  = rs.getInt("qtAmigos"),
                        qtPasseios= rs.getInt("qtPasseios"),
                        municipio = rs.getString("municipio"),
                        senha     = rs.getString("senha"),
                        email     = rs.getString("email"),
                        nascimento= rs.getString("nascimento"),
                        qtEmpresas  = rs.getInt("qtEmpresas")
                    )
                }
            }
        }
        return null
    }

    fun getPessoaByUsuarioESenha(usuario: String, senha: String): Pessoa? {
        val sql = "SELECT * FROM Pessoa WHERE usuario = ? AND senha = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                stmt.setString(2, senha)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    return Pessoa(
                        usuario   = rs.getString("usuario"),
                        name      = rs.getString("name"),
                        description = rs.getString("description"),
                        imageUrl  = rs.getString("imageURL"),
                        qtAmigos  = rs.getInt("qtAmigos"),
                        qtPasseios= rs.getInt("qtPasseios"),
                        municipio = rs.getString("municipio"),
                        senha     = rs.getString("senha"),
                        email     = rs.getString("email"),
                        nascimento= rs.getString("nascimento"),
                        qtEmpresas  = rs.getInt("qtEmpresas")
                    )
                }
            }
        }
        return null
    }

    fun addFriend(usuario: String, amigo: String): Boolean {
        val sql = "INSERT INTO PessoaAmigo (usuario, amigo) VALUES (?, ?)"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                stmt.setString(2, amigo)
                return try {
                    stmt.executeUpdate() > 0
                } catch (e: java.sql.SQLException) {
                    if (e.sqlState == "23505") false else throw e
                }
            }
        }
    }

    fun getFriends(usuario: String): List<String> {
        val amigos = mutableListOf<String>()
        val sql = "SELECT amigo FROM PessoaAmigo WHERE usuario = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    amigos.add(rs.getString("amigo"))
                }
            }
        }
        return amigos
    }

    fun incrementaPasseios(usuario: String): Boolean {
        val sql = """
            UPDATE Pessoa
               SET qtPasseios = qtPasseios + 1
             WHERE usuario = ?
        """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                return stmt.executeUpdate() > 0
            }
        }
    }

    fun incrementaAmigos(usuario: String): Boolean {
        val sql = """
      UPDATE Pessoa
         SET qtAmigos = qtAmigos + 1
       WHERE usuario = ?
    """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                return stmt.executeUpdate() > 0
            }
        }
    }

    fun isFriend(usuario: String, amigo: String): Boolean {
        val sql = """
        SELECT 1
          FROM PessoaAmigo
         WHERE usuario = ? AND amigo = ?
         LIMIT 1
    """.trimIndent()
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                stmt.setString(2, amigo)
                val rs = stmt.executeQuery()
                return rs.next()
            }
        }
    }

    fun getFriendPessoas(usuario: String): List<Pessoa> {
        val sql = """
        SELECT p.* 
          FROM PessoaAmigo pa
          JOIN Pessoa p ON p.usuario = pa.amigo
         WHERE pa.usuario = ?
        """.trimIndent()
        val list = mutableListOf<Pessoa>()
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    list += Pessoa(
                        usuario   = rs.getString("usuario"),
                        name      = rs.getString("name"),
                        description = rs.getString("description"),
                        imageUrl  = rs.getString("imageURL"),
                        qtAmigos  = rs.getInt("qtAmigos"),
                        qtPasseios= rs.getInt("qtPasseios"),
                        municipio = rs.getString("municipio"),
                        senha     = rs.getString("senha"),
                        email     = rs.getString("email"),
                        nascimento= rs.getString("nascimento"),
                        qtEmpresas = rs.getInt("qtEmpresas")
                    )
                }
            }
        }
        return list
    }

    fun updateMunicipio(usuario: String, municipio: String): Boolean {
        val sql = """
            UPDATE Pessoa
               SET municipio = ?
             WHERE usuario = ?
        """.trimIndent()
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, municipio)
                stmt.setString(2, usuario)
                return stmt.executeUpdate() > 0
            }
        }
    }

    fun updatePessoa(p: Pessoa): Boolean {
        val sql = """
        UPDATE Pessoa
           SET name        = ?,
               description = ?,
               imageURL    = ?,
               municipio   = ?,
               email       = ?,
               nascimento  = ?
         WHERE usuario     = ?
        """.trimIndent()

        getConnection().use { conn ->
            conn.autoCommit = false
            try {
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, p.name)
                    stmt.setString(2, p.description)
                    stmt.setString(3, p.imageUrl)
                    stmt.setString(4, p.municipio)
                    stmt.setString(5, p.email)
                    stmt.setString(6, p.nascimento)
                    stmt.setString(7, p.usuario)
                    val updated = stmt.executeUpdate() > 0
                    if (!updated) {
                        conn.rollback()
                        return false
                    }
                }
                conn.commit()
                return true
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
    }

    fun decrementaAmigos(usuario: String): Boolean {
        val sql = """
        UPDATE Pessoa
           SET qtAmigos = GREATEST(qtAmigos - 1, 0)
         WHERE usuario = ?
    """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                return stmt.executeUpdate() > 0
            }
        }
    }

    fun removeFriend(usuario: String, amigo: String): Boolean {
        val sql = "DELETE FROM PessoaAmigo WHERE usuario = ? AND amigo = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                stmt.setString(2, amigo)
                val deleted = stmt.executeUpdate() > 0
                if (deleted) {
                }
                return deleted
            }
        }
    }

    private fun ensureAdminUser() {
        val adminUsuario = "@adm"
        if (getPessoaByusuario(adminUsuario) == null) {
            val admin = Pessoa(
                usuario   = adminUsuario,
                senha     = "admin123",
                name      = "Administrador",
                description = "Usuário administrador",
                imageUrl  = null,
                qtAmigos  = 0,
                qtPasseios= 0,
                municipio = "",
                email     = "adm@gmail.com",
                nascimento= "",
                qtEmpresas  = 0
            )
            insertPessoa(admin)
        }
    }
    fun deletePessoa(usuario: String): Boolean {
        val conn = getConnection()
        conn.use { it ->
            it.autoCommit = false
            try {
                // 1) Recupera todos os amigos ligados a esse usuário
                val amigos = mutableSetOf<String>()
                it.prepareStatement(
                    "SELECT amigo FROM PessoaAmigo WHERE usuario = ?"
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        amigos.add(rs.getString("amigo"))
                    }
                }

                it.prepareStatement(
                    "SELECT usuario FROM PessoaAmigo WHERE amigo = ?"
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        amigos.add(rs.getString("usuario"))
                    }
                }

                // 2) Deleta amizades envolvendo esse usuário
                it.prepareStatement(
                    "DELETE FROM PessoaAmigo WHERE usuario = ? OR amigo = ?"
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.setString(2, usuario)
                    stmt.executeUpdate()
                }

                // 3) Decrementa qtAmigos de todos os usuários afetados
                for (amigo in amigos) {
                    it.prepareStatement(
                        "UPDATE Pessoa SET qtAmigos = GREATEST(qtAmigos - 1, 0) WHERE usuario = ?"
                    ).use { stmt ->
                        stmt.setString(1, amigo)
                        stmt.executeUpdate()
                    }
                }

                // 4) Notificações
                it.prepareStatement(
                    "DELETE FROM Notificacao WHERE usuarioDestinado = ? OR usuarioRemetente = ?"
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.setString(2, usuario)
                    stmt.executeUpdate()
                }

                // 5) Comentários de chat
                it.prepareStatement(
                    "DELETE FROM ComentarioChat WHERE usuario = ?"
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeUpdate()
                }

                // 6) Pessoa_Chat
                it.prepareStatement(
                    "DELETE FROM Pessoa_Chat WHERE usuario = ?"
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeUpdate()
                }

                it.prepareStatement(
                    """
    DELETE FROM Pessoa_Chat
     WHERE idChat IN (
         SELECT idChat
           FROM ChatPasseio
          WHERE usuarioAdministrador = ?
     )
    """.trimIndent()
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeUpdate()
                }

                it.prepareStatement(
                    """
    DELETE FROM ComentarioChat
     WHERE idChat IN (
         SELECT idChat
           FROM ChatPasseio
          WHERE usuarioAdministrador = ?
     )
    """.trimIndent()
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeUpdate()
                }

                // 7) Chats administrados
                it.prepareStatement(
                    "DELETE FROM ChatPasseio WHERE usuarioAdministrador = ?"
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeUpdate()
                }

                // 8) Apaga TODOS os ComentarioPasseio associados a passeios criados por este usuário
                it.prepareStatement(
                    """
                DELETE FROM ComentarioPasseio
                  WHERE idPasseioComunidade IN (
                      SELECT idPasseioComunidade
                        FROM PasseioComunidade
                       WHERE usuario = ?
                  )
                """.trimIndent()
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeUpdate()
                }

                // 9) Passeios criados
                it.prepareStatement(
                    "DELETE FROM PasseioComunidade WHERE usuario = ?"
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeUpdate()
                }

                it.prepareStatement(
                    """
    DELETE FROM ComunidadETag
     WHERE idcomunidade IN (
         SELECT idcomunidade
           FROM Comunidade
          WHERE administratorUser = ?
     )
    """.trimIndent()
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeUpdate()
                }

                // 10) Comunidades que administra
                it.prepareStatement(
                    "DELETE FROM Comunidade WHERE administratorUser = ?"
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeUpdate()
                }

                // 11) Favoritos
                it.prepareStatement(
                    "DELETE FROM ComunidadeFavorita WHERE usuario = ?"
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeUpdate()
                }

                it.prepareStatement(
                    "DELETE FROM PasseioEvento WHERE usuario = ?"
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeUpdate()
                }
                it.prepareStatement(
                    "DELETE FROM EventoFavorito WHERE usuario = ?"
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeUpdate()
                }

                // 12) Finalmente, apaga a pessoa
                val deleted = it.prepareStatement(
                    "DELETE FROM Pessoa WHERE usuario = ?"
                ).use { stmt ->
                    stmt.setString(1, usuario)
                    stmt.executeUpdate() > 0
                }

                it.commit()
                return deleted
            } catch (ex: Exception) {
                it.rollback()
                throw ex
            } finally {
                it.autoCommit = true
            }
        }
    }

    private fun createFollowEmpresaTable() {
        // cria a tabela de “seguidores” de empresas
        val sql = """
        CREATE TABLE IF NOT EXISTS PessoaSegueEmpresa (
            usuario        VARCHAR(255) NOT NULL,
            empresaUsuario VARCHAR(255) NOT NULL,
            criado_em      TIMESTAMP    DEFAULT now(),
            PRIMARY KEY (usuario, empresaUsuario),
            FOREIGN KEY (usuario)        REFERENCES Pessoa(usuario)  ON DELETE CASCADE,
            FOREIGN KEY (empresaUsuario) REFERENCES Empresa(usuario) ON DELETE CASCADE
        );
        """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { it.execute() }
        }
    }

    fun followEmpresa(usuario: String, empresaUsuario: String): Boolean {
        val sql = "INSERT INTO PessoaSegueEmpresa (usuario, empresaUsuario) VALUES (?, ?)"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                stmt.setString(2, empresaUsuario)
                return try {
                    stmt.executeUpdate() > 0
                } catch (e: java.sql.SQLException) {
                    // 23505 = duplicate key
                    if (e.sqlState == "23505") false else throw e
                }
            }
        }
    }

    fun unfollowEmpresa(usuario: String, empresaUsuario: String): Boolean {
        val sql = "DELETE FROM PessoaSegueEmpresa WHERE usuario = ? AND empresaUsuario = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                stmt.setString(2, empresaUsuario)
                return stmt.executeUpdate() > 0
            }
        }
    }

    fun isFollowingEmpresa(usuario: String, empresaUsuario: String): Boolean {
        val sql = """
          SELECT 1
            FROM PessoaSegueEmpresa
           WHERE usuario = ? AND empresaUsuario = ?
           LIMIT 1
        """.trimIndent()
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                stmt.setString(2, empresaUsuario)
                return stmt.executeQuery().next()
            }
        }
    }

    fun getEmpresasSeguidas(usuario: String): List<String> {
        val empresas = mutableListOf<String>()
        val sql = "SELECT empresaUsuario FROM PessoaSegueEmpresa WHERE usuario = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    empresas.add(rs.getString("empresaUsuario"))
                }
            }
        }
        return empresas
    }

    fun incrementaEmpresas(usuario: String): Boolean {
        val sql = "UPDATE Pessoa SET qtEmpresas = qtEmpresas + 1 WHERE usuario = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                return stmt.executeUpdate() > 0
            }
        }
    }

    fun decrementaEmpresas(usuario: String): Boolean {
        val sql = """
    UPDATE Pessoa
      SET qtEmpresas = GREATEST(qtEmpresas - 1, 0)
    WHERE usuario = ?
  """.trimIndent()
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                return stmt.executeUpdate() > 0
            }
        }
    }

    fun getEmpresasSeguidasFull(usuario: String): List<Empresa> {
        val empresas = mutableListOf<Empresa>()
        val sql = """
        SELECT e.usuario, e.senha, e.name, e.description, e.imageURL, e.email, e.cnpj
          FROM PessoaSegueEmpresa pse
          JOIN Empresa e ON pse.empresaUsuario = e.usuario
         WHERE pse.usuario = ?
    """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, usuario)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    empresas += Empresa(
                        usuario     = rs.getString("usuario"),
                        senha       = rs.getString("senha"),
                        name        = rs.getString("name"),
                        description = rs.getString("description"),
                        imageUrl    = rs.getString("imageURL"),
                        email       = rs.getString("email"),
                        cnpj        = rs.getString("cnpj")
                    )
                }
            }
        }
        return empresas
    }

}