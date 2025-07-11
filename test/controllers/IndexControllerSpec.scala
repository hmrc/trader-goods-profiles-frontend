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

package controllers

import base.SpecBase
import config.{FrontendAppConfig, Service}
import connectors.{DownloadDataConnector, TraderProfileConnector}
import models.{Email, TraderProfile}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, spy, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import java.time.Instant
import scala.concurrent.Future

class IndexControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockTraderProfileConnector = mock[TraderProfileConnector]
  private val mockDownloadDataConnector  = mock[DownloadDataConnector]
  private val mockConfig                 = mock[FrontendAppConfig]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockTraderProfileConnector, mockDownloadDataConnector, mockConfig)
  }

  "Index Controller" - {

    val address   = "somebody@email.com"
    val timestamp = Instant.now
    val email     = Email(address, timestamp)

    "when download feature flag is true" - {
      "and trader has credential role of user" - {
        val app           = applicationBuilder(userAnswers = None).build()
        val mockAppConfig = spy(app.injector.instanceOf[FrontendAppConfig])
        when(mockAppConfig.downloadFileEnabled).thenReturn(true)

        "when email is present" - {
          "must redirect to ProfileSetupController if no profile present" in {
            when(mockTraderProfileConnector.checkTraderProfile(any())(any())).thenReturn(Future.successful(false))
            when(mockDownloadDataConnector.getEmail(any())).thenReturn(Future.successful(Some(email)))

            val application = applicationBuilder(userAnswers = None)
              .overrides(bind[FrontendAppConfig].toInstance(mockAppConfig))
              .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
              .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
              .build()

            running(application) {
              val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)
              val result  = route(application, request).value

              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual controllers.profile.routes.ProfileSetupController
                .onPageLoad()
                .url

              verify(mockTraderProfileConnector).checkTraderProfile(any())(any())
              verify(mockDownloadDataConnector).getEmail(any())
            }
          }

          "must redirect to HomePageController if no profile present" in {
            when(mockTraderProfileConnector.checkTraderProfile(any())(any())).thenReturn(Future.successful(false))
            when(mockDownloadDataConnector.getEmail(any())).thenReturn(Future.successful(Some(email)))

            val application = applicationBuilder(userAnswers = None)
              .overrides(bind[FrontendAppConfig].toInstance(mockAppConfig))
              .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
              .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
              .build()

            running(application) {
              val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)
              val result  = route(application, request).value

              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual controllers.profile.routes.ProfileSetupController
                .onPageLoad()
                .url

              verify(mockTraderProfileConnector).checkTraderProfile(any())(any())
              verify(mockDownloadDataConnector).getEmail(any())
            }
          }

          "must redirect to HomePageController if no profile present and eori has not changed" in {
            when(mockTraderProfileConnector.checkTraderProfile(any())(any())).thenReturn(Future.successful(true))
            when(mockTraderProfileConnector.getTraderProfile(any()))
              .thenReturn(
                Future.successful(
                  TraderProfile("name", "address", Some("postcode"), Some("country"), eoriChanged = false)
                )
              )
            when(mockDownloadDataConnector.getEmail(any())).thenReturn(Future.successful(Some(email)))

            val application =
              applicationBuilder(userAnswers = None)
                .overrides(bind[FrontendAppConfig].toInstance(mockAppConfig))
                .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
                .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
                .build()

            running(application) {
              val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)
              val result  = route(application, request).value

              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url

              verify(mockTraderProfileConnector).checkTraderProfile(any())(any())
              verify(mockDownloadDataConnector).getEmail(any())
              verify(mockTraderProfileConnector).getTraderProfile(any())
            }
          }

          "must redirect to UkimsNumberChangeController if no profile present and eori has changed" in {
            when(mockTraderProfileConnector.checkTraderProfile(any())(any())).thenReturn(Future.successful(true))
            when(mockTraderProfileConnector.getTraderProfile(any()))
              .thenReturn(
                Future.successful(
                  TraderProfile("name", "address", Some("postcode"), Some("country"), eoriChanged = true)
                )
              )
            when(mockDownloadDataConnector.getEmail(any())).thenReturn(Future.successful(Some(email)))

            val application = applicationBuilder(userAnswers = None)
              .overrides(bind[FrontendAppConfig].toInstance(mockAppConfig))
              .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
              .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
              .build()

            running(application) {
              val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)
              val result  = route(application, request).value

              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual controllers.newUkims.routes.UkimsNumberChangeController
                .onPageLoad()
                .url

              verify(mockTraderProfileConnector).checkTraderProfile(any())(any())
              verify(mockDownloadDataConnector).getEmail(any())
              verify(mockTraderProfileConnector).getTraderProfile(any())
            }
          }
        }

        "when email is not present must redirect to custom email frontend" in {
          when(mockDownloadDataConnector.getEmail(any())).thenReturn(Future.successful(None))
          when(mockConfig.customsEmailUrl).thenReturn(Service("localhost", "3000", "http"))

          val application = applicationBuilder(userAnswers = None)
            .overrides(bind[FrontendAppConfig].toInstance(mockAppConfig))
            .overrides(
              bind[DownloadDataConnector].toInstance(mockDownloadDataConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value contains "/manage-email-cds/service/trader-goods-profiles"

            verify(mockDownloadDataConnector).getEmail(any())
          }
        }
      }
      "and trader has credential role of assistant" - {
        val app           = applicationBuilderAssistant(userAnswers = None).build()
        val mockAppConfig = spy(app.injector.instanceOf[FrontendAppConfig])
        when(mockAppConfig.downloadFileEnabled).thenReturn(true)

        "must redirect to ProfileSetupController if no profile present" in {
          when(mockTraderProfileConnector.checkTraderProfile(any())(any())).thenReturn(Future.successful(false))
          when(mockDownloadDataConnector.getEmail(any())).thenReturn(Future.successful(Some(email)))

          val application = applicationBuilderAssistant(userAnswers = None)
            .overrides(bind[FrontendAppConfig].toInstance(mockAppConfig))
            .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
            .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual controllers.profile.routes.ProfileSetupController.onPageLoad().url

            verify(mockTraderProfileConnector).checkTraderProfile(any())(any())
            verify(mockDownloadDataConnector, never()).getEmail(any())
          }
        }

        "must redirect to HomePageController if no profile present" in {
          when(mockTraderProfileConnector.checkTraderProfile(any())(any())).thenReturn(Future.successful(false))
          when(mockDownloadDataConnector.getEmail(any())).thenReturn(Future.successful(Some(email)))

          val application = applicationBuilderAssistant(userAnswers = None)
            .overrides(bind[FrontendAppConfig].toInstance(mockAppConfig))
            .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
            .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual controllers.profile.routes.ProfileSetupController.onPageLoad().url

            verify(mockTraderProfileConnector).checkTraderProfile(any())(any())
            verify(mockDownloadDataConnector, never()).getEmail(any())
          }
        }

        "must redirect to HomePageController if no profile present and eori has not changed" in {
          when(mockTraderProfileConnector.checkTraderProfile(any())(any())).thenReturn(Future.successful(true))
          when(mockTraderProfileConnector.getTraderProfile(any()))
            .thenReturn(
              Future.successful(
                TraderProfile("name", "address", Some("postcode"), Some("country"), eoriChanged = false)
              )
            )
          when(mockDownloadDataConnector.getEmail(any())).thenReturn(Future.successful(Some(email)))

          val application = applicationBuilderAssistant(userAnswers = None)
            .overrides(bind[FrontendAppConfig].toInstance(mockAppConfig))
            .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
            .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url

            verify(mockTraderProfileConnector).checkTraderProfile(any())(any())
            verify(mockDownloadDataConnector, never()).getEmail(any())
            verify(mockTraderProfileConnector).getTraderProfile(any())
          }
        }

        "must redirect to UkimsNumberChangeController if no profile present and eori has changed" in {
          when(mockTraderProfileConnector.checkTraderProfile(any())(any())).thenReturn(Future.successful(true))
          when(mockTraderProfileConnector.getTraderProfile(any()))
            .thenReturn(
              Future.successful(TraderProfile("name", "address", Some("postcode"), Some("country"), eoriChanged = true))
            )
          when(mockDownloadDataConnector.getEmail(any())).thenReturn(Future.successful(Some(email)))

          val application = applicationBuilderAssistant(userAnswers = None)
            .overrides(bind[FrontendAppConfig].toInstance(mockAppConfig))
            .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
            .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual controllers.newUkims.routes.UkimsNumberChangeController
              .onPageLoad()
              .url

            verify(mockTraderProfileConnector).checkTraderProfile(any())(any())
            verify(mockDownloadDataConnector, never()).getEmail(any())
            verify(mockTraderProfileConnector).getTraderProfile(any())
          }
        }
      }
    }

    "when download feature flag is false" - {
      val app           = applicationBuilder(userAnswers = None).build()
      val mockAppConfig = spy(app.injector.instanceOf[FrontendAppConfig])
      when(mockAppConfig.downloadFileEnabled).thenReturn(false)

      "must redirect to ProfileSetupController if no profile present" in {
        when(mockTraderProfileConnector.checkTraderProfile(any())(any())).thenReturn(Future.successful(false))
        when(mockDownloadDataConnector.getEmail(any())).thenReturn(Future.successful(Some(email)))

        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[FrontendAppConfig].toInstance(mockAppConfig))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual controllers.profile.routes.ProfileSetupController.onPageLoad().url

          verify(mockTraderProfileConnector).checkTraderProfile(any())(any())
          verify(mockDownloadDataConnector, never()).getEmail(any())
        }
      }

      "must redirect to HomePageController if no profile present" in {
        when(mockTraderProfileConnector.checkTraderProfile(any())(any())).thenReturn(Future.successful(false))
        when(mockDownloadDataConnector.getEmail(any())).thenReturn(Future.successful(Some(email)))

        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[FrontendAppConfig].toInstance(mockAppConfig))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual controllers.profile.routes.ProfileSetupController.onPageLoad().url

          verify(mockTraderProfileConnector).checkTraderProfile(any())(any())
          verify(mockDownloadDataConnector, never()).getEmail(any())
        }
      }

      "must redirect to HomePageController if no profile present and eori has not changed" in {
        when(mockTraderProfileConnector.checkTraderProfile(any())(any())).thenReturn(Future.successful(true))
        when(mockTraderProfileConnector.getTraderProfile(any()))
          .thenReturn(
            Future.successful(TraderProfile("name", "address", Some("postcode"), Some("country"), eoriChanged = false))
          )
        when(mockDownloadDataConnector.getEmail(any())).thenReturn(Future.successful(Some(email)))

        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[FrontendAppConfig].toInstance(mockAppConfig))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url

          verify(mockTraderProfileConnector).checkTraderProfile(any())(any())
          verify(mockDownloadDataConnector, never()).getEmail(any())
          verify(mockTraderProfileConnector).getTraderProfile(any())
        }
      }

      "must redirect to UkimsNumberChangeController if no profile present and eori has changed" in {
        when(mockTraderProfileConnector.checkTraderProfile(any())(any())).thenReturn(Future.successful(true))
        when(mockTraderProfileConnector.getTraderProfile(any()))
          .thenReturn(
            Future.successful(TraderProfile("name", "address", Some("postcode"), Some("country"), eoriChanged = true))
          )
        when(mockDownloadDataConnector.getEmail(any())).thenReturn(Future.successful(Some(email)))

        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[FrontendAppConfig].toInstance(mockAppConfig))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual controllers.newUkims.routes.UkimsNumberChangeController
            .onPageLoad()
            .url

          verify(mockTraderProfileConnector).checkTraderProfile(any())(any())
          verify(mockDownloadDataConnector, never()).getEmail(any())
          verify(mockTraderProfileConnector).getTraderProfile(any())
        }
      }
    }
  }
}
