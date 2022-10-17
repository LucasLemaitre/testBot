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
package com.rultor.agents.daemons;

import com.jcabi.aspects.Immutable;
import com.jcabi.aspects.RetryOnFailure;
import com.jcabi.aspects.Timeable;
import com.jcabi.immutable.Array;
import com.jcabi.log.Logger;
import com.jcabi.manifests.Manifests;
import com.jcabi.ssh.Shell;
import com.jcabi.ssh.Ssh;
import com.jcabi.xml.XML;
import com.rultor.Time;
import com.rultor.agents.Required;
import com.rultor.agents.shells.TalkShells;
import com.rultor.profiles.ProfileDeprecations;
import com.rultor.spi.Agent;
import com.rultor.spi.Profile;
import com.rultor.spi.Talk;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.xembly.Directive;
import org.xembly.Directives;

/**
 * Starts daemon.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@Immutable
@ToString
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("PMD.ExcessiveImports")
public final class StartsDaemon implements Agent {

    /**
     * Paths to match.
     */
    private static final Array<String> PATHS = new Array<>(
        "/talk/shell[host and port and login and key]",
        "/talk/daemon[script and dir and not(started) and not(ended)]",
        "/talk/daemon[dir != '']"
    );

    /**
     * Profile to get assets from.
     */
    private final transient Profile profile;

    /**
     * Ctor.
     * @param prof Profile
     */
    public StartsDaemon(final Profile prof) {
        this.profile = prof;
    }

    @Override
    public void execute(final Talk talk) throws IOException {
        if (new Required(StartsDaemon.PATHS).isIt(talk)) {
            talk.modify(
                new Directives()
                    .xpath("/talk/daemon[not(started)]")
                    .strict(1)
                    .add("started").set(new Time().iso())
            );
            talk.modify(this.process(talk.read()));
        }
    }

    /**
     * Process talk.
     *
     * The annotation here is a TEMPORARY solution. It will be removed in the
     * future. We need it because the SSH shell is not dropping the connection
     * when the command is in the background.
     *
     * @param xml The XML to process.
     * @return List of directives
     */
    @Timeable(limit = 1, unit = TimeUnit.MINUTES)
    public Iterable<Directive> process(final XML xml) {
        final Directives dirs = new Directives()
            .xpath("/talk/daemon[not(ended)]")
            .strict(1);
        try {
            this.run(xml);
        } catch (final IOException ex) {
            dirs.add("ended").set(new Time().iso()).up()
                .add("code").set("128").up()
                .add("tail").set(ex.getLocalizedMessage());
            Logger.warn(this, "%[exception]s", ex);
        }
        return dirs;
    }

    /**
     * Run daemon.
     * @param xml XML with talk
     * @return Directory where it started
     * @throws IOException If fails
     */
    @RetryOnFailure
    public String run(final XML xml) throws IOException {
        final Shell shell = new TalkShells(xml).get();
        new ProfileDeprecations(this.profile).print(shell);
        final String dir = xml.xpath("/talk/daemon/dir/text()").get(0);
        final XML daemon = xml.nodes("/talk/daemon").get(0);
        final String script = String.join(
            "\n",
            "#!/bin/bash",
            "set -x",
            "set -e",
            "set -o pipefail",
            "cd $(dirname $0)",
            "echo $$ > pid",
            String.format(
                "echo %s",
                Ssh.escape(
                    String.format(
                        "%s %s",
                        Manifests.read("Rultor-Version"),
                        Manifests.read("Rultor-Revision")
                    )
                )
            ),
            "date",
            "uptime",
            this.upload(shell, dir),
            daemon.xpath("script/text()").get(0)
        );
        new Shell.Safe(shell).exec(
            String.format("cd %s; cat > run.sh", Ssh.escape(dir)),
            IOUtils.toInputStream(script, StandardCharsets.UTF_8),
            Logger.stream(Level.INFO, this),
            Logger.stream(Level.WARNING, this)
        );
        new Shell.Empty(new Shell.Safe(shell)).exec(
            String.join(
                " && ",
                String.format("cd %s", Ssh.escape(dir)),
                "chmod a+x run.sh",
                "echo 'run.sh failed to start' > stdout",
                // @checkstyle LineLength (1 line)
                "( ( nohup ./run.sh </dev/null >stdout 2>&1; echo $? >status ) </dev/null >/dev/null 2>&1 & )"
            )
        );
        Logger.info(this, "Daemon started at %s", dir);
        return dir;
    }

    /**
     * Upload assets.
     * @param shell Shell
     * @param dir Directory
     * @return Script to use
     * @throws IOException If fails
     */
    private String upload(final Shell shell, final String dir)
        throws IOException {
        final long start = System.currentTimeMillis();
        String script = "";
        try {
            for (final Map.Entry<String, InputStream> asset
                : this.profile.assets().entrySet()) {
                shell.exec(
                    String.format(
                        "cat > %s",
                        Ssh.escape(String.format("%s/%s", dir, asset.getKey()))
                    ),
                    asset.getValue(),
                    Logger.stream(Level.INFO, true),
                    Logger.stream(Level.WARNING, true)
                );
                Logger.info(
                    this, "\"%s\" uploaded into %s in %[ms]s",
                    asset.getKey(), dir,
                    System.currentTimeMillis() - start
                );
            }
            this.gpg(shell, dir);
        } catch (final Profile.ConfigException ex) {
            script = Logger.format(
                "cat << EOT\n%s\nEOT\nexit -1",
                ex.getLocalizedMessage()
            );
        }
        return script;
    }

    /**
     * Upload GPG keys.
     * @param shell Shell
     * @param dir Dir
     * @throws IOException If fails
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private void gpg(final Shell shell, final String dir) throws IOException {
        final Collection<XML> entries = this.profile.read().nodes(
            "/p/entry[@key='decrypt']/entry"
        );
        if (!entries.isEmpty()) {
            final String[] names = {"pubring.gpg", "secring.gpg"};
            for (final String name : names) {
                shell.exec(
                    String.join(
                        " &&  ",
                        String.format("cd %s  ", Ssh.escape(dir)),
                        "mkdir -p .gpg",
                        String.format("cat > \".gpg/%s\"", name)
                    ),
                    this.ring(name),
                    Logger.stream(Level.INFO, true),
                    Logger.stream(Level.WARNING, true)
                );
            }
            Logger.info(this, "GPG keys uploaded to %s", dir);
        }
    }

    /**
     * Get contents of ring.
     * @param name Name
     * @return Content
     * @throws IOException If fails
     */
    private InputStream ring(final String name) throws IOException {
        return new ByteArrayInputStream(
            Base64.decodeBase64(
                IOUtils.toByteArray(
                    this.getClass().getResourceAsStream(
                        String.format("%s.base64", name)
                    )
                )
            )
        );
    }

}
