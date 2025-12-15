package com.blockstream.data.extensions

import com.eygraber.uri.Uri

/**
 * Returns the value of the first query parameter named [key] for this [Uri],
 * safely handling both hierarchical and opaque URIs.
 *
 * Why: Some URI implementations (including KMP/Android-style APIs) only allow
 * `getQueryParameter` on hierarchical URIs (those with an authority path), and
 * will throw for opaque URIs such as `liquidnetwork:lq1...?...`.
 *
 * Behavior:
 * - If the URI is hierarchical (`isHierarchical == true`), this delegates to
 *   the standard `getQueryParameter(key)` which performs decoding consistent
 *   with the underlying URI library.
 * - If the URI is opaque, it performs a very light parsing of the raw
 *   `query` portion by splitting on `&` and `=` and returning the first match
 *   for [key]. In this opaque branch, the returned value is not percent-decoded
 *   and `+` is not converted to space.
 *
 * Notes and limitations for opaque URIs:
 * - This parser is minimal and does not handle all encoding edge cases.
 * - If you need robust decoding (`%xx`, `+` to space) or to parse directly from
 *   a raw URI string (without first creating a `Uri`), prefer
 *   `com.blockstream.common.utils.UriUtils.getQueryParameter(uriString, name)`.
 *
 * Examples:
 * - Opaque: `"liquidnetwork:lq1...?...&assetid=abc&amount=1".toKmpUri().getSafeQueryParameter("assetid")`
 *   returns `"abc"`.
 * - Hierarchical: `"https://example.com/?q=hello%20world".toKmpUri().getSafeQueryParameter("q")`
 *   returns `"hello world"` (decoded by the underlying library).
 *
 * @param key the name of the query parameter to look up
 * @return the parameter value if present; otherwise `null`
 */
fun Uri.getSafeQueryParameter(key: String): String? {
    if (isHierarchical) return getQueryParameter(key)

    query?.let { query ->
        query.split("&").forEach { parts ->
            parts.split("=").also {
                if (it.getOrNull(0) == key) return it.getOrNull(1)
            }
        }
    }

    return null
}
