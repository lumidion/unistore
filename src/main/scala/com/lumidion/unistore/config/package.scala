package com.lumidion.unistore

import zio.aws.core.AwsError
import zio.aws.s3.model.primitives.{BucketName, ObjectKey}
import zio.aws.sts.model.AssumeRoleRequest
import zio.aws.sts.Sts
import zio.aws.sts.Sts.assumeRole
import zio.ZIO

import software.amazon.awssdk.auth.credentials.{
  AwsBasicCredentials,
  AwsCredentialsProvider,
  AwsSessionCredentials,
  StaticCredentialsProvider
}
import software.amazon.awssdk.regions.Region

package object config {
  sealed trait StorageConfig

  final case class AwsS3StorageConfig(
      bucketName: BucketName,
      objectKey: ObjectKey,
      region: Option[Region],
      configuredCredentialsProvider: Option[AwsCredentialsProvider]
  ) extends StorageConfig

  object AwsS3StorageConfig {
    def fromAssumeRoleRequest(
        bucketName: String,
        objectKey: String,
        request: AssumeRoleRequest,
        region: Option[Region]
    ): ZIO[Sts, AwsError, AwsS3StorageConfig] = for {
      res <- assumeRole(request)
      credentials <- res.getCredentials
      awsCredentials = AwsSessionCredentials.create(
        credentials.accessKeyId,
        credentials.secretAccessKey,
        credentials.sessionToken
      )
      provider = StaticCredentialsProvider.create(awsCredentials)
    } yield AwsS3StorageConfig(BucketName(bucketName), ObjectKey(objectKey), region, Some(provider))

    def fromBasicCredentials(
        bucketName: String,
        objectKey: String,
        accessKeyId: String,
        secretAccessKey: String,
        region: Option[Region]
    ): ZIO[Any, Nothing, AwsS3StorageConfig] =
      ZIO.succeed(
        AwsS3StorageConfig(
          BucketName(bucketName),
          ObjectKey(objectKey),
          region,
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
      ZIO.succeed(AwsS3StorageConfig(BucketName(bucketName), ObjectKey(objectKey), None, None))
  }

  final case class LocalStorageConfig(filePath: String) extends StorageConfig
}
