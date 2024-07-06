package com.drogovski.apprenteez.core

import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.Inside
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import doobie.implicits.*
import com.drogovski.apprenteez.fixtures.UserFixture
import org.postgresql.util.PSQLException
import com.drogovski.apprenteez.domain.user.User

class UsersSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Inside
    with Matchers
    with UserFixture
    with DbSpec {
  override val initScript: String = "sql/users.sql"
  given logger: Logger[IO]        = Slf4jLogger.getLogger[IO]

  "Users 'algebra'" - {
    "should retrieve a user by email" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          maybeUser <- users.find(Daniel.email)
        } yield maybeUser

        program.asserting(_ shouldBe Some(Daniel))
      }
    }
    "should return None when the user with provided email does not exist" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          maybeUser <- users.find("example@email.com")
        } yield maybeUser

        program.asserting(_ shouldBe None)
      }
    }
    "should create a new user" in {
      transactor.use { xa =>
        val program = for {
          users  <- LiveUsers[IO](xa)
          userId <- users.create(NewUser)
        } yield userId

        program.asserting(_ shouldBe NewUser.email)
      }
    }
    "should fail to create a new user when email already exists in db" in {
      transactor.use { xa =>
        val program = for {
          users  <- LiveUsers[IO](xa)
          userId <- users.create(Daniel).attempt
        } yield userId

        program.asserting(outcome =>
          inside(outcome) {
            case Left(e) =>
              e shouldBe a[PSQLException]
            case _ => fail()

          }
        )
      }
    }
    "should update the user" in {
      transactor.use { xa =>
        val program = for {
          users       <- LiveUsers[IO](xa)
          updatedUser <- users.update(UpdatedRiccardo)
        } yield updatedUser

        program.asserting(_ shouldBe Some(UpdatedRiccardo))
      }
    }
    "should return None when updating the user that does not exist" in {
      transactor.use { xa =>
        val program = for {
          users       <- LiveUsers[IO](xa)
          updatedUser <- users.update(NewUser)
        } yield updatedUser

        program.asserting(_ shouldBe None)
      }
    }
    "should delete a user" in {
      transactor.use { xa =>
        val program = for {
          users  <- LiveUsers[IO](xa)
          result <- users.delete(Daniel.email)
          maybeUser <- sql"SELECT * FROM users WHERE email = ${Daniel.email}"
            .query[User]
            .option
            .transact(xa)
        } yield (result, maybeUser)

        program.asserting { case (result, maybeUser) =>
          result shouldBe true
          maybeUser shouldBe None
        }
      }
    }
    "should not delete a user that does not exist" in {
      transactor.use { xa =>
        val program = for {
          users  <- LiveUsers[IO](xa)
          result <- users.delete("nonexistinguser@email.com")
        } yield result

        program.asserting(_ shouldBe false)
      }

    }
  }
}
