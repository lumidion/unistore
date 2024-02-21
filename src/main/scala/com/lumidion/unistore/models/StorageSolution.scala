package com.lumidion.unistore.models

enum StorageSolution(private val id: String) {
  case AWS_S3 extends StorageSolution("aws_s3")
  case LOCAL extends StorageSolution("local")
}

object StorageSolution {
  private val codeMap: Map[String, StorageSolution] =
    StorageSolution.values.map(solution => (solution.id, solution)).toMap

  def parseFromString(id: String): Option[StorageSolution] = codeMap.get(id)
}
