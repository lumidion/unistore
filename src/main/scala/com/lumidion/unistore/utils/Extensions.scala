package com.lumidion.unistore.utils

import zio.{Tag, ZIO, ZLayer}

import com.lumidion.unistore.models.errors.UnistoreError

private[unistore] object Extensions {
  object ZIOOps {
    extension [R, E, A](value: ZIO[R, E, A])
      def leftZIOToAppErr(func: E => UnistoreError): ZIO[R, UnistoreError, A] =
        value.foldZIO(err => ZIO.fail(func(err)), res => ZIO.succeed(res))
  }

  object ZLayerOps {
    extension [R, E, A](value: ZLayer[R, E, A])
      def leftZLayerToAppErr(func: E => UnistoreError)(implicit
          tag: Tag[A]
      ): ZLayer[R, UnistoreError, A] =
        value.foldLayer(err => ZLayer.fail(func(err)), res => ZLayer.succeed(res.get))
  }
}
