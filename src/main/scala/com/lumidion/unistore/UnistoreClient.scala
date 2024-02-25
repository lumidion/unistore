package com.lumidion.unistore

import com.lumidion.unistore.clients.{AwsS3Client, LocalStorageClient}
import com.lumidion.unistore.config.{AwsS3StorageConfig, LocalStorageConfig, StorageConfig}
import com.lumidion.unistore.models.errors.{ConfigError, CredentialsRetrievalError, UnistoreError}
import com.lumidion.unistore.utils.Extensions.ZIOOps.*
import com.lumidion.unistore.utils.Extensions.ZLayerOps.*

import zio.aws.core.config.AwsConfig
import zio.aws.netty.NettyHttpClient
import zio.aws.sts.model.AssumeRoleRequest
import zio.aws.sts.Sts
import zio.ZIO

class UnistoreClient(
    awsS3BucketName: Option[String] = None,
    awsS3ObjectKey: Option[String] = None,
    awsAccessKeyId: Option[String] = None,
    awsSecretAccessKey: Option[String] = None,
    awsIamAssumeRoleRequest: Option[AssumeRoleRequest] = None,
    localFilePath: Option[String] = None,
    stringEncoding: Option[String] = None
) {
  private def toStorageConfig: ZIO[Any, UnistoreError, Option[StorageConfig]] =
    (
      awsS3BucketName,
      awsS3ObjectKey,
      awsAccessKeyId,
      awsSecretAccessKey,
      awsIamAssumeRoleRequest,
      localFilePath
    ) match {
      case (Some(bucketName), Some(objectKey), _, _, _, None) =>
        (awsAccessKeyId, awsSecretAccessKey, awsIamAssumeRoleRequest) match {
          case (Some(accessKey), Some(secretAccessKey), None) =>
            AwsS3StorageConfig
              .fromBasicCredentials(
                bucketName,
                objectKey,
                accessKey,
                secretAccessKey
              )
              .asSome

          case (None, None, Some(assumeRoleRequest)) =>
            val layer = (NettyHttpClient.default >>> AwsConfig.default >>> Sts.live)
              .leftZLayerToAppErr(err => CredentialsRetrievalError(new Exception(err.getMessage)))
            AwsS3StorageConfig
              .fromAssumeRoleRequest(bucketName, objectKey, assumeRoleRequest)
              .asSome
              .leftZIOToAppErr(err =>
                CredentialsRetrievalError(new Exception(err.toThrowable.getMessage))
              )
              .provide(layer)
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
      case (None, None, None, None, None, Some(filePath)) =>
        ZIO.some(LocalStorageConfig(filePath))
      case (None, None, None, None, None, None) =>
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
      byteArrayOpt <- clientOpt.fold(ZIO.succeed(None))(_.loadFile.asSome)
    } yield byteArrayOpt

  def loadFileAsString: ZIO[Any, UnistoreError, Option[String]] =
    for {
      fileOpt <- loadFileAsBytes
      res = fileOpt.map { fileBytes =>
        stringEncoding
          .map(new String(fileBytes, _))
          .getOrElse(String(fileBytes))
      }
    } yield res
}