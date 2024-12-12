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
import models.{TraderProfile, UserAnswers}
import navigation.{FakeProfileNavigator, ProfileNavigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{atLeastOnce, never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.profile._
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
import utils.Constants._
import viewmodels.checkAnswers.profile._
import viewmodels.govuk.SummaryListFluency
import views.html.profile.CyaMaintainProfileView

import scala.concurrent.Future

class CyaMaintainProfileControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  private lazy val journeyRecoveryContinueUrl = controllers.profile.routes.ProfileController.onPageLoad().url

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

          val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"), false)

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

          val action = controllers.profile.routes.CyaMaintainProfileController.onSubmitNirms()

          running(application) {
            val list = createChangeList(application, userAnswers)

            val request =
              FakeRequest(GET, controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirms().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaMaintainProfileView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, action, hasNirmsKey)(request, messages(application)).toString
          }
        }

        "must redirect to Journey Recovery if no answers are found" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {

            val request =
              FakeRequest(GET, controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirms().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                .url
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request =
              FakeRequest(GET, controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirms().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }
      }

      "for a POST" - {

        "when user answers can remove Nirms and update user profile" - {

          "must update the profile and redirect to the Profile Page" in {
            val traderProfile        = TraderProfile(testEori, "1", Some("2"), Some("3"), eoriChanged = false)
            val updatedTraderProfile = TraderProfile(testEori, "1", None, Some("3"), eoriChanged = false)

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
            when(mockTraderProfileConnector.submitTraderProfile(any())(any()))
              .thenReturn(Future.successful(Done))
            when(mockAuditService.auditMaintainProfile(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))

            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[AuditService].toInstance(mockAuditService),
                bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute))
              )
              .build()

            running(application) {

              val request =
                FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNirms().url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual onwardRoute.url

              withClue("must call the relevant services") {
                verify(mockTraderProfileConnector)
                  .submitTraderProfile(eqTo(updatedTraderProfile))(any())
                verify(mockTraderProfileConnector)
                  .getTraderProfile(eqTo(testEori))(any())
                verify(mockAuditService)
                  .auditMaintainProfile(
                    eqTo(traderProfile),
                    eqTo(updatedTraderProfile),
                    eqTo(AffinityGroup.Individual)
                  )(
                    any()
                  )
              }
            }
          }
        }

        "must redirect to Journey recovery" - {

          "when the data is invalid" in {

            val userAnswers = emptyUserAnswers
              .set(RemoveNirmsPage, true)
              .success
              .value
              .set(HasNirmsUpdatePage, true)
              .success
              .value

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

              val request =
                FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNirms().url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual
                controllers.problem.routes.JourneyRecoveryController
                  .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                  .url

              verify(mockTraderProfileConnector, never()).getTraderProfile(any())(any())

              withClue("must not call the audit connector") {
                verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
              }
            }

          }

          "when user doesn't answer yes or no" in {

            val traderProfile              = TraderProfile(testEori, "1", Some("2"), Some("3"), false)
            val mockTraderProfileConnector = mock[TraderProfileConnector]

            when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .overrides(
                  bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
                )
                .build()

            running(application) {
              val request =
                FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNirms().url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual
                controllers.problem.routes.JourneyRecoveryController
                  .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                  .url
            }

          }
        }

        "must let the play error handler deal with connector failure when getTraderProfile request fails" in {

          val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"), false)

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
            val request = FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNirms().url)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }

            withClue("must not call the audit connector") {
              verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
            }
          }

        }

        "must let the play error handler deal with connector failure when submitTraderProfile request fails" in {

          val traderProfile        = TraderProfile(testEori, "1", Some("2"), Some("3"), false)
          val updatedTraderProfile = TraderProfile(testEori, "1", None, Some("3"), eoriChanged = false)

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
          when(mockTraderProfileConnector.submitTraderProfile(any())(any()))
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
            val request = FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNirms().url)
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
            val request = FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNirms().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }
      }
    }

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

          val action = controllers.profile.routes.CyaMaintainProfileController.onSubmitUkimsNumber()

          running(application) {
            val list = createChangeList(application, userAnswers)

            val request =
              FakeRequest(GET, controllers.profile.routes.CyaMaintainProfileController.onPageLoadUkimsNumber().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaMaintainProfileView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, action, ukimsNumberKey)(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery if no answers are found" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {

            val request =
              FakeRequest(GET, controllers.profile.routes.CyaMaintainProfileController.onPageLoadUkimsNumber().url)

            val result = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                .url
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request =
              FakeRequest(GET, controllers.profile.routes.CyaMaintainProfileController.onPageLoadUkimsNumber().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }
      }

      "for a POST" - {

        "when user answers can change UKIMS Number and update user profile" - {

          "when user answer is the same and no change is needed redirect to the Profile Page" - {
            val ukims         = "newUkims"
            val traderProfile = TraderProfile(testEori, ukims, Some("2"), Some("3"), eoriChanged = false)

            val userAnswers = emptyUserAnswers
              .set(UkimsNumberUpdatePage, ukims)
              .success
              .value

            val mockTraderProfileConnector = mock[TraderProfileConnector]
            val mockAuditService           = mock[AuditService]

            when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)
            when(mockTraderProfileConnector.submitTraderProfile(any())(any()))
              .thenReturn(Future.successful(Done))
            when(mockAuditService.auditMaintainProfile(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))

            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[AuditService].toInstance(mockAuditService),
                bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute))
              )
              .build()

            running(application) {

              val request =
                FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitUkimsNumber().url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual onwardRoute.url
              verify(mockTraderProfileConnector, never())
                .submitTraderProfile(any())(any())
            }

            withClue("must call the audit connector with the supplied details") {
              verify(mockAuditService)
                .auditMaintainProfile(eqTo(traderProfile), eqTo(traderProfile), eqTo(AffinityGroup.Individual))(
                  any()
                )
            }
          }

          "must update the profile and redirect to the Profile Page" - {
            val newUkims             = "newUkims"
            val traderProfile        = TraderProfile(testEori, "1", Some("2"), Some("3"), false)
            val updatedTraderProfile = TraderProfile(testEori, newUkims, Some("2"), Some("3"), eoriChanged = false)

            val userAnswers = emptyUserAnswers
              .set(UkimsNumberUpdatePage, newUkims)
              .success
              .value

            val mockTraderProfileConnector = mock[TraderProfileConnector]
            val mockAuditService           = mock[AuditService]

            when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)
            when(mockTraderProfileConnector.submitTraderProfile(any())(any()))
              .thenReturn(Future.successful(Done))
            when(mockAuditService.auditMaintainProfile(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))

            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[AuditService].toInstance(mockAuditService),
                bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute))
              )
              .build()

            running(application) {

              val request =
                FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitUkimsNumber().url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual onwardRoute.url
              verify(mockTraderProfileConnector)
                .submitTraderProfile(eqTo(updatedTraderProfile))(any())
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

              val request =
                FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitUkimsNumber().url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual
                controllers.problem.routes.JourneyRecoveryController
                  .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                  .url

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
            val request =
              FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitUkimsNumber().url)
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

          val traderProfile        = TraderProfile(testEori, "1", Some("2"), Some("3"), false)
          val updatedTraderProfile = TraderProfile(testEori, newUkims, Some("2"), Some("3"), eoriChanged = false)

          val userAnswers = emptyUserAnswers
            .set(UkimsNumberUpdatePage, newUkims)
            .success
            .value

          val mockTraderProfileConnector = mock[TraderProfileConnector]
          val mockAuditService           = mock[AuditService]

          when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)
          when(mockTraderProfileConnector.submitTraderProfile(any())(any()))
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
            val request =
              FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitUkimsNumber().url)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }

            withClue("must call the audit connector with the supplied details") {
              verify(mockAuditService, atLeastOnce())
                .auditMaintainProfile(eqTo(traderProfile), eqTo(updatedTraderProfile), eqTo(AffinityGroup.Individual))(
                  any()
                )
            }
          }

        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request =
              FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitUkimsNumber().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }
      }
    }

    "Has NIPHLS" - {

      def createChangeList(app: Application, userAnswers: UserAnswers): SummaryList = SummaryListViewModel(
        rows = Seq(
          HasNiphlSummary.rowUpdate(userAnswers)(messages(app)),
          NiphlNumberSummary.rowUpdate(userAnswers)(messages(app))
        ).flatten
      )

      "for a GET" - {

        "must return OK and the correct view" in {

          val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"), false)

          val userAnswers = emptyUserAnswers
            .set(RemoveNiphlPage, true)
            .success
            .value
            .set(HasNiphlUpdatePage, false)
            .success
            .value
            .set(TraderProfileQuery, traderProfile)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          val action = controllers.profile.routes.CyaMaintainProfileController.onSubmitNiphl()

          running(application) {
            val list = createChangeList(application, userAnswers)

            val request =
              FakeRequest(GET, controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphl().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaMaintainProfileView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, action, hasNiphlKey)(request, messages(application)).toString
          }
        }

        "must redirect to Journey Recovery if no answers are found" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {

            val request =
              FakeRequest(GET, controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphl().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                .url
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request =
              FakeRequest(GET, controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphl().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }
      }

      "for a POST" - {

        "when user answers can remove Niphl and update user profile" - {

          "must update the profile and redirect to the Profile Page" - {
            val traderProfile        = TraderProfile(testEori, "1", Some("2"), Some("3"), false)
            val updatedTraderProfile = TraderProfile(testEori, "1", Some("2"), None, eoriChanged = false)

            val userAnswers = emptyUserAnswers
              .set(RemoveNiphlPage, true)
              .success
              .value
              .set(HasNiphlUpdatePage, false)
              .success
              .value
              .set(TraderProfileQuery, traderProfile)
              .success
              .value

            val mockTraderProfileConnector = mock[TraderProfileConnector]
            val mockAuditService           = mock[AuditService]

            when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)
            when(mockTraderProfileConnector.submitTraderProfile(any())(any()))
              .thenReturn(Future.successful(Done))
            when(mockAuditService.auditMaintainProfile(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))

            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[AuditService].toInstance(mockAuditService),
                bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute))
              )
              .build()

            running(application) {

              val request =
                FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNiphl().url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual onwardRoute.url
              verify(mockTraderProfileConnector, atLeastOnce())
                .submitTraderProfile(eqTo(updatedTraderProfile))(any())
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

          "when the data is invalid" in {
            val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"), false)

            val userAnswers = emptyUserAnswers
              .set(RemoveNiphlPage, true)
              .success
              .value
              .set(HasNiphlUpdatePage, true)
              .success
              .value

            val mockTraderProfileConnector = mock[TraderProfileConnector]
            val mockAuditService           = mock[AuditService]

            when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)

            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

            running(application) {

              val request =
                FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNiphl().url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual
                controllers.problem.routes.JourneyRecoveryController
                  .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                  .url

              verify(mockTraderProfileConnector, never()).getTraderProfile(any())(any())

              withClue("must not call the audit connector") {
                verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
              }
            }

          }

          "when user doesn't answer yes or no" in {

            val traderProfile              = TraderProfile(testEori, "1", Some("2"), Some("3"), false)
            val mockTraderProfileConnector = mock[TraderProfileConnector]

            when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .overrides(
                  bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
                )
                .build()

            running(application) {
              val request =
                FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNiphl().url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual
                controllers.problem.routes.JourneyRecoveryController
                  .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                  .url
            }

          }
        }

        "must let the play error handler deal with connector failure when getTraderProfile request fails" in {

          val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"), false)

          val userAnswers = emptyUserAnswers
            .set(RemoveNiphlPage, true)
            .success
            .value
            .set(HasNiphlUpdatePage, false)
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
            val request = FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNiphl().url)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }

            withClue("must not call the audit connector") {
              verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
            }
          }

        }

        "must let the play error handler deal with connector failure when submitTraderProfile request fails" in {

          val traderProfile        = TraderProfile(testEori, "1", Some("2"), Some("3"), false)
          val updatedTraderProfile = TraderProfile(testEori, "1", Some("2"), None, eoriChanged = false)

          val userAnswers = emptyUserAnswers
            .set(RemoveNiphlPage, true)
            .success
            .value
            .set(HasNiphlUpdatePage, false)
            .success
            .value
            .set(TraderProfileQuery, traderProfile)
            .success
            .value

          val mockTraderProfileConnector = mock[TraderProfileConnector]
          val mockAuditService           = mock[AuditService]

          when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)
          when(mockTraderProfileConnector.submitTraderProfile(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

          running(application) {
            val request = FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNiphl().url)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }
          }
          withClue("must call the audit connector with the supplied details") {
            verify(mockAuditService, atLeastOnce())
              .auditMaintainProfile(eqTo(traderProfile), eqTo(updatedTraderProfile), eqTo(AffinityGroup.Individual))(
                any()
              )
          }

        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNiphl().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }
      }
    }

    "NIRMS Number" - {

      def createChangeList(app: Application, userAnswers: UserAnswers): SummaryList = SummaryListViewModel(
        rows = Seq(
          HasNirmsSummary.rowUpdate(userAnswers)(messages(app)),
          NirmsNumberSummary.rowUpdate(userAnswers)(messages(app))
        ).flatten
      )

      val traderProfile = TraderProfile(testEori, "1", Some("RMS-GB-123456"), Some("3"), eoriChanged = false)

      val updatedTraderProfile = TraderProfile(testEori, "1", Some("RMS-GB-555555"), Some("3"), eoriChanged = false)

      val nirmsNumberAnswers = emptyUserAnswers
        .set(HasNirmsUpdatePage, true)
        .success
        .value
        .set(NirmsNumberUpdatePage, "RMS-GB-555555")
        .success
        .value
        .set(TraderProfileQuery, traderProfile)
        .success
        .value

      val nirmsNumberAnswersNoUpdate = emptyUserAnswers
        .set(HasNirmsUpdatePage, true)
        .success
        .value
        .set(NirmsNumberUpdatePage, "RMS-GB-123456")
        .success
        .value
        .set(TraderProfileQuery, traderProfile)
        .success
        .value

      "for a GET" - {

        "must return OK and the correct view" in {

          val mockTraderProfileConnector = mock[TraderProfileConnector]

          when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)
          when(mockTraderProfileConnector.submitTraderProfile(any())(any()))
            .thenReturn(Future.successful(Done))

          val application = applicationBuilder(userAnswers = Some(nirmsNumberAnswersNoUpdate))
            .overrides(
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

          val action = controllers.profile.routes.CyaMaintainProfileController.onSubmitNirmsNumber()

          running(application) {
            val list = createChangeList(application, nirmsNumberAnswersNoUpdate)

            val request =
              FakeRequest(GET, controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirmsNumber().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaMaintainProfileView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, action, nirmsNumberKey)(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery if no answers are found" in {

          val mockTraderProfileConnector = mock[TraderProfileConnector]

          when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)
          when(mockTraderProfileConnector.submitTraderProfile(any())(any()))
            .thenReturn(Future.successful(Done))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

          running(application) {

            val request =
              FakeRequest(GET, controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirmsNumber().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                .url
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request =
              FakeRequest(GET, controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirmsNumber().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }
      }

      "for a POST" - {

        "must update the profile and redirect to the Profile Page" in {

          val mockTraderProfileConnector = mock[TraderProfileConnector]
          val mockAuditService           = mock[AuditService]

          when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)
          when(mockTraderProfileConnector.submitTraderProfile(any())(any()))
            .thenReturn(Future.successful(Done))
          when(mockAuditService.auditMaintainProfile(any(), any(), any())(any))
            .thenReturn(Future.successful(Done))

          val application = applicationBuilder(userAnswers = Some(nirmsNumberAnswers))
            .overrides(
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
              bind[AuditService].toInstance(mockAuditService),
              bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute))
            )
            .build()

          running(application) {

            val request =
              FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNirmsNumber().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url
            verify(mockTraderProfileConnector, atLeastOnce())
              .submitTraderProfile(eqTo(updatedTraderProfile))(any())
          }

          withClue("must call the audit connector with the supplied details") {
            verify(mockAuditService)
              .auditMaintainProfile(eqTo(traderProfile), eqTo(updatedTraderProfile), eqTo(AffinityGroup.Individual))(
                any()
              )
          }
        }

        "must let the play error handler deal with connector failure when getTraderProfile request fails" in {

          val mockTraderProfileConnector = mock[TraderProfileConnector]
          val mockAuditService           = mock[AuditService]

          when(mockTraderProfileConnector.getTraderProfile(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))

          val application =
            applicationBuilder(userAnswers = Some(nirmsNumberAnswers))
              .overrides(
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNirmsNumber().url)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }

            withClue("must not call the audit connector") {
              verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
            }
          }

        }

        "must let the play error handler deal with connector failure when submitTraderProfile request fails" in {

          val mockTraderProfileConnector = mock[TraderProfileConnector]
          val mockAuditService           = mock[AuditService]

          when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)
          when(mockTraderProfileConnector.submitTraderProfile(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))
          when(mockAuditService.auditMaintainProfile(any(), any(), any())(any))
            .thenReturn(Future.successful(Done))

          val application =
            applicationBuilder(userAnswers = Some(nirmsNumberAnswers))
              .overrides(
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNirmsNumber().url)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }

            withClue("must call the audit connector with the supplied details") {
              verify(mockAuditService, atLeastOnce())
                .auditMaintainProfile(eqTo(traderProfile), eqTo(updatedTraderProfile), eqTo(AffinityGroup.Individual))(
                  any()
                )
            }
          }

        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request =
              FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNirmsNumber().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }

        "must redirect to Journey Recovery if data is invalid" in {

          val invalidNirmsNumberAnswers = emptyUserAnswers
            .set(HasNirmsUpdatePage, false)
            .success
            .value
            .set(NirmsNumberUpdatePage, "RMS-GB-555555")
            .success
            .value
            .set(TraderProfileQuery, traderProfile)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(invalidNirmsNumberAnswers)).build()

          running(application) {
            val request =
              FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNirmsNumber().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
              .url
          }
        }

      }
    }

    "NIPHL Number" - {

      def createChangeList(app: Application, userAnswers: UserAnswers): SummaryList = SummaryListViewModel(
        rows = Seq(
          HasNiphlSummary.rowUpdate(userAnswers)(messages(app)),
          NiphlNumberSummary.rowUpdate(userAnswers)(messages(app))
        ).flatten
      )

      val traderProfile = TraderProfile(testEori, "1", None, Some("3"), eoriChanged = false)

      val updatedTraderProfile = TraderProfile(testEori, "1", None, Some("5"), eoriChanged = false)

      val niphlNumberAnswers = emptyUserAnswers
        .set(HasNiphlUpdatePage, true)
        .success
        .value
        .set(NiphlNumberUpdatePage, "5")
        .success
        .value
        .set(TraderProfileQuery, traderProfile)
        .success
        .value

      val niphlNumberAnswersNoUpdate = emptyUserAnswers
        .set(HasNiphlUpdatePage, true)
        .success
        .value
        .set(NiphlNumberUpdatePage, "3")
        .success
        .value
        .set(TraderProfileQuery, traderProfile)
        .success
        .value

      "for a GET" - {

        "must return OK and the correct view" in {

          val mockTraderProfileConnector = mock[TraderProfileConnector]

          when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)
          when(mockTraderProfileConnector.submitTraderProfile(any())(any()))
            .thenReturn(Future.successful(Done))

          val application = applicationBuilder(userAnswers = Some(niphlNumberAnswersNoUpdate))
            .overrides(
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

          val action = controllers.profile.routes.CyaMaintainProfileController.onSubmitNiphlNumber()

          running(application) {
            val list = createChangeList(application, niphlNumberAnswersNoUpdate)

            val request =
              FakeRequest(GET, controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphlNumber().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaMaintainProfileView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, action, niphlNumberKey)(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery if no answers are found" in {

          val mockTraderProfileConnector = mock[TraderProfileConnector]

          when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)
          when(mockTraderProfileConnector.submitTraderProfile(any())(any()))
            .thenReturn(Future.successful(Done))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

          running(application) {

            val request =
              FakeRequest(GET, controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphlNumber().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                .url
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request =
              FakeRequest(GET, controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphlNumber().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }
      }

      "for a POST" - {

        "must update the profile and redirect to the Profile Page" in {

          val mockTraderProfileConnector = mock[TraderProfileConnector]
          val mockAuditService           = mock[AuditService]

          when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)
          when(mockTraderProfileConnector.submitTraderProfile(any())(any()))
            .thenReturn(Future.successful(Done))
          when(mockAuditService.auditMaintainProfile(any(), any(), any())(any))
            .thenReturn(Future.successful(Done))

          val application = applicationBuilder(userAnswers = Some(niphlNumberAnswers))
            .overrides(
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
              bind[AuditService].toInstance(mockAuditService),
              bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute))
            )
            .build()

          running(application) {

            val request =
              FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNiphlNumber().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url
            verify(mockTraderProfileConnector, atLeastOnce())
              .submitTraderProfile(eqTo(updatedTraderProfile))(any())
          }

          withClue("must call the audit connector with the supplied details") {
            verify(mockAuditService, atLeastOnce())
              .auditMaintainProfile(eqTo(traderProfile), eqTo(updatedTraderProfile), eqTo(AffinityGroup.Individual))(
                any()
              )
          }
        }

        "must let the play error handler deal with connector failure when getTraderProfile request fails" in {

          val mockTraderProfileConnector = mock[TraderProfileConnector]
          val mockAuditService           = mock[AuditService]

          when(mockTraderProfileConnector.getTraderProfile(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))

          val application =
            applicationBuilder(userAnswers = Some(niphlNumberAnswers))
              .overrides(
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNiphlNumber().url)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }

            withClue("must not call the audit connector") {
              verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
            }
          }

        }

        "must let the play error handler deal with connector failure when submitTraderProfile request fails" in {

          val mockTraderProfileConnector = mock[TraderProfileConnector]
          val mockAuditService           = mock[AuditService]

          when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)
          when(mockTraderProfileConnector.submitTraderProfile(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))
          when(mockAuditService.auditMaintainProfile(any(), any(), any())(any))
            .thenReturn(Future.successful(Done))

          val application =
            applicationBuilder(userAnswers = Some(niphlNumberAnswers))
              .overrides(
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNiphlNumber().url)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }

            withClue("must call the audit connector with the supplied details") {
              verify(mockAuditService, atLeastOnce())
                .auditMaintainProfile(any(), any(), any())(
                  any()
                )
            }
          }

        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request =
              FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNiphlNumber().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }

        "must redirect to Journey Recovery if data is invalid" in {

          val invalidNiphlNumberAnswers = emptyUserAnswers
            .set(HasNirmsUpdatePage, false)
            .success
            .value
            .set(NirmsNumberUpdatePage, "NIPHL")
            .success
            .value
            .set(TraderProfileQuery, traderProfile)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(invalidNiphlNumberAnswers)).build()

          running(application) {
            val request =
              FakeRequest(POST, controllers.profile.routes.CyaMaintainProfileController.onSubmitNiphlNumber().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
              .url
          }
        }
      }
    }
  }
}
