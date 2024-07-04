package com.drogovski.apprenteez.modules

import com.drogovski.apprenteez.config.PostgresConfig
import cats.effect.kernel.{Async, Resource}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

object Database {
  def makePostgresResource[F[_]: Async](config: PostgresConfig): Resource[F, HikariTransactor[F]] =
    for {
      ec <- ExecutionContexts.fixedThreadPool(config.nThreads)
      xa <- HikariTransactor.newHikariTransactor[F](
        "org.postgresql.Driver",
        config.url,
        config.user,
        config.password,
        ec
      )
    } yield xa
}
