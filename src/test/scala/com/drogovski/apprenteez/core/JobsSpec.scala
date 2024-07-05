package com.drogovski.apprenteez.core

import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.drogovski.apprenteez.fixtures.JobFixture
import com.drogovski.apprenteez.domain.job.Job
import com.drogovski.apprenteez.domain.job.JobFilter
import com.drogovski.apprenteez.domain.pagination.Pagination

class JobsSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with JobFixture with DbSpec {
  val initScript: String   = "sql/jobs.sql"
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Jobs 'algebra'" - {
    "should return no job if the given UUID does not exist" in {
      transactor.use { xa =>
        val program = for {
          jobs      <- LiveJobs[IO](xa)
          retrieved <- jobs.find(NotFoundJobUuid)
        } yield retrieved

        program.asserting(_ shouldBe None)
      }
    }
    "should return job that exist in database" in {
      transactor.use { xa =>
        val program = for {
          jobs      <- LiveJobs[IO](xa)
          retrieved <- jobs.find(AwesomeJobUuid)
        } yield retrieved

        program.asserting(_ shouldBe Some(AwesomeJob))
      }
    }
    "should retrieve all jobs in database" in {
      transactor.use { xa =>
        val program = for {
          jobs      <- LiveJobs[IO](xa)
          retrieved <- jobs.all()
        } yield retrieved

        program.asserting(_ shouldBe List(AwesomeJob))
      }
    }
    "should create new job in database" in {
      transactor.use { xa =>
        val program = for {
          jobs     <- LiveJobs[IO](xa)
          newJobId <- jobs.create("owner@email.com", RockTheJvmNewJob)
          maybeJob <- jobs.find(newJobId)
        } yield maybeJob

        program.asserting { _.map(_.jobInfo) shouldBe Some(RockTheJvmNewJob) }
      }
    }
    "should update job in database" in {
      transactor.use { xa =>
        val program = for {
          jobs       <- LiveJobs[IO](xa)
          updatedJob <- jobs.update(AwesomeJobUuid, UpdatedAwesomeJob.jobInfo)
        } yield updatedJob
        program.asserting { _.map(_.jobInfo) shouldBe Some(UpdatedAwesomeJob.jobInfo) }
      }
    }
    "should return None when trying to update a job that does not exist" in {
      transactor.use { xa =>
        val program = for {
          jobs     <- LiveJobs[IO](xa)
          maybeJob <- jobs.update(NotFoundJobUuid, UpdatedAwesomeJob.jobInfo)
        } yield maybeJob
        program.asserting { _ shouldBe None }
      }
    }
    "should delete job in database" in {
      transactor.use { xa =>
        val program = for {
          jobs                <- LiveJobs[IO](xa)
          numberOfDeletedJobs <- jobs.delete(AwesomeJobUuid)
          retrievedJobs       <- jobs.all()
        } yield (numberOfDeletedJobs, retrievedJobs)
        program.asserting { case (numberOfDeletedJobs, retrievedJobs) =>
          numberOfDeletedJobs shouldBe 1
          retrievedJobs shouldBe List.empty[Job]
        }
      }
    }
    "should not delete none existing job in database" in {
      transactor.use { xa =>
        val program = for {
          jobs                <- LiveJobs[IO](xa)
          numberOfDeletedJobs <- jobs.delete(NotFoundJobUuid)
          retrievedJobs       <- jobs.all()
        } yield (numberOfDeletedJobs, retrievedJobs)
        program.asserting { case (numberOfDeletedJobs, retrievedJobs) =>
          numberOfDeletedJobs shouldBe 0
          retrievedJobs shouldBe List(AwesomeJob)
        }
      }
    }
    "should filter remote jobs" in {
      transactor.use { xa =>
        val program = for {
          jobs         <- LiveJobs[IO](xa)
          filteredJobs <- jobs.all(JobFilter(remote = true), Pagination.default)
        } yield filteredJobs
        program.asserting { _ shouldBe List() }
      }
    }
    "should filter jobs by tags" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          filteredJobs <- jobs.all(
            JobFilter(tags = List("scala", "cats", "zio")),
            Pagination.default
          )
        } yield filteredJobs
        program.asserting { _ shouldBe List(AwesomeJob) }
      }
    }
  }
}
