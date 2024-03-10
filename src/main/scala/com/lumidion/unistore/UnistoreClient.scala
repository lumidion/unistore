package com.lumidion.unistore

import com.lumidion.unistore.clients.{AwsS3Client, LocalStorageClient}
import com.lumidion.unistore.config.{AwsS3StorageConfig, LocalStorageConfig, StorageConfig}
import com.lumidion.unistore.models.errors.{ConfigError, CredentialsRetrievalError, UnistoreError}
import com.lumidion.unistore.utils.Extensions.{OptionStringOps, ZIOOps, ZLayerOps}

import zio.aws.core.config.{AwsConfig, ClientCustomization}
import zio.aws.netty.NettyHttpClient
import zio.aws.sts.model.AssumeRoleRequest
import zio.aws.sts.Sts
import zio.ZIO

import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder

class UnistoreClient(
    awsS3BucketName: Option[String] = None,
    awsS3ObjectKey: Option[String] = None,
    awsAccessKeyId: Option[String] = None,
    awsSecretAccessKey: Option[String] = None,
    awsIamAssumeRoleRequest: Option[AssumeRoleRequest] = None,
    awsRegion: Option[String] = None,
    localFilePath: Option[String] = None,
    localFallbackFilePath: Option[String] = None,
    stringEncoding: Option[String] = None
) {
  private def toStorageConfig: ZIO[Any, UnistoreError, Option[StorageConfig]] =
    (
      awsS3BucketName,
      awsS3ObjectKey,
      awsAccessKeyId,
      awsSecretAccessKey,
      awsIamAssumeRoleRequest,
      awsRegion,
      localFilePath
    ) match {
      case (Some(bucketName), Some(objectKey), _, _, _, regionOpt, None) =>
        (awsAccessKeyId, awsSecretAccessKey, awsIamAssumeRoleRequest) match {
          case (Some(accessKey), Some(secretAccessKey), None) =>
            for {
              validatedRegionOpt <- regionOpt.toRegion
              config <- AwsS3StorageConfig
                .fromBasicCredentials(
                  bucketName,
                  objectKey,
                  accessKey,
                  secretAccessKey,
                  validatedRegionOpt
                )
                .asSome
            } yield config

          case (None, None, Some(assumeRoleRequest)) =>
            val layer = (NettyHttpClient.default >>> AwsConfig.default >>> Sts.live)
              .leftZLayerToAppErr(err => CredentialsRetrievalError(new Exception(err.getMessage)))
            for {
              validatedRegionOpt <- regionOpt.toRegion
              config <- AwsS3StorageConfig
                .fromAssumeRoleRequest(bucketName, objectKey, assumeRoleRequest, validatedRegionOpt)
                .asSome
                .leftZIOToAppErr(err =>
                  CredentialsRetrievalError(new Exception(err.toThrowable.getMessage))
                )
                .provide(layer)
            } yield config

          case (Some(accessKey), Some(secretAccessKey), Some(assumeRoleRequest)) =>
            for {
              validatedRegion <- regionOpt.toRegion
              finalRegion = validatedRegion.getOrElse(AwsS3Client.defaultRegion)
              awsConfigWithStaticCreds = AwsConfig.customized(new ClientCustomization {
                override def customize[Client, Builder <: AwsClientBuilder[Builder, Client]](
                    builder: Builder
                ): Builder = builder
                  .credentialsProvider(
                    StaticCredentialsProvider.create(
                      AwsBasicCredentials.create(accessKey, secretAccessKey)
                    )
                  )
                  .region(finalRegion)
              })
              layer = (NettyHttpClient.default >>> awsConfigWithStaticCreds >>> Sts.live)
                .leftZLayerToAppErr(err => CredentialsRetrievalError(new Exception(err.getMessage)))
              config <- AwsS3StorageConfig
                .fromAssumeRoleRequest(bucketName, objectKey, assumeRoleRequest, Some(finalRegion))
                .asSome
                .leftZIOToAppErr(err =>
                  CredentialsRetrievalError(new Exception(err.toThrowable.getMessage))
                )
                .provide(layer)
            } yield config
          case (None, None, None) =>
            AwsS3StorageConfig.withoutCredentials(bucketName, objectKey).asSome
          case _ =>
            ZIO.fail(
              ConfigError(
                new Exception(
                  "Basic credentials cannot be specified alongside assume role request. Please verify that only one option is configured and try again."
                )
              )
            )
        }
      case (None, None, None, None, None, None, Some(filePath)) =>
        ZIO.some(LocalStorageConfig(filePath))
      case (None, None, None, None, None, None, None) =>
        ZIO.none
      case _ =>
        ZIO.fail(
          ConfigError(
            new Exception(
              "Config for aws s3 storage found alongside config for local storage. Please ensure that only one storage solution is specified"
            )
          )
        )
    }

  def loadFileAsBytes: ZIO[Any, UnistoreError, Option[Array[Byte]]] =
    for {
      storageConf <- toStorageConfig
      clientOpt = storageConf.map {
        case finalConf: AwsS3StorageConfig => new AwsS3Client(finalConf)
        case finalConf: LocalStorageConfig => new LocalStorageClient(finalConf)
      }
      initialByteArrayOpt <- clientOpt
        .fold[ZIO[Any, UnistoreError, Option[Array[Byte]]]](ZIO.succeed(None))(_.loadFile.asSome)
      finalByteArrayOpt <-
        if (initialByteArrayOpt.isEmpty && localFallbackFilePath.isDefined) {
          for {
            fallbackPath <- ZIO
              .fromOption(localFallbackFilePath)
              .mapError(_ =>
                ConfigError(
                  new Exception(
                    "Expected fallback path to be defined when processing fallback option."
                  )
                )
              )
            client = new LocalStorageClient(LocalStorageConfig(fallbackPath))
            res <- client.loadFile.asSome.foldZIO(_ => ZIO.succeed(None), res => ZIO.succeed(res))
          } yield res
        } else ZIO.succeed(initialByteArrayOpt)
    } yield finalByteArrayOpt

  def loadFileAsString: ZIO[Any, UnistoreError, Option[String]] =
    for {
      fileOpt <- loadFileAsBytes
      res = fileOpt.map { fileBytes =>
        stringEncoding
          .map(new String(fileBytes, _))
          .getOrElse(new String(fileBytes))
      }
    } yield res
}
