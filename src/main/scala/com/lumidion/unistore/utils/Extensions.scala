package com.lumidion.unistore.utils

import com.lumidion.unistore.models.errors.{ConfigError, UnistoreError}

import zio.{Tag, ZIO, ZLayer}

import scala.jdk.CollectionConverters.*
import software.amazon.awssdk.regions.Region

private[unistore] object Extensions {

  implicit class OptionStringOps(value: Option[String]) {
    private lazy val regionsById =
      Region.regions().asScala.map(region => (region.id(), region)).toMap

    def toRegion: ZIO[Any, UnistoreError, Option[Region]] = {
      value.fold[ZIO[Any, UnistoreError, Option[Region]]](ZIO.none) { regionId =>
        ZIO.fromEither {
          regionsById
            .get(regionId)
            .toRight(ConfigError(new Exception(s"Invalid region id given. Region id: $regionId")))
        }.asSome
      }
    }
  }

  implicit class ZIOOps[R, E, A](value: ZIO[R, E, A]) {
    def leftZIOToAppErr(func: E => UnistoreError): ZIO[R, UnistoreError, A] =
      value.foldZIO(err => ZIO.fail(func(err)), res => ZIO.succeed(res))
  }

  implicit class ZLayerOps[R, E, A](value: ZLayer[R, E, A]) {
    def leftZLayerToAppErr(func: E => UnistoreError)(implicit
        tag: Tag[A]
    ): ZLayer[R, UnistoreError, A] =
      value.foldLayer(err => ZLayer.fail(func(err)), res => ZLayer.succeed(res.get))
  }
}
