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
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
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
            goodsRecordConnector.filterRecordsByField(request.eori, value, "traderRef").flatMap {
              case Some(traderRef) =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(TraderReferencePage, value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield
                  if (traderRef.pagination.totalRecords == 0) {
                    Redirect(navigator.nextPage(TraderReferencePage, mode, updatedAnswers))
                  } else {
                    val formWithApiErrors =
                      createFormWithErrors(form, value, "traderReference.error.traderRefNotUnique")
                    BadRequest(view(formWithApiErrors, onSubmitAction))
                  }
              case None            =>
                Future.successful(
                  Redirect(
                    controllers.goodsProfile.routes.GoodsRecordsLoadingController
                      .onPageLoad(Some(RedirectUrl(onSubmitAction.url)))
                  )
                )
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
            goodsRecordConnector.filterRecordsByField(request.eori, value, "traderRef").flatMap {
              case Some(records) =>
                for {
                  oldRecord      <- goodsRecordConnector.getRecord(request.eori, recordId)
                  updatedAnswers <-
                    Future.fromTry(request.userAnswers.set(TraderReferenceUpdatePage(recordId), value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield
                  if (records.pagination.totalRecords == 0 || oldRecord.traderRef == value) {
                    Redirect(navigator.nextPage(TraderReferenceUpdatePage(recordId), mode, updatedAnswers))
                      .addingToSession(dataUpdated -> (oldRecord.traderRef != value).toString)
                      .addingToSession(pageUpdated -> traderReference)
                  } else {
                    val formWithApiErrors =
                      createFormWithErrors(form, value, "traderReference.error.traderRefNotUnique")
                    BadRequest(view(formWithApiErrors, onSubmitAction))
                  }
              case None          =>
                Future.successful(
                  Redirect(
                    controllers.goodsProfile.routes.GoodsRecordsLoadingController
                      .onPageLoad(Some(RedirectUrl(onSubmitAction.url)))
                  )
                )
            }
        )
    }
}
