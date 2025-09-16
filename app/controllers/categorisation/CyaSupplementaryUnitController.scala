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

package controllers.categorisation

import com.google.inject.Inject
import connectors.GoodsRecordConnector
import controllers.BaseController
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ProfileAuthenticateAction}
import models.helper.SupplementaryUnitUpdateJourney
import models.{NormalMode, SupplementaryRequest}
import navigation.CategorisationNavigator
import pages.categorisation.CyaSupplementaryUnitPage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.{AuditService, DataCleansingService}
import utils.SessionData._
import viewmodels.checkAnswers.{HasSupplementaryUnitSummary, SupplementaryUnitSummary}
import viewmodels.govuk.summarylist._
import views.html.categorisation.CyaSupplementaryUnitView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CyaSupplementaryUnitController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  goodsRecordConnector: GoodsRecordConnector,
  dataCleansingService: DataCleansingService,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  view: CyaSupplementaryUnitView,
  navigator: CategorisationNavigator,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val errorMessage: String                = "Unable to create Supplementary Unit."
  private def continueUrl(recordId: String): Call =
    controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoadUpdate(NormalMode, recordId)

  def onPageLoad(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      SupplementaryRequest.build(request.userAnswers, request.eori, recordId) match {
        case Right(_)     =>
          val list = SummaryListViewModel(
            rows = Seq(
              HasSupplementaryUnitSummary.rowUpdate(request.userAnswers, recordId),
              SupplementaryUnitSummary.rowUpdate(request.userAnswers, recordId)
            ).flatten
          )
          Ok(view(list, recordId))
        case Left(errors) =>
          dataCleansingService.deleteMongoData(request.userAnswers.id, SupplementaryUnitUpdateJourney)
          logErrorsAndContinue(errorMessage, continueUrl(recordId), errors)
      }
    }

  def onSubmit(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      SupplementaryRequest.build(request.userAnswers, request.eori, recordId) match {
        case Right(model) =>
          val initialHasSuppUnitOpt = request.session.get(initialValueOfHasSuppUnit).map(_.toBoolean)
          val initialSuppUnitOpt    = request.session.get(initialValueOfSuppUnit)
          val finalHasSuppUnitOpt   = model.hasSupplementaryUnit
          val finalSuppUnitOpt      = model.supplementaryUnit
          val isValueChanged        =
            initialHasSuppUnitOpt != finalHasSuppUnitOpt ||
              compareSupplementaryUnits(initialSuppUnitOpt, finalSuppUnitOpt)
          val finalSuppUnitBD       = convertToBigDecimal(finalSuppUnitOpt)
          val isSuppUnitRemoved     =
            (initialHasSuppUnitOpt
              .contains(true) && finalHasSuppUnitOpt.contains(false)) || finalSuppUnitBD.contains(BigDecimal(0))
          auditService.auditFinishUpdateSupplementaryUnitGoodsRecord(
            recordId,
            request.affinityGroup,
            model
          )
          goodsRecordConnector.getRecord(recordId).flatMap {
            case Some(oldRecord) =>
              for {
                _ <- goodsRecordConnector.updateSupplementaryUnitForGoodsRecord(recordId, model, oldRecord)
                _ <- dataCleansingService.deleteMongoData(request.userAnswers.id, SupplementaryUnitUpdateJourney)
              } yield Redirect(
                navigator.nextPage(CyaSupplementaryUnitPage(recordId), NormalMode, request.userAnswers)
              )
                .addingToSession(dataUpdated -> isValueChanged.toString)
                .addingToSession(dataRemoved -> isSuppUnitRemoved.toString)
                .addingToSession(pageUpdated -> supplementaryUnit)
            case None            =>
              Future.successful(Redirect(controllers.problem.routes.RecordNotFoundController.onPageLoad()))
          }
        case Left(errors) =>
          dataCleansingService.deleteMongoData(request.userAnswers.id, SupplementaryUnitUpdateJourney)
          Future.successful(
            logErrorsAndContinue(errorMessage, continueUrl(recordId), errors)
          )
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

  private def convertToBigDecimal(value: Option[String]): Option[BigDecimal] =
    value.flatMap(v => Try(BigDecimal(v)).toOption)

}
