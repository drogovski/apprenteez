package com.drogovski.apprenteez.domain

import cats.*
import cats.implicits.*
import org.http4s.Response
import tsec.authentication.{AugmentedJWT, JWTAuthenticator, SecuredRequest}
import tsec.mac.jca.HMACSHA256
import com.drogovski.apprenteez.domain.user.*
import tsec.authorization.BasicRBAC
import doobie.util.Get.Basic
import tsec.authorization.*
import tsec.authentication.TSecAuthService
import org.http4s.Status

object security {
  type CryptoAlg           = HMACSHA256
  type JwtToken            = AugmentedJWT[CryptoAlg, String]
  type Authenticator[F[_]] = JWTAuthenticator[F, String, User, CryptoAlg]
  type AuthRoute[F[_]]     = PartialFunction[SecuredRequest[F, User, JwtToken], F[Response[F]]]
  type AuthRBAC[F[_]]      = BasicRBAC[F, Role, User, JwtToken]

  // RBAC
  // BasicRBAC[F, Role, User, JwtToken]
  given authRole[F[_]: MonadThrow]: AuthorizationInfo[F, Role, User] with {
    override def fetchInfo(u: User): F[Role] = u.role.pure[F]
  }
  def allRoles[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC.all[F, Role, User, JwtToken]

  def adminOnly[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC(Role.ADMIN)

  def rectruiterOnly[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC(Role.RECRUITER)

  case class Authorizations[F[_]](rbacRoutes: Map[AuthRBAC[F], List[AuthRoute[F]]])

  object Authorizations {
    given combiner[F[_]]: Semigroup[Authorizations[F]] = Semigroup.instance[Authorizations[F]] {
      (authA, authB) =>
        Authorizations(authA.rbacRoutes |+| authB.rbacRoutes)
    }
  }

  // AuthRoute -> Authorizations -> TSecAuthService -> HttpRoute

  extension [F[_]](authRoute: AuthRoute[F])
    def restrictedTo(rbac: AuthRBAC[F]): Authorizations[F] =
      Authorizations(Map(rbac -> List(authRoute)))

  given auth2tsec[F[_]: MonadThrow]
      : Conversion[Authorizations[F], TSecAuthService[User, JwtToken, F]] =
    authz => {
      // this responds with 401 always
      val unauthorizedService: TSecAuthService[User, JwtToken, F] =
        TSecAuthService[User, JwtToken, F] { _ =>
          Response[F](Status.Unauthorized).pure[F]
        }

      // val rbac: AuthRBAC[F]       = ???
      // val authRoute: AuthRoute[F] = ???
      // val tsec = TSecAuthService.withAuthorizationHandler(rbac)(authRoute, unauthorizedService)

      authz.rbacRoutes // map [RBAC, List[AuthRoute[F]]]
        .toSeq
        .foldLeft(unauthorizedService) { case (acc, (rbac, routes)) =>
          // merge routes into one
          val bigRoute = routes.reduce(_.orElse(_))
          // build a new service, fall back to the acc if rbac/route fails
          TSecAuthService.withAuthorizationHandler(rbac)(bigRoute, acc.run)
        }
    }
}
