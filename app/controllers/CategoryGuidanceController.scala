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

import connectors.{GoodsRecordConnector, TraderProfileConnector}
import controllers.actions._
import logging.Logging
import models.helper.CategorisationUpdate
import models.{Category1, Category1NoExemptions, Category2, CategoryRecord, NiphlsAndOthers, NiphlsOnly, NoRedirectScenario, NormalMode, Scenario, StandardNoAssessments}
import navigation.Navigator
import pages.CategoryGuidancePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.RecordCategorisationsQuery
import repositories.SessionRepository
import services.{AuditService, CategorisationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CategoryGuidanceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CategoryGuidanceController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CategoryGuidanceView,
  auditService: AuditService,
  categorisationService: CategorisationService,
  navigator: Navigator,
  goodsRecordConnector: GoodsRecordConnector,
  sessionRepository: SessionRepository,
  traderProfileConnector: TraderProfileConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      categorisationService
        .requireCategorisation(request, recordId)
        .flatMap { userAnswers =>
          val recordCategorisations = userAnswers.get(RecordCategorisationsQuery)
          val categorisationInfo    = recordCategorisations.flatMap(_.records.get(recordId))
          val scenario              = categorisationInfo.map(Scenario.getRedirectScenarios)
          scenario match {
            case Some(Category1NoExemptions | StandardNoAssessments) =>
              CategoryRecord
                .build(userAnswers, request.eori, recordId)
                .map { categoryRecord =>
                  goodsRecordConnector
                    .updateCategoryForGoodsRecord(request.eori, recordId, categoryRecord)
                    .map { _ =>
                      Redirect(navigator.nextPage(CategoryGuidancePage(recordId, scenario), NormalMode, userAnswers))
                    }
                }
                .getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url)))

            case Some(NiphlsOnly) =>
              //TODO this isn't right
              val categoryRecord = CategoryRecord.buildForNiphlsOnlyCategory(request.eori, recordId)

              for {
                traderProfile <- traderProfileConnector.getTraderProfile(request.eori) //TODO test failure
                //TODO this shouldn't even necessarily happen
                goodsRecord   <- goodsRecordConnector.updateCategoryForGoodsRecord(request.eori, recordId, categoryRecord)
              } yield {
                //TODO might not be best place for this to happen just conceptually
                val category = if (traderProfile.niphlNumber.isDefined){
                  Category2
                } else {
                  Category1
                }

                Redirect(navigator.nextPage(CategoryGuidancePage(recordId, Some(category)), NormalMode, userAnswers))
              }

            case Some(NiphlsAndOthers) =>
              //TODO this isn't right
              val categoryRecord = CategoryRecord.buildForNiphlsOnlyCategory(request.eori, recordId)

              for {
                traderProfile <- traderProfileConnector.getTraderProfile(request.eori) //TODO test failure
                goodsRecord <- goodsRecordConnector.updateCategoryForGoodsRecord(request.eori, recordId, categoryRecord)
              } yield {
                //TODO might not be best place for this to happen just conceptually
                 if (traderProfile.niphlNumber.isDefined) {
                  Redirect(navigator.nextPage(CategoryGuidancePage(recordId),NormalMode, userAnswers))
                } else {
                  Redirect(navigator.nextPage(CategoryGuidancePage(recordId, Some(Category1)), NormalMode, userAnswers))
                }
              }

            case Some(NoRedirectScenario) =>
              Future.successful(Ok(view(recordId)))
          }
        }
        .recover { e =>
          logger.error(e.getMessage)
          Redirect(routes.JourneyRecoveryController.onPageLoad().url)
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

      Redirect(navigator.nextPage(CategoryGuidancePage(recordId), NormalMode, request.userAnswers))
  }
}
