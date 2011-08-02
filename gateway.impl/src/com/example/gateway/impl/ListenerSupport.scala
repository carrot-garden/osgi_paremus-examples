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

package com.example.gateway.impl

import actors.Actor._
import collection.mutable.{HashMap, HashSet}
import com.example.gateway.GatewayListener
import com.example.gateway.messages.Message
import actors.Actor
import aQute.bnd.annotation.component.Reference

trait ListenerSupport {
  private val listeners = new HashMap[GatewayListener, AsyncGatewayListener]

  @Reference(`type`='*')
  def addListener(listener: GatewayListener) {
    listeners.synchronized {
      val async = new AsyncGatewayListener(listener)
      async.start

      listeners += listener -> async
    }
  }

  def removeListener(listener: GatewayListener) {
    listeners.synchronized {
      listeners -= listener
    }
  }

  protected def notifyListeners(msgs: Traversable[Message]) {
    listeners.synchronized {
      listeners.values.foreach(_ ! msgs)
    }
  }

  protected def notifyListeners(msg: Message) {
    listeners.synchronized {
      listeners.values.foreach(_ ! msg)
    }
  }

  class AsyncGatewayListener(underlying: GatewayListener) extends Actor {
    def act() {
      loop {
        receive {
          // TODO this makes remote call in actor thread - bad...refactor to use ThreadPool
          case msgs: Iterable[Message] => msgs.foreach(underlying.receive(_))
          case msg: Message => underlying.receive(msg)
        }
      }
    }
  }
}