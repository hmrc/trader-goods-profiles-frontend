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
import connectors.TraderProfileConnector
import models.{NormalMode, TraderProfile}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.http.NotFoundException
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.profile.ProfileView

import scala.concurrent.Future

class ProfileControllerSpec extends SpecBase with MockitoSugar {

  private lazy val profileRoute          = routes.ProfileController.onPageLoad().url
  private val mockTraderProfileConnector = mock[TraderProfileConnector]
  private val mockSessionRepository      = mock[SessionRepository]
  when(mockSessionRepository.set(any())) thenReturn Future
    .successful(true)
  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

  "Profile Controller" - {

    "must return OK and the correct view for a GET" in {

      val profileResponse = TraderProfile(
        actorId = "actorId",
        ukimsNumber = "Ukims number",
        nirmsNumber = Some("NIRMS number"),
        niphlNumber = Some("NIPHL number"),
        eoriChanged = false
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future
        .successful(profileResponse)

      implicit val message: Messages = messages(application)

      val detailsList = SummaryListViewModel(
        rows = Seq(
          Some(UkimsNumberSummary.row(profileResponse.ukimsNumber)),
          Some(HasNirmsSummary.row(profileResponse.nirmsNumber.isDefined, NormalMode)),
          NirmsNumberSummary.row(profileResponse.nirmsNumber),
          Some(HasNiphlSummary.row(profileResponse.niphlNumber.isDefined, NormalMode)),
          NiphlNumberSummary.row(profileResponse.niphlNumber, NormalMode)
        ).flatten
      )

      running(application) {
        val request = FakeRequest(GET, profileRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ProfileView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(detailsList, false, "", false, false)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET without NIRMS number" in {

      val profileResponse = TraderProfile(
        actorId = "actorId",
        ukimsNumber = "Ukims Number",
        nirmsNumber = None,
        niphlNumber = Some("Niphl Number"),
        eoriChanged = false
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future
        .successful(profileResponse)

      implicit val message: Messages = messages(application)

      val detailsList = SummaryListViewModel(
        rows = Seq(
          Some(UkimsNumberSummary.row(profileResponse.ukimsNumber)),
          Some(HasNirmsSummary.row(profileResponse.nirmsNumber.isDefined, NormalMode)),
          NirmsNumberSummary.row(profileResponse.nirmsNumber),
          Some(HasNiphlSummary.row(profileResponse.niphlNumber.isDefined, NormalMode)),
          NiphlNumberSummary.row(profileResponse.niphlNumber, NormalMode)
        ).flatten
      )

      running(application) {
        val request = FakeRequest(GET, profileRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ProfileView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(detailsList, false, "", false, false)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET without NIPHL number" in {

      val profileResponse = TraderProfile(
        actorId = "actorId",
        ukimsNumber = "Ukims Number",
        nirmsNumber = Some("Nirms Number"),
        niphlNumber = None,
        eoriChanged = false
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future
        .successful(profileResponse)

      implicit val message: Messages = messages(application)

      val detailsList = SummaryListViewModel(
        rows = Seq(
          Some(UkimsNumberSummary.row(profileResponse.ukimsNumber)),
          Some(HasNirmsSummary.row(profileResponse.nirmsNumber.isDefined, NormalMode)),
          NirmsNumberSummary.row(profileResponse.nirmsNumber),
          Some(HasNiphlSummary.row(profileResponse.niphlNumber.isDefined, NormalMode)),
          NiphlNumberSummary.row(profileResponse.niphlNumber, NormalMode)
        ).flatten
      )

      running(application) {
        val request = FakeRequest(GET, profileRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ProfileView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(detailsList, false, "", false, false)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET without NIRMS and NIPHL number" in {

      val profileResponse = TraderProfile(
        actorId = "actorId",
        ukimsNumber = "Ukims Number",
        nirmsNumber = None,
        niphlNumber = None,
        eoriChanged = false
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future
        .successful(profileResponse)

      implicit val message: Messages = messages(application)

      val detailsList = SummaryListViewModel(
        rows = Seq(
          Some(UkimsNumberSummary.row(profileResponse.ukimsNumber)),
          Some(HasNirmsSummary.row(profileResponse.nirmsNumber.isDefined, NormalMode)),
          NirmsNumberSummary.row(profileResponse.nirmsNumber),
          Some(HasNiphlSummary.row(profileResponse.niphlNumber.isDefined, NormalMode)),
          NiphlNumberSummary.row(profileResponse.niphlNumber, NormalMode)
        ).flatten
      )

      running(application) {
        val request = FakeRequest(GET, profileRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ProfileView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(detailsList, false, "", false, false)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future
        .failed(new NotFoundException("Failed to find record"))

      running(application) {
        val request = FakeRequest(GET, profileRoute)

        val result = route(application, request).value

        intercept[Exception] {
          await(result)
        }
      }
    }

  }
}
