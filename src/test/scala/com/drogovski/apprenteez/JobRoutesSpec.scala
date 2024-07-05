package com.drogovski.apprenteez

import cats.effect.*
import cats.implicits.*
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger
import org.http4s.*
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.http4s.dsl.Http4sDsl
import java.util.UUID

import com.drogovski.apprenteez.fixtures.JobFixture
import com.drogovski.apprenteez.core.*
import com.drogovski.apprenteez.domain.job.*
import com.drogovski.apprenteez.http.routes.JobRoutes
import doobie.util.update.Update

class JobRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with JobFixture {
  val jobs: Jobs[IO] = new Jobs[IO] {
    override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] =
      IO.pure(NewJobUuid)
    override def all(): IO[List[Job]] =
      IO.pure(List(AwesomeJob))
    override def find(id: UUID): IO[Option[Job]] =
      if (id == AwesomeJob.id) IO.pure(Some(AwesomeJob)) else IO.pure(None)
    override def update(id: UUID, jobInfo: JobInfo): IO[Option[Job]] =
      if (id == AwesomeJob.id) IO.pure(Some(UpdatedAwesomeJob)) else IO.pure(None)
    override def delete(id: UUID): IO[Int] =
      if (id == AwesomeJob.id) IO.pure(1) else IO.pure(0)
  }

  given logger: Logger[IO]      = Slf4jLogger.getLogger[IO]
  val jobRoutes: HttpRoutes[IO] = JobRoutes[IO](jobs).routes

  //////////////////////////////////////////////////////////////////////////////////
  ///// TESTS
  //////////////////////////////////////////////////////////////////////////////////

  "JobRoutes" - {
    "should return a job with a given id" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.GET, uri = Uri.unsafeFromString(s"jobs/$AwesomeJobUuid"))
        )
        retrieved <- response.as[Job]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe AwesomeJob
      }
    }
    "should return all jobs" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs/")
        )
        retrieved <- response.as[List[Job]]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List(AwesomeJob)
      }
    }
    "should create a new job" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs/create")
            .withEntity(AwesomeJob.jobInfo)
        )
        retrieved <- response.as[UUID]
      } yield {
        response.status shouldBe Status.Created
        retrieved shouldBe NewJobUuid
      }
    }
    "should update a job" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(
            method = Method.PUT,
            uri = Uri.unsafeFromString(s"/jobs/$AwesomeJobUuid")
          ).withEntity(UpdatedAwesomeJob.jobInfo)
        )
        retrieved <- response.as[Job]
        responseInvalid <- jobRoutes.orNotFound.run(
          Request(
            method = Method.PUT,
            uri = Uri.unsafeFromString(s"/jobs/wronguuid")
          ).withEntity(UpdatedAwesomeJob.jobInfo)
        )
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe UpdatedAwesomeJob
        responseInvalid.status shouldBe Status.NotFound
      }
    }
    "should delete a job" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(
            method = Method.DELETE,
            uri = Uri.unsafeFromString(s"/jobs/$AwesomeJobUuid")
          )
        )
        responseInvalid <- jobRoutes.orNotFound.run(
          Request(
            method = Method.DELETE,
            uri = Uri.unsafeFromString(s"/jobs/wronguuid")
          )
        )
      } yield {
        response.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }
  }
}
