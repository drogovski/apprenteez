package com.drogovski.apprenteez.domain

import tsec.authentication.{AugmentedJWT, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256
import com.drogovski.apprenteez.domain.user.User

object security {
  type CryptoAlg           = HMACSHA256
  type JwtToken            = AugmentedJWT[CryptoAlg, String]
  type Authenticator[F[_]] = JWTAuthenticator[F, String, User, CryptoAlg]
}
