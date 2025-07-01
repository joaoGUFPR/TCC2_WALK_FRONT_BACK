package com.example.prototipopasseios.controller


import com.example.prototipopasseios.model.ChatPasseio
import com.example.prototipopasseios.model.ComentarioChat
import com.example.prototipopasseios.model.ComentarioPasseioComunidade
import com.example.prototipopasseios.model.ComentarioPasseioEvento
import com.example.prototipopasseios.model.Comunidade
import com.example.prototipopasseios.model.Empresa
import com.example.prototipopasseios.model.Evento
import com.example.prototipopasseios.model.Municipio
import com.example.prototipopasseios.model.Notificacao
import com.example.prototipopasseios.model.PasseioComunidade
import com.example.prototipopasseios.model.PasseioEvento
import com.example.prototipopasseios.model.Pessoa
import com.google.gson.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface PessoaApiService {
    @GET("pessoas/{usuario}")
    suspend fun getPessoa(@Path("usuario") usuario: String): Response<Pessoa>

    @Multipart
    @POST("pessoas")
    suspend fun createPessoa(
        @Part("usuario") usuario: RequestBody,
        @Part("senha") senha: RequestBody,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody,
        @Part("qtAmigos") qtAmigos: RequestBody,
        @Part("qtPasseios") qtPasseios: RequestBody,
        @Part("municipio") municipio: RequestBody,
        @Part("email") email: RequestBody,
        @Part("nascimento") nascimento: RequestBody,
        @Part image: MultipartBody.Part? = null,
        @Part("qtEmpresas") qtEmpresas: RequestBody

    ): Response<Unit>


    @GET("comunidades")
    suspend fun getComunidades(): Response<List<Comunidade>>

    @Multipart
    @POST("comunidades")
    suspend fun createComunidade(
        @Part("name") name: RequestBody,
        @Part("administratorUser") administratorUser: RequestBody,
        @Part("descricao") descricao: RequestBody,
        @Part("regras") regras: RequestBody,
        @Part("municipio") municipio: RequestBody,
        @Part("tags") tags: RequestBody, // Envie as tags como uma string separada por vírgula
        @Part image: MultipartBody.Part? = null  // imagem é opcional
    ): Response<Unit>

    @GET("passeios")
    suspend fun getPasseios(): Response<List<PasseioComunidade>>

    @GET("passeios/{idComunidade}")
    suspend fun getPasseiosByComunidade(@Path("idComunidade") id: Int): Response<List<PasseioComunidade>>

    @Multipart
    @POST("passeios")
    suspend fun createPasseio(
        @Part("idComunidade") idComunidade: RequestBody,
        @Part("usuario") usuario: RequestBody,
        @Part("horario") horario: RequestBody,
        @Part("descricaoPasseio") descricaoPasseio: RequestBody,
        @Part("localizacao") localizacao: RequestBody
    ): Response<Unit>

    @GET("passeios/{idPasseioComunidade}/comentarios")
    suspend fun getComentariosByPasseio(@Path("idPasseioComunidade") id: Int): Response<List<ComentarioPasseioComunidade>>

    @Multipart
    @POST("comentarios")
    suspend fun createComentario(
        @Part("idPasseioComunidade") idPasseioComunidade: RequestBody,
        @Part("idComunidade") idComunidade: RequestBody,
        @Part("usuario") usuario: RequestBody,
        @Part("horario") horario: RequestBody,
        @Part("descricaoComentario") descricaoComentario: RequestBody
    ): Response<Unit>

    @Multipart
    @POST("chat")
    suspend fun createChat(
        @Part("idPasseio") idPasseio: RequestBody,
        @Part("tipo") tipo: RequestBody,                       // <— novo campo
        @Part("usuarioAdministrador") usuarioAdministrador: RequestBody
    ): Response<Unit>

    @Multipart
    @POST("chat/criarComUsuarios")
    suspend fun createChatComUsuarios(
        @Part("idPasseio") idPasseio: RequestBody,
        @Part("tipo") tipo: RequestBody,
        @Part("usuarioAdministrador") usuarioAdministrador: RequestBody,
        @Part("usuarios") usuarios: RequestBody  // Usuários separados por vírgula
    ): Response<Unit>

    @Multipart
    @POST("login")
    suspend fun login(
        @Part("usuario") usuario: RequestBody,
        @Part("senha") senha: RequestBody
    ): Response<JsonElement>
    @GET("chat/byUsuario/{usuario}")
    suspend fun getChatByUsuario(@Path("usuario") usuario: String): Response<List<ChatPasseio>>
    @GET("chat/{idChat}/comentarios")
    suspend fun getChatComentarios(@Path("idChat") idChat: Int): Response<List<ComentarioChat>>
    @GET("chat/passeio/{idPasseio}/{tipo}")
    suspend fun getChatByPasseio(
        @Path("idPasseio") idPasseio: Int,
        @Path("tipo") tipo: String                // <— receber “EVENTO” ou “COMUNIDADE”
    ): Response<List<ChatPasseio>>
    @Multipart
    @POST("comunidades/favoritar")
    suspend fun favoritarComunidade(
        @Part("usuario") usuario: RequestBody,
        @Part("idComunidade") idComunidade: RequestBody
    ): Response<Unit>
    @GET("comunidades/usuario/{usuario}")
    suspend fun getComunidadesByUsuario(@Path("usuario") usuario: String): Response<List<Comunidade>>
    @GET("comunidades/favoritas/{usuario}")
    suspend fun getComunidadeFavoritaByUsuario(@Path("usuario") usuario: String): Response<List<Comunidade>>
    @DELETE("comunidades/{id}")
    suspend fun deleteComunidade(@Path("id") id: Int): Response<Unit>
    @DELETE("passeios/{idPasseioComunidade}")
    suspend fun deletePasseio(@Path("idPasseioComunidade") idPasseioComunidade: Int): Response<Unit>
    @DELETE("comentarios/{idComentarioPasseio}")
    suspend fun deleteComentario(@Path("idComentarioPasseio") idComentarioPasseio: Int): Response<Unit>
    @Multipart
    @PUT("comunidades/{id}")
    suspend fun updateComunidade(
        @Path("id") id: Int,
        @Part("name") name: RequestBody,
        @Part("administratorUser") administratorUser: RequestBody,
        @Part("descricao") descricao: RequestBody,
        @Part("regras") regras: RequestBody,
        @Part("municipio") municipio: RequestBody,
        @Part("tags") tags: RequestBody,
        @Part("existingImageUrl") existingImageUrl: RequestBody, // novo campo para manter a imagem atual
        @Part image: MultipartBody.Part? = null
    ): Response<Unit>
    @GET("passeios/usuario/{usuario}")
    suspend fun getPasseiosByUsuario(@Path("usuario") usuario: String): Response<List<PasseioComunidade>>

    @GET("pessoas/{usuario}/amigos")
    suspend fun getFriends(@Path("usuario") usuario: String): Response<List<String>>

    @POST("pessoas/{usuario}/amigos")
    suspend fun addFriend(
        @Path("usuario") usuario: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    @GET("notificacoes/usuario/{usuario}")
    suspend fun getNotificacoes(@Path("usuario") usuario: String): Response<List<Notificacao>>

    @POST("notificacoes/{usuarioDestinado}/{usuarioRemetente}/responder")
    suspend fun responderNotif(
        @Path("usuarioDestinado") usuarioDestinado: String,
        @Path("usuarioRemetente") usuarioRemetente: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    @GET("pessoas/{usuario}/amigos/{amigo}")
    suspend fun isFriend(
        @Path("usuario") usuario: String,
        @Path("amigo") amigo: String
    ): Response<Map<String, Boolean>>

    @GET("pessoas/{usuario}/amigos")
    suspend fun getFriendPessoas(
        @Path("usuario") usuario: String
    ): Response<List<Pessoa>>

    @DELETE("pessoas/{usuario}/amigos/{amigo}")
    suspend fun removeFriend(
        @Path("usuario") usuario: String,
        @Path("amigo")   amigo: String
    ): Response<Void>

    @FormUrlEncoded
    @PUT("pessoas/{usuario}/municipio")
    suspend fun updateMunicipio(
        @Path("usuario") usuario: String,
        @Field("municipio") municipio: String
    ): Response<Pessoa>

    @Multipart
    @PUT("pessoas/{usuario}")
    suspend fun updatePessoa(
        @Path("usuario") usuario: String,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody,
        @Part("municipio") municipio: RequestBody,
        @Part("email") email: RequestBody,
        @Part("nascimento") nascimento: RequestBody,
        @Part("existingImageUrl") existingImageUrl: RequestBody,
        @Part image: MultipartBody.Part? = null
    ): Response<Unit>

    @GET("chat/{idChat}/membros")
    suspend fun getChatMembers(
        @Path("idChat") idChat: Int
    ): Response<List<String>>

    @FormUrlEncoded
    @POST("chat/{idChat}/addPessoa")
    suspend fun addPessoaChat(
        @Path("idChat") idChat: Int,
        @Field("usuario") usuario: String
    ): Response<Unit>

    @DELETE("chat/{idChat}/pessoa/{usuario}")
    suspend fun removePessoaChat(
        @Path("idChat") idChat: Int,
        @Path("usuario") usuario: String
    ): Response<Unit>

    @DELETE("chat/{idChat}")
    suspend fun deleteChat(
        @Path("idChat")   idChat: Int,
        @Query("usuario") usuario: String
    ): Response<Unit>


    @DELETE("notificacoes/{usuarioDestinado}/{usuarioRemetente}")
    suspend fun deleteNotificacao(
        @Path("usuarioDestinado") usuarioDestinado: String,
        @Path("usuarioRemetente") usuarioRemetente: String
    ): Response<Unit>

    @GET("chat/membro/{usuario}")
    suspend fun getChatsByMember(
        @Path("usuario") usuario: String
    ): Response<List<ChatPasseio>>

    @GET("chat/{idChat}")
    suspend fun getChatById(@Path("idChat") idChat: Int): Response<ChatPasseio>

    @GET("municipios")
    suspend fun getMunicipios(): Response<List<Municipio>>
    @DELETE("pessoas/{usuario}")
    suspend fun deletePessoa(@Path("usuario") usuario: String): Response<Unit>
    @GET("pessoas")
    suspend fun getAllPessoas(): Response<List<Pessoa>>
    @GET("empresas")
    suspend fun getAllEmpresas(): Response<List<Empresa>>

    @GET("empresas/{usuario}")
    suspend fun getEmpresaByUsuario(@Path("usuario") usuario: String): Response<Empresa>

    @Multipart
    @POST("empresas")
    suspend fun createEmpresa(
        @Part("usuario") usuario: RequestBody,
        @Part("senha") senha: RequestBody,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody,
        @Part("email") email: RequestBody,
        @Part image: MultipartBody.Part? = null,
        @Part("cnpj") cnpj: RequestBody
    ): Response<Unit>

    @Multipart
    @PUT("empresas/{usuario}")
    suspend fun updateEmpresa(
        @Path("usuario") usuario: String,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody,
        @Part("email") email: RequestBody,
        @Part("cnpj") cnpj: RequestBody,
        @Part("existingImageUrl") existingImageUrl: RequestBody,
        @Part image: MultipartBody.Part? = null
    ): Response<Unit>

    @DELETE("empresas/{usuario}")
    suspend fun deleteEmpresa(@Path("usuario") usuario: String): Response<Unit>

    @GET("eventos")
    suspend fun getAllEventos(): Response<List<Evento>>

    @GET("eventos/usuario/{usuario}")
    suspend fun getEventosByUsuario(@Path("usuario") usuario: String): Response<List<Evento>>

    @GET("eventos/{id}")
    suspend fun getEventoById(@Path("id") id: Int): Response<Evento>

    @GET("eventos/favoritos/{usuario}")
    suspend fun getEventosFavoritosByUsuario(@Path("usuario") usuario: String): Response<List<Evento>>

    @Multipart
    @POST("eventos")
    suspend fun createEvento(
        @Part("name") name: RequestBody,
        @Part("administratorUser") administratorUser: RequestBody,
        @Part("administrador") administrador: RequestBody,
        @Part("descricao") descricao: RequestBody,
        @Part("dataEvento") dataEvento: RequestBody,
        @Part("municipios") municipios: RequestBody, // "Muni1, Muni2, Muni3"
        @Part("tags") tags: RequestBody,             // "tagA, tagB"
        @Part image: MultipartBody.Part? = null,
        @Part("local") local: RequestBody

    ): Response<Unit>

    @Multipart
    @PUT("eventos/{id}")
    suspend fun updateEvento(
        @Path("id") id: Int,
        @Part("name") name: RequestBody,
        @Part("administratorUser") administratorUser: RequestBody,
        @Part("administrador") administrador: RequestBody,
        @Part("descricao") descricao: RequestBody,
        @Part("dataEvento") dataEvento: RequestBody,
        @Part("municipios") municipios: RequestBody,
        @Part("tags") tags: RequestBody,
        @Part("existingImageUrl") existingImageUrl: RequestBody,
        @Part image: MultipartBody.Part? = null,
        @Part("local") local: RequestBody
    ): Response<Unit>

    @DELETE("eventos/{id}")
    suspend fun deleteEvento(@Path("id") id: Int): Response<Unit>

    @GET("passeios/evento/{idEvento}")
    suspend fun getPasseiosByEvento(@Path("idEvento") id: Int): Response<List<PasseioEvento>>

    @DELETE("passeios/evento/{idPasseioEvento}")
    suspend fun deletePasseioEvento(@Path("idPasseioEvento") id: Int): Response<Unit>

    @Multipart
    @POST("eventos/favoritar")
    suspend fun favoritarEventos(
        @Part("usuario") usuario: RequestBody,
        @Part("idEvento") idEvento: RequestBody
    ): Response<Unit>

    @Multipart
    @POST("passeios/evento")
    suspend fun createPasseioEvento(
        @Part("idEvento") idEvento: RequestBody,
        @Part("usuario") usuario: RequestBody,
        @Part("horario") horario: RequestBody,
        @Part("descricaoPasseio") descricaoPasseio: RequestBody
    ): Response<Unit>

    @GET("/passeios/evento/{idPasseioEvento}/comentarios")
    suspend fun getComentariosByPasseioEvento(@Path("idPasseioEvento") id: Int): Response<List<ComentarioPasseioEvento>>

    @Multipart
    @POST("comentarios/evento")
    suspend fun createComentarioEvento(
        @Part("idPasseioEvento") idPasseioEvento: RequestBody,
        @Part("idEvento") idEvento: RequestBody,
        @Part("usuario") usuario: RequestBody,
        @Part("horario") horario: RequestBody,
        @Part("descricaoComentario") descricaoComentario: RequestBody
    ): Response<Unit>

    @DELETE("comentarios/evento/{idComentarioPasseioEvento}")
    suspend fun deleteComentarioEvento(@Path("idComentarioPasseioEvento") idComentarioPasseioEvento: Int): Response<Unit>

    @POST("empresas/{empresaUsuario}/seguir")
    @FormUrlEncoded
    suspend fun followEmpresa(
        @Path("empresaUsuario") empresaUsuario: String,
        @Field("follower") follower: String
    ): Response<Unit>

    @DELETE("empresas/{empresaUsuario}/seguir/{follower}")
    suspend fun unfollowEmpresa(
        @Path("empresaUsuario") empresaUsuario: String,
        @Path("follower") follower: String
    ): Response<Unit>

    @GET("empresas/{empresaUsuario}/seguidores/{follower}")
    suspend fun isFollowingEmpresa(
        @Path("empresaUsuario") empresaUsuario: String,
        @Path("follower") follower: String
    ): Response<Map<String, Boolean>>

    @GET("empresas/{empresaUsuario}/seguidores")
    suspend fun getFollowersOfEmpresa(
        @Path("empresaUsuario") empresaUsuario: String
    ): Response<List<String>>

    @GET("pessoas/{usuario}/empresasSeguidas")
    suspend fun getEmpresasSeguidas(
        @Path("usuario") usuario: String
    ): Response<List<Empresa>>

    @GET("passeios/evento/usuario/{usuario}")
    suspend fun getPasseiosEventoByUsuario(@Path("usuario") usuario: String): Response<List<PasseioEvento>>
}

