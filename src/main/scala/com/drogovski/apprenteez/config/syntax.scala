package com.drogovski.apprenteez.config

import pureconfig.ConfigSource
import pureconfig.ConfigReader
import cats.implicits.*
import cats.MonadThrow
import scala.reflect.ClassTag
import pureconfig.error.ConfigReaderException

object syntax {
  extension (source: ConfigSource)
    def loadF[F[_], A](using reader: ConfigReader[A], F: MonadThrow[F], tag: ClassTag[A]): F[A] =
      F.pure(source.load[A]).flatMap {
          case Left(errors) => F.raiseError[A](ConfigReaderException(errors))
          case Right(value) => F.pure(value)
      }  
      
}
