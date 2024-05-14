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
import connectors.RouterConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import logging.Logging
import models.TraderProfile
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.{HasNiphlSummary, HasNirmsSummary, NiphlNumberSummary, NirmsNumberSummary, UkimsNumberSummary}
import viewmodels.govuk.summarylist._
import views.html.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            routerConnector: RouterConnector
                                          )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

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

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val (maybeErrors, maybeModel) = TraderProfile.build(request.userAnswers).pad

      val errors = maybeErrors.map { errors =>
        errors.toChain.toList.map(_.message).mkString(", ")
      }.getOrElse("")

      maybeModel.map { model =>
        routerConnector.submitTraderProfile(model).map { _ =>
          Redirect(routes.HomePageController.onPageLoad())
        }
      }.getOrElse {
        logger.warn(s"Unable to create Trader profile.  Missing pages: $errors")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }
}
