package com.drogovski.apprenteez.domain

object pagination {
  final case class Pagination(limit: Int, offset: Int)

  object Pagination {
    val defaultPageSize = 20
    val defaultOffset   = 0

    def apply(maybeLimit: Option[Int], maybeOffset: Option[Int]) =
      new Pagination(maybeLimit.getOrElse(20), maybeOffset.getOrElse(0))

    def default =
      new Pagination(defaultPageSize, defaultOffset)
  }
}
