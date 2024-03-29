package com.lumidion.unistore.clients

import com.lumidion.unistore.clients.AwsS3Client.defaultRegion
import com.lumidion.unistore.config.AwsS3StorageConfig
import com.lumidion.unistore.models.errors.{ConfigError, FileRetrievalError, UnistoreError}
import com.lumidion.unistore.utils.Extensions.{ZIOOps, ZLayerOps}

import zio.{ZIO, ZLayer}
import zio.aws.core.config.{AwsConfig, ClientCustomization}
import zio.aws.netty.NettyHttpClient
import zio.aws.s3.model.GetObjectRequest
import zio.aws.s3.S3
import zio.aws.s3.S3.getObject
import zio.stream.ZSink

import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.regions.Region

private[unistore] class AwsS3Client(config: AwsS3StorageConfig) extends StorageClient {

  private val layer: ZLayer[Any, UnistoreError, S3] = {
    val awsConfig = config.configuredCredentialsProvider
      .map { provider =>
        AwsConfig.customized(new ClientCustomization {
          override def customize[Client, Builder <: AwsClientBuilder[Builder, Client]](
              builder: Builder
          ): Builder =
            builder.credentialsProvider(provider).region(config.region.getOrElse(defaultRegion))
        })
      }
      .getOrElse(AwsConfig.default)

    NettyHttpClient.default >>> awsConfig >>> S3.live
  }
    .leftZLayerToAppErr(err => ConfigError(new Exception(err.getMessage)))

  def loadFile: ZIO[Any, UnistoreError, Array[Byte]] = {
    val request = GetObjectRequest(bucket = config.bucketName, key = config.objectKey)

    {
      for {
        res <- getObject(request)
          .leftZIOToAppErr(err => FileRetrievalError(new Exception(err.toThrowable.getMessage)))
        bytes <- res.output
          .run(ZSink.collectAll)
          .leftZIOToAppErr(err => FileRetrievalError(new Exception(err.toThrowable.getMessage)))
      } yield bytes.toArray
    }
      .provideLayer(layer)
  }
}

private[unistore] object AwsS3Client {
  val defaultRegion: Region = Region.US_WEST_1
}
