package com.lumidion.unistore.clients

import com.lumidion.unistore.models.errors.UnistoreError

import zio.ZIO

private[unistore] trait StorageClient {
  def loadFile: ZIO[Any, UnistoreError, Array[Byte]]
}
