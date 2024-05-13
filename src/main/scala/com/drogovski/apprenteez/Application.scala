package com.drogovski.apprenteez


import cats.effect.IOApp
import org.http4s.ember.server.EmberServerBuilder
import com.drogovski.apprenteez.http.routes.HealthRoutes
import cats.effect.IO

object Application extends IOApp.Simple{
 

  override def run = EmberServerBuilder
    .default[IO]
    .withHttpApp(HealthRoutes[IO].routes.orNotFound)
    .build
    .use(_ => IO.println("Server ready! Elooooo") *> IO.never)
}
