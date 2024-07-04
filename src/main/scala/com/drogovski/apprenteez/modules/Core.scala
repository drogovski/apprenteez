package com.drogovski.apprenteez.modules

import cats.effect.*
import cats.implicits.*
import com.drogovski.apprenteez.core.Jobs
import com.drogovski.apprenteez.core.LiveJobs
import doobie.util.transactor.Transactor

final class Core[F[_]] private (val jobs: Jobs[F]) {}

// postgres -> jobs -> core -> httpApi
object Core {

  def apply[F[_]: Async](xa: Transactor[F]): Resource[F, Core[F]] =
    Resource
      .eval(LiveJobs[F](xa))
      .map(jobs => new Core(jobs))
}
