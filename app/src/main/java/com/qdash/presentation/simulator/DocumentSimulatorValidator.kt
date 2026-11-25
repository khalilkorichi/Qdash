package com.qdash.presentation.simulator

import com.qdash.core.utils.AmountConversionEngine
import com.qdash.core.utils.PostalCcpUtils
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
        val completionPercentage: Float,
        val missingFieldLabels: List<String> = emptyList()
    )

    fun validate(state: DocumentSimulatorUiState): ValidationResult {
        val errMap = mutableMapOf<String, String>()
        val warnings = mutableListOf<String>()
        val missingLabels = mutableListOf<String>()
        var filledCount = 0
        var totalCount = 0

        if (state.selectedDocType == DocumentType.CHEQUE) {
            totalCount = 6

            // 1. Amount
            if (state.chequeAmount.isBlank()) {
                errMap["chequeAmount"] = "المبلغ مطلوب بالأرقام"
                missingLabels.add("المبلغ")
            } else {
                val amt = parseAmount(state.chequeAmount)
                if (amt == null || amt <= 0) {
                    errMap["chequeAmount"] = "أدخل مبلغاً صحيحاً أكبر من الصفر"
                    missingLabels.add("المبلغ")
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
                missingLabels.add("المستفيد")
            } else {
                filledCount++
            }

            // 3. CCP Account
            if (state.chequeCcp.isBlank()) {
                errMap["chequeCcp"] = "رقم الحساب البريدي (CCP) مطلوب"
                missingLabels.add("رقم CCP")
            } else {
                filledCount++
            }

            // 4. Clé
            if (state.chequeKey.isBlank()) {
                errMap["chequeKey"] = "المفتاح مطلوب"
                missingLabels.add("المفتاح")
            } else if (state.chequeKey.length != 2) {
                errMap["chequeKey"] = "المفتاح يتكون من رقمين"
                missingLabels.add("المفتاح")
            } else {
                filledCount++
                addCcpKeyWarningIfMismatch(state.chequeCcp, state.chequeKey, warnings)
            }

            // 5. Place
            if (state.chequePlace.isBlank()) {
                errMap["chequePlace"] = "المكان مطلوب"
                missingLabels.add("المكان")
            } else {
                filledCount++
            }

            // 6. Date
            if (state.chequeDate.isBlank()) {
                errMap["chequeDate"] = "التاريخ مطلوب"
                missingLabels.add("التاريخ")
            } else {
                filledCount++
            }

        } else {
            // Chèque Secours (SFP 01)
            val requiresBeneficiary = state.sfpOperation == SfpOperationType.VERSEMENT ||
                state.sfpOperation == SfpOperationType.VIREMENT
            totalCount = if (requiresBeneficiary) 9 else 7

            // 1. Operation type is implicitly selected (always VERSEMENT/RETRAIT/VIREMENT)
            filledCount++

            // 2. CCP
            if (state.sfpCcp.isBlank()) {
                errMap["sfpCcp"] = "رقم الحساب مطلوب"
                missingLabels.add("رقم CCP")
            } else {
                filledCount++
            }

            // 3. Key
            if (state.sfpKey.isBlank()) {
                errMap["sfpKey"] = "المفتاح مطلوب"
                missingLabels.add("المفتاح")
            } else if (state.sfpKey.length != 2) {
                errMap["sfpKey"] = "المفتاح يتكون من رقمين"
                missingLabels.add("المفتاح")
            } else {
                filledCount++
                addCcpKeyWarningIfMismatch(state.sfpCcp, state.sfpKey, warnings)
            }

            // 4. Amount
            if (state.sfpAmount.isBlank()) {
                errMap["sfpAmount"] = "المبلغ مطلوب"
                missingLabels.add("المبلغ")
            } else {
                val amt = parseAmount(state.sfpAmount)
                if (amt == null || amt <= 0) {
                    errMap["sfpAmount"] = "أدخل مبلغاً صحيحاً أكبر من الصفر"
                    missingLabels.add("المبلغ")
                } else {
                    filledCount++
                }
            }

            // 5. Sender Name
            if (state.sfpSenderNom.isBlank() || state.sfpSenderPrenom.isBlank()) {
                errMap["sfpSenderName"] = "معلومات المرسل (الاسم واللقب) مطلوبة"
                missingLabels.add("اسم المرسل")
            } else {
                filledCount++
            }

            // 6. Sender Address
            if (state.sfpSenderAddress.isBlank()) {
                errMap["sfpSenderAddress"] = "عنوان المرسل مطلوب"
                missingLabels.add("عنوان المرسل")
            } else {
                filledCount++
            }

            // 7. Place / Date
            if (state.sfpPlace.isBlank() || state.sfpDate.isBlank()) {
                errMap["sfpPlaceDate"] = "المكان والتاريخ مطلوبان"
                missingLabels.add("المكان والتاريخ")
            } else {
                filledCount++
            }

            // For Versement and Virement, Beneficiary info is required
            if (requiresBeneficiary) {
                if (state.sfpBeneficiaryNom.isBlank() || state.sfpBeneficiaryPrenom.isBlank()) {
                    errMap["sfpBeneficiaryName"] = "معلومات المستفيد (المرسل إليه) مطلوبة"
                    missingLabels.add("اسم المستفيد")
                } else {
                    filledCount++
                }

                if (state.sfpBeneficiaryAddress.isBlank()) {
                    warnings.add("تنبيه: يفضل ملء عنوان المستفيد لتسهيل مطابقة الحوالة.")
                    missingLabels.add("عنوان المستفيد")
                } else {
                    filledCount++
                }
            }

            if (state.sfpReleveOperations && (state.sfpReleveDu.isBlank() || state.sfpReleveAu.isBlank())) {
                warnings.add("تنبيه: تم تحديد «كشف العمليات» — يُنصح بتحديد تاريخ البداية (من) والنهاية (إلى).")
            }
        }

        val percentage = (filledCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
        return ValidationResult(
            errors = errMap,
            warnings = warnings,
            completionPercentage = percentage,
            missingFieldLabels = missingLabels
        )
    }

    private fun addCcpKeyWarningIfMismatch(
        ccp: String,
        key: String,
        warnings: MutableList<String>
    ) {
        if (ccp.isBlank() || key.length != 2) return
        val expectedKey = PostalCcpUtils.calculateCcpKey(ccp) ?: return
        if (!PostalCcpUtils.isValidCcpKey(ccp, key)) {
            warnings.add("تنبيه: مفتاح الحساب ($key) لا يطابق المفتاح المحسوب لحساب CCP (المتوقع: $expectedKey).")
        }
    }

    private fun parseAmount(value: String): Double? {
        return AmountConversionEngine.parseFlexibleAmount(value)
    }

    internal fun applyAutofill(
        state: DocumentSimulatorUiState,
        profile: PostalProfile,
        role: PostalProfileRole
    ): DocumentSimulatorUiState {
        val profileCity = profile.city?.trim()?.takeIf { it.isNotEmpty() }
        return if (state.selectedDocType == DocumentType.CHEQUE) {
            when (role) {
                PostalProfileRole.SELF -> {
                    val holderName = "${profile.lastName} ${profile.firstName}".trim()
                        .ifBlank { profile.fullName.trim() }
                    val holderAddress = profile.address?.takeIf { it.isNotBlank() }
                        ?: profileCity
                        ?: ""
                    state.copy(
                        chequeCcp = profile.accountNumber,
                        chequeKey = profile.accountKey,
                        chequeBeneficiary = if (state.chequeWordsLanguage == WordsLanguage.FRENCH) "Moi-même" else "لنفسي",
                        chequePlace = profileCity ?: state.chequePlace,
                        chequeAccountHolder = holderName,
                        chequeAccountAddress = holderAddress
                    )
                }
                PostalProfileRole.BENEFICIARY -> state.copy(
                    chequeBeneficiary = "${profile.lastName} ${profile.firstName} ccp ${profile.accountNumber} clé ${profile.accountKey}",
                    chequePlace = if (state.chequePlace.isBlank() && profileCity != null) profileCity else state.chequePlace
                )
                else -> state
            }
        } else {
            when (role) {
                PostalProfileRole.SELF, PostalProfileRole.SENDER -> state.copy(
                    sfpSenderNom = profile.lastName,
                    sfpSenderPrenom = profile.firstName,
                    sfpSenderAddress = profile.address ?: "",
                    sfpSenderPhone = profile.phone ?: "",
                    sfpPlace = profileCity ?: state.sfpPlace,
                    sfpCcp = if (state.sfpOperation == SfpOperationType.RETRAIT) profile.accountNumber else state.sfpCcp,
                    sfpKey = if (state.sfpOperation == SfpOperationType.RETRAIT) profile.accountKey else state.sfpKey
                )
                PostalProfileRole.BENEFICIARY -> {
                    val fillsAccount = state.sfpOperation == SfpOperationType.VERSEMENT ||
                        state.sfpOperation == SfpOperationType.VIREMENT
                    state.copy(
                        sfpBeneficiaryNom = profile.lastName,
                        sfpBeneficiaryPrenom = profile.firstName,
                        sfpBeneficiaryAddress = profile.address ?: "",
                        sfpBeneficiaryPhone = profile.phone ?: "",
                        sfpPlace = if (state.sfpPlace.isBlank() && profileCity != null) profileCity else state.sfpPlace,
                        sfpCcp = if (fillsAccount) profile.accountNumber else state.sfpCcp,
                        sfpKey = if (fillsAccount) profile.accountKey else state.sfpKey
                    )
                }
            }
        }
    }

    internal fun getGuideFieldName(docType: DocumentType, step: Int): String? {
        return if (docType == DocumentType.CHEQUE) {
            when (step) {
                0 -> "chequeCcp"
                1 -> "chequeAmount"
                2 -> "chequeBeneficiary"
                3 -> "chequePlace"
                4 -> "chequeDate"
                5 -> "chequeSignature"
                else -> null
            }
        } else {
            when (step) {
                0 -> "sfpOperation"
                1 -> "sfpCcp"
                2 -> "sfpAmount"
                3 -> "sfpSenderName"
                4 -> "sfpSenderAddress"
                5 -> "sfpBeneficiaryName"
                6 -> "sfpBeneficiaryAddress"
                7 -> "sfpPlaceDate"
                8 -> "sfpSignature"
                else -> null
            }
        }
    }

    internal fun getStepIndexForField(docType: DocumentType, fieldName: String): Int {
        return if (docType == DocumentType.CHEQUE) {
            when (fieldName) {
                "chequeCcp", "chequeKey" -> 0
                "chequeAmount" -> 1
                "chequeBeneficiary" -> 2
                "chequePlace" -> 3
                "chequeDate" -> 4
                "chequeSignature" -> 5
                else -> -1
            }
        } else {
            when (fieldName) {
                "sfpOperation", "sfpJustificatifCcp", "sfpAvisCredit", "sfpCarnetCheques", "sfpCodeConfidentiel", "sfpRip" -> 0
                "sfpCcp", "sfpKey" -> 1
                "sfpAmount" -> 2
                "sfpSenderNom", "sfpSenderPrenom", "sfpSenderName" -> 3
                "sfpSenderAddress", "sfpSenderPhone" -> 4
                "sfpBeneficiaryNom", "sfpBeneficiaryPrenom", "sfpBeneficiaryName" -> 5
                "sfpBeneficiaryAddress", "sfpBeneficiaryPhone" -> 6
                "sfpPlace", "sfpDate", "sfpMotif" -> 7
                "sfpSignature", "sfpIdDescription" -> 8
                else -> -1
            }
        }
    }
}
