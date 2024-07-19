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
import models.helper.CreateRecordJourney
import models.requests.DataRequest
import models.{Country, GoodsRecord, UserAnswers, ValidationError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import queries.CountriesQuery
import repositories.SessionRepository
import services.{AuditService, DataCleansingService}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.CyaCreateRecordView

import scala.concurrent.{ExecutionContext, Future}

class CyaCreateRecordController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CyaCreateRecordView,
  goodsRecordConnector: GoodsRecordConnector,
  dataCleansingService: DataCleansingService,
  ottConnector: OttConnector,
  sessionRepository: SessionRepository,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    GoodsRecord.build(request.userAnswers, request.eori) match {
      case Right(_)     =>
        request.userAnswers.get(CountriesQuery) match {
          case Some(countries) => Future.successful(displayView(request.userAnswers, countries))
          case None            =>
            for {
              countries               <- ottConnector.getCountries
              updatedAnswersWithQuery <- Future.fromTry(request.userAnswers.set(CountriesQuery, countries))
              _                       <- sessionRepository.set(updatedAnswersWithQuery)
            } yield displayView(updatedAnswersWithQuery, countries)
        }
      case Left(errors) => Future.successful(logErrorsAndContinue(errors, request))
    }
  }

  def displayView(userAnswers: UserAnswers, countries: Seq[Country])(implicit request: Request[_]): Result = {
    val list = SummaryListViewModel(
      rows = Seq(
        TraderReferenceSummary.row(userAnswers),
        UseTraderReferenceSummary.row(userAnswers),
        GoodsDescriptionSummary.row(userAnswers),
        CountryOfOriginSummary.row(userAnswers, countries),
        CommodityCodeSummary.row(userAnswers)
      ).flatten
    )
    Ok(view(list))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    GoodsRecord.build(request.userAnswers, request.eori) match {
      case Right(model) =>
        auditService.auditFinishCreateGoodsRecord(request.eori, request.affinityGroup, request.userAnswers)
        for {
          goodsRecordResponse <- goodsRecordConnector.submitGoodsRecord(model)
          _                   <- dataCleansingService.deleteMongoData(request.userAnswers.id, CreateRecordJourney)
        } yield Redirect(routes.CreateRecordSuccessController.onPageLoad(goodsRecordResponse.recordId))
      case Left(errors) => Future.successful(logErrorsAndContinue(errors, request))
    }
  }

  def logErrorsAndContinue(errors: data.NonEmptyChain[ValidationError], request: DataRequest[AnyContent]): Result = {
    val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")

    val continueUrl = RedirectUrl(routes.CreateRecordStartController.onPageLoad().url)
    dataCleansingService.deleteMongoData(request.userAnswers.id, CreateRecordJourney)
    logger.warn(s"Unable to create Goods Record.  Missing pages: $errorMessages")
    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }
}
