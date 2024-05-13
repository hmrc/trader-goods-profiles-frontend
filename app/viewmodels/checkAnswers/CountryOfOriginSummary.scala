package viewmodels.checkAnswers

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.CountryOfOriginPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object CountryOfOriginSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(CountryOfOriginPage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "countryOfOrigin.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlFormat.escape(answer).toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.CountryOfOriginController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("countryOfOrigin.change.hidden"))
          )
        )
    }
}
