package com.drogovski.apprenteez.http.routes

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
import org.typelevel.ci.CIStringSyntax
import tsec.mac.jca.HMACSHA256
import tsec.authentication.IdentityStore
import cats.data.OptionT
import tsec.authentication.JWTAuthenticator
import scala.concurrent.duration.DurationInt

import com.drogovski.apprenteez.core.*
import com.drogovski.apprenteez.domain.auth.*
import com.drogovski.apprenteez.domain.security.*
import com.drogovski.apprenteez.domain.user.*
import com.drogovski.apprenteez.fixtures.UserFixture
import org.http4s.headers.Authorization
import tsec.jws.mac.JWTMac

class AuthRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with UserFixture {

  //////////////////////////////////////////////////////////////////////////////////
  ///// PREP
  //////////////////////////////////////////////////////////////////////////////////
  val mockedAuth: Auth[IO] = new Auth[IO] {
    def login(email: String, password: String): IO[Option[JwtToken]] =
      if (email == danielEmail && password == danielPassword)
        mockedAuthenticator.create(danielEmail).map(Some(_))
      else None.pure[IO]
    def signUp(newUserInfo: NewUserInfo): IO[Option[User]] =
      if (newUserInfo.email == riccardoEmail)
        IO.pure(Some(Riccardo))
      else IO.pure(None)
    def changePassword(
        email: String,
        newPasswordInfo: NewPasswordInfo
    ): IO[Either[String, Option[User]]] =
      if (email == danielEmail)
        if (newPasswordInfo.oldPassword == danielPassword)
          IO.pure(Right(Some(Daniel)))
        else
          IO.pure(Left("Invalid password"))
      else
        IO.pure(Right(None))

    def delete(email: String): IO[Boolean] = IO.pure(true)

    def authenticator: Authenticator[IO] = mockedAuthenticator
  }

  val mockedAuthenticator: Authenticator[IO] = {
    val key = HMACSHA256.unsafeGenerateKey
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == danielEmail) OptionT.pure(Daniel)
      else if (email == riccardoEmail) OptionT.pure(Riccardo)
      else OptionT.none[IO, User]

    JWTAuthenticator.unbacked.inBearerToken(
      1.day,
      None,
      idStore,
      key
    )
  }

  extension (r: Request[IO])
    def withBearerToken(a: JwtToken): Request[IO] =
      r.putHeaders {
        val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](a.jwt)
        Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))
      }

  given logger: Logger[IO]       = Slf4jLogger.getLogger[IO]
  val authRoutes: HttpRoutes[IO] = AuthRoutes[IO](mockedAuth).routes

  //////////////////////////////////////////////////////////////////////////////////
  ///// TESTS
  //////////////////////////////////////////////////////////////////////////////////

  "AuthRoutes" - {
    "should return a 401 - unauthorized if login fails" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(danielEmail, "wrongpass"))
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }
    "should return a 200 - OK + a JWT token if login is successful" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(danielEmail, danielPassword))
        )
      } yield {
        response.status shouldBe Status.Ok
        response.headers.get(ci"Authorization") shouldBe defined
      }
    }
    "should return a 400 - Bad Request if the user to create already exists" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewUserDaniel)
        )
      } yield {
        response.status shouldBe Status.BadRequest
      }
    }
    "should return a 201 - Created if the user to create already exists" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewUserRiccardo)
        )
      } yield {
        response.status shouldBe Status.Created
      }
    }
    "should return a 200 - OK if logging out with a valid JWT token" in {
      for {
        jwtToken <- mockedAuthenticator.create(danielEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout").withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }
    "should return a 401 - Unauthorized if logging out without a valid JWT token" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }
    "should return a 404 - Notfound if changing password for user that does not exist" in {
      for {
        jwtToken <- mockedAuthenticator.create(riccardoEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(riccardoPassword, "newpass"))
        )
      } yield {
        response.status shouldBe Status.NotFound
      }
    }
    "should return a 403 - Forbidden if old password is incorrect" in {
      for {
        jwtToken <- mockedAuthenticator.create(danielEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo("wrongpass", "newpass"))
        )
      } yield {
        response.status shouldBe Status.Forbidden
      }
    }
    "should return a 401 - Unauthorized if changing password without a JWT" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withEntity(NewPasswordInfo("wrongpass", "newpass"))
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }
    "should return a 200 - Ok if changing password with existing user, valid JWT and password" in {
      for {
        jwtToken <- mockedAuthenticator.create(danielEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(danielPassword, "newpass"))
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }
    "should return a 401 - Unauthorized if a non-admin tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(riccardoEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/daniel@rockthejvm.com")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }
    "should return a 200 - Ok when an admin tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(danielEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/riccardo@rockthejvm.com")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }
  }
}
