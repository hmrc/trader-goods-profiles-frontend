package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class GoodsRecordsFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "goodsRecords.error.required"
  val lengthKey   = "goodsRecords.error.length"
  val maxLength   = 100

  val form = new GoodsRecordsFormProvider()()

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
