/*
 * Copyright (c) 2023-2025 European Commission
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
package eu.europa.ec.eudi.iso18013.transfer.sample

import COSE.AlgorithmID.ECDSA_256
import COSE.HeaderKeys.Algorithm
import COSE.OneKey
import COSE.Sign1Message
import com.upokecenter.cbor.CBORObject
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.multipaz.crypto.EcPublicKey
import org.multipaz.mdoc.mso.MobileSecurityObjectGenerator
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Date
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val bc = BouncyCastleProvider()

// The issuer key pair and certificate are generated per test run instead of being committed,
// so that no private key material is stored in source control.
private val issuerKeyPair: KeyPair = KeyPairGenerator.getInstance("EC", bc)
    .apply { initialize(ECGenParameterSpec("secp256r1")) }
    .generateKeyPair()

@JvmSynthetic
internal val issuerPrivateKey: PrivateKey = issuerKeyPair.private

@JvmSynthetic
internal val issuerCertificate: X509Certificate = run {
    val subject = X500Name("CN=Sample Issuer DS (test only),O=IBM Verify,C=US")
    val now = System.currentTimeMillis()
    val builder = JcaX509v3CertificateBuilder(
        subject,
        BigInteger.valueOf(now),
        Date(now - 24L * 60L * 60L * 1000L),
        Date(now + 365L * 24L * 60L * 60L * 1000L),
        subject,
        issuerKeyPair.public
    )
    val signer = JcaContentSignerBuilder("SHA256withECDSA").setProvider(bc).build(issuerKeyPair.private)
    JcaX509CertificateConverter().setProvider(bc).getCertificate(builder.build(signer))
}

@get:JvmSynthetic
internal val PrivateKey.oneKey
    get() = OneKey(null, this)

@OptIn(ExperimentalTime::class)
@JvmSynthetic
internal fun generateMso(
    digestAlg: String,
    docType: String,
    authKey: EcPublicKey,
    nameSpaces: CBORObject,
) =
    MobileSecurityObjectGenerator(org.multipaz.crypto.Algorithm.fromHashAlgorithmIdentifier(digestAlg), docType, authKey)
        .apply {
            val now = Clock.System.now()
            val validUntil =
                Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + 1000L * 60L * 60L * 24L * 365L)
            setValidityInfo(now, now, validUntil, null)

            val digestIds = nameSpaces.entries.associate { (nameSpace, issuerSignedItems) ->
                nameSpace.AsString() to calculateDigests(digestAlg, issuerSignedItems)
            }
            digestIds.forEach { (nameSpace, digestIds) ->
                addDigestIdsForNamespace(nameSpace, digestIds)
            }
        }
        .generate()

@JvmSynthetic
internal fun calculateDigests(
    digestAlg: String,
    issuerSignedItems: CBORObject
): Map<Long, ByteArray> {
    return issuerSignedItems.values.associate { issuerSignedItemBytes ->
        val issuerSignedItem = issuerSignedItemBytes.getEmbeddedCBORObject()
        val digest = MessageDigest.getInstance(digestAlg)
            .digest(issuerSignedItemBytes.EncodeToBytes())
        issuerSignedItem["digestID"].AsInt32().toLong() to digest
    }
}

@JvmSynthetic
internal fun signMso(mso: ByteArray) = Sign1Message(false, true).apply {
    protectedAttributes.Add(Algorithm.AsCBOR(), ECDSA_256.AsCBOR())
    unprotectedAttributes.Add(33L, issuerCertificate.encoded)
    SetContent(mso.withTag24())
    sign(issuerPrivateKey.oneKey)
}.EncodeToCBORObject()

@JvmSynthetic
internal fun generateData(
    issuerNameSpaces: CBORObject,
    issuerAuth: CBORObject,
): ByteArray {
    return mapOf(
        "nameSpaces" to issuerNameSpaces,
        "issuerAuth" to issuerAuth,
    ).let { CBORObject.FromObject(it).EncodeToBytes() }
}
