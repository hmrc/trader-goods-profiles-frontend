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
import forms.RemoveGoodsRecordFormProvider
import models.GoodsRecordsPagination.firstPage

import javax.inject.Inject
import models.{Location, NormalMode}
import navigation.Navigator
import pages.RemoveGoodsRecordPage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.RemoveGoodsRecordView

import scala.concurrent.{ExecutionContext, Future}

class RemoveGoodsRecordController @Inject() (
  override val messagesApi: MessagesApi,
  goodsRecordConnector: GoodsRecordConnector,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: RemoveGoodsRecordFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: RemoveGoodsRecordView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(recordId: String, location: Location): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      Ok(view(form, recordId, location))
    }

  def onSubmit(recordId: String, location: Location): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, recordId, location))),
          {
            case true  =>
              goodsRecordConnector
                .removeGoodsRecord(request.eori, recordId)
                .map {
                  case true  => Redirect(navigator.nextPage(RemoveGoodsRecordPage, NormalMode, request.userAnswers))
                  case false =>
                    navigator.journeyRecovery(
                      Some(RedirectUrl(routes.GoodsRecordsController.onPageLoad(firstPage).url))
                    )
                }
            case false =>
              Future.successful(Redirect(navigator.nextPage(RemoveGoodsRecordPage, NormalMode, request.userAnswers)))
          }
        )
    }
}
