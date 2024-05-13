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

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      validateUserAnswers(request.userAnswers) match {
        case Right(true) =>
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

  def validateUserAnswers(userAnswers: UserAnswers): Either[Result, Boolean] = userAnswers match {
    case x if x.get(UkimsNumberPage).isEmpty =>
      Left(Redirect(routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(routes.UkimsNumberController.onPageLoad(NormalMode).url)))))
    case x if x.get(HasNirmsPage).isEmpty =>
      Left(Redirect(routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(routes.UkimsNumberController.onPageLoad(NormalMode).url)))))
    case x if x.get(HasNiphlPage).isEmpty =>
      Left(Redirect(routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(routes.UkimsNumberController.onPageLoad(NormalMode).url)))))
    case x if x.get(HasNirmsPage).contains(true) && x.get(NirmsNumberPage).isEmpty =>
      Left(Redirect(routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(routes.UkimsNumberController.onPageLoad(NormalMode).url)))))
    case x if x.get(HasNiphlPage).contains(true) && x.get(NiphlNumberPage).isEmpty =>
      Left(Redirect(routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(routes.UkimsNumberController.onPageLoad(NormalMode).url)))))
    case x if x.get(HasNirmsPage).isEmpty && x.get(NirmsNumberPage).isDefined =>
      Left(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    case x if x.get(HasNiphlPage).isEmpty && x.get(NiphlNumberPage).isDefined =>
      Left(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    case _ => Right(true)
  }

  // TODO Add onSubmit
}
