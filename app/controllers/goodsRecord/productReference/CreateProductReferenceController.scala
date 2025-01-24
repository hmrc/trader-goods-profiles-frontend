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
import navigation.GoodsRecordNavigator
import pages.goodsRecord.ProductReferencePage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AuditService
import views.html.goodsRecord.ProductReferenceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateProductReferenceController @Inject() (
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

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(ProductReferencePage, form)

      val onSubmitAction =
        controllers.goodsRecord.productReference.routes.CreateProductReferenceController.onSubmit(mode)

      Ok(view(preparedForm, onSubmitAction))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val onSubmitAction =
        controllers.goodsRecord.productReference.routes.CreateProductReferenceController.onSubmit(mode)

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, onSubmitAction))),
          value =>
            goodsRecordConnector.isproductReferenceUnique(value).flatMap {
              case true  =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(ProductReferencePage, value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(ProductReferencePage, mode, updatedAnswers))
              case false =>
                val formWithApiErrors =
                  createFormWithErrors(form, value, "productReference.error.traderRefNotUnique")
                Future.successful(BadRequest(view(formWithApiErrors, onSubmitAction)))
            }
        )
    }

}
