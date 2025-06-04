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

package controllers.goodsProfile

import connectors.GoodsRecordConnector
import controllers.BaseController
import controllers.actions.*
import forms.goodsProfile.RemoveGoodsRecordFormProvider
import models.GoodsRecordsPagination.firstPage
import models.{Location, NormalMode}
import navigation.GoodsProfileNavigator
import pages.goodsProfile.RemoveGoodsRecordPage
import pages.goodsRecord.{ProductReferencePage, ProductReferenceUpdatePage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.AuditService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.goodsProfile.RemoveGoodsRecordView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveGoodsRecordController @Inject() (
  override val messagesApi: MessagesApi,
  goodsRecordConnector: GoodsRecordConnector,
  navigator: GoodsProfileNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: RemoveGoodsRecordFormProvider,
  auditService: AuditService,
  val controllerComponents: MessagesControllerComponents,
  view: RemoveGoodsRecordView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(recordId: String, location: Location): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      auditService.auditStartRemoveGoodsRecord(request.eori, request.affinityGroup, recordId)

      val maybeProductRef = request.userAnswers.get(ProductReferenceUpdatePage(recordId))

      maybeProductRef match {
        case Some(productRef) =>
          Future.successful(Ok(view(form, recordId, location, productRef)))
        case None =>
          goodsRecordConnector.getRecord(recordId).map { record =>
            val productRef = record.traderRef
            Ok(view(form, recordId, location, productRef))
          }.recover {
            case ex =>
              logger.error(s"Failed to fetch record for ID $recordId: ${ex.getMessage}", ex)
              Redirect(controllers.problem.routes.JourneyRecoveryController.onPageLoad())
          }
      }
    }


  def onSubmit(recordId: String, location: Location): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val maybeProductRef = request.userAnswers.get(ProductReferenceUpdatePage(recordId))

      def processForm(productRef: String): Future[Result] = {
        form.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, recordId, location, productRef))),
          {
            case true =>
              goodsRecordConnector.removeGoodsRecord(recordId).map { value =>
                auditService.auditFinishRemoveGoodsRecord(request.eori, request.affinityGroup, recordId)
                if (value) {
                  Redirect(navigator.nextPage(RemoveGoodsRecordPage, NormalMode, request.userAnswers))
                } else {
                  navigator.journeyRecovery(
                    Some(RedirectUrl(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(firstPage).url))
                  )
                }
              }
            case false =>
              Future.successful(Redirect(navigator.nextPage(RemoveGoodsRecordPage, NormalMode, request.userAnswers)))
          }
        )
      }

      maybeProductRef match {
        case Some(productRef) =>
          processForm(productRef)
        case None =>
          goodsRecordConnector.getRecord(recordId).flatMap { record =>
            val productRef = record.traderRef
            processForm(productRef)
          }.recover {
            case ex =>
              logger.error(s"Failed to fetch record for ID $recordId: ${ex.getMessage}", ex)
              Redirect(controllers.problem.routes.JourneyRecoveryController.onPageLoad())
          }
      }
    }

}
