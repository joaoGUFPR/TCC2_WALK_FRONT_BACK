package com.example.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

class DBHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "walk.db"
        const val DATABASE_VERSION = 1
        const val TABLE_COMUNIDADE = "Comunidade"
        const val TABLE_IMAGEM = "Imagem"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableComunidade="""
               CREATE TABLE $TABLE_COMUNIDADE (
               idComunidade INTEGER PRIMARY KEY,
               administrador TEXT,
               imagem BLOB,
               nome TEXT,
               descricao TEXT,
               regras TEXT,
               municipio TEXT
               )
           """.trimIndent()

        val createTableTag="""
               CREATE TABLE Tag (
               idTag INTEGER PRIMARY KEY,
               nome TEXT
               )
           """.trimIndent()

        val createTableTag_Comunidade="""
               CREATE TABLE Tag_Comunidade (
               idComunidade INTEGER,
               idTag INTEGER,
               PRIMARY KEY (idTag, idComunidade),
               FOREIGN KEY (idComunidade) REFERENCES Comunidade(idComunidade),
               FOREIGN KEY (idTag) REFERENCES Tag(idTag)
               )
           """.trimIndent()

        db.execSQL(createTableComunidade)
        db.execSQL(createTableTag)
        db.execSQL(createTableTag_Comunidade)

        // Inserção inicial na tabela Comunidade (sem imagem inicialmente)
        val insertInitialComunidade = """
            INSERT INTO $TABLE_COMUNIDADE (idComunidade, nome, descricao, regras, municipio)
            VALUES (1, "Bar", "Para ir ao bar", "Regras comunidade bar", "Curitiba"),
                   (2, "Jogos", "Para marcar jogos", "Regras comunidade jogos", "Londrina")
        """.trimIndent()
        db.execSQL(insertInitialComunidade)

        // Inserção inicial na tabela Tag (opcional)
        val insertInitialTag = """
            INSERT INTO Tag (idTag, nome)
            VALUES (1, "Social"),
                   (2, "Esporte"),
                   (3, "Lazer")
        """.trimIndent()
        db.execSQL(insertInitialTag)

        // Inserção inicial na tabela Tag_Comunidade (opcional)
        val insertInitialTag_Comunidade = """
            INSERT INTO Tag_Comunidade (idComunidade, idTag)
            VALUES (1, 1), 
                   (1, 3), 
                   (2, 2), 
                   (2, 3)
        """.trimIndent()
        db.execSQL(insertInitialTag_Comunidade)
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Implement logic to handle database upgrades here
        // For example, you can drop and recreate tables
        // or perform schema migrations
    }
}