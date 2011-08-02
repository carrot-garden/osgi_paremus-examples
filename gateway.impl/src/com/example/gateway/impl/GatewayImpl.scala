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

import com.example.gateway._
import messages._

import collection.JavaConversions._
import java.{util => ju}
import aQute.bnd.annotation.component.Activate
import aQute.bnd.annotation.component.Component
import aQute.bnd.annotation.metatype.Meta.OCD

@OCD(factory=true)    
@Component(
    immediate=true,
    provide=Array(
        classOf[Gateway], 
        classOf[GatewaySPI], 
        classOf[PricingEngineClient]
        ),
    properties=Array("service.exported.interfaces=*"),
    designate=classOf[GatewayConfig]
    )
class GatewayImpl extends AnyRef 
	with GatewaySPI 
	with PricingEngineClient 
	with ListenerSupport 
	with PricerSupport 
	with ClientSupport {
  
  private var myID: Option[String] = None

  @Activate
  def activate(properties: ju.Map[String, Any]) {
    myID = properties.toMap.get(PricingEngineClient.ID) match {
      case Some(str: String) => Some(str)
      case Some(other) => throw new IllegalArgumentException("Invalid id type")
      case None => throw new IllegalStateException("No id specified")
    }
  }

  protected def gatewayID = {
    myID.getOrElse(throw new IllegalStateException("Component not activated correctly"))
  }

  def request(loginToken: AnyRef, quotes: ju.Collection[QuoteRequest]) = {
    notifyListeners(quotes)
    
    getConnectionByToken(loginToken) match {
      case Some(state) => {
        if (state.blocked) {
          sendToClient(state) {
            for (c <- quotes) yield new QuoteRequestReject(c.getId, "blocked")
          }
        }
        else {          
          for (quote <- quotes) {
            if (!submitPrice(state.id, quote)) {
              sendToClient(state) {
                new QuoteRequestReject(quote.getId, "pricing engine unavailable") :: Nil
              }
            }
          }
        }
      }
      case None => {
        throw new IllegalStateException("Unknown loginToken " + loginToken)
      }
    }
  }

  def receivePrice(clientID: String, quote: Quote) = {
    getConnectionByID(clientID) match {
      case Some(state) => {
        sendToClient(state) {
          quote :: Nil
        }
      }
      case None => // TODO persist etc..
    }
  }
}