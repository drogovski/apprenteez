package com.drogovski.apprenteez.fixtures

import cats.effect.IO
import com.drogovski.apprenteez.core.Users
// import com.drogovski.apprenteez.domain.auth.*
import com.drogovski.apprenteez.domain.user.*

// import com.drogovski.apprenteez.domain.auth

trait UserFixture {

  // val mockedUsers: Users[IO] = new Users[IO] {
  //   override def find(email: String): IO[Option[User]] =
  //     if (email == danielEmail) IO.pure(Some(Daniel))
  //     else IO.pure(None)
  //   override def create(user: User): IO[String]       = IO.pure(user.email)
  //   override def update(user: User): IO[Option[User]] = IO.pure(Some(user))
  //   override def delete(email: String): IO[Boolean]   = IO.pure(true)
  // }

  val Daniel = User(
    "daniel@rockthejvm.com",
    "rockthejvm",
    Some("Daniel"),
    Some("Ciocirlan"),
    Some("Rock the JVM"),
    Role.ADMIN
  )
  val danielEmail    = Daniel.email
  val danielPassword = "rockthejvm"

  val Riccardo = User(
    "riccardo@rockthejvm.com",
    "riccardorulez",
    Some("Riccardo"),
    Some("Cardin"),
    Some("Rock the JVM"),
    Role.RECRUITER
  )
  val riccardoEmail    = Riccardo.email
  val riccardoPassword = "riccardorulez"

  val NewUser = User(
    "newuser@gmail.com",
    "$2a$10$6LQt4xy4LzqQihZiRZGG0eeeDwDCvyvthICXzPKQDQA3C47LtrQFy",
    Some("John"),
    Some("Doe"),
    Some("Some company"),
    Role.RECRUITER
  )

  val UpdatedRiccardo = User(
    "riccardo@rockthejvm.com",
    "$2a$10$PUD6CznGVHntJFsOOeV4NezBgBUs6irV3sC9fa6ufc0xp9VLYyHZ.",
    Some("RICCARDO"),
    Some("CARDIN"),
    Some("Adobe"),
    Role.RECRUITER
  )
}
