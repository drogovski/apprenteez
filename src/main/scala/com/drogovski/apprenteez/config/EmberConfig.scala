package com.drogovski.apprenteez.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import com.comcast.ip4s.{Host, Port}
import pureconfig.error.CannotConvert

// generates given configReader: ConfigReader[EmberConfig]
final case class EmberConfig(host: Host, port: Port) derives ConfigReader

object EmberConfig {
  given hostReader: ConfigReader[Host] = ConfigReader[String].emap { hostString =>
    Host.fromString(hostString) match {
      case None =>
        Left(CannotConvert(hostString, Host.getClass.toString, s"Invalid host string: $hostString"))
      case Some(host) => 
        Right(host)
    }
  }
  
  given portReader: ConfigReader[Port] = ConfigReader[Int].emap { portInt =>
    Port.fromInt(portInt) match {
      case None => 
        Left(CannotConvert(portInt.toString, Port.getClass.toString, s"Invalid port format: $portInt"))
      case Some(port) => 
        Right(port)
    }
  }
}