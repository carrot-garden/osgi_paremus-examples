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

import com.example.gateway.GatewayClient
import scala.collection.mutable.HashMap
import java.{ util => ju }
import collection.JavaConversions._
import collection.JavaConversions
import com.example.gateway.messages.Connection
import com.example.gateway.messages.Message
import scala.actors.Actor
import scala.actors.Actor._
import java.util.ArrayList

import aQute.bnd.annotation.component.Reference


trait ClientSupport extends ListenerSupport {
  private val connections = new HashMap[String, ConnectionState]()
  private val tokensToConnections = new HashMap[String, ConnectionState]()

  private val clients = new HashMap[String, GatewayClient]

  @Reference(`type`='*')
  def addClient(client: GatewayClient, attributes: ju.Map[String, Any]) {
    updateState(attributes.toMap) {
      state =>
        {
          clients += state.id -> client
          new ConnectionState(state.id, state.token, state.blocked, Some(client))
        }
    }
  }

  def removeClient(client: GatewayClient, attributes: ju.Map[String, Any]) {
    updateState(attributes.toMap) {
      state =>
        {
          clients -= state.id
          new ConnectionState(state.id, state.token, state.blocked, None)
        }
    }
  }

  def getBlocked = {
    val blocked: Set[String] = connections.synchronized {
      connections.withFilter(_._2.blocked).map(_._1).toSet
    }
    JavaConversions.asJavaSet(blocked)
  }

  def getConnected = {
    JavaConversions.asJavaSet(connections.synchronized {
      connections.keySet.toSet
    })
  }

  def connect(id: String) = {
    def doConnect = {
      val t = ju.UUID.randomUUID.toString
      val state = new ConnectionState(id, Some(t), false, clients.get(id))
      connections += id -> state
      tokensToConnections += t -> state
      t
    }
    connections.synchronized {
      connections.get(id) match {
        case Some(state) => {
          state.token match {
            case Some(t) => t
            case None => doConnect
          }
        }
        case None => doConnect
      }
    }
  }

  def disconnect(loginToken: Object) {
    loginToken match {
      case t: String => {
        connections.synchronized {
          tokensToConnections.remove(t) match {
            case Some(state) => {
              val newState = new ConnectionState(state.id, None, state.blocked, clients.get(state.id))
              connections += state.id -> state
            }
            case None => // TODO log unknown connection
          }
        }
      }
      case _ => // TODO log invalid token
    }
  }

  def setBlock(id: String, blocked: Boolean) {
    connections.synchronized {
      connections.get(id) match {
        case Some(state) => {
          connections += id -> new ConnectionState(state.id, state.token, blocked, state.client)
        }
        case None => {
          throw new IllegalStateException("Not connected " + id)
        }
      }
    }
  }

  private def updateState(attributes: Map[String, Any])(f: ConnectionState => ConnectionState) {
    attributes.get(GatewayClient.ID) match {
      case Some(id: String) => {
        val s = connections.synchronized {
          val newState = connections.get(id) match {
            case Some(state) => {
              f(state)
            }
            case None => {
              val nullState = new ConnectionState(id, None, false, None)
              f(nullState)
            }
          }
          connections += id -> newState
          newState.token match {
            case Some(t) => tokensToConnections += t -> newState
            case None => // TODO log not connected client id
          }

          newState
        }

        s.worker match {
          case Some(_) => sendToClient(s)(List(new Connection(s.id)))
          case None => // cannot send a message to an unconnected client
        }        
      }
      case Some(other) => {
        // TODO log invalid client id
        None
      }
      case None => {
        // TODO log unknown client id
        None
      }
    }
  }

  protected def getConnectionByToken(loginToken: AnyRef): Option[ConnectionState] = {
    loginToken match {
      case str: String => {
        connections.synchronized {
          tokensToConnections.get(str)
        }
      }
      case _ => None
    }
  }

  protected def getConnectionByID(clientID: String): Option[ConnectionState] = {
    connections.synchronized {
      connections.get(clientID)
    }
  }

  protected def sendToClient(state: ConnectionState)(messages: => TraversableOnce[Message]) {
    state.worker match {
      case Some(worker) => {
        notifyListeners(messages)

        worker ! {
          messages
        }
      }
      case None => throw new IllegalStateException("No client connected for " + state.id)
    }
  }
}

private [impl] case class ConnectionState(id: String, token: Option[String], blocked: Boolean, client: Option[GatewayClient]) {
  val worker: Option[Actor] = client match {
    case Some(c) => {
      Some(actor {
        loop {
          receive {
            case msgs: Iterable[Message] => {
              val send = new ArrayList[Message](msgs)
              if (send.size > 0) {
                try {
                  // TODO this makes remote method call in actor loop - bad... use threadpool instead
                  c.receive(send)
                } catch {
                  // TODO better logging
                  case e: Throwable => e.printStackTrace()
                }
              }
            }
            case other => println("Discarded " + other)
          }
        }
      })
    }
    case None => None
  }
}