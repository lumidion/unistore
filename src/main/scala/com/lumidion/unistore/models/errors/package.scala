package com.lumidion.unistore.models

package object errors {
  sealed trait UnistoreError {
    val errorName: String
    val ex: Exception
  }

  final case class ConfigError(ex: Exception) extends UnistoreError {
    override val errorName = "Config Error"
  }

  final case class FileRetrievalError(ex: Exception) extends UnistoreError {
    override val errorName = "File Retrieval Error"
  }

  final case class CredentialsRetrievalError(ex: Exception) extends UnistoreError {
    override val errorName = "Credentials Retrieval Error"
  }
}
