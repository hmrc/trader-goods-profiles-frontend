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

import controllers.actions.{AuthoriseAction, SessionRequestAction}
import forms.NiphlsNumberFormProvider
import models.{NiphlsNumber, TraderGoodsProfile, Ukims}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.NiphlsNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NiphlsNumberController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthoriseAction,
  sessionRequest: SessionRequestAction, // Here
  view: NiphlsNumberView,
  formProvider: NiphlsNumberFormProvider,
  sessionService: SessionService
)(implicit ec: ExecutionContext) extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (authorise andThen sessionRequest) { implicit request =>
    request.userAnswers.traderGoodsProfile match {
      case Some(tgp) => {
        val preparedForm = form.fill(tgp.niphlsNumber.value)
        Ok(view(preparedForm))
      }
      case None => {
        Ok(view(form))
      }
    }
  }

  // Must have .asyc to handle session repository future result
  def onSubmit: Action[AnyContent] = (authorise andThen sessionRequest).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        // needs to be future.successful now since we use .async
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        // if no errors in validation, take the number and do stuff with it
        niphlsNumber => {

          // create and updated tgp model object with the niphls number
          val updatedTgpModel = request.userAnswers.traderGoodsProfile match {
            case Some(tgpModelObject) => tgpModelObject.copy(niphlsNumber = NiphlsNumber(niphlsNumber))
            case None => TraderGoodsProfile(Ukims("someukims"), NiphlsNumber(niphlsNumber))
          }

          // create an udated user answers object with the updated tgp one inside
          val updatedUserAnswers = request.userAnswers.copy(traderGoodsProfile = Some(updatedTgpModel))

          // update the answers and handle session error and success cases.
          // session error happens when it can't connect to the repository for some reason.
          sessionService.updateUserAnswers(updatedUserAnswers).value.map {
            case Left(sessionError) =>
              Redirect(routes.JourneyRecoveryController.onPageLoad().url) // Probs redirect somewhere else
            case Right(success) =>
              Redirect(routes.DummyController.onPageLoad.url) // Go to the next page if it worked.
          }
        }
      )
  }

}
