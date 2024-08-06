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
import connectors.GoodsRecordConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import logging.Logging
import models.helper.SupplementaryUnitUpdateJourney
import models.requests.DataRequest
import models.{NormalMode, SupplementaryRequest, ValidationError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.DataCleansingService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.SessionData.{dataRemoved, dataUpdated, initialValueOfHasSuppUnit, initialValueOfSuppUnit, pageUpdated, supplementaryUnit}
import viewmodels.checkAnswers.{HasSupplementaryUnitSummary, SupplementaryUnitSummary}
import viewmodels.govuk.summarylist._
import views.html.CyaSupplementaryUnitView

import scala.concurrent.{ExecutionContext, Future}

class CyaSupplementaryUnitController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  dataCleansingService: DataCleansingService,
  goodsRecordConnector: GoodsRecordConnector,
  val controllerComponents: MessagesControllerComponents,
  view: CyaSupplementaryUnitView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      SupplementaryRequest.build(request.userAnswers, request.eori, recordId) match {
        case Right(_)     =>
          val list = SummaryListViewModel(
            rows = Seq(
              HasSupplementaryUnitSummary.rowUpdate(request.userAnswers, recordId),
              SupplementaryUnitSummary.rowUpdate(request.userAnswers, recordId)
            ).flatten
          )
          Ok(view(list, recordId))
        case Left(errors) => logErrorsAndContinue(errors, recordId, request)
      }
  }

  def logErrorsAndContinue(
    errors: data.NonEmptyChain[ValidationError],
    recordId: String,
    request: DataRequest[AnyContent]
  ): Result = {
    val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")

    val continueUrl = RedirectUrl(routes.HasSupplementaryUnitController.onPageLoadUpdate(NormalMode, recordId).url)

    logger.error(s"Unable to create Supplementary Unit.  Missing pages: $errorMessages")
    dataCleansingService.deleteMongoData(request.userAnswers.id, SupplementaryUnitUpdateJourney)
    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  def onSubmit(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      SupplementaryRequest.build(request.userAnswers, request.eori, recordId) match {
        case Right(model) =>
          //TODO : Audit service implementation

          val initialHasSuppUnitOpt = request.session.get(initialValueOfHasSuppUnit).map(_.toBoolean)
          val initialSuppUnitOpt    = request.session.get(initialValueOfSuppUnit)

          val finalHasSuppUnitOpt = model.hasSupplementaryUnit
          val finalSuppUnitOpt    = model.supplementaryUnit

          val isValueChanged    =
            initialHasSuppUnitOpt != finalHasSuppUnitOpt ||
              compareSupplementaryUnits(initialSuppUnitOpt, finalSuppUnitOpt)
          val isSuppUnitRemoved =
            initialHasSuppUnitOpt.contains(true) && finalHasSuppUnitOpt.contains(false)

          goodsRecordConnector.updateSupplementaryUnitForGoodsRecord(request.eori, recordId, model).map { _ =>
            dataCleansingService.deleteMongoData(request.userAnswers.id, SupplementaryUnitUpdateJourney)
            Redirect(routes.SingleRecordController.onPageLoad(recordId))
              .addingToSession(dataUpdated -> isValueChanged.toString)
              .addingToSession(dataRemoved -> isSuppUnitRemoved.toString)
              .addingToSession(pageUpdated -> supplementaryUnit)

          }
        case Left(errors) => Future.successful(logErrorsAndContinue(errors, recordId, request))
      }
  }

  private def compareSupplementaryUnits(
    initialSuppUnitOpt: Option[String],
    finalSuppUnitOpt: Option[String]
  ): Boolean = {
    val initialSuppUnitBD = BigDecimal(initialSuppUnitOpt.getOrElse("0"))
    val finalSuppUnitBD   = BigDecimal(finalSuppUnitOpt.getOrElse("0"))
    initialSuppUnitBD != finalSuppUnitBD
  }
}
