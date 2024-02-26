# Unistore

## About 

Unistore is a client for a parsing single files in a cloud-agnostic manner. The main use case is when, as a developer, you want to provide your clients with the ability to store application-relevant files as they please, using any cloud provider of their choice.

## Example

The following example loads a text file from AWS:

```scala
import com.lumidion.unistore.UnistoreClient

import zio.{ZIO, ZIOAppDefault}

object MyApp extends ZIOAppDefault {

  def run = {
    val client = new UnistoreClient(Some("<bucket-name>"), Some("<key-name>"))

    for {
      str <- client.loadFileAsString
      _ <- ZIO.logInfo(str.getOrElse(""))
    } yield ()
  }

}
```