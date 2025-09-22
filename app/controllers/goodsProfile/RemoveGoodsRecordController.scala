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
import exceptions.RecordNotFoundException
import forms.goodsProfile.RemoveGoodsRecordFormProvider
import models.GoodsRecordsPagination.firstPage
import models.requests.DataRequest
import models.{GoodsProfileLocation, Location}
import navigation.GoodsProfileNavigator
import pages.goodsRecord.ProductReferenceUpdatePage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.http.UpstreamErrorResponse
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
  sessionRepository: SessionRepository,
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

      getProductReference(recordId)
        .map { productRef =>
          Ok(view(form, recordId, location, productRef))
        }
        .recover {
          case _: RecordNotFoundException                      =>
            Redirect(controllers.problem.routes.RecordNotFoundController.onPageLoad())
          case e: UpstreamErrorResponse if e.statusCode == 404 =>
            Redirect(controllers.problem.routes.RecordNotFoundController.onPageLoad())
          case e                                               =>
            logger.error(s"[RemoveGoodsRecordController][onPageLoad] Failed to fetch record $recordId", e)
            Redirect(controllers.problem.routes.JourneyRecoveryController.onPageLoad())
        }
    }

  def onSubmit(recordId: String, location: Location): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      getProductReference(recordId)
        .flatMap { productRef =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, recordId, location, productRef))),
              {
                case true  => handleRemove(recordId, location)
                case false => Future.successful(Redirect(getCancelRedirect(location, recordId)))
              }
            )
        }
        .recover {
          case e: UpstreamErrorResponse if e.statusCode == 404 =>
            Redirect(controllers.problem.routes.RecordNotFoundController.onPageLoad())
          case e: Exception                                    =>
            logger.error(s"[RemoveGoodsRecordController][onSubmit] Failed processing form for record $recordId", e)
            Redirect(controllers.problem.routes.JourneyRecoveryController.onPageLoad())
        }
    }

  private def getProductReference(recordId: String)(implicit request: DataRequest[_]): Future[String] =
    request.userAnswers.get(ProductReferenceUpdatePage(recordId)) match {
      case Some(productRef) => Future.successful(productRef)
      case None             =>
        goodsRecordConnector.getRecord(recordId).map(_.traderRef).recover { case _: RecordNotFoundException =>
          "Unknown product"
        }
    }

  private def handleRemove(recordId: String, location: Location)(implicit request: DataRequest[_]): Future[Result] =
    goodsRecordConnector
      .removeGoodsRecord(recordId)
      .flatMap { removed =>
        if (removed) {
          val updatedAnswers = request.userAnswers
            .remove(ProductReferenceUpdatePage(recordId))
            .getOrElse(request.userAnswers)

          for {
            _ <- sessionRepository.set(updatedAnswers)
            _ <- auditService.auditFinishRemoveGoodsRecord(request.eori, request.affinityGroup, recordId)
          } yield Redirect(navigator.nextPageAfterRemoveGoodsRecord(updatedAnswers, location))
        } else {
          auditService.auditFinishRemoveGoodsRecord(request.eori, request.affinityGroup, recordId).map { _ =>
            Redirect(controllers.problem.routes.RecordNotFoundController.onPageLoad())
          }
        }
      }
      .recover {
        case e: UpstreamErrorResponse if e.statusCode == 404 =>
          Redirect(controllers.problem.routes.RecordNotFoundController.onPageLoad())
        case e: Exception                                    =>
          logger.error(s"Error removing record $recordId: ${e.getMessage}", e)
          Redirect(controllers.problem.routes.JourneyRecoveryController.onPageLoad())
      }

  private def getCancelRedirect(location: Location, recordId: String): play.api.mvc.Call =
    location match {
      case GoodsProfileLocation =>
        controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(firstPage)
      case _                    =>
        controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId)
    }
}
