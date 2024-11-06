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

package controllers.profile

import base.SpecBase
import base.TestConstants.testEori
import connectors.TraderProfileConnector
import controllers.routes
import models.{TraderProfile, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.profile.UkimsNumberUpdatePage
import play.api.Application
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.{running, _}
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import viewmodels.checkAnswers.profile.UkimsNumberSummary
import viewmodels.govuk.SummaryListFluency
import views.html.profile.CyaNewUkimsNumberView

import scala.concurrent.Future

class CyaNewUkimsNumberControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  private lazy val journeyRecoveryContinueUrl = routes.HomePageController.onPageLoad().url

  private def onwardRoute = Call("GET", "/foo")

  "CyaNewUkimsNumber Controller" - {

    "UKIMS Number" - {

      def createChangeList(app: Application, userAnswers: UserAnswers): SummaryList = SummaryListViewModel(
        rows = Seq(
          UkimsNumberSummary.rowUpdate(userAnswers)(messages(app))
        ).flatten
      )

      "for a GET" - {

        "must return OK and the correct view" in {

          val userAnswers = emptyUserAnswers
            .set(UkimsNumberUpdatePage, "newUkims")
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val list = createChangeList(application, userAnswers)

            val request = FakeRequest(GET, controllers.profile.routes.CyaNewUkimsNumberController.onPageLoad().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaNewUkimsNumberView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list)(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery if no answers are found" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {

            val request = FakeRequest(GET, controllers.profile.routes.CyaNewUkimsNumberController.onPageLoad().url)

            val result = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl))).url
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, controllers.profile.routes.CyaNewUkimsNumberController.onPageLoad().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }

      "for a POST" - {

        "must submit the advice request and redirect to HomeController" in {
          val newUkims             = "newUkims"
          val traderProfile        = TraderProfile(testEori, "1", Some("2"), Some("3"), eoriChanged = true)
          val updatedTraderProfile = traderProfile.copy(ukimsNumber = newUkims)

          val userAnswers = emptyUserAnswers
            .set(UkimsNumberUpdatePage, newUkims)
            .success
            .value

          val mockTraderProfileConnector = mock[TraderProfileConnector]
          val mockAuditService           = mock[AuditService]
          val sessionRepository          = mock[SessionRepository]

          when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)
          when(mockTraderProfileConnector.submitTraderProfile(any(), any())(any()))
            .thenReturn(Future.successful(Done))
          when(mockAuditService.auditMaintainProfile(any(), any(), any())(any))
            .thenReturn(Future.successful(Done))

          when(sessionRepository.set(any())).thenReturn(Future.successful(true))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
              bind[AuditService].toInstance(mockAuditService),
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(sessionRepository)
            )
            .build()

          running(application) {

            val request = FakeRequest(POST, controllers.profile.routes.CyaNewUkimsNumberController.onSubmit().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url
            verify(mockTraderProfileConnector)
              .submitTraderProfile(eqTo(updatedTraderProfile), eqTo(testEori))(any())
          }

          withClue("must call the audit connector with the supplied details") {
            verify(mockAuditService)
              .auditMaintainProfile(eqTo(traderProfile), eqTo(updatedTraderProfile), eqTo(AffinityGroup.Individual))(
                any()
              )
          }
        }

        "must redirect to Journey recovery" - {

          "when the data is invalid" in {

            val userAnswers = emptyUserAnswers

            val mockTraderProfileConnector = mock[TraderProfileConnector]
            val mockAuditService           = mock[AuditService]

            when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(
              TraderProfile(testEori, "ukims", None, None, eoriChanged = false)
            )

            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

            running(application) {

              val request = FakeRequest(POST, controllers.profile.routes.CyaNewUkimsNumberController.onSubmit().url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual
                routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl))).url

              verify(mockTraderProfileConnector, never()).getTraderProfile(any())(any())

              withClue("must not call the audit connector") {
                verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
              }
            }

          }
        }

        "must let the play error handler deal with connector failure when getTraderProfile request fails" in {

          val userAnswers = emptyUserAnswers
            .set(UkimsNumberUpdatePage, "newUkims")
            .success
            .value

          val mockTraderProfileConnector = mock[TraderProfileConnector]
          val mockAuditService           = mock[AuditService]

          when(mockTraderProfileConnector.getTraderProfile(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

          running(application) {
            val request = FakeRequest(POST, controllers.profile.routes.CyaNewUkimsNumberController.onSubmit().url)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }

            withClue("must not call the audit connector") {
              verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
            }
          }

        }

        "must let the play error handler deal with connector failure when submitTraderProfile request fails" in {

          val newUkims = "newUkims"

          val traderProfile        = TraderProfile(testEori, "1", Some("2"), Some("3"), eoriChanged = true)
          val updatedTraderProfile = traderProfile.copy(ukimsNumber = newUkims)

          val userAnswers = emptyUserAnswers
            .set(UkimsNumberUpdatePage, newUkims)
            .success
            .value

          val mockTraderProfileConnector = mock[TraderProfileConnector]
          val mockAuditService           = mock[AuditService]

          when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)
          when(mockTraderProfileConnector.submitTraderProfile(any(), any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))
          when(mockAuditService.auditMaintainProfile(any(), any(), any())(any))
            .thenReturn(Future.successful(Done))

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

          running(application) {
            val request = FakeRequest(POST, controllers.profile.routes.CyaNewUkimsNumberController.onSubmit().url)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }

            withClue("must call the audit connector with the supplied details") {
              verify(mockAuditService)
                .auditMaintainProfile(eqTo(traderProfile), eqTo(updatedTraderProfile), eqTo(AffinityGroup.Individual))(
                  any()
                )
            }
          }

        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(POST, controllers.profile.routes.CyaNewUkimsNumberController.onSubmit().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }
    }
  }
}
