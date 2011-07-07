/* 
 *  This file is part of the Torrefactor project
 *  Copyright 2011 Guillaume Martres <smarter@ubuntu.com>
 *  Copyright 2011 Florian Vessaz <florian.vessaz@gmail.com> 
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *      1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *      2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */

package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * This class represent a tracker.
 */
abstract class Tracker {
    public String uri;
    public String statusMessage;
    public enum Event { none, started, stopped, completed };

    protected long lastActive = 0;
    protected int interval = 1200;   // 20 min
    protected int port;
    protected int seeders = -1;
    protected int leechers = -1;
    protected int uniqKey;

    
    public Tracker () {}

    /**
     * Create a new tracker for the given uri
     *
     * @param _uri    the uri of the tracker
     */
    public Tracker (String _uri) {}

    /**
     * Create a new tracker for the given uri and use a uniqKey
     *
     * @param _uri        the uri of the tracker
     * @param uniqKey    our own uniqKey
     */
    public Tracker (String _uri, int uniqKey) {}

    /**
     * Announce an event to the tracker.
     *
     * @param torrent    the torrent concerned by the event
     * @param event        the event to announce
     */
    abstract ArrayList<Pair<byte[], Integer>>
        announce (Torrent torrent, Event event)
        throws IOException, InvalidBDecodeException;

    /**
     * Return the inverval at which we should announce.
     */
    public int getInverval () {
        return this.interval;
    }

    /**
     * Return the time at which the last announce was made.
     */
    public long getLastActive () {
        return this.lastActive;
    }

    /**
     * Return the time at which we should do the next announce.
     */
    public long getNextAnnounceTime () {
        return this.lastActive + (((long) this.interval) * 1000);
    }

    /**
     * Return true if we are allowed to announce again.
     */
    public boolean canAnnounce () {
        if (System.currentTimeMillis() < this.lastActive
                                         + (((long) this.interval) * 1000)) {
            return false;
        }
        return true;
    }

    protected void updateActive () {
        this.lastActive = System.currentTimeMillis();
    }

}
