package br.com

import br.com.database.*
import br.com.model.*
import database.NotificacaoDAO
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import model.Notificacao
import java.io.File

fun Application.configureRouting() {
    DatabaseFactory.init()
    val empresaDao = EmpresaDAO()
    val pessoaDao = PessoaDAO()
    val comunidadeDao = ComunidadeDAO()
    val passeioComunidadeDAO = PasseioComunidadeDAO()
    val comentarioPasseioDAO = ComentarioPasseioDAO()
    val chatPasseioDAO = ChatPasseioDAO()
    val comentarioChatDAO = ComentarioChatDAO()
    val notificacaoDao = NotificacaoDAO()
    val eventoDao = EventoDAO()
    val passeioEventoDao = PasseioEventoDAO()
    val comentarioPasseioEventoDAO = ComentarioPasseioEventoDAO()



    routing {
        static("/uploads") {
            files("uploads")
        }

        post("/login") {
            // Extrai parâmetros form-urlencoded: pode ser "@usuario" ou CNPJ
            val params  = call.receiveParameters()
            val loginId = params["usuario"]  // aqui cabe tanto "@user" (Pessoa) quanto "CNPJ" (Empresa)
            val senha   = params["senha"]

            if (loginId.isNullOrBlank() || senha.isNullOrBlank()) {
                call.respondText(
                    "Campos obrigatórios ausentes",
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }

            // 1) Tenta autenticar como Pessoa
            val pessoa = pessoaDao.getPessoaByUsuarioESenha(loginId, senha)
            if (pessoa != null) {
                call.respond(pessoa)
                return@post
            }

            // 2) Se não encontrou Pessoa, tenta autenticar como Empresa pelo CNPJ
            val empresa = empresaDao.getEmpresaByCnpjESenha(loginId, senha)
            if (empresa != null) {
                call.respond(empresa)
                return@post
            }

            // 3) Se nenhum dos dois existiu, retorna Unauthorized
            call.respond(
                HttpStatusCode.Unauthorized,
                "Usuário ou senha inválidos"
            )
        }


        get("/pessoas") {
            val pessoas = pessoaDao.getAllPessoas()
            call.respond(pessoas)
        }

        get("/pessoas/{usuario}") {
            val usuario = call.parameters["usuario"]
            if (usuario == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")
                return@get
            }
            val pessoa = pessoaDao.getPessoaByusuario(usuario)
            if (pessoa == null) {
                call.respond(HttpStatusCode.NotFound, "Pessoa não encontrada")
            } else {
                call.respond(pessoa)
            }
        }

        put("/pessoas/{usuario}") {
            val usuarioPath = call.parameters["usuario"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")

            var name: String? = null
            var description: String? = null
            var municipio: String? = null
            var email: String? = null
            var nascimento: String? = null
            var imageUrl: String? = null
            var existingImageUrl: String? = null

            // Recebe multipart/form-data com possíveis campos: name, description, municipio, email, nascimento, existingImageUrl e file image
            call.receiveMultipart().forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> when (part.name) {
                        "name"               -> name = part.value
                        "description"        -> description = part.value
                        "municipio"          -> municipio = part.value
                        "email"              -> email = part.value
                        "nascimento"         -> nascimento = part.value
                        "existingImageUrl"   -> existingImageUrl = part.value
                    }
                    is PartData.FileItem -> {
                        val uploadsDir = File("uploads")
                        if (!uploadsDir.exists()) uploadsDir.mkdirs()
                        val fileName = part.originalFileName ?: "img_${System.currentTimeMillis()}.jpg"
                        val file = File(uploadsDir, fileName)
                        file.writeBytes(part.streamProvider().readBytes())
                        // Ajuste para o seu host
                        imageUrl = "http://192.168.0.89:8080/uploads/$fileName"
                    }
                    else -> { /* ignora outros */ }
                }
                part.dispose()
            }

            // validação básica
            if (name == null || description == null || municipio == null || email == null || nascimento == null) {
                return@put call.respond(HttpStatusCode.BadRequest, "Campos obrigatórios faltando")
            }

            // monta o objeto Pessoa completo
            val pessoa = Pessoa(
                usuario    = usuarioPath,
                name       = name!!,
                description= description!!,
                imageUrl   = imageUrl ?: existingImageUrl,
                qtAmigos   = 0,        // não alterado aqui
                qtPasseios = 0,
                municipio  = municipio!!,
                senha      = "",       // não alteramos senha via este endpoint
                email      = email!!,
                nascimento = nascimento!!,
                qtEmpresas   = 0
            )

            val updated = pessoaDao.updatePessoa(pessoa)
            if (updated) {
                call.respond(HttpStatusCode.OK, "Perfil atualizado com sucesso")
            } else {
                call.respond(HttpStatusCode.NotFound, "Usuário não encontrado")
            }
        }



        post("/pessoas") {
            val multipart = call.receiveMultipart()
            var usuario: String? = null
            var name: String? = null
            var description: String? = null
            var qtAmigos: Int? = null
            var qtPasseios: Int? = null
            var municipio: String? = null
            var imageUrl: String? = null
            var senha: String? = null
            var email: String? = null
            var nascimento: String? = null
            var qtEmpresas: Int? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "usuario"    -> usuario    = part.value
                            "name"       -> name       = part.value
                            "description"-> description= part.value
                            "qtAmigos"   -> qtAmigos   = part.value.toIntOrNull()
                            "qtPasseios" -> qtPasseios = part.value.toIntOrNull()
                            "municipio"  -> municipio  = part.value
                            "email"      -> email      = part.value
                            "nascimento" -> nascimento = part.value
                            "senha"      -> senha      = part.value
                            "qtEmpresas" -> qtEmpresas = part.value.toIntOrNull()
                        }
                    }
                    is PartData.FileItem -> {
                        // Define o diretório onde o arquivo (imagem) será salvo
                        val uploadsDir = File("uploads")
                        if (!uploadsDir.exists()) uploadsDir.mkdirs()

                        // Usa o nome original ou gera um nome único
                        val fileName = part.originalFileName ?: "image_${System.currentTimeMillis()}.jpg"
                        val file = File(uploadsDir, fileName)
                        file.writeBytes(part.streamProvider().readBytes())

                        // URL para acessar a imagem (ajuste o IP e porta conforme seu ambiente)
                        imageUrl = "http://192.168.0.89:8080/uploads/$fileName"
                    }
                    else -> { /* Ignora outros tipos de PartData */ }
                }
                part.dispose()
            }

            // Verifica se os campos obrigatórios foram preenchidos
            if (usuario == null || name == null || description == null || qtAmigos == null || qtPasseios == null || municipio == null || qtEmpresas == null) {
                call.respondText("Campos obrigatórios ausentes", status = HttpStatusCode.BadRequest)
                return@post
            }

            // Criação de variáveis imutáveis com asserção explícita
            val safeusuario: String = usuario!!
            val safeName: String = name!!
            val safeDescription: String = description!!
            val safeQtAmigos: Int = qtAmigos!!
            val safeQtPasseios: Int = qtPasseios!!
            val safeMunicipio: String = municipio!!
            val safeSenha: String = senha!!
            val safeEmail: String = email!!
            val safeNascimento: String = nascimento!!
            val safeQtEmpresas: Int = qtEmpresas!!

            val pessoa = Pessoa(safeusuario, safeName, safeDescription, imageUrl, safeQtAmigos, safeQtPasseios, safeMunicipio, safeSenha, safeEmail, safeNascimento, safeQtEmpresas)
            val inserted = pessoaDao.insertPessoa(pessoa)
            if (inserted) {
                call.respondText("Pessoa criada com sucesso", status = HttpStatusCode.Created)
            } else {
                call.respondText("Erro ao criar Pessoa", status = HttpStatusCode.InternalServerError)
            }
        }

        delete("/pessoas/{usuario}") {
            val usuario = call.parameters["usuario"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")

            // tenta apagar pelo DAO
            val removed = pessoaDao.deletePessoa(usuario)
            if (removed) {
                call.respond(HttpStatusCode.OK, "Pessoa '$usuario' removida com sucesso")
            } else {
                call.respond(HttpStatusCode.NotFound, "Usuário '$usuario' não encontrado")
            }
        }


        put("/pessoas/{usuario}/municipio") {
            val usuario = call.parameters["usuario"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")
            val params = call.receiveParameters()
            val municipio = params["municipio"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Campo 'municipio' ausente")

            // Verifica se o usuário existe
            val pessoa = pessoaDao.getPessoaByusuario(usuario)
            if (pessoa == null) {
                call.respond(HttpStatusCode.NotFound, "Pessoa não encontrada")
                return@put
            }

            // Atualiza o município
            val updated = pessoaDao.updateMunicipio(usuario, municipio)
            if (updated) {
                // Retorna o objeto atualizado
                val pessoaAtualizada = pessoaDao.getPessoaByusuario(usuario)!!
                call.respond(pessoaAtualizada)
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Falha ao atualizar município")
            }
        }


        post("/pessoas/{usuario}/amigos") {
            val remetente = call.parameters["usuario"]!!
            val body = call.receive<Map<String, String>>()
            val amigo   = body["amigo"]   ?: return@post call.respond(HttpStatusCode.BadRequest)
            val horario = body["horario"] ?: return@post call.respond(HttpStatusCode.BadRequest)

            val notif = Notificacao(
                usuarioDestinado = amigo,
                usuarioRemetente = remetente,
                descricao        = "$remetente quer ser seu amigo.",
                horario          = horario,
                lido             = false
            )
            val inserted = notificacaoDao.insert(notif)
            if (inserted) {
                call.respond(HttpStatusCode.Created)
            } else {
                // já existe notificação ou erro de PK
                call.respond(HttpStatusCode.Conflict, "Notificação já existe")
            }
        }
        get("/pessoas/{usuario}/amigos/{amigo}") {
            val usuario = call.parameters["usuario"]!!
            val amigo   = call.parameters["amigo"]!!
            // Retorna 200 + true/false em JSON
            val existe = pessoaDao.isFriend(usuario, amigo)
            call.respond(mapOf("amigo" to existe))
        }

        get("/pessoas/{usuario}/amigos") {
            val raw = call.parameters["usuario"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Usuario ausente")
            // adiciona o @ de volta antes de buscar
            val usuario = if (raw.startsWith("@")) raw else "@$raw"
            val lista = pessoaDao.getFriendPessoas(usuario)
            call.respond(lista)
        }

        delete("/pessoas/{usuario}/amigos/{amigo}") {
            val usuario  = call.parameters["usuario"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")
            val amigo    = call.parameters["amigo"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Parâmetro 'amigo' ausente")

            // 1) Verifica se são amigos (pelo menos em um dos sentidos)
            if (!pessoaDao.isFriend(usuario, amigo) && !pessoaDao.isFriend(amigo, usuario)) {
                return@delete call.respond(HttpStatusCode.NotFound, "Amizade não encontrada")
            }

            // 2) Remove as duas entradas (A→B e B→A), mas NÃO decrementa aqui
            val removed1 = pessoaDao.removeFriend(usuario, amigo)
            val removed2 = pessoaDao.removeFriend(amigo, usuario)

            if (removed1 || removed2) {
                // 3) Agora sim, decrementa qtAmigos **UMA VEZ** para cada usuário que teve a amizade removida.
                //    Só devemos decrementar se a linha correspondente existia antes.
                if (removed1) { // se existia (usuario, amigo)
                    pessoaDao.decrementaAmigos(usuario)
                }
                if (removed2) { // se existia (amigo, usuario)
                    pessoaDao.decrementaAmigos(amigo)
                }

                // 4) Apaga notificações recíprocas, se for o caso (opcional)
                notificacaoDao.deleteNotificacao(usuarioDestinado = amigo, usuarioRemetente = usuario)
                notificacaoDao.deleteNotificacao(usuarioDestinado = usuario, usuarioRemetente = amigo)

                call.respond(HttpStatusCode.OK, "Amizade desfeita com sucesso")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Erro ao desfazer amizade")
            }
        }


        post("/notificacoes/{usuarioDestinado}/{usuarioRemetente}/responder") {
            val usuarioDest = call.parameters["usuarioDestinado"]!!
            val usuarioRem  = call.parameters["usuarioRemetente"]!!
            val action      = call.receive<Map<String, String>>()["action"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Campo 'action' ausente")

            // se ACCEPT, cria vínculo de amizade
            if (action == "ACCEPT") {
                pessoaDao.addFriend(usuarioRem, usuarioDest)
                pessoaDao.addFriend(usuarioDest, usuarioRem)
                pessoaDao.incrementaAmigos(usuarioRem)
                pessoaDao.incrementaAmigos(usuarioDest)
            }
            // marca como lida
            val updated = notificacaoDao.markRead(usuarioDest, usuarioRem)
            if (updated) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound, "Notificação não encontrada")
            }
        }

            // 2) Listar todas as notificações
        get("/notificacoes/usuario/{usuario}") {
            val usuario = call.parameters["usuario"]!!
            val list = notificacaoDao.findByUsuarioDestinado(usuario)
            if (list.isEmpty()) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(list)
            }
        }

        delete("/notificacoes/{usuarioDestinado}/{usuarioRemetente}") {
            val uDest = call.parameters["usuarioDestinado"]!!
            val uRem  = call.parameters["usuarioRemetente"]!!
            val removed = notificacaoDao.deleteNotificacao(uDest, uRem)
            if (removed) {
                call.respond(HttpStatusCode.OK, "Notificação removida")
            } else {
                call.respond(HttpStatusCode.NotFound, "Notificação não encontrada")
            }
        }





        get("/comunidades") {
            val comunidades = comunidadeDao.getAllComunidades()
            call.respond(comunidades)
        }

        get("/comunidades/{id}") {
            val idParam = call.parameters["id"]
            val id = idParam?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'id' ausente ou inválido")
                return@get
            }
            val comunidade = comunidadeDao.getComunidadeById(id)
            if (comunidade == null) {
                call.respond(HttpStatusCode.NotFound, "Comunidade não encontrada")
            } else {
                call.respond(comunidade)
            }
        }

        post("/comunidades") {
            val multipart = call.receiveMultipart()
            var name: String? = null
            var administratorUser: String? = null
            var descricao: String? = null
            var regras: String? = null
            var municipio: String? = null
            var imageUrl: String? = null
            val tags = arrayListOf<String>()

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "name" -> name = part.value
                            "administratorUser" -> administratorUser = part.value
                            "descricao" -> descricao = part.value
                            "regras" -> regras = part.value // Se regras não for enviado, defina um padrão abaixo
                            "municipio" -> municipio = part.value
                            "tags" -> {
                                // Aceita tags separadas por vírgula, ex.: "tag1, tag2, tag3"
                                val tagValues = part.value.split(",").map { it.trim() }
                                tags.addAll(tagValues.filter { it.isNotEmpty() })
                            }
                        }
                    }
                    is PartData.FileItem -> {
                        val uploadsDir = File("uploads")
                        if (!uploadsDir.exists()) uploadsDir.mkdirs()

                        val fileName = part.originalFileName ?: "image_${System.currentTimeMillis()}.jpg"
                        val file = File(uploadsDir, fileName)
                        file.writeBytes(part.streamProvider().readBytes())
                        imageUrl = "http://192.168.0.89:8080/uploads/$fileName"
                    }
                    else -> { /* Ignora outros tipos de PartData */ }
                }
                part.dispose()
            }

            // Se "regras" não for enviado, pode ser definido um valor padrão (ex.: string vazia)
            if (regras == null) regras = ""

            // Verifica se os campos obrigatórios foram preenchidos (não exigindo "administrator")
            if (name == null || administratorUser == null || descricao == null || municipio == null) {
                call.respondText("Campos obrigatórios ausentes", status = HttpStatusCode.BadRequest)
                return@post
            }

            val comunidade = Comunidade(
                idComunidade = 0,
                imageUrl = imageUrl,
                name = name!!,
                administratorUser = administratorUser!!,
                administrator = "", // O campo "administrator" será obtido via join, portanto pode ser deixado vazio
                descricao = descricao!!,
                regras = regras!!,
                municipio = municipio!!,
                tags = tags
            )

            val inserted = comunidadeDao.insertComunidade(comunidade)
            if (inserted) {
                call.respondText("Comunidade criada com sucesso", status = HttpStatusCode.Created)
            } else {
                call.respondText("Erro ao criar Comunidade", status = HttpStatusCode.InternalServerError)
            }
        }

        post("/comunidades/favoritar") {
            val multipart = call.receiveMultipart()
            var usuario: String? = null
            var idComunidade: Int? = null

            multipart.forEachPart { part ->
                if (part is PartData.FormItem) {
                    when (part.name) {
                        "usuario" -> usuario = part.value
                        "idComunidade" -> idComunidade = part.value.toIntOrNull()
                    }
                }
                part.dispose()
            }

            if (usuario.isNullOrBlank() || idComunidade == null) {
                call.respond(HttpStatusCode.BadRequest, "Dados inválidos")
                return@post
            }

            val dao = ComunidadeDAO()
            val user = usuario!!
            val comId = idComunidade!!

            val already = dao.isFavorita(user, comId)
            val success = if (already) {
                dao.unfavoriteComunidade(user, comId)
            } else {
                dao.favoriteComunidade(user, comId)
            }

            if (success) {
                val msg = if (already)
                    "Comunidade desfavoritada com sucesso"
                else
                    "Comunidade favoritada com sucesso"
                call.respond(HttpStatusCode.OK, msg)
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Falha ao atualizar favorito")
            }
        }

        get("/comunidades/usuario/{usuario}") {
            val usuarioParam = call.parameters["usuario"]
            if (usuarioParam == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")
                return@get
            }
            val comunidades = comunidadeDao.getComunidadesByUsuario(usuarioParam)
            if (comunidades.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "Nenhuma comunidade encontrada para o usuário: $usuarioParam")
            } else {
                call.respond(comunidades)
            }
        }
        get("/comunidades/favoritas/{usuario}") {
            val usuarioParam = call.parameters["usuario"]
            if (usuarioParam == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")
                return@get
            }
            val comunidades = comunidadeDao.getComunidadeFavoritaByUsuario(usuarioParam)
            if (comunidades.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "Nenhuma comunidade favorita encontrada para o usuário: $usuarioParam")
            } else {
                call.respond(comunidades)
            }
        }

        delete("/comunidades/{id}") {
            val idParam = call.parameters["id"]
            val id = idParam?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'id' ausente ou inválido")
                return@delete
            }
            val deleted = comunidadeDao.deleteComunidade(id)
            if (deleted) {
                call.respond(HttpStatusCode.OK, "Comunidade excluída com sucesso")
            } else {
                call.respond(HttpStatusCode.NotFound, "Comunidade não encontrada")
            }
        }

        put("/comunidades/{id}") {
            val idParam = call.parameters["id"]
            val id = idParam?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'id' ausente ou inválido")
                return@put
            }
            var name: String? = null
            var administratorUser: String? = null
            var descricao: String? = null
            var regras: String? = null
            var municipio: String? = null
            var imageUrl: String? = null
            var existingImageUrl: String? = null
            val tags = arrayListOf<String>()

            call.receiveMultipart().forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "name" -> name = part.value
                            "administratorUser" -> administratorUser = part.value
                            "descricao" -> descricao = part.value
                            "regras" -> regras = part.value
                            "municipio" -> municipio = part.value
                            "tags" -> {
                                val tagValues = part.value.split(",").map { it.trim() }
                                tags.addAll(tagValues.filter { it.isNotEmpty() })
                            }
                            "existingImageUrl" -> existingImageUrl = part.value
                        }
                    }
                    is PartData.FileItem -> {
                        val uploadsDir = File("uploads")
                        if (!uploadsDir.exists()) uploadsDir.mkdirs()
                        val fileName = part.originalFileName ?: "image_${System.currentTimeMillis()}.jpg"
                        val file = File(uploadsDir, fileName)
                        file.writeBytes(part.streamProvider().readBytes())
                        imageUrl = "http://192.168.0.89:8080/uploads/$fileName"
                    }
                    else -> { }
                }
                part.dispose()
            }

            if (name == null || administratorUser == null || descricao == null || municipio == null) {
                call.respond(HttpStatusCode.BadRequest, "Campos obrigatórios ausentes")
                return@put
            }
            if (regras == null) regras = ""

            // Se nenhuma nova imagem foi enviada, use o existingImageUrl (se estiver presente)
            val finalImageUrl = imageUrl ?: existingImageUrl

            val comunidade = Comunidade(
                idComunidade = id,
                imageUrl = finalImageUrl,
                name = name!!,
                administratorUser = administratorUser!!,
                administrator = "", // Esse campo será preenchido via join, se necessário
                descricao = descricao!!,
                regras = regras!!,
                municipio = municipio!!,
                tags = tags
            )

            val updated = comunidadeDao.updateComunidade(comunidade)
            if (updated) {
                call.respond(HttpStatusCode.OK, "Comunidade atualizada com sucesso")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Erro ao atualizar a comunidade")
            }
        }





        get("/passeios") {
            val passeios = passeioComunidadeDAO.getAllPasseios()
            call.respond(passeios)
        }

        get("/passeios/{idComunidade}") {
            val idParam = call.parameters["idComunidade"]
            val id = idParam?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'idComunidade' ausente ou inválido")
                return@get
            }
            val passeios = passeioComunidadeDAO.getPasseiosByComunidade(id)
            if (passeios.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "Passeios não encontrados")
            } else {
                call.respond(passeios)
            }
        }
        post("/passeios") {
            val multipart = call.receiveMultipart()
            var idComunidade: Int? = null
            var usuario: String? = null
            var horario: String? = null
            var descricaoPasseio: String? = null
            var localizacao: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "idComunidade" -> idComunidade = part.value.toIntOrNull()
                            "usuario" -> usuario = part.value
                            "horario" -> horario = part.value
                            "descricaoPasseio" -> descricaoPasseio = part.value
                            "localizacao" -> localizacao = part.value
                        }
                    }
                    else -> { /* Ignora outros tipos de PartData */ }
                }
                part.dispose()
            }

            // Verifica se os campos obrigatórios foram preenchidos
            if (idComunidade == null || usuario == null || horario == null ||
                descricaoPasseio == null || localizacao == null) {
                call.respond(HttpStatusCode.BadRequest, "Campos obrigatórios ausentes")
                return@post
            }

            // Cria variáveis imutáveis com tipos não-nulos utilizando '!!'
            val safeIdComunidade: Int = idComunidade!!
            val safeUsuario: String = usuario!!
            val safeHorario: String = horario!!
            val safeDescricaoPasseio: String = descricaoPasseio!!
            val safeLocalizacao: String = localizacao!!

            // Busca a Pessoa pelo 'usuario'
            val pessoa = pessoaDao.getPessoaByusuario(safeUsuario)
            if (pessoa == null) {
                call.respond(HttpStatusCode.NotFound, "Pessoa não encontrada")
                return@post
            }

            // Busca a Comunidade pelo id
            val comunidade = comunidadeDao.getComunidadeById(safeIdComunidade)
            if (comunidade == null) {
                call.respond(HttpStatusCode.NotFound, "Comunidade não encontrada")
                return@post
            }

            // Cria o objeto PasseioComunidade (imagem e nome serão obtidos via JOIN com Pessoa)
            val passeio = PasseioComunidade(
                idPasseioComunidade = 0,
                usuario = pessoa.usuario,// Será gerado pelo banco
                nomePessoa = pessoa.name,
                imagem = pessoa.imageUrl,
                horario = safeHorario,
                descricaoPasseio = safeDescricaoPasseio,
                localizacao = safeLocalizacao
            )

            val inserted = passeioComunidadeDAO.insertPasseioComunidade(passeio, pessoa, comunidade)
            if (inserted) {
                val atualizado = pessoaDao.incrementaPasseios(pessoa.usuario)
                call.respondText("Passeio criado com sucesso", status = HttpStatusCode.Created)
            } else {
                call.respondText("Erro ao criar Passeio", status = HttpStatusCode.InternalServerError)
            }
        }

        get("/passeios/{idPasseioComunidade}/comentarios") {
            val idPasseioComunidade = call.parameters["idPasseioComunidade"]?.toIntOrNull()
            if (idPasseioComunidade == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'idPasseioComunidade' ausente ou inválido")
                return@get
            }
            val comentarios = comentarioPasseioDAO.getComentariosByPasseio(idPasseioComunidade)
            call.respond(comentarios)
        }

        delete("/passeios/{idPasseioComunidade}") {
            val idPasseio = call.parameters["idPasseioComunidade"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Parâmetro ausente ou inválido")

            // 1) Busca quem é o dono antes de apagar
            val usuarioDono = passeioComunidadeDAO.getUsuarioByPasseio(idPasseio)
                ?: return@delete call.respond(HttpStatusCode.NotFound, "Passeio não encontrado")

            // 2) Executa todo o delete (comentários, chats e o próprio passeio)
            val deleted = passeioComunidadeDAO.deletePasseioComunidade(idPasseio)
            if (!deleted) {
                return@delete call.respond(HttpStatusCode.NotFound, "Passeio não encontrado")
            }

            // 3) Decrementa o contador de passeios daquele usuário
            passeioComunidadeDAO.decrementaPasseios(usuarioDono)

            // 4) Responde sucesso
            call.respond(HttpStatusCode.OK, "Passeio excluído com sucesso")
        }

        get("/passeios/usuario/{usuario}") {
            println(">>> CHEGOU NO BACK: ${call.request.httpMethod.value} ${call.request.uri}")
            val usuarioParam = call.parameters["usuario"]
            if (usuarioParam == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")
                return@get
            }
            val passeios = passeioComunidadeDAO.getPasseiosByUsuario(usuarioParam)
            if (passeios.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "Nenhum passeio encontrado para o usuário: $usuarioParam")
            } else {
                call.respond(passeios)
            }
        }



        post("/comentarios") {
            val multipart = call.receiveMultipart()
            var idPasseioComunidade: Int? = null
            var idComunidade: Int? = null
            var usuario: String? = null
            var horario: String? = null
            var descricaoComentario: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "idPasseioComunidade" -> idPasseioComunidade = part.value.toIntOrNull()
                            "idComunidade" -> idComunidade = part.value.toIntOrNull()
                            "usuario" -> usuario = part.value
                            "horario" -> horario = part.value
                            "descricaoComentario" -> descricaoComentario = part.value
                        }
                    }
                    else -> { /* Ignora outros tipos de PartData */ }
                }
                part.dispose()
            }

            // Cria cópias imutáveis para evitar problemas de smart cast
            val safeIdPasseioComunidade = idPasseioComunidade
            val safeIdComunidade = idComunidade
            val safeUsuario = usuario
            val safeHorario = horario
            val safeDescricaoComentario = descricaoComentario

            if (safeIdPasseioComunidade == null || safeIdComunidade == null || safeUsuario == null ||
                safeHorario == null || safeDescricaoComentario == null) {
                call.respond(HttpStatusCode.BadRequest, "Campos obrigatórios ausentes")
                return@post
            }

            // Cria o objeto ComentarioPasseio; imagem e nomePessoa serão obtidos via JOIN com Pessoa
            val comentario = ComentarioPasseio(
                idComentarioPasseio = 0,
                horario = safeHorario,
                descricaoComentario = safeDescricaoComentario,
                imagem = "",
                nomePessoa = "",
                usuario = safeUsuario
            )

            val inserted = comentarioPasseioDAO.insertComentario(comentario, safeIdPasseioComunidade, safeIdComunidade, safeUsuario)
            if (inserted) {
                call.respondText("Comentário inserido com sucesso", status = HttpStatusCode.Created)
            } else {
                call.respondText("Erro ao inserir comentário", status = HttpStatusCode.InternalServerError)
            }
        }
        // Endpoints relacionados ao Chat

// Retorna todos os chats
        get("/chat") {
            val chats = chatPasseioDAO.getAllChatPasseios()
            call.respond(chats)
        }

// Recupera os comentários de um chat específico (usando somente o idChat)
        get("/chat/{idChat}/comentarios") {
            val idChatParam = call.parameters["idChat"]
            val safeIdChat = idChatParam?.toIntOrNull()
            if (safeIdChat == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'idChat' ausente ou inválido")
                return@get
            }
            val comentarios = comentarioChatDAO.getComentariosByChat(safeIdChat)
            call.respond(comentarios)
        }

        delete("/comentarios/{idComentarioPasseio}") {
            val idParam = call.parameters["idComentarioPasseio"]
            val idComentarioPasseio = idParam?.toIntOrNull()
            if (idComentarioPasseio == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'idComentarioPasseio' ausente ou inválido")
                return@delete
            }
            val deleted = comentarioPasseioDAO.deleteComentarioPasseio(idComentarioPasseio)
            if (deleted) {
                call.respond(HttpStatusCode.OK, "Comentário excluído com sucesso")
            } else {
                call.respond(HttpStatusCode.NotFound, "Comentário não encontrado")
            }
        }


// Cria um chat (usa o idPasseio para associação, mas retorna o idChat criado)
        post("/chat") {
            val multipart = call.receiveMultipart()
            var idPasseio: Int? = null
            var tipoPasseio: String? = null
            var usuarioAdministrador: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "idPasseio" -> idPasseio = part.value.toIntOrNull()
                            "tipo" -> tipoPasseio = part.value
                            "usuarioAdministrador" -> usuarioAdministrador = part.value
                        }
                    }
                    else -> { }
                }
                part.dispose()
            }

            val safeIdPasseio = idPasseio ?: return@post call.respond(
                HttpStatusCode.BadRequest, "Campo 'idPasseio' ausente ou inválido"
            )
            val safeTipoPasseio = tipoPasseio?.takeIf { it == "EVENTO" || it == "COMUNIDADE" }
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    "Campo 'tipo' ausente ou inválido (deve ser 'EVENTO' ou 'COMUNIDADE')"
                )
            val safeUsuarioAdministrador = usuarioAdministrador
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Campo 'usuarioAdministrador' ausente")

            // Insere no banco e recebe o idChat
            val idChat = chatPasseioDAO.insertChatPasseio(safeIdPasseio, safeTipoPasseio, safeUsuarioAdministrador)
            if (idChat != null) {
                // RETORNA APENAS O STATUS 201, sem corpo de texto
                call.respond(HttpStatusCode.Created)
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }


// Adiciona um usuário a um chat existente usando somente o idChat
        post("/chat/addPessoa") {
            val multipart = call.receiveMultipart()
            var usuario: String? = null
            var idChat: Int? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "usuario" -> usuario = part.value
                            "idChat" -> idChat = part.value.toIntOrNull()
                        }
                    }
                    else -> { }
                }
                part.dispose()
            }

            val safeUsuario = usuario ?: return@post call.respond(
                HttpStatusCode.BadRequest, "Campo 'usuario' ausente"
            )
            val safeIdChat = idChat ?: return@post call.respond(
                HttpStatusCode.BadRequest, "Campo 'idChat' ausente ou inválido"
            )

            // A DAO espera (usuario: String, idChat: Int)
            val added = chatPasseioDAO.addPessoaToChat(safeUsuario, safeIdChat)
            if (added) {
                call.respondText("Usuário adicionado ao chat com sucesso", status = HttpStatusCode.Created)
            } else {
                call.respondText("Erro ao adicionar usuário ao chat", status = HttpStatusCode.InternalServerError)
            }
        }

// Cria um chat e adiciona vários usuários
        post("/chat/criarComUsuarios") {
            val multipart = call.receiveMultipart()
            var idPasseio: Int? = null
            var tipoPasseio: String? = null
            var usuarioAdministrador: String? = null
            var usuariosStr: String? = null // Lista de usuários separados por vírgula

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "idPasseio" -> idPasseio = part.value.toIntOrNull()
                            "tipo" -> tipoPasseio = part.value
                            "usuarioAdministrador" -> usuarioAdministrador = part.value
                            "usuarios" -> usuariosStr = part.value  // Ex: "user1, user2, user3"
                        }
                    }
                    else -> { }
                }
                part.dispose()
            }

            val safeIdPasseio = idPasseio ?: return@post call.respond(
                HttpStatusCode.BadRequest, "Campo 'idPasseio' ausente ou inválido"
            )
            val safeTipoPasseio = tipoPasseio?.takeIf { it == "EVENTO" || it == "COMUNIDADE" }
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Campo 'tipo' ausente ou inválido")
            val safeUsuarioAdministrador = usuarioAdministrador ?: return@post call.respond(
                HttpStatusCode.BadRequest, "Campo 'usuarioAdministrador' ausente"
            )
            val safeUsuariosStr = usuariosStr ?: return@post call.respond(
                HttpStatusCode.BadRequest, "Campo 'usuarios' ausente"
            )

            if (chatPasseioDAO.existsChatForPasseio(safeIdPasseio,  safeTipoPasseio)) {
                return@post call.respond(HttpStatusCode.Conflict, "Já existe um chat para este passeio")
            }

            // Cria o chat e obtém o idChat gerado
            val idChat = chatPasseioDAO.insertChatPasseio(safeIdPasseio, safeTipoPasseio, safeUsuarioAdministrador)
            if (idChat == null) {
                call.respond(HttpStatusCode.InternalServerError, "Erro ao criar Chat")
                return@post
            }

            // Converte a string de usuários em uma lista
            val listaUsuarios = safeUsuariosStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            // Insere cada usuário na tabela usando somente o idChat
            var erroInsercao = false
            listaUsuarios.forEach { user ->
                if (!chatPasseioDAO.addPessoaToChat(user, idChat)) {
                    erroInsercao = true
                }
            }

            if (erroInsercao) {
                call.respond(HttpStatusCode.InternalServerError, "Chat criado, mas houve erro ao inserir alguns usuários")
            } else {
                call.respondText("Chat criado com sucesso, idChat: $idChat", status = HttpStatusCode.Created)
            }
        }

// Insere um comentário no chat (usando somente o idChat)
        // Insere um comentário no chat (usando somente o idChat)



// Retorna os chats vinculados a um usuário
        get("/chat/byUsuario/{usuario}") {
            println(">>> CHEGOU NO BACK: ${call.request.httpMethod.value} ${call.request.uri}")
            val usuarioParam = call.parameters["usuario"]
            val safeUsuario = usuarioParam ?: return@get call.respond(
                HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente ou inválido"
            )
            val chats = chatPasseioDAO.getChatByUsuario(safeUsuario)
            if (chats.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "Nenhum chat encontrado para o usuário $safeUsuario")
            } else {
                call.respond(chats)
            }
        }

        get("/chat/passeio/{idPasseio}/{tipo}") {
            val idPasseioParam = call.parameters["idPasseio"]?.toIntOrNull()
            val tipoParam = call.parameters["tipo"]
            if (idPasseioParam == null || tipoParam.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'idPasseio' ou 'tipo' ausente ou inválido")
                return@get
            }
            if (tipoParam != "EVENTO" && tipoParam != "COMUNIDADE") {
                call.respond(HttpStatusCode.BadRequest, "Tipo deve ser 'EVENTO' ou 'COMUNIDADE'")
                return@get
            }
            val chats = chatPasseioDAO.getChatsByPasseio(idPasseioParam, tipoParam)
            if (chats.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "Nenhum chat encontrado para o passeio $idPasseioParam do tipo $tipoParam")
            } else {
                call.respond(chats)
            }
        }

        get("/chat/{idChat}/membros") {
            val idChat = call.parameters["idChat"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Parâmetro 'idChat' ausente ou inválido")

            // Busca o chat completo (inclui lista de usuários)
            val chat = chatPasseioDAO.getChatById(idChat)
            if (chat == null) {
                return@get call.respond(HttpStatusCode.NotFound, "Chat não encontrado")
            }

            // Retorna somente a lista de membros
            call.respond(chat.membros)
        }

        post("/chat/{idChat}/addPessoa") {
            val idChat = call.parameters["idChat"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Parâmetro 'idChat' ausente ou inválido")

            // Para simplificar, receba application/x-www-form-urlencoded: campo "usuario"
            val form = call.receiveParameters()
            val usuario = form["usuario"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Campo 'usuario' ausente")

            val added = chatPasseioDAO.addPessoaToChat(usuario, idChat)
            if (added) {
                call.respond(HttpStatusCode.Created, "Usuário adicionado ao chat com sucesso")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Falha ao adicionar usuário ao chat")
            }
        }

        delete("/chat/{idChat}") {
            val idChat    = call.parameters["idChat"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "ID do chat inválido")
            // quem está pedindo a exclusão? passamos via query param ?usuario=...
            val usuarioReq = call.request.queryParameters["usuario"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")

            val chat = chatPasseioDAO.getChatById(idChat)
                ?: return@delete call.respond(HttpStatusCode.NotFound, "Chat não encontrado")

            if (chat.usuarioAdministrador != usuarioReq) {
                return@delete call.respond(HttpStatusCode.Forbidden, "Apenas o administrador pode excluir este chat")
            }

            val removed = chatPasseioDAO.deleteChat(idChat)
            if (removed) {
                call.respond(HttpStatusCode.OK, "Chat e membros removidos com sucesso")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Falha ao remover chat")
            }
        }

        delete("/chat/{idChat}/pessoa/{usuario}") {
            val idChat = call.parameters["idChat"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Parâmetro 'idChat' ausente ou inválido")
            val usuario = call.parameters["usuario"]?.takeIf { it.isNotBlank() }
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")

            // Para remover, usaremos o mesmo DAO (basta um DELETE em Pessoa_Chat)
            val sql = "DELETE FROM Pessoa_Chat WHERE idChat = ? AND usuario = ?"
            val deleted = DatabaseFactory.getConnection().use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setInt(1, idChat)
                    stmt.setString(2, usuario)
                    stmt.executeUpdate() > 0
                }
            }

            if (deleted) {
                call.respond(HttpStatusCode.OK, "Usuário removido do chat com sucesso")
            } else {
                call.respond(HttpStatusCode.NotFound, "Usuário não era membro ou chat não encontrado")
            }
        }

        // logo depois de outros GETs, por exemplo após get("/comunidades")
        get("/municipios") {
            // reutiliza a instância de ComunidadeDAO que já criou a tabela de Municipio
            val municipios = comunidadeDao.getAllMunicipios()
            call.respond(municipios)
        }

        post("/empresas") {
            val multipart = call.receiveMultipart()

            var usuario: String? = null
            var name: String? = null
            var description: String? = null
            var senha: String? = null
            var email: String? = null
            var cnpj: String? = null
            var imageUrl: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "usuario"    -> usuario    = part.value
                            "name"       -> name       = part.value
                            "description"-> description= part.value
                            "senha"      -> senha      = part.value
                            "email"      -> email      = part.value
                            "cnpj"       -> cnpj       = part.value
                        }
                    }
                    is PartData.FileItem -> {
                        // Salva a imagem em disco, dentro de /uploads
                        val uploadsDir = File("uploads")
                        if (!uploadsDir.exists()) uploadsDir.mkdirs()

                        val originalFileName = part.originalFileName ?: "img_${System.currentTimeMillis()}.jpg"
                        val file = File(uploadsDir, originalFileName).apply {
                            writeBytes(part.streamProvider().readBytes())
                        }

                        // Ajuste esta URL de acesso conforme seu ambiente (host/porta)
                        imageUrl = "http://192.168.0.89:8080/uploads/$originalFileName"
                    }
                    else -> { /* ignora outros tipos */ }
                }
                part.dispose()
            }

            // Verifica campos obrigatórios
            if (usuario == null
                || name == null
                || description == null
                || senha == null
                || email == null
                || cnpj == null
            ) {
                call.respondText(
                    "Campos obrigatórios ausentes",
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }

            val novaEmpresa = Empresa(
                usuario    = usuario!!,
                name       = name!!,
                description= description!!,
                imageUrl   = imageUrl,
                senha      = senha!!,
                email      = email!!,
                cnpj       = cnpj!!
            )
            val inserted = empresaDao.insertEmpresa(novaEmpresa)
            if (inserted) {
                call.respondText("Empresa criada com sucesso", status = HttpStatusCode.Created)
            } else {
                call.respondText("Erro ao criar Empresa", status = HttpStatusCode.InternalServerError)
            }
        }

        get("/empresas") {
            val todas = empresaDao.getAllEmpresas()
            call.respond(todas)
        }

        get("/empresas/{usuario}") {
            val usuarioParam = call.parameters["usuario"]
            if (usuarioParam == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")
                return@get
            }
            val empresa = empresaDao.getEmpresaByUsuario(usuarioParam)
            if (empresa == null) {
                call.respond(HttpStatusCode.NotFound, "Empresa não encontrada")
            } else {
                call.respond(empresa)
            }
        }


        put("/empresas/{usuario}") {
            val usuarioPath = call.parameters["usuario"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")

            var name: String? = null
            var description: String? = null
            var email: String? = null
            var cnpj: String? = null
            var imageUrl: String? = null
            var existingImageUrl: String? = null

            call.receiveMultipart().forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "name"             -> name = part.value
                            "description"      -> description = part.value
                            "email"            -> email = part.value
                            "cnpj"             -> cnpj = part.value
                            "existingImageUrl" -> existingImageUrl = part.value
                        }
                    }
                    is PartData.FileItem -> {
                        // Salva a nova imagem
                        val uploadsDir = File("uploads")
                        if (!uploadsDir.exists()) uploadsDir.mkdirs()

                        val fileName = part.originalFileName ?: "img_${System.currentTimeMillis()}.jpg"
                        val file = File(uploadsDir, fileName).apply {
                            writeBytes(part.streamProvider().readBytes())
                        }
                        imageUrl = "http://192.168.0.89:8080/uploads/$fileName"
                    }
                    else -> { /* ignora */ }
                }
                part.dispose()
            }

            // Campos obrigatórios: name, description, email e cnpj
            if (name == null || description == null || email == null || cnpj == null) {
                call.respond(HttpStatusCode.BadRequest, "Campos obrigatórios faltando")
                return@put
            }

            // Recupera a Empresa existente para obter a senha atual
            val empresaExistente = empresaDao.getEmpresaByUsuario(usuarioPath)
            if (empresaExistente == null) {
                call.respond(HttpStatusCode.NotFound, "Empresa não encontrada")
                return@put
            }

            // Cria objeto Empresa completo com senha inalterada
            val empresaAtualizada = Empresa(
                usuario     = usuarioPath,
                name        = name!!,
                description = description!!,
                imageUrl    = imageUrl ?: existingImageUrl,
                senha       = empresaExistente.senha,
                email       = email!!,
                cnpj        = cnpj!!
            )
            val updated = empresaDao.updateEmpresa(empresaAtualizada)
            if (updated) {
                call.respond(HttpStatusCode.OK, "Empresa atualizada com sucesso")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Falha ao atualizar Empresa")
            }
        }

        delete("/empresas/{usuario}") {
            val usuarioParam = call.parameters["usuario"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")

            val removed = empresaDao.deleteEmpresa(usuarioParam)
            if (removed) {
                call.respond(HttpStatusCode.OK, "Empresa '$usuarioParam' removida com sucesso")
            } else {
                call.respond(HttpStatusCode.NotFound, "Empresa '$usuarioParam' não encontrada")
            }
        }

        get("/eventos") {
            val lista = eventoDao.getAllEventos()
            call.respond(lista)
        }

// GET /eventos/{id}
        get("/eventos/{id}") {
            val idParam = call.parameters["id"]?.toIntOrNull()
            if (idParam == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'id' inválido")
                return@get
            }
            val evento = eventoDao.getEventoById(idParam)
            if (evento == null) {
                call.respond(HttpStatusCode.NotFound, "Evento não encontrado")
            } else {
                call.respond(evento)
            }
        }

        get("/eventos/favoritos/{usuario}") {
            val usuarioParam = call.parameters["usuario"]
            if (usuarioParam == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")
                return@get
            }
            val eventos = eventoDao.getEventosFavoritosByUsuario(usuarioParam)
            if (eventos.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "Nenhum evento favorito encontrado para o usuário: $usuarioParam")
            } else {
                call.respond(eventos)
            }
        }

// POST /eventos
        post("/eventos") {
            val multipart = call.receiveMultipart()
            var name: String? = null
            var administratorUser: String? = null
            var administrador: String? = null
            var descricao: String? = null
            var local: String? = null
            var dataEvento: String? = null
            val municipios = arrayListOf<String>()
            val tags = arrayListOf<String>()
            var imageUrl: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "name"              -> name = part.value
                            "administratorUser" -> administratorUser = part.value
                            "administrador"     -> administrador = part.value
                            "descricao"         -> descricao = part.value
                            "dataEvento"        -> dataEvento = part.value
                            "municipios"        -> {
                                // Aceita municípios separados por vírgula, ex.: "Mun1, Mun2, Mun3"
                                val munValues = part.value
                                    .split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                municipios.addAll(munValues)
                            }
                            "tags" -> {
                                // Aceita tags separadas por vírgula, ex.: "tagA, tagB"
                                val tagValues = part.value
                                    .split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                tags.addAll(tagValues)
                            }
                            "local"        -> local = part.value

                        }
                    }
                    is PartData.FileItem -> {
                        val uploadsDir = File("uploads")
                        if (!uploadsDir.exists()) uploadsDir.mkdirs()

                        val fileName = part.originalFileName
                            ?: "image_${System.currentTimeMillis()}.jpg"
                        val file = File(uploadsDir, fileName)
                        file.writeBytes(part.streamProvider().readBytes())
                        imageUrl = "http://192.168.0.89:8080/uploads/$fileName"
                    }
                    else -> { /* ignora outros tipos de PartData */ }
                }
                part.dispose()
            }

            // Campos obrigatórios (tags e imagem não são obrigatórios)
            if (name == null
                || administratorUser == null
                || administrador == null
                || descricao == null
                || dataEvento == null
                || municipios.isEmpty()
            ) {
                call.respondText("Campos obrigatórios ausentes", status = HttpStatusCode.BadRequest)
                return@post
            }

            val evento = Evento(
                idEvento = 0,
                imageUrl = imageUrl,
                name = name!!,
                administratorUser = administratorUser!!,
                administrator = administrador!!,
                descricao = descricao!!,
                dataEvento = dataEvento!!,
                municipios = municipios,
                tags = tags,
                local = local!!
            )

            val inserted = eventoDao.insertEvento(evento)
            if (inserted) {
                call.respondText("Evento criado com sucesso", status = HttpStatusCode.Created)
            } else {
                call.respondText("Erro ao criar Evento", status = HttpStatusCode.InternalServerError)
            }
        }



// PUT /eventos/{id}
        put("/eventos/{id}") {
            val idParam = call.parameters["id"]?.toIntOrNull()
            if (idParam == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'id' inválido")
                return@put
            }

            var name: String? = null
            var administratorUser: String? = null
            var administrador: String? = null
            var descricao: String? = null
            var dataEvento: String? = null
            val municipios = arrayListOf<String>()
            val tags = arrayListOf<String>()
            var imageUrl: String? = null
            var existingImageUrl: String? = null
            var local: String? = null

            call.receiveMultipart().forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "name"               -> name = part.value
                            "administratorUser"  -> administratorUser = part.value
                            "administrador"      -> administrador = part.value
                            "descricao"          -> descricao = part.value
                            "dataEvento"         -> dataEvento = part.value
                            "municipios"         -> {
                                // Se vier algum valor, preencha; caso contrário, fica vazio
                                part.value
                                    .split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                    .also { municipios.addAll(it) }
                            }
                            "tags" -> {
                                part.value
                                    .split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                    .also { tags.addAll(it) }
                            }
                            "existingImageUrl"   -> existingImageUrl = part.value
                            "local"          -> local = part.value
                        }
                    }
                    is PartData.FileItem -> {
                        // salva a nova imagem
                        val uploadsDir = File("uploads")
                        if (!uploadsDir.exists()) uploadsDir.mkdirs()

                        val fileName = part.originalFileName
                            ?: "image_${System.currentTimeMillis()}.jpg"
                        val file = File(uploadsDir, fileName)
                        file.writeBytes(part.streamProvider().readBytes())
                        imageUrl = "http://192.168.0.89:8080/uploads/$fileName"
                    }
                    else -> { }
                }
                part.dispose()
            }

            // Apenas valide que os campos essenciais não sejam nulos:
            if (name.isNullOrBlank()
                || administratorUser.isNullOrBlank()
                || administrador.isNullOrBlank()
                || descricao.isNullOrBlank()
                || dataEvento.isNullOrBlank()
            ) {
                call.respond(HttpStatusCode.BadRequest, "Campos obrigatórios ausentes")
                return@put
            }

            // Usa a imagem enviada ou mantém a anterior (se houver)
            val finalImageUrl = imageUrl ?: existingImageUrl

            // Monta objeto Evento, mesmo que municipios e tags estejam vazios
            val evento = Evento(
                idEvento = idParam,
                imageUrl = finalImageUrl,
                name = name!!,
                administratorUser = administratorUser!!,
                administrator = administrador!!,
                descricao = descricao!!,
                dataEvento = dataEvento!!,
                municipios = municipios,
                tags = tags,
                local = local!!
            )

            val updated = eventoDao.updateEvento(evento)
            if (updated) {
                call.respond(HttpStatusCode.OK, "Evento atualizado com sucesso")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Falha ao atualizar Evento")
            }
        }
// DELETE /eventos/{id}
        delete("/eventos/{id}") {
            val idParam = call.parameters["id"]?.toIntOrNull()
            if (idParam == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'id' inválido")
                return@delete
            }
            val deleted = eventoDao.deleteEvento(idParam)
            if (deleted) {
                call.respond(HttpStatusCode.OK, "Evento excluído com sucesso")
            } else {
                call.respond(HttpStatusCode.NotFound, "Evento não encontrado")
            }
        }


        get("/passeios/evento/{idEvento}") {
            // 1) extrai e converte idEvento…
            val idParam = call.parameters["idEvento"]?.toIntOrNull()
            if (idParam == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'idEvento' inválido")
                return@get
            }
            // 2) busca no DAO:
            val passeios = passeioEventoDao.getPasseioByEvento(idParam)
            if (passeios.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "Nenhum passeio de evento encontrado para id $idParam")
            } else {
                call.respond(passeios)
            }
        }

        get("/eventos/usuario/{usuario}") {
            val usuarioParam = call.parameters["usuario"]
            if (usuarioParam.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")
                return@get
            }
            val eventos = eventoDao.getEventosByUsuario(usuarioParam)
            if (eventos.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "Nenhum evento encontrado para o usuário: $usuarioParam")
            } else {
                call.respond(eventos)
            }
        }

// Depois (sempre retorna 200 com lista, vazia se não houver):
        get("/eventos/usuario/{usuario}") {
            val usuarioParam = call.parameters["usuario"]
            if (usuarioParam.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")
                return@get
            }
            val eventos = eventoDao.getEventosByUsuario(usuarioParam)
            call.respond(eventos)
        }



        post("/eventos/favoritar") {
            val multipart = call.receiveMultipart()
            var usuario: String? = null
            var idEvento: Int? = null

            multipart.forEachPart { part ->
                if (part is PartData.FormItem) {
                    when (part.name) {
                        "usuario" -> usuario = part.value
                        "idEvento" -> idEvento = part.value.toIntOrNull()
                    }
                }
                part.dispose()
            }

            if (usuario.isNullOrBlank() || idEvento == null) {
                call.respond(HttpStatusCode.BadRequest, "Dados inválidos")
                return@post
            }

            val dao = EventoDAO()
            val user = usuario!!
            val comId = idEvento!!

            val already = dao.isFavorita(user, comId)
            val success = if (already) {
                dao.unfavoriteEvento(user, comId)
            } else {
                dao.favoriteEvento(user, comId)
            }

            if (success) {
                val msg = if (already)
                    "Evento desfavoritado com sucesso"
                else
                    "Evento favoritado com sucesso"
                call.respond(HttpStatusCode.OK, msg)
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Falha ao atualizar favorito")
            }
        }


        // 3) Criar um novo passeio de evento
        post("/passeios/evento") {
            val multipart = call.receiveMultipart()
            var idEvento: Int? = null
            var usuario: String? = null
            var horario: String? = null
            var descricaoPasseio: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "idEvento" -> idEvento = part.value.toIntOrNull()
                            "usuario" -> usuario = part.value
                            "horario" -> horario = part.value
                            "descricaoPasseio" -> descricaoPasseio = part.value
                        }
                    }
                    else -> { /* Ignora outros tipos de PartData */ }
                }
                part.dispose()
            }

            // Verifica se os campos obrigatórios foram preenchidos
            if (idEvento == null || usuario == null || horario == null ||
                descricaoPasseio == null) {
                call.respond(HttpStatusCode.BadRequest, "Campos obrigatórios ausentes")
                return@post
            }

            // Cria variáveis imutáveis com tipos não-nulos utilizando '!!'
            val safeIdEvento: Int = idEvento!!
            val safeUsuario: String = usuario!!
            val safeHorario: String = horario!!
            val safeDescricaoPasseio: String = descricaoPasseio!!

            // Busca a Pessoa pelo 'usuario'
            val pessoa = pessoaDao.getPessoaByusuario(safeUsuario)
            if (pessoa == null) {
                call.respond(HttpStatusCode.NotFound, "Pessoa não encontrada")
                return@post
            }


            val evento = eventoDao.getEventoById(safeIdEvento)
            if (evento == null) {
                call.respond(HttpStatusCode.NotFound, "Evento não encontrado")
                return@post
            }


            val passeio = PasseioEvento(
                idPasseioEvento = 0,
                usuario = pessoa.usuario,// Será gerado pelo banco
                nomePessoa = pessoa.name,
                imagem = pessoa.imageUrl,
                horario = safeHorario,
                descricaoPasseio = safeDescricaoPasseio
            )

            val inserted = passeioEventoDao.insertPasseioEvento(passeio, pessoa, evento)
            if (inserted) {
                val atualizado = pessoaDao.incrementaPasseios(pessoa.usuario)
                call.respondText("Passeio criado com sucesso", status = HttpStatusCode.Created)
            } else {
                call.respondText("Erro ao criar Passeio", status = HttpStatusCode.InternalServerError)
            }
        }

        // 4) Deletar passeio de evento por ID
        delete("/passeios/evento/{id}") {
            val idPasseio = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Parâmetro ausente ou inválido")

            // 1) Busca quem é o dono antes de apagar
            val usuarioDono = passeioEventoDao.getUsuarioByPasseioEvento(idPasseio)
                ?: return@delete call.respond(HttpStatusCode.NotFound, "Passeio não encontrado")

            // 2) Executa todo o delete (comentários, chats e o próprio passeio)
            val deleted = passeioEventoDao.deletePasseioEvento(idPasseio)
            if (!deleted) {
                return@delete call.respond(HttpStatusCode.NotFound, "Passeio não encontrado")
            }

            // 3) Decrementa o contador de passeios daquele usuário
            passeioComunidadeDAO.decrementaPasseios(usuarioDono)

            // 4) Responde sucesso
            call.respond(HttpStatusCode.OK, "Passeio excluído com sucesso")
        }

        get("/passeios/evento/{idPasseioEvento}/comentarios") {
            val idPasseioEvento = call.parameters["idPasseioEvento"]?.toIntOrNull()
            if (idPasseioEvento == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'idPasseioEvento' ausente ou inválido")
                return@get
            }
            val comentarios = comentarioPasseioEventoDAO.getComentariosByPasseio(idPasseioEvento)
            call.respond(comentarios)
        }

        get("/passeios/evento/usuario/{usuario}") {
            val usuario = call.parameters["usuario"]
            if (usuario.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' inválido")
                return@get
            }
            val passeios = passeioEventoDao.getPasseioEventoByUsuario(usuario)
            call.respond(passeios)
        }

        post("/comentarios/evento") {
            val multipart = call.receiveMultipart()
            var idPasseioEvento: Int? = null
            var idEvento: Int? = null
            var usuario: String? = null
            var horario: String? = null
            var descricaoComentario: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "idPasseioEvento" -> idPasseioEvento = part.value.toIntOrNull()
                            "idEvento" -> idEvento = part.value.toIntOrNull()
                            "usuario" -> usuario = part.value
                            "horario" -> horario = part.value
                            "descricaoComentario" -> descricaoComentario = part.value
                        }
                    }
                    else -> { /* Ignora outros tipos de PartData */ }
                }
                part.dispose()
            }

            // Cria cópias imutáveis para evitar problemas de smart cast
            val safeIdPasseioEvento = idPasseioEvento
            val safeIdEvento = idEvento
            val safeUsuario = usuario
            val safeHorario = horario
            val safeDescricaoComentario = descricaoComentario

            if (safeIdPasseioEvento == null || safeIdEvento == null || safeUsuario == null ||
                safeHorario == null || safeDescricaoComentario == null) {
                call.respond(HttpStatusCode.BadRequest, "Campos obrigatórios ausentes")
                return@post
            }

            // Cria o objeto ComentarioPasseio; imagem e nomePessoa serão obtidos via JOIN com Pessoa
            val comentario = ComentarioPasseioEvento(
                idComentarioPasseioEvento = 0,
                horario = safeHorario,
                descricaoComentario = safeDescricaoComentario,
                imagem = "",
                nomePessoa = "",
                usuario = safeUsuario
            )

            val inserted = comentarioPasseioEventoDAO.insertComentario(comentario, safeIdPasseioEvento, safeIdEvento, safeUsuario)
            if (inserted) {
                call.respondText("Comentário inserido com sucesso", status = HttpStatusCode.Created)
            } else {
                call.respondText("Erro ao inserir comentário", status = HttpStatusCode.InternalServerError)
            }
        }

        delete("/comentarios/evento/{idComentarioPasseioEvento}") {
            val idParam = call.parameters["idComentarioPasseioEvento"]
            val idComentarioPasseioEvento = idParam?.toIntOrNull()
            if (idComentarioPasseioEvento == null) {
                call.respond(HttpStatusCode.BadRequest, "Parâmetro 'idComentarioPasseioEvento' ausente ou inválido")
                return@delete
            }
            val deleted = comentarioPasseioEventoDAO.deleteComentarioPasseioEvento(idComentarioPasseioEvento)
            if (deleted) {
                call.respond(HttpStatusCode.OK, "Comentário excluído com sucesso")
            } else {
                call.respond(HttpStatusCode.NotFound, "Comentário não encontrado")
            }
        }

        post("/empresas/{empresaUsuario}/seguir") {
            val empresaParam  = call.parameters["empresaUsuario"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Parâmetro 'empresaUsuario' ausente")
            val body          = call.receiveParameters()
            val followerParam = body["follower"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Campo 'follower' ausente")

            val follower = followerParam    // ex: "@fulano"
            val empresa  = empresaParam     // ex: "@empresaX"

            // 1) Insere o follow na relação many-to-many
            val inserted = pessoaDao.followEmpresa(follower, empresa)
            if (!inserted) {
                return@post call.respond(HttpStatusCode.Conflict, "Já segue esta empresa")
            }

            // 2) Incrementa o contador de seguidores na Empresa
            val incremented = pessoaDao.incrementaEmpresas(follower)
            if (!incremented) {
                // caso dê algum problema ao atualizar o contador, podemos fazer rollback do follow
                pessoaDao.unfollowEmpresa(follower, empresa)
                return@post call.respond(
                    HttpStatusCode.InternalServerError,
                    "Erro ao atualizar contador de seguidores"
                )
            }

            // 3) Tudo OK
            call.respond(HttpStatusCode.Created)
        }



// 2) Deixar de seguir
        delete("/empresas/{empresaUsuario}/seguir/{follower}") {
            val empresaParam  = call.parameters["empresaUsuario"]!!
            val followerParam = call.parameters["follower"]!!
            val follower = followerParam
            val empresa  = empresaParam

            if (pessoaDao.unfollowEmpresa(follower, empresa)) {
                // decrementa
                pessoaDao.decrementaEmpresas(follower)
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

// 3) Verificar se já segue
        get("/empresas/{empresaUsuario}/seguidores/{follower}") {
            val empresaParam  = call.parameters["empresaUsuario"]!!
            val followerParam = call.parameters["follower"]!!

            val follower = followerParam
            val empresa  = empresaParam

            val following = pessoaDao.isFollowingEmpresa(follower, empresa)
            call.respond(mapOf("following" to following))
        }
        // 4) Listar todos os seguidores de uma empresa
        get("/empresas/{empresaUsuario}/seguidores") {
            val empresa = call.parameters["empresaUsuario"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val list    = pessoaDao.getEmpresasSeguidas(empresa)
            if (list.isEmpty()) call.respond(HttpStatusCode.NoContent)
            else               call.respond(list)
        }

        get("/pessoas/{usuario}/empresasSeguidas") {
            val usuario = call.parameters["usuario"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Parâmetro 'usuario' ausente")
            val empresas = pessoaDao.getEmpresasSeguidasFull(usuario)
            if (empresas.isEmpty()) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(empresas)
            }
        }
    }
    }
