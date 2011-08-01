/* Copyright 2011 Paremus Limited
 * 
 * Licensed under the Apache License, Version 2.0 (the License)
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License. 
 * see http://www.apache.org/licenses/LICENSE-2.0 
 */

package com.example.gateway.persistence;

import com.example.gateway.GatewayListener;
import com.example.gateway.messages.Message;

import java.io.*;
import java.util.concurrent.atomic.AtomicLong;

public class FilePersistenceListener implements GatewayListener {
    private File dir;

    private final AtomicLong seq = new AtomicLong();

    private boolean initialized;
    private boolean failed;

    public void setDir(File dir) {
        this.dir = dir;
    }

    public synchronized void receive(Message message) {
        if (failed) return;

        if (!initialized) {
            failed = !dir.exists() && !dir.mkdirs();
            initialized = true;
        }

        File f = new File(dir, "message-" + seq.incrementAndGet());
        ObjectOutputStream oos = null;

        try {
            FileOutputStream out = new FileOutputStream(f);
            oos = new ObjectOutputStream(out);
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            failed = true;
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
    }
}
