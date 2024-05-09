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

package controllers.actions

import javax.inject.Inject
import controllers.routes
import models.CheckMode
import models.requests.DataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}

import scala.concurrent.{ExecutionContext, Future}

class ValidateMaintainProfileAnswersActionImpl @Inject() (implicit val executionContext: ExecutionContext)
    extends ValidateMaintainProfileAnswersAction {

  override protected def refine[A](request: DataRequest[A]): Future[Either[Result, DataRequest[A]]] =
    request.userAnswers.maintainProfileAnswers match {
      // TODO change to appropriate error page
      case x if x.ukimsNumber.isEmpty                                 =>
        Future.successful(Left(Redirect(routes.UkimsNumberController.onPageLoad(CheckMode))))
      // TODO change to appropriate error page
      case x if x.hasNirms.isEmpty                                    =>
        Future.successful(Left(Redirect(routes.NirmsQuestionController.onPageLoad(CheckMode))))
      // TODO change to appropriate error page
      case x if x.nirmsNumber.isEmpty && x.hasNirms.contains(true)    =>
        Future.successful(Left(Redirect(routes.NirmsNumberController.onPageLoad(CheckMode))))
      // TODO change to appropriate error page
      case x if x.hasNiphl.isEmpty                                    =>
        Future.successful(Left(Redirect(routes.NiphlQuestionController.onPageLoad(CheckMode))))
      // TODO change to appropriate error page
      case x if x.niphlNumber.isEmpty && x.hasNiphl.contains(true)    =>
        Future.successful(Left(Redirect(routes.NiphlNumberController.onPageLoad(CheckMode))))
      // TODO change to appropriate error page
      case x if x.hasNirms.contains(false) && x.nirmsNumber.isDefined =>
        Future.successful(Left(Redirect(routes.DummyController.onPageLoad)))
      // TODO change to appropriate error page
      case x if x.hasNiphl.contains(false) && x.niphlNumber.isDefined =>
        Future.successful(Left(Redirect(routes.DummyController.onPageLoad)))
      case _                                                          => Future.successful(Right(request))
    }

}

trait ValidateMaintainProfileAnswersAction extends ActionRefiner[DataRequest, DataRequest]
