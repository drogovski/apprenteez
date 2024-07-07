package com.drogovski.apprenteez.fixtures

import cats.effect.IO
import com.drogovski.apprenteez.core.Users
import com.drogovski.apprenteez.domain.auth.*
import com.drogovski.apprenteez.domain.user.*

import com.drogovski.apprenteez.domain.auth

trait UserFixture {

  val mockedUsers: Users[IO] = new Users[IO] {
    override def find(email: String): IO[Option[User]] =
      if (email == danielEmail) IO.pure(Some(Daniel))
      else IO.pure(None)
    override def create(user: User): IO[String]       = IO.pure(user.email)
    override def update(user: User): IO[Option[User]] = IO.pure(Some(user))
    override def delete(email: String): IO[Boolean]   = IO.pure(true)
  }

  val Daniel = User(
    "daniel@rockthejvm.com",
    "$2a$10$j6Y44..0CQhQxqiGD6YXEOMARgxkuH1AlC58tDre7cWZGNv43zXl.",
    Some("Daniel"),
    Some("Ciocirlan"),
    Some("Rock the JVM"),
    Role.ADMIN
  )
  val danielEmail    = Daniel.email
  val danielPassword = "rockthejvm"

  val Riccardo = User(
    "riccardo@rockthejvm.com",
    "$2a$10$VaXKC4.KJZAvmn1/KfPe3Oek86.hgKpD/FdDbzZLzlMQBpi62HNfC",
    Some("Riccardo"),
    Some("Cardin"),
    Some("Rock the JVM"),
    Role.RECRUITER
  )
  val riccardoEmail    = Riccardo.email
  val riccardoPassword = "riccardorulez"

  val NewUser = User(
    "newuser@gmail.com",
    "$2a$10$HJLK.8OHv30/0FazuD4dvOWGspiivgy1xvP05F300LyM/d96eALTi",
    Some("John"),
    Some("Doe"),
    Some("Some company"),
    Role.RECRUITER
  )

  val UpdatedRiccardo = User(
    "riccardo@rockthejvm.com",
    "$2a$10$XfNpAxH32uBuGWhhYOlvV.v1R00U5CH1VEGwxcTEhJhf4Ji6itnXa",
    Some("RICCARDO"),
    Some("CARDIN"),
    Some("Adobe"),
    Role.RECRUITER
  )
}
