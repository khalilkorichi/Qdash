package com.qdash.presentation.simulator

import com.qdash.domain.model.PostalProfile
import com.qdash.domain.model.PostalProfileRole

/**
 * Helper to perform validation on postal document simulator values.
 * Extracted from DocumentSimulatorViewModel to keep the ViewModel under the SIZE-001 500-line limit.
 */
object DocumentSimulatorValidator {

    data class ValidationResult(
        val errors: Map<String, String>,
        val warnings: List<String>,
        val completionPercentage: Float
    )

    fun validate(state: DocumentSimulatorUiState): ValidationResult {
        val errMap = mutableMapOf<String, String>()
        val warnings = mutableListOf<String>()
        var filledCount = 0
        var totalCount = 0

        if (state.selectedDocType == DocumentType.CHEQUE) {
            totalCount = 6

            // 1. Amount
            if (state.chequeAmount.isBlank()) {
                errMap["chequeAmount"] = "المبلغ مطلوب بالأرقام"
            } else {
                val amt = parseAmount(state.chequeAmount)
                if (amt == null || amt <= 0) {
                    errMap["chequeAmount"] = "أدخل مبلغاً صحيحاً أكبر من الصفر"
                } else {
                    filledCount++
                    if (state.chequeAmount.contains(Regex("[.,]")) && !state.chequeAmount.endsWith("00")) {
                        warnings.add("تنبيه: الصك يملأ بالدينار وليس السنتيم، تأكد من حذف الصفرين الإضافيين!")
                    }
                }
            }

            // 2. Beneficiary
            if (state.chequeBeneficiary.isBlank()) {
                errMap["chequeBeneficiary"] = "اسم المستفيد مطلوب (لأمر)"
            } else {
                filledCount++
            }

            // 3. CCP Account
            if (state.chequeCcp.isBlank()) {
                errMap["chequeCcp"] = "رقم الحساب البريدي (CCP) مطلوب"
            } else {
                filledCount++
            }

            // 4. Clé
            if (state.chequeKey.isBlank()) {
                errMap["chequeKey"] = "المفتاح مطلوب"
            } else if (state.chequeKey.length != 2) {
                errMap["chequeKey"] = "المفتاح يتكون من رقمين"
            } else {
                filledCount++
            }

            // 5. Place
            if (state.chequePlace.isBlank()) {
                errMap["chequePlace"] = "المكان مطلوب"
            } else {
                filledCount++
            }

            // 6. Date
            if (state.chequeDate.isBlank()) {
                errMap["chequeDate"] = "التاريخ مطلوب"
            } else {
                filledCount++
            }

        } else {
            // SFP 01
            val isVersement = state.sfpOperation == SfpOperationType.VERSEMENT
            totalCount = if (isVersement) 10 else 7

            // 1. Operation type is implicitly selected (always VERSEMENT/RETRAIT/VIREMENT)
            filledCount++

            // 2. CCP
            if (state.sfpCcp.isBlank()) {
                errMap["sfpCcp"] = "رقم الحساب مطلوب"
            } else {
                filledCount++
            }

            // 3. Key
            if (state.sfpKey.isBlank()) {
                errMap["sfpKey"] = "المفتاح مطلوب"
            } else if (state.sfpKey.length != 2) {
                errMap["sfpKey"] = "المفتاح يتكون من رقمين"
            } else {
                filledCount++
            }

            // 4. Amount
            if (state.sfpAmount.isBlank()) {
                errMap["sfpAmount"] = "المبلغ مطلوب"
            } else {
                val amt = parseAmount(state.sfpAmount)
                if (amt == null || amt <= 0) {
                    errMap["sfpAmount"] = "أدخل مبلغاً صحيحاً أكبر من الصفر"
                } else {
                    filledCount++
                }
            }

            // 5. Sender Name
            if (state.sfpSenderNom.isBlank() || state.sfpSenderPrenom.isBlank()) {
                errMap["sfpSenderName"] = "معلومات المرسل (الاسم واللقب) مطلوبة"
            } else {
                filledCount++
            }

            // 6. Sender Address
            if (state.sfpSenderAddress.isBlank()) {
                errMap["sfpSenderAddress"] = "عنوان المرسل مطلوب"
            } else {
                filledCount++
            }

            // 7. Place / Date
            if (state.sfpPlace.isBlank() || state.sfpDate.isBlank()) {
                errMap["sfpPlaceDate"] = "المكان والتاريخ مطلوبان"
            } else {
                filledCount++
            }

            // For Versement, Beneficiary info is required
            if (isVersement) {
                if (state.sfpBeneficiaryNom.isBlank() || state.sfpBeneficiaryPrenom.isBlank()) {
                    errMap["sfpBeneficiaryName"] = "معلومات المستفيد (المرسل إليه) مطلوبة للدفع"
                } else {
                    filledCount++
                }

                if (state.sfpBeneficiaryAddress.isBlank()) {
                    warnings.add("تنبيه: يفضل ملء عنوان المستفيد لتسهيل مطابقة الحوالة.")
                } else {
                    filledCount++
                }

                // Placeholder to represent beneficiary requirement
                filledCount++
            }
        }

        val percentage = (filledCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
        return ValidationResult(
            errors = errMap,
            warnings = warnings,
            completionPercentage = percentage
        )
    }

    private fun parseAmount(value: String): Double? {
        val compact = value.trim().replace(" ", "")
        if (compact.isBlank()) return null
        val normalized = if (compact.count { it == ',' } == 1 && !compact.contains('.')) {
            compact.replace(',', '.')
        } else {
            compact
        }
        return normalized.toDoubleOrNull()
    }
}
