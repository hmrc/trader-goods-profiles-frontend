package base

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

trait WireMockServerSpec {

  implicit lazy val wireMock: WireMockServer = new WireMockServer(options().dynamicPort())

  val wireMockHost: String = "localhost"
  lazy val wireMockPort: Int = wireMock.port()
}
