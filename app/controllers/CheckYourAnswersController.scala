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

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.{CheckMode, NormalMode, UserAnswers}
import models.requests.DataRequest
import pages.{HasNiphlPage, HasNirmsPage, NiphlNumberPage, NirmsNumberPage, UkimsNumberPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.{HasNiphlSummary, HasNirmsSummary, NiphlNumberSummary, NirmsNumberSummary, UkimsNumberSummary}
import viewmodels.govuk.summarylist._
import views.html.CheckYourAnswersView

import scala.language.postfixOps

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView
                                          ) extends FrontendBaseController with I18nSupport {

  val continueUrl = RedirectUrl(routes.UkimsNumberController.onPageLoad(NormalMode).url)
  val missingAnswersRedirect = Redirect(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  val incorrectAnswersRedirect = Redirect(routes.JourneyRecoveryController.onPageLoad())

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      request.userAnswers match {
        case x if missingUkimsNumber(x)
          || missingHasNirms(x)
          || missingNirmsNumber(x)
          || missingHasNiphl(x)
          || missingNiphlNumber(x)
        => missingAnswersRedirect
        case x if incorrectHasNirms(x) || incorrectHasNiphl(x) => incorrectAnswersRedirect
        case _ => {
          val list = SummaryListViewModel(
            rows = Seq(
              UkimsNumberSummary.row(request.userAnswers),
              HasNirmsSummary.row(request.userAnswers),
              NirmsNumberSummary.row(request.userAnswers),
              HasNiphlSummary.row(request.userAnswers),
              NiphlNumberSummary.row(request.userAnswers)
            ).flatten
          )
          Ok(view(list))
        }
      }
  }


  def missingUkimsNumber(userAnswers: UserAnswers): Boolean = userAnswers.get(UkimsNumberPage).isEmpty
  def missingHasNirms(userAnswers: UserAnswers): Boolean = userAnswers.get(HasNirmsPage).isEmpty
  def missingNirmsNumber(userAnswers: UserAnswers): Boolean = userAnswers.get(HasNirmsPage).contains(true) && userAnswers.get(NirmsNumberPage).isEmpty
  def missingHasNiphl(userAnswers: UserAnswers): Boolean = userAnswers.get(HasNiphlPage).isEmpty
  def missingNiphlNumber(userAnswers: UserAnswers): Boolean = userAnswers.get(HasNiphlPage).contains(true) && userAnswers.get(NiphlNumberPage).isEmpty
  def incorrectHasNirms(userAnswers: UserAnswers): Boolean = userAnswers.get(HasNirmsPage).isEmpty && userAnswers.get(NirmsNumberPage).isDefined
  def incorrectHasNiphl(userAnswers: UserAnswers): Boolean = userAnswers.get(HasNiphlPage).isEmpty && userAnswers.get(NiphlNumberPage).isDefined

  // TODO Add onSubmit
}
