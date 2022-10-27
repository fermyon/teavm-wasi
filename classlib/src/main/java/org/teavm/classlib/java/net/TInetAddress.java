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

import java.io.IOException;
import org.teavm.classlib.java.io.TIOException;

public class TInetAddress {
    private final String name;
    private final int ip;

    private TInetAddress(String name) throws TUnknownHostException {
        this.name = name;
        this.ip = ipv4AddressForName(name);
    }

    public String getHostName() {
        return name;
    }

    public String getHostAddress() {
        try {
            return new TInetAddress(name).toString();
        } catch (TUnknownHostException e) {
            return null;    // Strange case
        }
    }

    public static TInetAddress getByName(String name) throws TUnknownHostException {
        try {
            TSocket.init();
            return new TInetAddress(name);
        } catch (TIOException | IOException e) {
            TUnknownHostException uhe = new TUnknownHostException(name);
            uhe.initCause(e);
            throw uhe;
        }
    }

    public byte[] getAddress() {
        byte[] res = new byte[4];
        res[0] = (byte) (ip >>> 24);
        res[1] = (byte) ((ip >>> 16) & 0xFF);
        res[2] = (byte) ((ip >>> 8) & 0xFF);
        res[3] = (byte) (ip & 0xFF);
        return res;
    }

    @Override
    public String toString() {
        byte[] addr = getAddress();
        return (int) ((addr[0] + 256) % 256) + "."
                + (int) ((addr[1] + 256) % 256) + "."
                + (int) ((addr[2] + 256) % 256) + "."
                + (int) ((addr[3] + 256) % 256);
    }

    public int getRawAddress() {
        return ip;
    }

    static native int ipv4AddressForName(String name) throws TUnknownHostException;

    public boolean equals(Object o) {
        return o instanceof TInetAddress && ((TInetAddress) o).ip == ip;
    }

    public int hashCode() {
        return ip;
    }
}
