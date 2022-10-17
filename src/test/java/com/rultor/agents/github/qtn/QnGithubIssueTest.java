/**
 * Copyright (c) 2009-2022 Yegor Bugayenko
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the rultor.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rultor.agents.github.qtn;

import com.jcabi.github.Comment;
import com.jcabi.github.Issue;
import com.jcabi.github.Repo;
import com.jcabi.github.mock.MkGithub;
import com.jcabi.matchers.XhtmlMatchers;
import com.jcabi.xml.StrictXML;
import com.jcabi.xml.XMLDocument;
import com.rultor.agents.github.Question;
import com.rultor.agents.github.Req;
import com.rultor.spi.Talk;
import java.net.URI;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.xembly.Directive;
import org.xembly.Directives;
import org.xembly.Xembler;

/**
 * Tests for ${@link QnGithubIssue}.
 *
 * @author Andrej Istomin (andrej.istomin.ikeen@gmail.com)
 * @version $Id$
 * @since 2.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class QnGithubIssueTest {

    /**
     * QnGithubIssue can pass github_issue as env variable.
     * @throws Exception In case of error.
     */
    @Test
    public void canAddGithubIssueVariable() throws Exception {
        final Repo repo = new MkGithub().randomRepo();
        final Issue issue = repo.issues().create("", "");
        issue.comments().post("test comment.");
        final Question origin = new Question() {
            @Override
            public Req understand(final Comment.Smart comment, final URI home) {
                return new Req() {
                    @Override
                    public Iterable<Directive> dirs() {
                        return new Directives().add("type").set("xxx").up();
                    }
                };
            }
        };
        MatcherAssert.assertThat(
            new StrictXML(
                new XMLDocument(
                    new Xembler(
                        new Directives().add("talk")
                            .attr("name", "abc")
                            .attr("later", "false")
                            .attr("number", "123")
                            .add("request")
                            .attr("id", "a1b2c3")
                            .add("author").set("yegor256").up()
                            .append(
                                new QnGithubIssue(origin).understand(
                                    new Comment.Smart(
                                        issue.comments().get(1)
                                    ),
                                    new URI("#")
                                ).dirs()
                            )
                            .addIf("args")
                    ).xml()
                ),
                Talk.SCHEMA
            ),
            XhtmlMatchers.hasXPaths(
                "/talk/request[type='xxx']",
                "/talk/request/args[count(arg) = 1]",
                String.format(
                    "/talk/request/args/arg[@name='github_issue' and .='%d']",
                    issue.number()
                )
            )
        );
    }
}
