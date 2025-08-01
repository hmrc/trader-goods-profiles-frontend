/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config

import com.google.inject.{Inject, Singleton}
import models.EnrolmentConfig
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration) {

  val host: String    = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  private val contactFrontendUrl           = configuration.get[String]("contact-frontend.url")
  private val contactFormServiceIdentifier = configuration.get[String]("contact-frontend.serviceId")
  private val playFrontendHost             = configuration.get[String]("play.frontend.host")

  def feedbackUrl(implicit request: RequestHeader): String = {
    val backUrl = s"${playFrontendHost + request.uri}"
    s"$contactFrontendUrl?service=$contactFormServiceIdentifier&backUrl=$backUrl"
  }

  val loginUrl: String           = configuration.get[String]("urls.login")
  val loginContinueUrl: String   = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String         = configuration.get[String]("urls.signOut")
  val signOutContinueUrl: String = configuration.get[String]("urls.signOutContinue")
  val feedbackFrontend: String   = configuration.get[String]("urls.feedbackFrontend")
  val registerTGPURL: String     = configuration.get[String]("urls.registerTGPURL")
  val ottCommodityUrl: String    = configuration.get[String]("urls.ott-commodity")

  private val exitSurveyBaseUrl: String = configuration.get[Service]("microservice.services.feedback-frontend").baseUrl
  val exitSurveyUrl: String             = s"$exitSurveyBaseUrl/feedback/trader-goods-profiles-frontend"

  val dataStoreBaseUrl: Service = configuration.get[Service]("microservice.services.trader-goods-profiles-data-store")
  val customsEmailUrl: Service  = configuration.get[Service]("microservice.services.customs-email-frontend")

  val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("features.welsh-translation")

  val requestingAdviceEnabled: Boolean =
    configuration.get[Boolean]("features.requesting-advice")

  val downloadFileEnabled: Boolean =
    configuration.get[Boolean]("features.download-file-enabled")

  val userAllowListEnabled: Boolean =
    configuration.get[Boolean]("features.user-allow-list-enabled")

  val getHistoricProfileEnabled: Boolean =
    configuration.get[Boolean]("features.get-historic-profile")

  val useEisPatchMethod: Boolean =
    configuration.get[Boolean]("features.use-eis-patch-method")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val timeout: Int   = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Long  = configuration.get[Int]("mongodb.timeToLiveInSeconds")
  val countryCacheKey = "ott_country_codes"

  val tgpEnrolmentIdentifier: EnrolmentConfig = configuration.get[EnrolmentConfig]("enrolment-config")

  val googleTagId: String = configuration.get[String]("gaTrackingId")
}
