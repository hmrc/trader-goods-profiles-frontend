package forms

import forms.behaviours.IntFieldBehaviours
import play.api.data.FormError

class SupplementaryUnitFormProviderSpec extends IntFieldBehaviours {

  val form = new SupplementaryUnitFormProvider()()

  ".value" - {

    val fieldName = "value"

    val minimum = 0
    val maximum = Int.MaxValue

    val validDataGenerator = intsInRangeWithCommas(minimum, maximum)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like intField(
      form,
      fieldName,
      nonNumericError  = FormError(fieldName, "supplementaryUnit.error.nonNumeric"),
      wholeNumberError = FormError(fieldName, "supplementaryUnit.error.wholeNumber")
    )

    behave like intFieldWithRange(
      form,
      fieldName,
      minimum       = minimum,
      maximum       = maximum,
      expectedError = FormError(fieldName, "supplementaryUnit.error.outOfRange", Seq(minimum, maximum))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "supplementaryUnit.error.required")
    )
  }
}
