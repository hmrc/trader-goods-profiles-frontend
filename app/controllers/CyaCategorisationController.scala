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
import models.helper.{CategorisationJourney, CategorisationJourney2}
import models.requests.DataRequest
import models.{CategorisationAnswers, CategorisationAnswers2, CategoryRecord, CategoryRecord2, NormalMode, Scenario, Scenario2, ValidationError}
import navigation.Navigator
import pages.{CyaCategorisationPage, CyaCategorisationPage2}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.{CategorisationDetailsQuery, CategorisationDetailsQuery2, RecategorisingQuery}
import queries.{CategorisationDetailsQuery, RecategorisingQuery, CategorisationDetailsQuery2}
import queries.{RecategorisingQuery, RecordCategorisationsQuery}
import repositories.SessionRepository
import services.{AuditService, CategorisationService, DataCleansingService}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
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
  dataCleansingService: DataCleansingService,
  goodsRecordConnector: GoodsRecordConnector,
  auditService: AuditService,
  navigator: Navigator,
  sessionRepository: SessionRepository,
  categorisationService: CategorisationService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

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
          logErrorsAndContinue(errors, recordId, request)

      }
  }

  def onPageLoad2(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val categorisationInfo = request.userAnswers.get(CategorisationDetailsQuery2(recordId))

    categorisationInfo.map{ info =>
      CategorisationAnswers2.build(request.userAnswers, recordId) match {
        case Right(_) =>

          val categorisationRows = info.categoryAssessments
            .flatMap(assessment =>
              AssessmentsSummary.row2(
                recordId,
                request.userAnswers,
                assessment,
                info.categoryAssessments.indexOf(assessment)
              )
            )

          val categorisationList = SummaryListViewModel(
            rows = categorisationRows
          )

          Ok(view(recordId, categorisationList, SummaryListViewModel(Seq.empty), SummaryListViewModel(Seq.empty)))

        case Left(errors) =>
          logErrorsAndContinue2(errors, recordId, request)
      }}.getOrElse(
      logErrorsAndContinue2("Failed to get categorisation details", recordId, request)
    )

  }

  def onSubmit2(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      CategoryRecord2.build(request.userAnswers, request.eori, recordId, categorisationService) match {
        case Right(categoryRecord) =>
          auditService.auditFinishCategorisation(
            request.eori,
            request.affinityGroup,
            recordId,
            categoryRecord.categoryAssessmentsWithExemptions,
            Scenario2.getResultAsInt(categoryRecord.category)
          )

          goodsRecordConnector.updateCategoryAndComcodeForGoodsRecord2(request.eori, recordId, categoryRecord).map { _ =>
            //  dataCleansingService.deleteMongoData(request.userAnswers.id, CategorisationJourney)

            Redirect(
              navigator.nextPage(
                CyaCategorisationPage2(recordId),
                NormalMode,
                request.userAnswers
              )
            )
          }

            case Left(error) => Future.successful(logErrorsAndContinue2(error, recordId, request))

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
        case Left(errors) => Future.successful(logErrorsAndContinue(errors, recordId, request))
      }
  }

  def logErrorsAndContinue(
    errors: data.NonEmptyChain[ValidationError],
    recordId: String,
    request: DataRequest[AnyContent]
  ): Result = {
    val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")

    val continueUrl = RedirectUrl(routes.CategoryGuidanceController.onPageLoad(recordId).url)

    logger.error(s"Unable to update Goods Profile.  Missing pages: $errorMessages")
    dataCleansingService.deleteMongoData(request.userAnswers.id, CategorisationJourney)
    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def logErrorsAndContinue2(
    errors: data.NonEmptyChain[ValidationError],
    recordId: String,
    request: DataRequest[AnyContent]
  ): Result = {
    val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")

    logErrorsAndContinue2(errorMessages, recordId, request)
  }

  private def logErrorsAndContinue2(
    errorMessage: String,
    recordId: String,
    request: DataRequest[AnyContent]
  ): Result = {

    val continueUrl = RedirectUrl(routes.CategorisationPreparationController.startCategorisation(recordId).url)

    logger.error(s"Unable to update Goods Profile: $errorMessage")
    dataCleansingService.deleteMongoData(request.userAnswers.id, CategorisationJourney2)
    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

}
