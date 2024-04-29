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

import controllers.actions._
import forms.UkimsNumberFormProvider
import models.{TraderGoodsProfile, UkimsNumber}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.UkimsNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UkimsNumberController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthoriseAction,
  view: UkimsNumberView,
  formProvider: UkimsNumberFormProvider,
  sessionRequest: SessionRequestAction,
  sessionService: SessionService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (authorise andThen sessionRequest) { implicit request =>
    val optionalUkimsNumber = request.userAnswers.traderGoodsProfile.flatMap(_.ukimsNumber)

    optionalUkimsNumber match {
      case Some(ukimsNumber) => Ok(view(form.fill(ukimsNumber.value)))
      case None              => Ok(view(form))
    }
  }

  def onSubmit: Action[AnyContent] = (authorise andThen sessionRequest).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        ukimsNumber => {
          val updatedTgpModelObject = request.userAnswers.traderGoodsProfile
            .map(_.copy(ukimsNumber = Some(UkimsNumber(ukimsNumber))))
            .getOrElse(TraderGoodsProfile(ukimsNumber = Some(UkimsNumber(ukimsNumber))))

          val updatedUserAnswers = request.userAnswers.copy(traderGoodsProfile = Some(updatedTgpModelObject))

          sessionService.updateUserAnswers(updatedUserAnswers).value.map {
            case Left(sessionError) => Redirect(routes.JourneyRecoveryController.onPageLoad().url)
            case Right(success)     => Redirect(routes.NirmsQuestionController.onPageLoad.url)
          }
        }
      )
  }

}
