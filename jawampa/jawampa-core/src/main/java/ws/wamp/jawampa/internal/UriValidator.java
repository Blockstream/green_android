/*
 * Copyright 2014 Matthias Einwag
 *
 * The jawampa authors license this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package ws.wamp.jawampa.internal;

import java.util.regex.Pattern;

import ws.wamp.jawampa.ApplicationError;

public abstract class UriValidator {
    
    static final Pattern LOOSE_URI = Pattern.compile("^([^\\s\\.#]+\\.)*([^\\s\\.#]+)$");
    static final Pattern LOOSE_URI_PREFIX = Pattern.compile("^([^\\s\\.#]+\\.)*([^\\s\\.#]+)?$");
    static final Pattern LOOSE_URI_WILDCARD = Pattern.compile("^(([^\\s\\.#]+\\.)|\\.)*([^\\s\\.#]+)?$");

    static final Pattern STRICT_URI = Pattern.compile("^([0-9a-z_]+\\.)*([0-9a-z_]+)$");
    static final Pattern STRICT_URI_PREFIX = Pattern.compile("^([0-9a-z_]+\\.)*([0-9a-z_]+)?$");
    static final Pattern STRICT_URI_WILDCARD = Pattern.compile("^(([0-9a-z_]+\\.)|\\.)*([0-9a-z_]+)?$");

    private static boolean tryValidate(String uri, Pattern pattern) {
        return uri!=null && pattern.matcher(uri).matches();
    }

    private static void validate(String uri, Pattern pattern) throws ApplicationError {
        boolean isValid = tryValidate(uri, pattern);
        if (!isValid)
            throw new ApplicationError(ApplicationError.INVALID_URI);
    }

    /**
     * Checks a WAMP Uri for validity.
     * @param uri The uri that should be validated
     * @param useStrictValidation Whether the strict Uri validation pattern
     * described in the WAMP spec shall be used for validation
     * @return true if Uri is valid, false otherwise
     */
    public static boolean tryValidate(String uri, boolean useStrictValidation) {
        return tryValidate(uri, useStrictValidation ? STRICT_URI : LOOSE_URI);
    }
    
    /**
     * Checks a WAMP Prefix Uri for validity.
     * @param uri The prefix uri that should be validated
     * @param useStrictValidation Whether the strict Uri validation pattern
     * described in the WAMP spec shall be used for validation
     * @return true if Prefix Uri is valid, false otherwise
     */
    public static boolean tryValidatePrefix(String uri, boolean useStrictValidation) {
        return tryValidate(uri, useStrictValidation ? STRICT_URI_PREFIX : LOOSE_URI_PREFIX);
    }
    
    /**
     * Checks a WAMP Wildcard Uri for validity.
     * @param uri The wildcard uri that should be validated
     * @param useStrictValidation Whether the strict Uri validation pattern
     * described in the WAMP spec shall be used for validation
     * @return true if Wildcard Uri is valid, false otherwise
     */
    public static boolean tryValidateWildcard(String uri, boolean useStrictValidation) {
        return tryValidate(uri, useStrictValidation ? STRICT_URI_WILDCARD : LOOSE_URI_WILDCARD);
    }

    /**
     * Checks a WAMP Uri for validity.
     * @param uri The uri that should be validated
     * @param useStrictValidation Whether the strict Uri validation pattern
     * described in the WAMP spec shall be used for validation
     * @throws ApplicationError If the Uri is not valid
     */
    public static void validate(String uri, boolean useStrictValidation) throws ApplicationError {
        validate(uri, useStrictValidation ? STRICT_URI : LOOSE_URI);
    }
    
    /**
     * Checks a WAMP Prefix Uri for validity.
     * @param uri The prefix uri that should be validated
     * @param useStrictValidation Whether the strict Uri validation pattern
     * described in the WAMP spec shall be used for validation
     * @throws ApplicationError If the Prefix Uri is not valid
     */
    public static void validatePrefix(String uri, boolean useStrictValidation) throws ApplicationError {
        validate(uri, useStrictValidation ? STRICT_URI_PREFIX : LOOSE_URI_PREFIX);
    }

    /**
     * Checks a WAMP Wildcard Uri for validity.
     * @param uri The wildcard uri that should be validated
     * @param useStrictValidation Whether the strict Uri validation pattern
     * described in the WAMP spec shall be used for validation
     * @throws ApplicationError If the Wildcard Uri is not valid
     */
    public static void validateWildcard(String uri, boolean useStrictValidation) throws ApplicationError {
        validate(uri, useStrictValidation ? STRICT_URI_WILDCARD : LOOSE_URI_WILDCARD);
    }
}
