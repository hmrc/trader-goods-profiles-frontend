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

package navigation

import base.SpecBase
import base.TestConstants.{testRecordId, userAnswersId}
import controllers.routes
import models._
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages._
import play.api.http.Status.SEE_OTHER
import services.CategorisationService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

class NavigationSpec extends SpecBase with BeforeAndAfterEach {

  private val mockCategorisationService = mock[CategorisationService]
  private val navigator                 = new Navigation(mockCategorisationService)

  override def beforeEach(): Unit = {
    reset(mockCategorisationService)
    super.beforeEach()
  }

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, emptyUserAnswers) mustBe routes.IndexController.onPageLoad()
      }

      "in Require Advice Journey" - {

        "must go from AdviceStartPage to NamePage" in {

          navigator.nextPage(AdviceStartPage(testRecordId), NormalMode, emptyUserAnswers) mustBe routes.NameController
            .onPageLoad(NormalMode, testRecordId)
        }

        "must go from NamePage to EmailPage" in {

          navigator.nextPage(NamePage(testRecordId), NormalMode, emptyUserAnswers) mustBe routes.EmailController
            .onPageLoad(
              NormalMode,
              testRecordId
            )
        }

        "must go from EmailPage to CyaRequestAdviceController" in {

          navigator.nextPage(
            EmailPage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.CyaRequestAdviceController.onPageLoad(testRecordId)
        }

        "must go from CyaRequestAdviceController to AdviceSuccess" in {

          navigator.nextPage(
            CyaRequestAdvicePage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.AdviceSuccessController
            .onPageLoad(testRecordId)
        }

      }

      "in Withdraw Advice Journey" - {

        "must go from WithdrawAdviceStartPage to ReasonForWithdrawAdvicePage when answer is Yes" in {
          val answers = UserAnswers(userAnswersId).set(WithdrawAdviceStartPage(testRecordId), true).success.value
          navigator.nextPage(
            WithdrawAdviceStartPage(testRecordId),
            NormalMode,
            answers
          ) mustBe routes.ReasonForWithdrawAdviceController
            .onPageLoad(testRecordId)
        }

        "must go from WithdrawAdviceStartPage to SingleRecordPage when answer is No" in {
          val answers = UserAnswers(userAnswersId).set(WithdrawAdviceStartPage(testRecordId), false).success.value
          navigator.nextPage(
            WithdrawAdviceStartPage(testRecordId),
            NormalMode,
            answers
          ) mustBe controllers.goodsRecord.routes.SingleRecordController
            .onPageLoad(testRecordId)
        }

        "must go from ReasonForWithdrawAdvicePage to WithdrawAdviceSuccessPage" in {

          navigator.nextPage(
            ReasonForWithdrawAdvicePage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.WithdrawAdviceSuccessController
            .onPageLoad(
              testRecordId
            )
        }

        "must go to JourneyRecoveryController when there is no answer for WithdrawAdviceStartPage" in {
          val continueUrl =
            RedirectUrl(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId).url)
          navigator.nextPage(
            WithdrawAdviceStartPage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.problem.routes.JourneyRecoveryController
            .onPageLoad(Some(continueUrl))
        }
      }

      "must go from ReviewReasonPage to Single Record page" in {
        val recordId = testRecordId
        navigator.nextPage(
          ReviewReasonPage(recordId),
          NormalMode,
          emptyUserAnswers
        ) mustEqual controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId)
      }

    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(
          UnknownPage,
          CheckMode,
          emptyUserAnswers
        ) mustBe controllers.problem.routes.JourneyRecoveryController.onPageLoad()
      }

      "in Require Advice Journey" - {

        "must go from NamePage to CyaRequestAdviceController" in {

          navigator.nextPage(
            NamePage(testRecordId),
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaRequestAdviceController.onPageLoad(testRecordId)
        }

        "must go from EmailPage to CyaRequestAdviceController" in {

          navigator.nextPage(
            EmailPage(testRecordId),
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaRequestAdviceController.onPageLoad(testRecordId)
        }
      }

    }

    ".journeyRecovery" - {

      "redirect to JourneyRecovery" - {

        "with no ContinueUrl if none supplied" in {
          val result = navigator.journeyRecovery()
          result.header.status mustEqual SEE_OTHER
          result.header
            .headers("Location") mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }

        "with ContinueUrl if one supplied" in {
          val redirectUrl = Some(RedirectUrl("/redirectUrl"))
          val result      = navigator.journeyRecovery(redirectUrl)
          result.header.status mustEqual SEE_OTHER
          result.header.headers("Location") mustEqual controllers.problem.routes.JourneyRecoveryController
            .onPageLoad(redirectUrl)
            .url
        }
      }
    }
  }
}
