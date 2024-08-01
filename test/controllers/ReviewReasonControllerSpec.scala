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
import base.TestConstants.{testEori, testRecordId, userAnswersId}
import connectors.{GoodsRecordConnector, TraderProfileConnector}
import forms.UkimsNumberFormProvider
import models.{NormalMode, TraderProfile, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{UkimsNumberPage, UkimsNumberUpdatePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.{ReviewReasonView, UkimsNumberView}
import views.html.helper.form

import java.time.Instant
import scala.concurrent.Future

class ReviewReasonControllerSpec extends SpecBase with MockitoSugar {

  //  private def onwardRoute = Call("GET", "/foo")
  //
  //  val formProvider = new UkimsNumberFormProvider()
  //  private val form = formProvider()
  //
  //
  //  val mockSessionRepository: SessionRepository = mock[SessionRepository]
  //

  private lazy val reviewReasonRoute = routes.ReviewReasonController.onPageLoad(testRecordId).url

  "ReviewReasonController" - {



    "onPageLoad" - {

      "must redirect to SingleRecordController when the record is not marked with 'toReview'" in {
        val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
        when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
          .thenReturn(Future.successful(goodsRecordResponse(Instant.now, Instant.now)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

          running(application) {
            val request = FakeRequest(GET, reviewReasonRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.SingleRecordController.onPageLoad(testRecordId).url
          }

      }
    }

  }

}