package com.drogovski.apprenteez

import cats.effect.IOApp
import org.http4s.ember.server.EmberServerBuilder
import cats.effect.IO
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import com.drogovski.apprenteez.config.AppConfig
import com.drogovski.apprenteez.config.syntax.loadF
import com.drogovski.apprenteez.modules.*
import cats.effect.IOLocal

object Application extends IOApp.Simple {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  override def run = ConfigSource.default.loadF[IO, AppConfig].flatMap {
    case AppConfig(postgresConfig, emberConfig) =>
      val appResource = for {
        xa      <- Database.makePostgresResource[IO](postgresConfig)
        core    <- Core[IO](xa)
        httpApi <- HttpApi[IO](core)
        server <- EmberServerBuilder
          .default[IO]
          .withHost(emberConfig.host)
          .withPort(emberConfig.port)
          .withHttpApp(httpApi.endpoits.orNotFound)
          .build
      } yield server

      appResource.use(_ => IO.println("Server ready! Elooooo") *> IO.never)

  }

}
