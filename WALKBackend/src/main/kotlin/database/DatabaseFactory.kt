package br.com.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

object DatabaseFactory {
    private lateinit var dataSource: HikariDataSource

    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://localhost:5432/PrototipoPasseio"
            username = "postgres"
            password = "Joao123!"
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
        }
        dataSource = HikariDataSource(config)
        println("Pool HikariCP configurado com sucesso!")
    }

    fun getConnection() = dataSource.connection
}
