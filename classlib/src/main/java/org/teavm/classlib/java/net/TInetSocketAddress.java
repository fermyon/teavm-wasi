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

public class TInetSocketAddress extends TSocketAddress {
    private final String host;
    private final TInetAddress address;
    private final int port;

    public TInetSocketAddress(String host, int port) {
        TInetAddress address;
        try {
            address = TInetAddress.getByName(host);
            host = address.getHostName();
        } catch (TUnknownHostException e) {
            address = null;
        }
        this.host = host;
        this.address = address;
        this.port = port;
    }

    public TInetSocketAddress(TInetAddress address, int port) {
        this.host = address.getHostName();
        this.address = address;
        this.port = port;
    }

    public TInetAddress getAddress() {
        return address;
    }

    public String getHostName() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean equals(Object o) {
        if (o instanceof TInetSocketAddress) {
            TInetSocketAddress a = (TInetSocketAddress) o;
            return a.address.equals(address) && a.port == port;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return port ^ address.hashCode();
    }

    public String toString() {
        return getHostName() + ":" + port;
    }
}
