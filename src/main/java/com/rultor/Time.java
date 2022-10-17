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
package com.rultor;

import com.jcabi.aspects.Immutable;
import java.text.ParseException;
import java.util.Date;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.time.DateFormatUtils;

/**
 * Date and time in ISO 8601.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 1.8.12
 */
@Immutable
@ToString
@EqualsAndHashCode(of = "millis")
public final class Time {

    /**
     * The time.
     */
    private final transient long millis;

    /**
     * Ctor.
     */
    public Time() {
        this(System.currentTimeMillis());
    }

    /**
     * Ctor.
     * @param date Date
     */
    public Time(final Date date) {
        this(date.getTime());
    }

    /**
     * Ctor.
     * @param msec Milliseconds
     */
    public Time(final long msec) {
        this.millis = msec;
    }

    /**
     * Ctor.
     * @param date Date
     */
    public Time(final String date) {
        this(Time.parse(date));
    }

    /**
     * Make ISO string.
     * @return Text
     */
    public String iso() {
        return DateFormatUtils.formatUTC(
            new Date(this.millis),
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        );
    }

    /**
     * Make date.
     * @return Date
     */
    public long msec() {
        return this.millis;
    }

    /**
     * Parse text.
     * @param date Date
     * @return Date
     */
    private static Date parse(final String date) {
        try {
            return DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT
                .parse(date);
        } catch (final ParseException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
