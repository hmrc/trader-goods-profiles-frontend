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
import forms.TraderReferenceFormProvider
import models.Mode
import models.helper.GoodsDetailsUpdate
import navigation.Navigator
import pages.{TraderReferencePage, TraderReferenceUpdatePage}
import play.api.data.FormError
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.SessionData._
import views.html.TraderReferenceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TraderReferenceController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
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

  private val form                                                         = formProvider()
  private def getMessage(key: String)(implicit messages: Messages): String = messages(key)

  def onPageLoadCreate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(TraderReferencePage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      val onSubmitAction = routes.TraderReferenceController.onSubmitCreate(mode)

      Ok(view(preparedForm, onSubmitAction))
    }

  def onPageLoadUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(TraderReferenceUpdatePage(recordId)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      auditService
        .auditStartUpdateGoodsRecord(
          request.eori,
          request.affinityGroup,
          GoodsDetailsUpdate,
          recordId
        )

      val onSubmitAction = routes.TraderReferenceController.onSubmitUpdate(mode, recordId)
      Ok(view(preparedForm, onSubmitAction)).removingFromSession(dataRemoved, dataUpdated, pageUpdated)
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val onSubmitAction = routes.TraderReferenceController.onSubmitCreate(mode)

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
                      form
                        .fill(value)
                        .copy(
                          errors =
                            Seq(elems = FormError("value", getMessage("traderReference.error.traderRefNotUnique")))
                        )
                    BadRequest(view(formWithApiErrors, onSubmitAction))
                  }
              case None            =>
                Future.successful(
                  Redirect(
                    routes.GoodsRecordsLoadingController
                      .onPageLoad(Some(RedirectUrl(onSubmitAction.url)))
                  )
                )
            }
        )
    }

  def onSubmitUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val onSubmitAction = routes.TraderReferenceController.onSubmitUpdate(mode, recordId)
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, onSubmitAction))),
          value => {
            val oldValueOpt    = request.userAnswers.get(TraderReferenceUpdatePage(recordId))
            val isValueChanged = oldValueOpt.exists(_ != value)
            goodsRecordConnector.getRecord(request.eori, recordId).flatMap { record =>
              if (record.traderRef != value) {
                goodsRecordConnector.filterRecordsByField(request.eori, value, "traderRef").flatMap {
                  case Some(records) =>
                    for {
                      updatedAnswers <-
                        Future.fromTry(request.userAnswers.set(TraderReferenceUpdatePage(recordId), value))
                      _              <- sessionRepository.set(updatedAnswers)
                    } yield
                      if (records.pagination.totalRecords == 0) {
                        Redirect(navigator.nextPage(TraderReferenceUpdatePage(recordId), mode, updatedAnswers))
                          .addingToSession(dataUpdated -> isValueChanged.toString)
                          .addingToSession(pageUpdated -> traderReference)
                      } else {
                        val formWithApiErrors =
                          form
                            .fill(value)
                            .copy(
                              errors =
                                Seq(elems = FormError("value", getMessage("traderReference.error.traderRefNotUnique")))
                            )
                        BadRequest(view(formWithApiErrors, onSubmitAction))
                      }
                  case None          =>
                    Future.successful(
                      Redirect(
                        routes.GoodsRecordsLoadingController
                          .onPageLoad(Some(RedirectUrl(onSubmitAction.url)))
                      )
                    )
                }
              } else {
                Future.successful(
                  Redirect(navigator.nextPage(TraderReferenceUpdatePage(recordId), mode, request.userAnswers))
                    .addingToSession(dataUpdated -> isValueChanged.toString)
                    .addingToSession(pageUpdated -> traderReference)
                )
              }
            }
          }
        )
    }
}
