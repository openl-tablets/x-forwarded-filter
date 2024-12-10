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

import de.qaware.xff.util.HttpHeaders;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for {@link RelativeRedirectFilter}.
 *
 * @author Rob Winch
 * @author Juergen Hoeller
 */
public class RelativeRedirectFilterTest {

    private RelativeRedirectFilter filter = new RelativeRedirectFilter();

    private HttpServletResponse response = Mockito.mock(HttpServletResponse.class);


    @Test(expected = IllegalArgumentException.class)
    public void sendRedirectHttpStatusWhenNot3xxThenIllegalArgumentException() {
        this.filter.setRedirectStatus(HttpStatus.OK.value());
    }

    @Test
    public void doFilterSendRedirectWhenDefaultsThenLocationAnd303() throws Exception {
        String location = "/foo";
        sendRedirect(location);

        InOrder inOrder = Mockito.inOrder(this.response);
        inOrder.verify(this.response).setStatus(HttpStatus.SEE_OTHER.value());
        inOrder.verify(this.response).setHeader(HttpHeaders.LOCATION, location);
    }

    @Test
    public void doFilterSendRedirectWhenCustomSendRedirectHttpStatusThenLocationAnd301() throws Exception {
        String location = "/foo";
        HttpStatus status = HttpStatus.MOVED_PERMANENTLY;
        this.filter.setRedirectStatus(status.value());
        sendRedirect(location);

        InOrder inOrder = Mockito.inOrder(this.response);
        inOrder.verify(this.response).setStatus(status.value());
        inOrder.verify(this.response).setHeader(HttpHeaders.LOCATION, location);
    }

    @Test
    public void wrapOnceOnly() throws Exception {
        HttpServletResponse original = new MockHttpServletResponse();

        MockFilterChain chain = new MockFilterChain();
        this.filter.doFilterInternal(new MockHttpServletRequest(), original, chain);

        HttpServletResponse wrapped1 = (HttpServletResponse) chain.getResponse();
        assertNotSame(original, wrapped1);

        chain.reset();
        this.filter.doFilterInternal(new MockHttpServletRequest(), wrapped1, chain);
        HttpServletResponse current = (HttpServletResponse) chain.getResponse();
        assertSame(wrapped1, current);

        chain.reset();
        HttpServletResponse wrapped2 = new HttpServletResponseWrapper(wrapped1);
        this.filter.doFilterInternal(new MockHttpServletRequest(), wrapped2, chain);
        current = (HttpServletResponse) chain.getResponse();
        assertSame(wrapped2, current);
    }


    private void sendRedirect(String location) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        this.filter.doFilterInternal(new MockHttpServletRequest(), this.response, chain);

        HttpServletResponse wrappedResponse = (HttpServletResponse) chain.getResponse();
        wrappedResponse.sendRedirect(location);
    }

}
