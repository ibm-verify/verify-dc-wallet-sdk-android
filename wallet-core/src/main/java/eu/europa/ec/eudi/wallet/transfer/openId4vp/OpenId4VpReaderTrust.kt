/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import android.util.Log
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.openid4vp.X509CertificateTrust
import java.security.cert.X509Certificate

interface OpenId4VpReaderTrust : X509CertificateTrust {
    var readerTrustStore: ReaderTrustStore?
    val result: ReaderTrustResult
}

class OpenId4VpReaderTrustImpl(
    override var readerTrustStore: ReaderTrustStore? = null
) : OpenId4VpReaderTrust {

    private var _result: ReaderTrustResult = ReaderTrustResult.Pending
    override val result: ReaderTrustResult
        get() = _result

    override suspend fun isTrusted(chain: List<X509Certificate>): Boolean {
        Log.d(TAG, "isTrusted() evaluating x5c certificate chain (length: ${chain.size})")
        chain.forEachIndexed { index, cert ->
            val sanList = try {
                cert.subjectAlternativeNames?.map { it.toString() }?.joinToString()
            } catch (e: Exception) {
                "error reading SAN: ${e.message}"
            }
            Log.d(
                TAG,
                "Certificate [$index]:\n" +
                        "  Subject: ${cert.subjectX500Principal?.name}\n" +
                        "  Issuer:  ${cert.issuerX500Principal?.name}\n" +
                        "  Serial:  ${cert.serialNumber?.toString(16)}\n" +
                        "  Valid:   ${cert.notBefore} -> ${cert.notAfter}\n" +
                        "  SigAlg:  ${cert.sigAlgName}\n" +
                        "  SANs:    $sanList"
            )
        }

        val store = readerTrustStore
        if (store == null) {
            Log.e(
                TAG,
                "isTrusted() FAILED: readerTrustStore is null! No trust anchors / ReaderTrustStore configured in wallet."
            )
            _result = ReaderTrustResult.Processed(chain, false)
            return false
        }

        Log.d(TAG, "isTrusted() delegating to readerTrustStore: ${store::class.java.name}")
        val validationResult = try {
            store.validateCertificationTrustPath(chain)
        } catch (e: Throwable) {
            Log.e(TAG, "isTrusted() exception during validateCertificationTrustPath", e)
            false
        }

        Log.d(TAG, "isTrusted() result: $validationResult")
        _result = ReaderTrustResult.Processed(chain, validationResult)
        return validationResult
    }

    companion object {
        private const val TAG = "OpenId4VpReaderTrust"
    }
}

sealed interface ReaderTrustResult {
    data class Processed(
        val chain: List<X509Certificate>,
        val isTrusted: Boolean
    ) : ReaderTrustResult

    data object Pending : ReaderTrustResult
}