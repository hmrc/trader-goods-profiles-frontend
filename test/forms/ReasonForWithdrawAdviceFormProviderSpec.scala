package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class ReasonForWithdrawAdviceFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "reasonForWithdrawAdvice.error.required"
  val lengthKey = "reasonForWithdrawAdvice.error.length"
  val maxLength = 512

  val form = new ReasonForWithdrawAdviceFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
