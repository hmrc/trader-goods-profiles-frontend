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

package controllers.goodsRecord.productReference

import connectors.GoodsRecordConnector
import controllers.BaseController
import controllers.actions._
import forms.goodsRecord.ProductReferenceFormProvider
import models.Mode
import models.helper.GoodsDetailsUpdate
import navigation.GoodsRecordNavigator
import pages.goodsRecord.ProductReferenceUpdatePage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AuditService
import utils.SessionData._
import views.html.goodsRecord.ProductReferenceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateProductReferenceController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: GoodsRecordNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  auditService: AuditService,
  formProvider: ProductReferenceFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ProductReferenceView,
  goodsRecordConnector: GoodsRecordConnector
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(ProductReferenceUpdatePage(recordId), form)

      auditService
        .auditStartUpdateGoodsRecord(
          request.eori,
          request.affinityGroup,
          GoodsDetailsUpdate,
          recordId
        )

      val onSubmitAction =
        controllers.goodsRecord.productReference.routes.UpdateProductReferenceController.onSubmit(mode, recordId)
      Ok(view(preparedForm, onSubmitAction)).removingFromSession(dataRemoved, dataUpdated, pageUpdated)
    }

  def onSubmit(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val onSubmitAction =
        controllers.goodsRecord.productReference.routes.UpdateProductReferenceController.onSubmit(mode, recordId)
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, onSubmitAction))),
          value =>
            goodsRecordConnector.getRecord(recordId).flatMap {
              case Some(oldRecord) =>
                for {
                  isProductReferenceUnique <- goodsRecordConnector.isProductReferenceUnique(value)
                  updatedAnswers           <- Future.fromTry(request.userAnswers.set(ProductReferenceUpdatePage(recordId), value))
                } yield
                  if (isProductReferenceUnique || oldRecord.traderRef == value) {
                    sessionRepository.set(updatedAnswers)
                    Redirect(navigator.nextPage(ProductReferenceUpdatePage(recordId), mode, updatedAnswers))
                      .addingToSession(dataUpdated -> (oldRecord.traderRef != value).toString)
                      .addingToSession(pageUpdated -> productReference)
                  } else {
                    val formWithApiErrors =
                      createFormWithErrors(form, value, "productReference.error.traderRefNotUnique")
                    BadRequest(view(formWithApiErrors, onSubmitAction))
                  }
              case None            =>
                Future.successful(Redirect(controllers.problem.routes.RecordNotFoundController.onPageLoad()))
            }
        )
    }
}
