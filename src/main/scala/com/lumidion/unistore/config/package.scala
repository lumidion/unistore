package com.lumidion.unistore

import com.lumidion.unistore.utils.Extensions.ZLayerOps.*

import zio.aws.core.AwsError
import zio.aws.s3.model.primitives.{BucketName, ObjectKey}
import zio.aws.sts.model.AssumeRoleRequest
import zio.aws.sts.Sts
import zio.aws.sts.Sts.assumeRole
import zio.ZIO

import software.amazon.awssdk.auth.credentials.{
  AwsBasicCredentials,
  AwsCredentialsProvider,
  StaticCredentialsProvider
}

package object config {
  sealed trait StorageConfig

  final case class AwsS3StorageConfig private (
      bucketName: BucketName,
      objectKey: ObjectKey,
      configuredCredentialsProvider: Option[AwsCredentialsProvider]
  ) extends StorageConfig

  object AwsS3StorageConfig {
    def fromAssumeRoleRequest(
        bucketName: String,
        objectKey: String,
        request: AssumeRoleRequest
    ): ZIO[Sts, AwsError, AwsS3StorageConfig] = for {
      res <- assumeRole(request)
      credentials <- res.getCredentials
      awsCredentials = AwsBasicCredentials.create(
        credentials.accessKeyId,
        credentials.secretAccessKey
      )
      provider = StaticCredentialsProvider.create(awsCredentials)
    } yield AwsS3StorageConfig(BucketName(bucketName), ObjectKey(objectKey), Some(provider))

    def fromBasicCredentials(
        bucketName: String,
        objectKey: String,
        accessKeyId: String,
        secretAccessKey: String
    ): ZIO[Any, Nothing, AwsS3StorageConfig] =
      ZIO.succeed(
        AwsS3StorageConfig(
          BucketName(bucketName),
          ObjectKey(objectKey),
          Some(
            StaticCredentialsProvider.create(
              AwsBasicCredentials.create(accessKeyId, secretAccessKey)
            )
          )
        )
      )

    def withoutCredentials(
        bucketName: String,
        objectKey: String
    ): ZIO[Any, Nothing, AwsS3StorageConfig] =
      ZIO.succeed(AwsS3StorageConfig(BucketName(bucketName), ObjectKey(objectKey), None))
  }

  final case class LocalStorageConfig(filePath: String) extends StorageConfig
}
