package com.drogovski.apprenteez


import cats.effect.IOApp
import org.http4s.ember.server.EmberServerBuilder
import com.drogovski.apprenteez.http.HttpApi
import cats.effect.IO
import pureconfig.ConfigSource
import com.drogovski.apprenteez.config.EmberConfig
import pureconfig.error.ConfigReaderException
import com.drogovski.apprenteez.config.syntax.loadF

object Application extends IOApp.Simple{
  
  val configSource = ConfigSource.default.load[EmberConfig]

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
