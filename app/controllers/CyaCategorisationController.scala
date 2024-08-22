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
import connectors.GoodsRecordConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.helper.CategorisationJourney
import models.{CategorisationAnswers, CategoryRecord, NormalMode, Scenario}
import navigation.Navigator
import pages.CyaCategorisationPage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import queries.{RecategorisingQuery, RecordCategorisationsQuery}
import repositories.SessionRepository
import services.{AuditService, DataCleansingService}
import viewmodels.checkAnswers.{AssessmentsSummary, HasSupplementaryUnitSummary, LongerCommodityCodeSummary, SupplementaryUnitSummary}
import viewmodels.govuk.summarylist._
import views.html.CyaCategorisationView

import scala.concurrent.{ExecutionContext, Future}

class CyaCategorisationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CyaCategorisationView,
  goodsRecordConnector: GoodsRecordConnector,
  dataCleansingService: DataCleansingService,
  auditService: AuditService,
  navigator: Navigator,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val errorMessage: String                = "Unable to update Goods Profile."
  private def continueUrl(recordId: String): Call = routes.CategoryGuidanceController.onPageLoad(recordId)

  def onPageLoad(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val categorisationAnswers = for {
        recordCategorisations <- request.userAnswers.get(RecordCategorisationsQuery)
        categorisationAnswers <- recordCategorisations.records.get(recordId)
      } yield categorisationAnswers

      val categorisationRows = categorisationAnswers match {
        case Some(categorisationInfo) =>
          categorisationInfo.categoryAssessments
            .flatMap(assessment =>
              AssessmentsSummary.row(
                recordId,
                request.userAnswers,
                assessment,
                categorisationInfo.categoryAssessments.indexOf(assessment)
              )
            )
        case None                     => Seq.empty
      }

      CategorisationAnswers.build(request.userAnswers, recordId) match {
        case Right(_) =>
          val categorisationList = SummaryListViewModel(
            rows = categorisationRows
          )

          val supplementaryUnitList = SummaryListViewModel(
            rows = Seq(
              HasSupplementaryUnitSummary.row(request.userAnswers, recordId),
              SupplementaryUnitSummary.row(request.userAnswers, recordId)
            ).flatten
          )

          val longerCommodityCodeList = SummaryListViewModel(
            rows = Seq(
              LongerCommodityCodeSummary.row(request.userAnswers, recordId)
            ).flatten
          )

          for {
            updatedUA <- Future.fromTry(request.userAnswers.set(RecategorisingQuery(recordId), false))
            _         <- sessionRepository.set(updatedUA)
          } yield updatedUA

          Ok(view(recordId, categorisationList, supplementaryUnitList, longerCommodityCodeList))

        case Left(errors) =>
          dataCleansingService.deleteMongoData(request.userAnswers.id, CategorisationJourney)
          logErrorsAndContinue(errorMessage, continueUrl(recordId), errors)

      }
  }

  def onSubmit(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      CategoryRecord.build(request.userAnswers, request.eori, recordId) match {
        case Right(model) =>
          auditService.auditFinishCategorisation(
            request.eori,
            request.affinityGroup,
            recordId,
            model.categoryAssessmentsWithExemptions,
            model.category
          )

          goodsRecordConnector.updateCategoryAndComcodeForGoodsRecord(request.eori, recordId, model).map { _ =>
            dataCleansingService.deleteMongoData(request.userAnswers.id, CategorisationJourney)
            Redirect(
              navigator.nextPage(
                CyaCategorisationPage(recordId, model, Scenario.getScenario(model)),
                NormalMode,
                request.userAnswers
              )
            )
          }
        case Left(errors) =>
          dataCleansingService.deleteMongoData(request.userAnswers.id, CategorisationJourney)
          Future.successful(logErrorsAndContinue(errorMessage, continueUrl(recordId), errors))
      }
  }

}
