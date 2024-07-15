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
import models.ott.CategorisationInfo
import models.requests.DataRequest
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
      val categorisationInfoOpt: Option[CategorisationInfo] =
        request.userAnswers.get(RecordCategorisationsQuery).flatMap(_.records.get(recordId))

      val originalComcodeOpt = categorisationInfoOpt.flatMap(_.originalCommodityCode)
      val latestComcodeOpt   = categorisationInfoOpt.map(_.commodityCode)

      val answerToFillFormWith = for {
        originalCode <- originalComcodeOpt
        latestCode   <- latestComcodeOpt
      } yield latestCode.drop(originalCode.length)

      val preparedForm = (categorisationInfoOpt, answerToFillFormWith) match {
        case (Some(categorisationInfo), Some(answerToFillFormWith)) if categorisationInfo.latestDoesNotMatchOriginal =>
          form.fill(answerToFillFormWith)
        case _                                                                                                       =>
          form
      }

      originalComcodeOpt match {
        case Some(shortCommodity) if commodityCodeSansTrailingZeros(shortCommodity).length == 6 =>
          Ok(view(preparedForm, mode, commodityCodeSansTrailingZeros(shortCommodity), recordId))
        case _                                                                                  =>
          Redirect(routes.JourneyRecoveryController.onPageLoad().url)
      }
  }

  def onSubmit(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val categorisationInfoOpt = request.userAnswers.get(RecordCategorisationsQuery).flatMap(_.records.get(recordId))

      val originalComcodeOpt = categorisationInfoOpt.flatMap(_.originalCommodityCode)
      val latestComcodeOpt   = categorisationInfoOpt.map(_.commodityCode)

      originalComcodeOpt match {
        case Some(shortCommodity) if commodityCodeSansTrailingZeros(shortCommodity).length == 6 =>
          val shortCode = commodityCodeSansTrailingZeros(shortCommodity)
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, shortCode, recordId))),
              value => {
                val longCommodityCode   = s"$shortCode$value"
                val shouldRedirectToCya = latestComcodeOpt.exists(latest =>
                  shortCode.concat(value) == latest &&
                    categorisationInfoOpt.exists(_.latestDoesNotMatchOriginal)
                )

                if (shouldRedirectToCya) {
                  Future.successful(
                    Redirect(
                      navigator
                        .nextPage(LongerCommodityCodePage(recordId, shouldRedirectToCya), mode, request.userAnswers)
                    )
                  )
                } else {
                  saveAnswerAndProceedWithJourney(mode, recordId, value, longCommodityCode, shortCode)
                }
              }
            )
        case _                                                                                  => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
      }
    }

  private def saveAnswerAndProceedWithJourney(
    mode: Mode,
    recordId: String,
    value: String,
    longCommodityCode: String,
    shortCode: String
  )(implicit request: DataRequest[AnyContent]) =
    (for {
      validCommodityCode      <- ottConnector.getCommodityCode(
                                   longCommodityCode,
                                   request.eori,
                                   request.affinityGroup,
                                   UpdateRecordJourney,
                                   Some(recordId)
                                 )
      updatedAnswers          <- Future.fromTry(request.userAnswers.set(LongerCommodityCodePage(recordId), value))
      updatedAnswersWithQuery <- Future.fromTry(updatedAnswers.set(LongerCommodityQuery(recordId), validCommodityCode))
      _                       <- sessionRepository.set(updatedAnswersWithQuery)
    } yield Redirect(navigator.nextPage(LongerCommodityCodePage(recordId), mode, updatedAnswersWithQuery))).recover {
      case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
        val formWithApiErrors =
          form.copy(errors = Seq(FormError("value", getMessage("longerCommodityCode.error.invalid"))))
        BadRequest(view(formWithApiErrors, mode, shortCode, recordId))
    }

  private def getMessage(key: String)(implicit messages: Messages): String = messages(key)

  private def commodityCodeSansTrailingZeros(commodityCode: String): String = {
    val codeNoZeros = commodityCode.reverse.dropWhile(x => x == '0').reverse

    if (codeNoZeros.length >= 6) {
      codeNoZeros
    } else {
      codeNoZeros + "0".repeat(6 - codeNoZeros.length)
    }
  }

}
