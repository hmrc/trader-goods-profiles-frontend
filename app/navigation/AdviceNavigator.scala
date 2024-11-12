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

import controllers.routes
import models.{NormalMode, UserAnswers}
import pages.Page
import pages.advice._
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import javax.inject.{Inject, Singleton}

@Singleton
class AdviceNavigator @Inject() extends Navigator {

  val normalRoutes: Page => UserAnswers => Call = {
    case p: AdviceStartPage             => _ => controllers.advice.routes.NameController.onPageLoad(NormalMode, p.recordId)
    case p: NamePage                    => _ => controllers.advice.routes.EmailController.onPageLoad(NormalMode, p.recordId)
    case p: EmailPage                   => _ => controllers.advice.routes.CyaRequestAdviceController.onPageLoad(p.recordId)
    case p: ReasonForWithdrawAdvicePage =>
      _ => controllers.advice.routes.WithdrawAdviceSuccessController.onPageLoad(p.recordId)
    case p: CyaRequestAdvicePage        => _ => controllers.advice.routes.AdviceSuccessController.onPageLoad(p.recordId)
    case p: WithdrawAdviceStartPage     => answers => navigateFromWithdrawAdviceStartPage(answers, p.recordId)
    case _                              => _ => routes.IndexController.onPageLoad()
  }

  val checkRoutes: Page => UserAnswers => Call = {
    case p: NamePage  => _ => controllers.advice.routes.CyaRequestAdviceController.onPageLoad(p.recordId)
    case p: EmailPage => _ => controllers.advice.routes.CyaRequestAdviceController.onPageLoad(p.recordId)
    case _            => _ => controllers.problem.routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromWithdrawAdviceStartPage(answers: UserAnswers, recordId: String): Call = {

    val continueUrl = RedirectUrl(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId).url)
    answers
      .get(WithdrawAdviceStartPage(recordId))
      .map {
        case false => controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId)
        case true  => controllers.advice.routes.ReasonForWithdrawAdviceController.onPageLoad(recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }
}
