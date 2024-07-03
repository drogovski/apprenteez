package com.drogovski.apprenteez


import cats.effect.IOApp
import org.http4s.ember.server.EmberServerBuilder
import com.drogovski.apprenteez.http.HttpApi
import cats.effect.IO
import pureconfig.ConfigSource
import com.drogovski.apprenteez.config.EmberConfig
import pureconfig.error.ConfigReaderException
import com.drogovski.apprenteez.config.syntax.loadF
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Application extends IOApp.Simple{
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO] 
  override def run = ConfigSource.default.loadF[IO, EmberConfig].flatMap { config =>
    EmberServerBuilder
      .default[IO]
      .withHost(config.host)
      .withPort(config.port)
      .withHttpApp(HttpApi[IO].endpoits.orNotFound)
      .build
      .use(_ => IO.println("Server ready! Elooooo") *> IO.never)
 
  }
         
}
