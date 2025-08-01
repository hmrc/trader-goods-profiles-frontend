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

package controllers.goodsRecord.countryOfOrigin

import controllers.BaseController
import controllers.actions.*
import forms.goodsRecord.HasCountryOfOriginChangeFormProvider
import models.Mode
import models.helper.GoodsDetailsUpdate
import navigation.GoodsRecordNavigator
import pages.goodsRecord.{CountryOfOriginUpdatePage, HasCountryOfOriginChangePage, OriginalCountryOfOriginPage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AuditService
import utils.SessionData.{dataRemoved, dataUpdated, pageUpdated}
import views.html.goodsRecord.HasCountryOfOriginChangeView

import javax.inject.Inject
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class HasCountryOfOriginChangeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: GoodsRecordNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  auditService: AuditService,
  formProvider: HasCountryOfOriginChangeFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: HasCountryOfOriginChangeView
)(implicit @unused ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      auditService
        .auditStartUpdateGoodsRecord(
          request.eori,
          request.affinityGroup,
          GoodsDetailsUpdate,
          recordId
        )

      val preparedForm = prepareForm(HasCountryOfOriginChangePage(recordId), form)

      Ok(view(preparedForm, mode, recordId)).removingFromSession(dataRemoved, dataUpdated, pageUpdated)
    }

  def onSubmit(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, recordId))),
          value =>
            if (value) {
              request.userAnswers.get(CountryOfOriginUpdatePage(recordId)) match {
                case Some(currentValue) =>
                  for {
                    updatedAnswers1 <- Future.fromTry(
                                         request.userAnswers.set(HasCountryOfOriginChangePage(recordId), value)
                                       )
                    updatedAnswers2 <- Future.fromTry(
                                         updatedAnswers1.set(OriginalCountryOfOriginPage(recordId), currentValue)
                                       )
                    _               <- sessionRepository.set(updatedAnswers2)
                  } yield Redirect(navigator.nextPage(HasCountryOfOriginChangePage(recordId), mode, updatedAnswers2))

                case None =>
                  Future.failed(new RuntimeException("Could not find data for Country of origin"))
              }

            } else {
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(HasCountryOfOriginChangePage(recordId), value))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(HasCountryOfOriginChangePage(recordId), mode, updatedAnswers))
            }
        )
    }

}
