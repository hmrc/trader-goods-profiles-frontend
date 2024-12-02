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
import models.outboundLink.OutboundLink.allLinks
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuditService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import javax.inject.Inject

class OutboundController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  auditService: AuditService,
  val controllerComponents: MessagesControllerComponents
) extends BaseController {

  def redirect(link: String): Action[AnyContent] = identify { implicit request =>
    val linkInfo = allLinks.find(_.link == link)

    linkInfo match {
      case Some(linkInfo) =>
        val message = messagesApi.preferred(request)(linkInfo.linkTextKey)
        auditService.auditOutboundClick(request.affinityGroup, request.eori, link, message)
        Redirect(linkInfo.link)
      case None           =>
        Redirect(
          controllers.problem.routes.JourneyRecoveryController
            .onPageLoad(continueUrl = Some(RedirectUrl(routes.HelpAndSupportController.onPageLoad().url)))
        )
    }
  }
}
