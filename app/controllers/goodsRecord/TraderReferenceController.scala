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

package controllers.goodsRecord

import connectors.GoodsRecordConnector
import controllers.BaseController
import controllers.actions._
import forms.goodsRecord.TraderReferenceFormProvider
import models.Mode
import models.helper.GoodsDetailsUpdate
import navigation.GoodsRecordNavigator
import pages.goodsRecord.{TraderReferencePage, TraderReferenceUpdatePage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AuditService
import utils.SessionData._
import views.html.goodsRecord.TraderReferenceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TraderReferenceController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: GoodsRecordNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  auditService: AuditService,
  formProvider: TraderReferenceFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TraderReferenceView,
  goodsRecordConnector: GoodsRecordConnector
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form                                     = formProvider()
  def onPageLoadCreate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(TraderReferencePage, form)

      val onSubmitAction = controllers.goodsRecord.routes.TraderReferenceController.onSubmitCreate(mode)

      Ok(view(preparedForm, onSubmitAction))
    }

  def onPageLoadUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(TraderReferenceUpdatePage(recordId), form)

      auditService
        .auditStartUpdateGoodsRecord(
          request.eori,
          request.affinityGroup,
          GoodsDetailsUpdate,
          recordId
        )

      val onSubmitAction = controllers.goodsRecord.routes.TraderReferenceController.onSubmitUpdate(mode, recordId)
      Ok(view(preparedForm, onSubmitAction)).removingFromSession(dataRemoved, dataUpdated, pageUpdated)
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val onSubmitAction = controllers.goodsRecord.routes.TraderReferenceController.onSubmitCreate(mode)

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, onSubmitAction))),
          value =>
            goodsRecordConnector.isTraderReferenceUnique(value).flatMap {
              case true  =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(TraderReferencePage, value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(TraderReferencePage, mode, updatedAnswers))
              case false =>
                val formWithApiErrors =
                  createFormWithErrors(form, value, "traderReference.error.traderRefNotUnique")
                Future.successful(BadRequest(view(formWithApiErrors, onSubmitAction)))
            }
        )
    }

  def onSubmitUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val onSubmitAction = controllers.goodsRecord.routes.TraderReferenceController.onSubmitUpdate(mode, recordId)
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, onSubmitAction))),
          value =>
            for {
              isTraderReferenceUnique <- goodsRecordConnector.isTraderReferenceUnique(value)
              oldRecord               <- goodsRecordConnector.getRecord(recordId)
              updatedAnswers          <-
                Future.fromTry(request.userAnswers.set(TraderReferenceUpdatePage(recordId), value))
            } yield
              if (isTraderReferenceUnique || oldRecord.traderRef == value) {
                sessionRepository.set(updatedAnswers)
                Redirect(navigator.nextPage(TraderReferenceUpdatePage(recordId), mode, updatedAnswers))
                  .addingToSession(dataUpdated -> (oldRecord.traderRef != value).toString)
                  .addingToSession(pageUpdated -> traderReference)
              } else {
                val formWithApiErrors =
                  createFormWithErrors(form, value, "traderReference.error.traderRefNotUnique")
                BadRequest(view(formWithApiErrors, onSubmitAction))
              }
        )
    }
}
