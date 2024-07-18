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
import models.requests.DataRequest
import models.{Category1, Category1NoExemptions, CategoryRecord, Mode, NiphlsAndOthers, NiphlsOnly, NoRedirectScenario, Scenario, StandardNoAssessments, UserAnswers}
import navigation.Navigator
import pages.CategoryGuidancePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{RecategorisingQuery, RecordCategorisationsQuery}
import services.{AuditService, CategorisationService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.SessionData.{dataUpdated, pageUpdated}
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
  traderProfileConnector: TraderProfileConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      categorisationService
        .requireCategorisation(request, recordId)
        .flatMap { userAnswers =>
          val recordCategorisations = userAnswers.get(RecordCategorisationsQuery)
          val categorisationInfo    = recordCategorisations.flatMap(_.records.get(recordId))
          val scenario              = categorisationInfo.map(Scenario.getRedirectScenarios)
          val areWeRecategorising   = userAnswers.get(RecategorisingQuery(recordId)).getOrElse(false)

          scenario match {
            case Some(Category1NoExemptions | StandardNoAssessments) =>
              CategoryRecord
                .build(userAnswers, request.eori, recordId)
                .map { categoryRecord =>
                  goodsRecordConnector
                    .updateCategoryForGoodsRecord(request.eori, recordId, categoryRecord)
                    .map { _ =>
                      Redirect(navigator.nextPage(CategoryGuidancePage(recordId, scenario), mode, userAnswers))
                        .removingFromSession(dataUpdated, pageUpdated)
                    }
                }
                .getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url)))

            case Some(NiphlsOnly) =>
              whenNiphlsOnly(mode, recordId, request, userAnswers)

            case Some(NiphlsAndOthers) =>
              whenNiphlsAndOthers(mode, recordId, userAnswers)

            case Some(NoRedirectScenario) if areWeRecategorising =>
              Future.successful(
                Redirect(
                  navigator.nextPage(CategoryGuidancePage(recordId, Some(NoRedirectScenario)), mode, userAnswers)
                )
              )

            case Some(NoRedirectScenario) =>
              Future.successful(Ok(view(mode, recordId)).removingFromSession(dataUpdated, pageUpdated))
          }
        }
        .recover { e =>
          logger.error(e.getMessage)
          Redirect(routes.JourneyRecoveryController.onPageLoad().url)
        }
    }

  private def whenNiphlsOnly(mode: Mode, recordId: String, request: DataRequest[AnyContent], userAnswers: UserAnswers)(
    implicit hc: HeaderCarrier
  ) =
    for {
      traderProfile <- traderProfileConnector.getTraderProfile(request.eori)
      categoryRecord = CategoryRecord.buildForNiphls(request.eori, recordId, traderProfile)
      _             <- goodsRecordConnector.updateCategoryForGoodsRecord(request.eori, recordId, categoryRecord)
    } yield Redirect(
      navigator.nextPage(CategoryGuidancePage(recordId, Some(Scenario.getScenario(categoryRecord))), mode, userAnswers)
    )

  private def whenNiphlsAndOthers(mode: Mode, recordId: String, userAnswers: UserAnswers)(implicit
    hc: HeaderCarrier,
    request: DataRequest[_]
  ) = {
    val traderProfile = traderProfileConnector.getTraderProfile(request.eori)

    traderProfile.map { profile =>
      if (profile.niphlNumber.isDefined) {
        Future.successful(Ok(view(mode, recordId)))
      } else {
        // User doesn't have NIPHLs so no point asking them anything
        val categoryRecord = CategoryRecord.buildForNiphls(request.eori, recordId, profile)

        for {
          _ <- goodsRecordConnector.updateCategoryForGoodsRecord(request.eori, recordId, categoryRecord)
        } yield Redirect(navigator.nextPage(CategoryGuidancePage(recordId, Some(Category1)), mode, userAnswers))
      }
    }
  }.flatten

  def onSubmit(mode: Mode, recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      auditService
        .auditStartUpdateGoodsRecord(
          request.eori,
          request.affinityGroup,
          CategorisationUpdate,
          recordId
        )

      Redirect(navigator.nextPage(CategoryGuidancePage(recordId), mode, request.userAnswers))
  }
}
