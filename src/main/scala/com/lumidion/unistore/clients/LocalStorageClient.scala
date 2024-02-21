package com.lumidion.unistore.clients

import zio.stream.{ZSink, ZStream}
import zio.ZIO

import com.lumidion.unistore.config.LocalStorageConfig
import com.lumidion.unistore.models.errors.{UnistoreError, FileRetrievalError}
import com.lumidion.unistore.utils.Extensions.ZIOOps.*

private[unistore] class LocalStorageClient(config: LocalStorageConfig) extends StorageClient {
  def loadFile: ZIO[Any, UnistoreError, Array[Byte]] =
    ZStream
      .fromFile(java.io.File(config.filePath))
      .run(ZSink.collectAll)
      .map(_.toArray)
      .leftZIOToAppErr(err =>
        FileRetrievalError(new Exception(s"Could not load file at path: ${config.filePath}"))
      )

}
