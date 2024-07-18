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
import base.TestConstants.testRecordId
import models.{Category1, Category2, Standard, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.RecategorisingQuery
import repositories.SessionRepository
import views.html.CategorisationResultView

import scala.concurrent.Future

class CategorisationResultControllerSpec extends SpecBase {

  "CategorisationResult Controller" - {

    "must return OK and the correct view for a GET" - {

      "Category1" in {

        val argCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        val mockSessionRepository                  = mock[SessionRepository]
        when(mockSessionRepository.set(argCaptor.capture())).thenReturn(Future.successful(true))

        val userAnswersWithRecategorisingMode = emptyUserAnswers
          .set(RecategorisingQuery(testRecordId), true)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswersWithRecategorisingMode))
          .overrides(bind[SessionRepository].to(mockSessionRepository))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.CategorisationResultController.onPageLoad(testRecordId, Category1).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CategorisationResultView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(testRecordId, Category1)(request, messages(application)).toString

          withClue("must delete the recategorising query answer") {
            val finalUA = argCaptor.getValue
            finalUA.get(RecategorisingQuery(testRecordId)) mustBe None
          }

        }
      }

      "Category2" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.CategorisationResultController.onPageLoad(testRecordId, Category2).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CategorisationResultView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(testRecordId, Category2)(request, messages(application)).toString
        }
      }

      "Standard" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.CategorisationResultController.onPageLoad(testRecordId, Standard).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CategorisationResultView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(testRecordId, Standard)(request, messages(application)).toString
        }
      }
    }
  }
}
