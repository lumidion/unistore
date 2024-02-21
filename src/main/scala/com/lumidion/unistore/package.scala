package com.lumidion

import zio.ZIO

import com.lumidion.unistore.clients.{AwsS3Client, LocalStorageClient}
import com.lumidion.unistore.config.{AwsS3StorageConfig, LocalStorageConfig, UnistoreConfig}
import com.lumidion.unistore.models.errors.UnistoreError

package object unistore {
  def loadFileAsBytes(
      config: UnistoreConfig
  ): ZIO[Any, UnistoreError, Option[Array[Byte]]] =
    for {
      storageConf <- config.toStorageConfig
      clientOpt = storageConf.map {
        case finalConf: AwsS3StorageConfig => new AwsS3Client(finalConf)
        case finalConf: LocalStorageConfig => new LocalStorageClient(finalConf)
      }
      byteArrayOpt <- clientOpt.fold(ZIO.succeed(None))(_.loadFile.asSome)
    } yield byteArrayOpt

  def loadFileAsString(
      config: UnistoreConfig
  ): ZIO[Any, UnistoreError, Option[String]] =
    for {
      fileOpt <- loadFileAsBytes(config)
    } yield fileOpt.map(String(_))
}
