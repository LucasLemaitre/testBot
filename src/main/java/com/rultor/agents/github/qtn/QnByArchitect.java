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

import com.jcabi.aspects.Immutable;
import com.jcabi.github.Comment;
import com.jcabi.github.Repo;
import com.rultor.agents.github.Answer;
import com.rultor.agents.github.Question;
import com.rultor.agents.github.Req;
import com.rultor.spi.Profile;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.cactoos.iterable.Mapped;
import org.cactoos.list.ListOf;

/**
 * Question by architect only (if configured).
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 1.45
 * @todo #1246:30min PR by ARC merge shouldn't require confirmation by ARC.
 *  Implement the asked in #1246. The tests have already benn implemented in
 *  QnByArchitectTest.acceptsIfMergeArchitectPull. After resolving this
 *  issue, uncomment the test.
 */
@Immutable
@ToString
@EqualsAndHashCode(of = { "profile", "xpath", "origin" })
public final class QnByArchitect implements Question {

    /**
     * Message bundle.
     */
    private static final ResourceBundle PHRASES =
        ResourceBundle.getBundle("phrases");

    /**
     * Profile.
     */
    private final transient Profile profile;

    /**
     * XPath.
     */
    private final transient String xpath;

    /**
     * Original question.
     */
    private final transient Question origin;

    /**
     * Ctor.
     * @param prof Profile
     * @param path XPath in profile with a list of logins
     * @param qtn Original question
     */
    public QnByArchitect(final Profile prof, final String path,
        final Question qtn) {
        this.profile = prof;
        this.xpath = path;
        this.origin = qtn;
    }

    @Override
    public Req understand(final Comment.Smart comment,
        final URI home) throws IOException {
        final Req req;
        final List<String> logins = new ListOf<>(
            new Mapped<>(
                input -> input.toLowerCase(Locale.ENGLISH),
                this.profile.read().xpath(this.xpath)
            )
        );
        final String author = comment.author()
            .login()
            .toLowerCase(Locale.ENGLISH);
        if (logins.contains(author)) {
            req = this.origin.understand(comment, home);
        } else if (logins.isEmpty()) {
            if (QnByArchitect.allowed(comment.issue().repo(), author)) {
                req = this.origin.understand(comment, home);
            } else {
                new Answer(comment).post(
                    true,
                    QnByArchitect.PHRASES.getString(
                        "QnByArchitect.read-only"
                    )
                );
                req = Req.DONE;
            }
        } else {
            new Answer(comment).post(
                true,
                String.format(
                    QnByArchitect.PHRASES.getString("QnByArchitect.denied"),
                    logins.get(0).toLowerCase(Locale.ENGLISH)
                )
            );
            req = Req.DONE;
        }
        return req;
    }

    /**
     * This repository allows this author to write into it.
     * @param repo The repo
     * @param author The author
     * @return TRUE if write access allowed
     * @throws IOException If fails
     */
    private static boolean allowed(final Repo repo,
        final String author) throws IOException {
        String perm;
        try {
            perm = repo.collaborators().permission(author);
        } catch (final AssertionError ex) {
            perm = "forbidden";
        }
        return "write".equals(perm) || "admin".equals(perm);
    }

}
