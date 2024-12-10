/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.qaware.xff.filter;

import de.qaware.xff.util.uri.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 *
 */
class ForwardedHeaderExtractingResponse extends HttpServletResponseWrapper {

    private static final String FOLDER_SEPARATOR = "/";

    private final HttpServletRequest request;

    public ForwardedHeaderExtractingResponse(HttpServletResponse response, HttpServletRequest request) {
        super(response);
        this.request = request;
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(location);

        // Absolute location
        if (builder.build().getScheme() != null) {
            super.sendRedirect(location);
            return;
        }

        // Network-path reference
        if (location.startsWith("//")) {
            String scheme = this.request.getScheme();
            super.sendRedirect(builder.scheme(scheme).toUriString());
            return;
        }


        String fragment = null;
        int fragmentIndex = location.indexOf('#');
        if (fragmentIndex != -1) {
            if (location.length() > fragmentIndex) {
                fragment = location.substring(fragmentIndex + 1);
            }
            location = location.substring(0, fragmentIndex);
        }

        String query = null;
        int queryIndex = location.indexOf('?');
        if (queryIndex != -1) {
            if (location.length() > queryIndex) {
                query = location.substring(queryIndex + 1);
            }
            location = location.substring(0, queryIndex);
        }

        // Relative to Servlet container root or to current request
        String path = (location.startsWith(FOLDER_SEPARATOR) ? location :
                applyRelativePath(this.request.getRequestURI(), location));

        String result = UriComponentsBuilder
                .fromHttpRequest(this.request)
                .replacePath(path)
                .replaceQuery(query)
                .fragment(fragment)
                .build().normalize().toUriString();

        super.sendRedirect(result);
    }

    private static String applyRelativePath(String path, String relativePath) {
        int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR);
        if (separatorIndex != -1) {
            String newPath = path.substring(0, separatorIndex);
            if (!relativePath.startsWith(FOLDER_SEPARATOR)) {
                newPath += FOLDER_SEPARATOR;
            }
            return newPath + relativePath;
        } else {
            return relativePath;
        }
    }
}
