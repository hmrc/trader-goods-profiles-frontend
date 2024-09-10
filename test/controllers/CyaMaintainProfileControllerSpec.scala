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
import base.TestConstants.testEori
import connectors.TraderProfileConnector
import models.{TraderProfile, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{HasNirmsUpdatePage, RemoveNirmsPage}
import play.api.Application
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.{running, _}
import queries.TraderProfileQuery
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import viewmodels.checkAnswers.HasNirmsSummary
import viewmodels.govuk.SummaryListFluency
import views.html.CyaMaintainProfileView

import scala.concurrent.Future

class CyaMaintainProfileControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  private lazy val journeyRecoveryContinueUrl = routes.ProfileController.onPageLoad().url

  private def onwardRoute = Call("GET", "/foo")

  "CyaMaintainProfile Controller" - {

    "Has NIRMS" - {

      def createChangeList(app: Application, userAnswers: UserAnswers): SummaryList = SummaryListViewModel(
        rows = Seq(
          HasNirmsSummary.rowUpdate(userAnswers)(messages(app))
        ).flatten
      )

      "for a GET" - {

        "must return OK and the correct view" in {

          val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"))

          val userAnswers = emptyUserAnswers
            .set(RemoveNirmsPage, true)
            .success
            .value
            .set(HasNirmsUpdatePage, false)
            .success
            .value
            .set(TraderProfileQuery, traderProfile)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          val action = routes.CyaMaintainProfileController.onSubmitNirms

          running(application) {
            val list = createChangeList(application, userAnswers)

            val request = FakeRequest(GET, routes.CyaMaintainProfileController.onPageLoadNirms.url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaMaintainProfileView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, action)(request, messages(application)).toString
          }
        }

        "must redirect to Journey Recovery if no answers are found" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {

            val request = FakeRequest(GET, routes.CyaMaintainProfileController.onPageLoadNirms.url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl))).url
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, routes.CyaMaintainProfileController.onPageLoadNirms.url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }

      "for a POST" - {

        "when user answers can remove Nirms and update user profile" - {

          "must update the profile and redirect to the Profile Page" - {
            val traderProfile        = TraderProfile(testEori, "1", Some("2"), Some("3"))
            val updatedTraderProfile = TraderProfile(testEori, "1", None, Some("3"))

            val userAnswers = emptyUserAnswers
              .set(RemoveNirmsPage, true)
              .success
              .value
              .set(HasNirmsUpdatePage, false)
              .success
              .value
              .set(TraderProfileQuery, traderProfile)
              .success
              .value

            val mockTraderProfileConnector = mock[TraderProfileConnector]
            val mockAuditService           = mock[AuditService]

            when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)
            when(mockTraderProfileConnector.submitTraderProfile(any(), any())(any()))
              .thenReturn(Future.successful(Done))
            when(mockAuditService.auditMaintainProfile(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))

            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[AuditService].toInstance(mockAuditService),
                bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
              )
              .build()

            running(application) {

              val request = FakeRequest(POST, routes.CyaMaintainProfileController.onSubmitNirms.url)

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
        }

        "must redirect to Journey recovery" - {

          "when the data is invalid" - {

            val userAnswers = emptyUserAnswers
              .set(RemoveNirmsPage, true)
              .success
              .value
              .set(HasNirmsUpdatePage, true)
              .success
              .value

            val mockTraderProfileConnector = mock[TraderProfileConnector]
            val mockAuditService           = mock[AuditService]

            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

            running(application) {

              val request = FakeRequest(POST, routes.CyaMaintainProfileController.onSubmitNirms.url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual
                routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl))).url

              withClue("must not call the trader profile connector") {
                verify(mockTraderProfileConnector, never()).getTraderProfile(any())(any())
              }

              withClue("must not call the audit connector") {
                verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
              }
            }

          }

          "when user doesn't answer yes or no" in {

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .build()

            running(application) {
              val request = FakeRequest(POST, routes.CyaMaintainProfileController.onSubmitNirms.url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual
                routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl))).url
            }

          }
        }

        "must let the play error handler deal with connector failure" in {

          val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"))

          val userAnswers = emptyUserAnswers
            .set(RemoveNirmsPage, true)
            .success
            .value
            .set(HasNirmsUpdatePage, false)
            .success
            .value
            .set(TraderProfileQuery, traderProfile)
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
            val request = FakeRequest(POST, routes.CyaMaintainProfileController.onSubmitNirms.url)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }

            withClue("must not call the audit connector") {
              verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
            }
          }

        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(POST, routes.CyaMaintainProfileController.onSubmitNirms.url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }
    }

  }
}
