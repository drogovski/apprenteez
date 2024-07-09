package com.drogovski.apprenteez.core

import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger
import org.scalatest.freespec.AsyncFreeSpec
import cats.data.OptionT
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import scala.concurrent.duration.DurationInt
import tsec.mac.jca.HMACSHA256
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import com.drogovski.apprenteez.fixtures.UserFixture
import com.drogovski.apprenteez.domain.security.Authenticator
import com.drogovski.apprenteez.domain.user.*
import com.drogovski.apprenteez.domain.auth.NewPasswordInfo
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash
import com.drogovski.apprenteez.config.SecurityConfig

class AuthSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with UserFixture {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val mockedSecurityConfig = SecurityConfig("secret", 1.day)

  "Auth 'algebra'" - {
    "login should return None if the user does not exist" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers)(mockedSecurityConfig)
        maybeToken <- auth.login("nonexisting@email.com", "password")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return None if the user exists but the password is wrong" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers)(mockedSecurityConfig)
        maybeToken <- auth.login(danielEmail, "password")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return a token if the user exists and the password is correct" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers)(mockedSecurityConfig)
        maybeToken <- auth.login(danielEmail, "rockthejvm")
      } yield maybeToken

      program.asserting(_ shouldBe defined)
    }

    "signing up should not create a user with an existing email" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers)(mockedSecurityConfig)
        maybeUser <- auth.signUp(
          NewUserInfo(
            danielEmail,
            "somePass",
            Some("Daniel"),
            Some("Whatever"),
            Some("Other Company")
          )
        )
      } yield maybeUser

      program.asserting(_ shouldBe None)
    }

    "signing up should create a completely new user" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers)(mockedSecurityConfig)
        maybeUser <- auth.signUp(
          NewUserInfo(
            "bibek@beebeck.com",
            "strongPass",
            Some("Bibek"),
            Some("Whatever"),
            Some("Beebeck")
          )
        )
      } yield maybeUser

      program.asserting {
        case Some(user) =>
          user.email shouldBe "bibek@beebeck.com"
          user.firstName shouldBe Some("Bibek")
          user.lastName shouldBe Some("Whatever")
          user.company shouldBe Some("Beebeck")
          user.role shouldBe Role.RECRUITER
        case _ => fail()

      }
    }

    "changePassword should return Right(None) if the user doesn't exist" in {
      val program = for {
        auth   <- LiveAuth[IO](mockedUsers)(mockedSecurityConfig)
        result <- auth.changePassword("fabb@fabzone.com", NewPasswordInfo("oldpass", "newpass"))
      } yield result

      program.asserting(_ shouldBe Right(None))
    }

    "changePassword should return Left with an error if the user exists and the password is incorrect" in {
      val program = for {
        auth   <- LiveAuth[IO](mockedUsers)(mockedSecurityConfig)
        result <- auth.changePassword(danielEmail, NewPasswordInfo("oldpass", "newpass"))
      } yield result

      program.asserting(_ shouldBe Left("Invalid password"))
    }

    "changePassword should succeed when all credentials are correct" in {
      val program = for {
        auth   <- LiveAuth[IO](mockedUsers)(mockedSecurityConfig)
        result <- auth.changePassword(danielEmail, NewPasswordInfo("rockthejvm", "scalarocks"))
        isNicePassword <- result match {
          case Right(Some(user)) =>
            BCrypt
              .checkpwBool[IO](
                "scalarocks",
                PasswordHash[BCrypt](user.hashedPassword)
              )
          case _ =>
            IO.pure(false)
        }
      } yield isNicePassword

      program.asserting(_ shouldBe true)
    }
  }
}
