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

package controllers.commodityCodeResult

import base.SpecBase
import base.TestConstants.{testEori, testRecordId}
import config.FrontendAppConfig
import connectors.GoodsRecordConnector
import forms.HasCorrectGoodsFormProvider
import models.router.requests.PutRecordRequest
import models.router.responses.GetGoodsRecordResponse
import models.{Commodity, NormalMode, UpdateGoodsRecord, UserAnswers}
import navigation.{FakeNavigation, Navigation}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import pages.goodsRecord.{CommodityCodeUpdatePage, HasCommodityCodeChangePage, HasCountryOfOriginChangePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.CommodityUpdateQuery
import repositories.SessionRepository
import services.{AuditService, AutoCategoriseService, CommodityService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.HasCorrectGoodsView

import java.time.Instant
import scala.concurrent.Future

class UpdateCommodityCodeResultControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider                                      = new HasCorrectGoodsFormProvider()
  private val form                                      = formProvider()
  private lazy val journeyRecoveryContinueUrl           =
    controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId).url
  private val warningPage: HasCountryOfOriginChangePage = HasCountryOfOriginChangePage(testRecordId)
  private val record                                    =
    goodsRecordResponse(Instant.parse("2022-11-18T23:20:19Z"), Instant.parse("2022-11-18T23:20:19Z"))
      .copy(recordId = testRecordId, eori = testEori)

  private val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, commodityCode = Some(testCommodity))

  private val mockGoodsRecordConnector  = mock[GoodsRecordConnector]
  private val mockAuditService          = mock[AuditService]
  private val mockSessionRepository     = mock[SessionRepository]
  private val mockFrontendAppConfig     = mock[FrontendAppConfig]
  private val mockCommodityService      = mock[CommodityService]
  private val mockAutoCategoriseService = mock[AutoCategoriseService]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      mockGoodsRecordConnector,
      mockAuditService,
      mockSessionRepository,
      mockFrontendAppConfig,
      mockCommodityService,
      mockAutoCategoriseService
    )
  }

  "UpdateCommodityCodeController" - {

    "For update journey" - {
      lazy val hasCorrectGoodsUpdateRoute =
        controllers.commodityCodeResult.routes.UpdateCommodityCodeResultController
          .onPageLoad(NormalMode, testRecordId)
          .url
      lazy val onSubmitAction: Call       =
        controllers.commodityCodeResult.routes.UpdateCommodityCodeResultController.onSubmit(NormalMode, testRecordId)
      val page: QuestionPage[Boolean]     = HasCorrectGoodsCommodityCodeUpdatePage(testRecordId)
      val updatePage                      = CommodityCodeUpdatePage(testRecordId)

      "must return OK and the correct view for a GET" in {

        val userAnswers =
          emptyUserAnswers
            .set(CommodityUpdateQuery(testRecordId), Commodity("654321", List("Description"), Instant.now, None))
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request                      = FakeRequest(GET, hasCorrectGoodsUpdateRoute)
          val appConfig: FrontendAppConfig = application.injector.instanceOf[FrontendAppConfig]
          val result                       = route(application, request).value
          val view                         = application.injector.instanceOf[HasCorrectGoodsView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            Commodity("654321", List("Description"), Instant.now, None),
            onSubmitAction,
            NormalMode,
            Some(testRecordId)
          )(
            request,
            messages(application),
            appConfig
          ).toString
        }
      }

      "must redirect on GET to JourneyRecovery Page if user doesn't have commodity answer" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsUpdateRoute)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val commodity   = Commodity("654321", List("Description"), Instant.now, None)
        val userAnswers = emptyUserAnswers
          .set(CommodityUpdateQuery(testRecordId), commodity)
          .success
          .value
          .set(page, true)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request                      = FakeRequest(GET, hasCorrectGoodsUpdateRoute)
          val appConfig: FrontendAppConfig = application.injector.instanceOf[FrontendAppConfig]
          val view                         = application.injector.instanceOf[HasCorrectGoodsView]
          val result                       = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(true),
            commodity,
            onSubmitAction,
            NormalMode,
            Some(testRecordId)
          )(
            request,
            messages(application),
            appConfig
          ).toString
        }
      }

      "onSubmit" - {

        "when true to change code is submitted" - {

          "must redirect to the next page when valid data is submitted" in {

            val userAnswers = emptyUserAnswers
              .set(updatePage, testCommodity.commodityCode)
              .success
              .value
              .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
              .success
              .value
              .set(warningPage, true)
              .success
              .value
              .set(HasCommodityCodeChangePage(testRecordId), true)
              .success
              .value
              .set(CommodityUpdateQuery(testRecordId), testCommodity)
              .success
              .value

            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))
            when(mockGoodsRecordConnector.putGoodsRecord(any(), any())(any())).thenReturn(Future.successful(Done))
            when(mockGoodsRecordConnector.getRecord(any())(any())).thenReturn(Future.successful(record))
            when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
            when(mockCommodityService.isCommodityCodeValid(any(), any())(any(), any()))
              .thenReturn(Future.successful(false))
            when(
              mockAutoCategoriseService.autoCategoriseRecord(any[GetGoodsRecordResponse], any[UserAnswers])(
                any(),
                any()
              )
            ).thenReturn(Future.successful(None))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                  bind[AuditService].toInstance(mockAuditService),
                  bind[CommodityService].toInstance(mockCommodityService),
                  bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
                )
                .build()

            running(application) {
              val request = FakeRequest(POST, hasCorrectGoodsUpdateRoute).withFormUrlEncodedBody(("value", "true"))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(
                result
              ).value mustEqual controllers.goodsRecord.commodityCode.routes.UpdatedCommodityCodeController
                .onPageLoad(testRecordId)
                .url
            }
          }

          "must PUT the goods record, cleanse the data and redirect to the Goods record Page" in {

            val userAnswers = emptyUserAnswers
              .set(updatePage, testCommodity.commodityCode)
              .success
              .value
              .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
              .success
              .value
              .set(warningPage, true)
              .success
              .value
              .set(HasCommodityCodeChangePage(testRecordId), true)
              .success
              .value
              .set(CommodityUpdateQuery(testRecordId), testCommodity)
              .success
              .value

            val newRecord = PutRecordRequest(
              actorId = record.eori,
              traderRef = record.traderRef,
              comcode = testCommodity.commodityCode,
              goodsDescription = record.goodsDescription,
              countryOfOrigin = record.countryOfOrigin,
              category = None,
              assessments = record.assessments,
              supplementaryUnit = record.supplementaryUnit,
              measurementUnit = record.measurementUnit,
              comcodeEffectiveFromDate = record.comcodeEffectiveFromDate,
              comcodeEffectiveToDate = record.comcodeEffectiveToDate
            )

            when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
            when(mockGoodsRecordConnector.putGoodsRecord(any(), any())(any())).thenReturn(Future.successful(Done))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))
            when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future.successful(record)
            when(mockCommodityService.isCommodityCodeValid(any(), any())(any(), any())) thenReturn Future.successful(
              false
            )
            when(
              mockAutoCategoriseService.autoCategoriseRecord(any[GetGoodsRecordResponse], any[UserAnswers])(
                any(),
                any()
              )
            ).thenReturn(Future.successful(None))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                  bind[AuditService].toInstance(mockAuditService),
                  bind[CommodityService].toInstance(mockCommodityService),
                  bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
                )
                .build()
            running(application) {
              val request = FakeRequest(POST, hasCorrectGoodsUpdateRoute)
                .withFormUrlEncodedBody(("value", "true"))
              val result  = route(application, request).value
              status(result) mustEqual SEE_OTHER

              redirectLocation(
                result
              ).value mustBe controllers.goodsRecord.commodityCode.routes.UpdatedCommodityCodeController
                .onPageLoad(testRecordId)
                .url

              verify(mockGoodsRecordConnector).putGoodsRecord(eqTo(newRecord), eqTo(testRecordId))(any())
              verify(mockSessionRepository, times(2)).set(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService)
                  .auditFinishUpdateGoodsRecord(
                    eqTo(testRecordId),
                    eqTo(AffinityGroup.Individual),
                    eqTo(expectedPayload)
                  )(
                    any()
                  )
              }
            }
          }

          "must PATCH the goods record, cleanse the data and redirect to the Goods record Page" in {

            val userAnswers = emptyUserAnswers
              .set(updatePage, testCommodity.commodityCode)
              .success
              .value
              .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
              .success
              .value
              .set(warningPage, true)
              .success
              .value
              .set(HasCommodityCodeChangePage(testRecordId), true)
              .success
              .value
              .set(CommodityUpdateQuery(testRecordId), testCommodity)
              .success
              .value

            when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

            when(mockGoodsRecordConnector.patchGoodsRecord(any())(any())).thenReturn(Future.successful(Done))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))
            when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future
              .successful(record)
            when(
              mockAutoCategoriseService.autoCategoriseRecord(
                any[GetGoodsRecordResponse],
                any[UserAnswers]
              )(any(), any())
            ).thenReturn(Future.successful(None))

            when(mockFrontendAppConfig.useEisPatchMethod) thenReturn false
            when(mockCommodityService.isCommodityCodeValid(any(), any())(any(), any())) thenReturn Future.successful(
              false
            )

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                  bind[AuditService].toInstance(mockAuditService),
                  bind[FrontendAppConfig].toInstance(mockFrontendAppConfig),
                  bind[CommodityService].toInstance(mockCommodityService),
                  bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
                )
                .build()

            running(application) {
              val request = FakeRequest(POST, hasCorrectGoodsUpdateRoute)
                .withFormUrlEncodedBody(("value", "true"))
              val result  = route(application, request).value
              status(result) mustEqual SEE_OTHER

              redirectLocation(
                result
              ).value mustEqual controllers.goodsRecord.commodityCode.routes.UpdatedCommodityCodeController
                .onPageLoad(testRecordId)
                .url

              verify(mockGoodsRecordConnector).patchGoodsRecord(any())(any())
              verify(mockSessionRepository, times(2)).set(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService)
                  .auditFinishUpdateGoodsRecord(
                    eqTo(testRecordId),
                    eqTo(AffinityGroup.Individual),
                    eqTo(expectedPayload)
                  )(any())
              }
            }
          }

          "when commodity code has not been changed must not update the goods record and redirect to the Home Page" in {

            val answer          = Commodity(record.comcode, List("test"), validityStartDate, None)
            val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, commodityCode = Some(answer))

            val userAnswers = emptyUserAnswers
              .set(updatePage, answer.commodityCode)
              .success
              .value
              .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
              .success
              .value
              .set(warningPage, true)
              .success
              .value
              .set(HasCommodityCodeChangePage(testRecordId), true)
              .success
              .value
              .set(CommodityUpdateQuery(testRecordId), answer)
              .success
              .value

            when(mockGoodsRecordConnector.getRecord(eqTo(testRecordId))(any())).thenReturn(Future.successful(record))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))
            when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
            when(mockCommodityService.isCommodityCodeValid(any(), any())(any(), any()))
              .thenReturn(Future.successful(false))
            when(
              mockAutoCategoriseService.autoCategoriseRecord(any[GetGoodsRecordResponse], any[UserAnswers])(
                any(),
                any()
              )
            ).thenReturn(Future.successful(None))

            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
                bind[SessionRepository].toInstance(mockSessionRepository),
                bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                bind[AuditService].toInstance(mockAuditService),
                bind[CommodityService].toInstance(mockCommodityService),
                bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
              )
              .build()

            running(application) {
              val request = FakeRequest(POST, hasCorrectGoodsUpdateRoute)
                .withFormUrlEncodedBody(("value", "true"))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(
                result
              ).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url

              verify(mockGoodsRecordConnector, atLeastOnce()).getRecord(eqTo(testRecordId))(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService, atLeastOnce())
                  .auditFinishUpdateGoodsRecord(
                    eqTo(testRecordId),
                    eqTo(AffinityGroup.Individual),
                    eqTo(expectedPayload)
                  )(
                    any()
                  )
              }
            }
          }

          "when user answers cannot create an update goods record" - {
            "must not submit anything, and redirect to Journey Recovery" in {
              when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future.successful(record)
              when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
              when(mockCommodityService.isCommodityCodeValid(any(), any())(any(), any())) thenReturn Future.successful(
                false
              )

              val application =
                applicationBuilder(userAnswers = Some(emptyUserAnswers))
                  .overrides(
                    bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                    bind[SessionRepository].toInstance(mockSessionRepository),
                    bind[AuditService].toInstance(mockAuditService),
                    bind[CommodityService].toInstance(mockCommodityService)
                  )
                  .build()

              running(application) {
                val request = FakeRequest(POST, hasCorrectGoodsUpdateRoute)
                  .withFormUrlEncodedBody(("value", "true"))
                val result  = route(application, request).value

                status(result) mustEqual SEE_OTHER
                redirectLocation(result).value mustEqual
                  controllers.problem.routes.JourneyRecoveryController
                    .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                    .url

                verify(mockGoodsRecordConnector, atLeastOnce()).getRecord(any())(any())
              }
            }

            "must not submit anything when record is not found, and must let the play error handler deal with connector failure" in {
              when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
              when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future.failed(
                new RuntimeException("Something went very wrong")
              )

              val application =
                applicationBuilder(userAnswers = Some(emptyUserAnswers))
                  .overrides(
                    bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                    bind[SessionRepository].toInstance(mockSessionRepository),
                    bind[AuditService].toInstance(mockAuditService)
                  )
                  .build()

              running(application) {
                val request = FakeRequest(POST, hasCorrectGoodsUpdateRoute)
                  .withFormUrlEncodedBody(("value", "true"))
                intercept[RuntimeException] {
                  await(route(application, request).value)
                  verify(mockGoodsRecordConnector, atLeastOnce()).getRecord(any())(any())
                }
              }
            }

            "must let the play error handler deal with connector failure when updating" in {
              val userAnswers = emptyUserAnswers
                .set(updatePage, testCommodity.commodityCode)
                .success
                .value
                .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
                .success
                .value
                .set(warningPage, true)
                .success
                .value
                .set(HasCommodityCodeChangePage(testRecordId), true)
                .success
                .value
                .set(CommodityUpdateQuery(testRecordId), testCommodity)
                .success
                .value

              when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
              when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
                .thenReturn(Future.successful(Done))
              when(mockGoodsRecordConnector.patchGoodsRecord(any())(any()))
                .thenReturn(Future.failed(new RuntimeException("Connector failed")))
              when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future.successful(record)
              when(mockCommodityService.isCommodityCodeValid(any(), any())(any(), any())) thenReturn Future.successful(
                false
              )

              val application =
                applicationBuilder(userAnswers = Some(userAnswers))
                  .overrides(
                    bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                    bind[AuditService].toInstance(mockAuditService),
                    bind[SessionRepository].toInstance(mockSessionRepository),
                    bind[CommodityService].toInstance(mockCommodityService)
                  )
                  .build()

              running(application) {
                val request = FakeRequest(POST, hasCorrectGoodsUpdateRoute)
                  .withFormUrlEncodedBody(("value", "true"))

                intercept[RuntimeException] {
                  await(route(application, request).value)
                }

                withClue("must call the audit connector with the supplied details") {
                  verify(mockAuditService, atLeastOnce())
                    .auditFinishUpdateGoodsRecord(
                      eqTo(testRecordId),
                      eqTo(AffinityGroup.Individual),
                      eqTo(expectedPayload)
                    )(
                      any()
                    )
                  verify(mockGoodsRecordConnector, atLeastOnce()).getRecord(any())(any())
                  verify(mockGoodsRecordConnector, atLeastOnce()).putGoodsRecord(any(), any())(any())
                }
              }
            }

          }
        }

        "when false to change code is submitted" - {
          "must redirect to correct page" in {

            val userAnswers = emptyUserAnswers
              .set(updatePage, testCommodity.commodityCode)
              .success
              .value
              .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
              .success
              .value
              .set(warningPage, true)
              .success
              .value
              .set(HasCommodityCodeChangePage(testRecordId), false)
              .success
              .value
              .set(CommodityUpdateQuery(testRecordId), testCommodity)
              .success
              .value

            when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
                  bind[SessionRepository].toInstance(mockSessionRepository)
                )
                .build()
            running(application) {
              val request =
                FakeRequest(POST, hasCorrectGoodsUpdateRoute)
                  .withFormUrlEncodedBody(("value", "false"))
              val result  = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual onwardRoute.url
            }
          }
        }
      }

      "must redirect on POST to JourneyRecovery Page if user doesn't have commodity answer" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsUpdateRoute)
              .withFormUrlEncodedBody(("value", ""))
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val commodity   = Commodity("654321", List("Description"), Instant.now, None)
        val userAnswers =
          emptyUserAnswers.set(CommodityUpdateQuery(testRecordId), commodity).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsUpdateRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm                    = form.bind(Map("value" -> ""))
          val appConfig: FrontendAppConfig = application.injector.instanceOf[FrontendAppConfig]
          val view                         = application.injector.instanceOf[HasCorrectGoodsView]
          val result                       = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, commodity, onSubmitAction, NormalMode, Some(testRecordId))(
            request,
            messages(application),
            appConfig
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()
        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsUpdateRoute)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsUpdateRoute)
              .withFormUrlEncodedBody(("value", "true"))
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }
  }
}
