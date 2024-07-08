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

import connectors.OttConnector
import controllers.actions._
import forms.LongerCommodityCodeFormProvider
import models.Mode
import models.helper.UpdateRecordJourney
import navigation.Navigator
import pages.LongerCommodityCodePage
import play.api.data.FormError
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{LongerCommodityQuery, RecordCategorisationsQuery}
import repositories.SessionRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.LongerCommodityCodeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LongerCommodityCodeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: LongerCommodityCodeFormProvider,
  ottConnector: OttConnector,
  val controllerComponents: MessagesControllerComponents,
  view: LongerCommodityCodeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode, recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val commodityCodeOption = request.userAnswers
        .get(RecordCategorisationsQuery)
        .flatMap(x => x.records.get(recordId).map(x => x.originalCommodityCode))
        .getOrElse(None)

      val oldLongerCodeSuffixOpt = commodityCodeOption.map { originalComcode =>
        val longerComcode = request.userAnswers
          .get(RecordCategorisationsQuery)
          .flatMap(x => x.records.get(recordId).map(x => x.commodityCode)).getOrElse("")
        longerComcode.drop(originalComcode.length)
      }

      val preparedForm = oldLongerCodeSuffixOpt match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      commodityCodeOption match {
        case Some(shortCommodity) if shortCommodity.length < 10 =>
          Ok(view(preparedForm, mode, shortCommodity, recordId))
        case _ => Redirect(routes.JourneyRecoveryController.onPageLoad().url)
      }
  }

  def onSubmit(mode: Mode, recordId: String): Action[AnyContent]           =
    (identify andThen getData andThen requireData).async { implicit request =>
      val commodityCodeOption = request.userAnswers
        .get(RecordCategorisationsQuery)
        .flatMap(x => x.records.get(recordId).map(x => x.originalCommodityCode))
        .getOrElse(None)

      val oldLongerCode = request.userAnswers
        .get(RecordCategorisationsQuery)
        .flatMap(x => x.records.get(recordId).map(x => x.commodityCode))

      commodityCodeOption match {
        case Some(shortCommodity) if shortCommodity.length < 10 =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, shortCommodity, recordId))),
              value => {
                val longCommodityCode = s"$shortCommodity$value"
                oldLongerCode match {
                  case Some(oldLongercode) if shortCommodity.concat(value) == oldLongercode =>
                    Future.successful(Redirect(routes.CyaCategorisationController.onPageLoad(recordId)))
                  case _                                             =>
                    (for {
                      validCommodityCode      <- ottConnector.getCommodityCode(
                                                   longCommodityCode,
                                                   request.eori,
                                                   request.affinityGroup,
                                                   UpdateRecordJourney,
                                                   Some(recordId)
                                                 )
                      updatedAnswers          <-
                        Future.fromTry(request.userAnswers.set(LongerCommodityCodePage(recordId), value))
                      updatedAnswersWithQuery <-
                        Future.fromTry(updatedAnswers.set(LongerCommodityQuery(recordId), validCommodityCode))
                      _                       <- sessionRepository.set(updatedAnswersWithQuery)
                    } yield Redirect(
                      navigator.nextPage(LongerCommodityCodePage(recordId), mode, updatedAnswersWithQuery)
                    )).recover { case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
                      val formWithApiErrors =
                        form
                          .copy(errors =
                            Seq(elems = FormError("value", getMessage("longerCommodityCode.error.invalid")))
                          )
                      BadRequest(view(formWithApiErrors, mode, shortCommodity, recordId))
                    }
                }
              }
            )
      }

    }
  private def getMessage(key: String)(implicit messages: Messages): String = messages(key)
}
