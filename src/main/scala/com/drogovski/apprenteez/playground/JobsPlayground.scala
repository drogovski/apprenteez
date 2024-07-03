package com.drogovski.apprenteez.playground

import cats.effect.*
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.util.*
import com.drogovski.apprenteez.domain.job.*
import com.drogovski.apprenteez.core.LiveJobs
import scala.io.StdIn

object JobsPlayground extends IOApp.Simple {
  val postgresResource: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool(16)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:apprenteez",
      "docker",
      "docker",
      ec
    )
  } yield xa

  val jobInfo = JobInfo.minimal(
    company = "Bibek",
    title = "Software Engineer",
    description = "Bibek job",
    externalUrl = "beebeck.com",
    remote = false,
    location = "Remote Worldwide"
  )

  override def run: IO[Unit] = postgresResource.use { xa =>
    for {
      jobs      <- LiveJobs[IO](xa)
      _         <- IO(println("Ready. Next...")) *> IO(StdIn.readLine)
      id        <- jobs.create("bibek@beebeck.com", jobInfo)
      _         <- IO(println("Created new job...")) *> IO(StdIn.readLine)
      list      <- jobs.all()
      _         <- IO(println(s"All jobs: $list. Next...")) *> IO(StdIn.readLine)
      _         <- jobs.update(id, jobInfo.copy(title = "ADHD Engineer"))
      newJob    <- jobs.find(id)
      _         <- IO(println(s"Updated job: $newJob. Next...")) *> IO(StdIn.readLine)
      _         <- jobs.delete(id)
      listAfter <- jobs.all()
      _         <- IO(println(s"Deleted job. List now: $listAfter. Next...")) *> IO(StdIn.readLine)
    } yield ()

  }
}
