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

import connectors.GoodsRecordConnector
import controllers.actions._
import models.{Category1NoExemptions, CategoryRecord, NoRedirectScenario, NormalMode, Scenario, StandardNoAssessments}
import models.helper.CategorisationUpdate
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.RecordCategorisationsQuery
import services.{AuditService, CategorisationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants.firstAssessmentIndex
import views.html.CategoryGuidanceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CategoryGuidanceController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CategoryGuidanceView,
  auditService: AuditService,
  categorisationService: CategorisationService,
  goodsRecordConnector: GoodsRecordConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      (for {
        ua <- categorisationService.requireCategorisation(request, recordId)
        recordCategorisations <- Future.successful(ua.get(RecordCategorisationsQuery))
        categorisationInfo <- Future.successful(recordCategorisations.flatMap(_.records.get(recordId)))
        scenario = categorisationInfo.map(Scenario.getRedirectScenarios)
      } yield scenario match {
        case Some(Category1NoExemptions | StandardNoAssessments) =>
          CategoryRecord.build(ua, request.eori, recordId).map { categoryRecord =>
            goodsRecordConnector
              .updateGoodsRecord(request.eori, recordId, categoryRecord)
              .map { _ =>
                Redirect(routes.CategorisationResultController.onPageLoad(recordId, scenario.get).url)
              }
          }.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url)))
        case Some(NoRedirectScenario) =>
          Future.successful(Ok(view(recordId)))
      }).flatMap(identity).recover {
        case _ => Redirect(routes.JourneyRecoveryController.onPageLoad().url)
      }
  }

  def onSubmit(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      auditService
        .auditStartUpdateGoodsRecord(
          request.eori,
          request.affinityGroup,
          CategorisationUpdate,
          recordId
        )

      Redirect(routes.AssessmentController.onPageLoad(NormalMode, recordId, firstAssessmentIndex).url)

  }
}
