package com.drogovski.apprenteez.core

import cats.data.OptionT
import cats.implicits.*
import cats.effect.*
import cats.effect.implicits.*
import tsec.authentication.IdentityStore
import tsec.authentication.BackingStore
import tsec.authentication.JWTAuthenticator
import tsec.common.SecureRandomId
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash
import org.typelevel.log4cats.Logger
import scala.concurrent.duration.DurationInt
import com.drogovski.apprenteez.domain.auth.*
import com.drogovski.apprenteez.domain.security.*
import com.drogovski.apprenteez.domain.user.*
import com.drogovski.apprenteez.config.SecurityConfig

trait Auth[F[_]] {
  def login(email: String, password: String): F[Option[JwtToken]]
  def signUp(newUserInfo: NewUserInfo): F[Option[User]]
  def changePassword(
      email: String,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]]
  def delete(email: String): F[Boolean]

  def authenticator: Authenticator[F]
}

class LiveAuth[F[_]: Async: Logger] private (
    users: Users[F],
    override val authenticator: Authenticator[F]
) extends Auth[F] {
  override def login(email: String, password: String): F[Option[JwtToken]] =
    for {
      maybeUser <- users.find(email)
      maybeValidatedUser <- maybeUser.filterA(user =>
        BCrypt.checkpwBool[F](
          password,
          PasswordHash[BCrypt](user.hashedPassword)
        )
      )
      maybeJwtToken <- maybeValidatedUser.traverse(user => authenticator.create(user.email))
    } yield maybeJwtToken
  override def signUp(newUserInfo: NewUserInfo): F[Option[User]] =
    users.find(newUserInfo.email).flatMap {
      case Some(_) => None.pure[F]
      case None =>
        for {
          hashedPassword <- BCrypt.hashpw[F](newUserInfo.password)
          user <- User(
            newUserInfo.email,
            hashedPassword,
            newUserInfo.firstName,
            newUserInfo.lastName,
            newUserInfo.company,
            Role.RECRUITER
          ).pure[F]
          _ <- users.create(user)
        } yield Some(user)
    }
  override def changePassword(
      email: String,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]] = {

    def checkAndUpdate(user: User, oldPass: String, newPass: String) =
      for {
        passCheck <- BCrypt.checkpwBool[F](
          newPasswordInfo.oldPassword,
          PasswordHash[BCrypt](user.hashedPassword)
        )
        passChangeResult <-
          if (passCheck) {
            updateUser(user, newPass).map(Right(_))
          } else {
            Left("Invalid password").pure[F]
          }
      } yield passChangeResult

    def updateUser(user: User, newPassword: String) =
      for {
        newHashedPassword <- BCrypt.hashpw[F](newPassword)
        updatedUser       <- users.update(user.copy(hashedPassword = newHashedPassword))
      } yield updatedUser

    val NewPasswordInfo(oldPassword, newPassword) = newPasswordInfo

    users.find(email).flatMap {
      case None       => Right(None).pure[F]
      case Some(user) => checkAndUpdate(user, oldPassword, newPassword)
    }
  }
  override def delete(email: String): F[Boolean] =
    users.delete(email)
}

object LiveAuth {
  def apply[F[_]: Async: Logger](users: Users[F])(securityConfig: SecurityConfig) =

    // 1 identity store
    val idStore: IdentityStore[F, String, User] = (email: String) => OptionT(users.find(email))

    // 2 bakcing store for JWT Tokens: BackingStore[F, id, JwtToken]
    // todo
    val tokenStoreF = Ref.of[F, Map[SecureRandomId, JwtToken]](Map.empty).map { ref =>
      new BackingStore[F, SecureRandomId, JwtToken] {
        override def get(id: SecureRandomId): OptionT[F, JwtToken] = OptionT(ref.get.map(_.get(id)))
        override def put(elem: JwtToken): F[JwtToken] =
          ref.modify(store => (store + (elem.id -> elem), elem))
        override def update(v: JwtToken): F[JwtToken]    = put(v)
        override def delete(id: SecureRandomId): F[Unit] = ref.modify(store => (store - id, ()))
      }
    }
    // 3 hashing key
    val keyF = HMACSHA256.buildKey[F](securityConfig.secret.getBytes("UTF-8"))

    for {
      key        <- keyF
      tokenStore <- tokenStoreF
      // 4 authenticator
      authenticator = JWTAuthenticator.backed.inBearerToken(
        expiryDuration = securityConfig.jwtExpiryDuration,
        maxIdle = None,
        identityStore = idStore,
        tokenStore = tokenStore,
        signingKey = key
      )
    } yield new LiveAuth[F](users, authenticator) // 5 live Auth
}
