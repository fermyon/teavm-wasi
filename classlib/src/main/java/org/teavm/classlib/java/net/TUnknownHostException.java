/*
 *  Copyright 2022 TeaVM Contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/*
 *  Copyright 2008-2015 Avian Contributors.
 *
 *  Permission to use, copy, modify, and/or distribute this software
 *  for any purpose with or without fee is hereby granted, provided
 *  that the above copyright notice and this permission notice appear
 *  in all copies.
 *
 *  There is NO WARRANTY for this software.  See license-avian.txt for
 *  details.
 */
package org.teavm.classlib.java.net;

import org.teavm.classlib.java.io.TIOException;

public class TUnknownHostException extends TIOException {
    public TUnknownHostException(String host) {
        super(host);
    }

    public TUnknownHostException() {
    }
}
