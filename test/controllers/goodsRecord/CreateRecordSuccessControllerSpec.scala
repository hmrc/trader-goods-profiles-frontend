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

package controllers.goodsRecord

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AutoCategoriseService
import views.html.goodsRecord.CreateRecordSuccessView

import scala.concurrent.Future

class CreateRecordSuccessControllerSpec extends SpecBase {

  private val mockAutoCategoriseService: AutoCategoriseService = mock[AutoCategoriseService]

  "CreateRecordSuccess Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockAutoCategoriseService.autoCategoriseRecord(any[String](), any())(any(), any())) thenReturn Future
        .successful(None)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(inject.bind[AutoCategoriseService].toInstance(mockAutoCategoriseService))
        .build()

      running(application) {
        val request =
          FakeRequest(GET, controllers.goodsRecord.routes.CreateRecordSuccessController.onPageLoad("test").url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CreateRecordSuccessView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view("test", None)(request, messages(application)).toString
      }
    }
  }
}
