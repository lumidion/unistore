package com.lumidion.unistore.clients

import zio.ZIO

import com.lumidion.unistore.models.errors.UnistoreError

private[unistore] trait StorageClient {
  def loadFile: ZIO[Any, UnistoreError, Array[Byte]]
}
