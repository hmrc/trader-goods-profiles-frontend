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

import cats.data
import com.google.inject.Inject
import connectors.{GoodsRecordConnector, OttConnector}
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import logging.Logging
import models.{Country, UpdateGoodsRecord, UserAnswers, ValidationError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import queries.CountriesQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.CyaUpdateRecordView

import scala.concurrent.{ExecutionContext, Future}

class CyaUpdateRecordController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CyaUpdateRecordView,
  goodsRecordConnector: GoodsRecordConnector,
  ottConnector: OttConnector,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      UpdateGoodsRecord.build(request.userAnswers, request.eori, recordId) match {
        case Right(_)     =>
          request.userAnswers.get(CountriesQuery) match {
            case Some(countries) => Future.successful(displayView(request.userAnswers, countries, recordId))
            case None            =>
              for {
                countries               <- ottConnector.getCountries
                updatedAnswersWithQuery <- Future.fromTry(request.userAnswers.set(CountriesQuery, countries))
                _                       <- sessionRepository.set(updatedAnswersWithQuery)
              } yield displayView(updatedAnswersWithQuery, countries, recordId)
          }
        case Left(errors) => Future.successful(logErrorsAndContinue(errors))
      }
  }

  def displayView(userAnswers: UserAnswers, countries: Seq[Country], recordId: String)(implicit
    request: Request[_]
  ): Result = {
    val list = SummaryListViewModel(
      rows = Seq(
        CountryOfOriginSummary.row(userAnswers, countries, recordId)
      ).flatten
    )
    Ok(view(list, recordId))
  }

  def onSubmit(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      UpdateGoodsRecord.build(request.userAnswers, request.eori, recordId) match {
        case Right(model) =>
          for {
            _ <- goodsRecordConnector.updateGoodsRecord(model)
            //TODO: route to correct location
          } yield Redirect(routes.HomePageController.onPageLoad())
        case Left(errors) => Future.successful(logErrorsAndContinue(errors))
      }
  }

  def logErrorsAndContinue(errors: data.NonEmptyChain[ValidationError]): Result = {
    val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")

    val continueUrl = RedirectUrl(routes.CreateRecordStartController.onPageLoad().url)

    logger.warn(s"Unable to update Goods Record.  Missing pages: $errorMessages")
    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }
}
