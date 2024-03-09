package com.lumidion.unistore.clients

import com.lumidion.unistore.config.LocalStorageConfig
import com.lumidion.unistore.models.errors.{FileRetrievalError, UnistoreError}
import com.lumidion.unistore.utils.Extensions.ZIOOps

import zio.stream.{ZSink, ZStream}
import zio.ZIO

import java.io.File

private[unistore] class LocalStorageClient(config: LocalStorageConfig) extends StorageClient {
  def loadFile: ZIO[Any, UnistoreError, Array[Byte]] =
    ZStream
      .fromFile(new File(config.filePath))
      .run(ZSink.collectAll)
      .map(_.toArray)
      .leftZIOToAppErr(err =>
        FileRetrievalError(
          new Exception(
            s"Could not load file at path: ${config.filePath}. Error message: ${err.getMessage}"
          )
        )
      )
}
